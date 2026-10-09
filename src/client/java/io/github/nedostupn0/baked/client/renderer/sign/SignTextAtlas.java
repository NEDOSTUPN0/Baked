package io.github.nedostupn0.baked.client.renderer.sign;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * Render targets with pre-rendered sign text, drawn by the vanilla text renderer once per change. Tiles span {@link
 * #TILE_WIDTH}x{@link #TILE_HEIGHT} text units at {@link #SCALE} texels per unit, so centred lines keep their half-unit
 * offsets.
 */
public final class SignTextAtlas {
    public static final int TILE_WIDTH = 96;
    public static final int TILE_HEIGHT = 48;
    private static final int SCALE = 2;
    private static final int PAGE_SIZE = 2048;
    private static final int COLUMNS = PAGE_SIZE / (TILE_WIDTH * SCALE);
    private static final int ROWS = PAGE_SIZE / (TILE_HEIGHT * SCALE);
    private static final int TILES_PER_PAGE = COLUMNS * ROWS;
    private static final int FULL_BRIGHT = 15728880;
    private static final Vector4fc TRANSPARENT = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);

    private static final List<Page> pages = new ArrayList<>();
    private static final Projection projection = new Projection();
    private static ProjectionMatrixBuffer projectionBuffer;

    private SignTextAtlas() {}

    /** One line as {@link net.minecraft.client.renderer.blockentity.AbstractSignRenderer#submitSignText} submits it. */
    public record Line(FormattedCharSequence text, float x, float y, int color, int outlineColor) {}

    public static final class Tile {
        final Page page;
        final int index;
        List<Line> lines;

        Tile(Page page, int index, List<Line> lines) {
            this.page = page;
            this.index = index;
            this.lines = lines;
        }

        public Identifier texture() {
            return page.location;
        }

        public int pageIndex() {
            return page.index;
        }

        /** Left edge in 0..1 page space. */
        public float u0() {
            return (float) (index % COLUMNS) * TILE_WIDTH * SCALE / PAGE_SIZE;
        }

        /** Top edge (text-space y = -TILE_HEIGHT/2) in 0..1 page space, as rendered. */
        public float vTop() {
            return (float) (index / COLUMNS) * TILE_HEIGHT * SCALE / PAGE_SIZE;
        }

        public float uSize() {
            return (float) TILE_WIDTH * SCALE / PAGE_SIZE;
        }

        public float vSize() {
            return (float) TILE_HEIGHT * SCALE / PAGE_SIZE;
        }
    }

    private static final class Page extends AbstractTexture {
        final int index;
        final Identifier location;
        final BitSet used = new BitSet(TILES_PER_PAGE);
        final List<Tile> tiles = new ArrayList<>();
        final GpuTexture depthTexture;
        final GpuTextureView depthView;
        boolean dirty;

        Page(int index) {
            this.index = index;
            this.location = Identifier.fromNamespaceAndPath("baked", "sign_text/" + index);
            GpuDevice device = RenderSystem.getDevice();
            // Same usage flags as the GUI picture-in-picture targets: sampled, copy destination, render attachment.
            this.texture = device.createTexture(() -> "baked sign text " + index, 13, GpuFormat.RGBA8_UNORM, PAGE_SIZE, PAGE_SIZE, 1, 1);
            this.textureView = device.createTextureView(this.texture);
            this.depthTexture = device.createTexture(() -> "baked sign text depth " + index, 9, GpuFormat.D32_FLOAT, PAGE_SIZE, PAGE_SIZE, 1, 1);
            this.depthView = device.createTextureView(this.depthTexture);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            Minecraft.getInstance().getTextureManager().register(location, this);
        }

        @Override
        public void close() {
            super.close();
            depthView.close();
            depthTexture.close();
        }
    }

    public static Tile allocate(List<Line> lines) {
        for (Page page : pages) {
            int index = page.used.nextClearBit(0);
            if (index < TILES_PER_PAGE) return claim(page, index, lines);
        }
        Page page = new Page(pages.size());
        pages.add(page);
        return claim(page, 0, lines);
    }

    private static Tile claim(Page page, int index, List<Line> lines) {
        page.used.set(index);
        Tile tile = new Tile(page, index, lines);
        page.tiles.add(tile);
        page.dirty = true;
        return tile;
    }

    public static void update(Tile tile, List<Line> lines) {
        tile.lines = lines;
        tile.page.dirty = true;
    }

    public static void release(Tile tile) {
        tile.page.used.clear(tile.index);
        tile.page.tiles.remove(tile);
        tile.page.dirty = true;
    }

    public static void reset() {
        for (Page page : pages) Minecraft.getInstance().getTextureManager().release(page.location);
        pages.clear();
    }

    /**
     * Re-renders pages with changed tiles. Render thread, outside level rendering: it swaps the projection and shares
     * the feature dispatcher.
     */
    public static void renderDirtyPages() {
        boolean any = false;
        for (Page page : pages) any |= page.dirty;
        if (!any) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        FeatureRenderDispatcher dispatcher = minecraft.gameRenderer.featureRenderDispatcher();
        if (projectionBuffer == null) projectionBuffer = new ProjectionMatrixBuffer("baked sign text");

        RenderSystem.backupProjectionMatrix();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        try {
            projection.setupOrtho(-1000.0F, 1000.0F, PAGE_SIZE, PAGE_SIZE, true);
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC);

            for (Page page : pages) {
                if (!page.dirty) continue;
                page.dirty = false;
                renderPage(page, dispatcher);
            }
        } finally {
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void renderPage(Page page, FeatureRenderDispatcher dispatcher) {
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(page.getTexture(), TRANSPARENT, page.depthTexture, 0.0);
        if (page.tiles.isEmpty()) return;

        SubmitNodeStorage storage = new SubmitNodeStorage();
        PoseStack poseStack = new PoseStack();
        for (Tile tile : page.tiles) {
            poseStack.pushPose();
            poseStack.translate((tile.index % COLUMNS) * TILE_WIDTH * SCALE, (tile.index / COLUMNS) * TILE_HEIGHT * SCALE, 0.0F);
            poseStack.scale(SCALE, SCALE, 1.0F);
            poseStack.translate(TILE_WIDTH / 2.0F, TILE_HEIGHT / 2.0F, 0.0F);
            for (Line line : tile.lines) {
                storage.submitText(poseStack, line.x(), line.y(), line.text(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT, line.color(), 0, line.outlineColor());
            }
            poseStack.popPose();
        }

        try (
            FeatureRenderDispatcher.PreparedFrame frame = dispatcher.prepareFrame(storage);
            RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "baked sign text", page.getTextureView(), Optional.empty(), page.depthView, OptionalDouble.empty())
        ) {
            RenderSystem.bindDefaultUniforms(renderPass);
            FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
        }
    }
}

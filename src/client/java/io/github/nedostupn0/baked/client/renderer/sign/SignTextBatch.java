package io.github.nedostupn0.baked.client.renderer.sign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.Vec3;

/**
 * Draws sign text from {@link SignTextAtlas} tiles: one quad per side instead of per glyph, no per-frame layout. The
 * board stays in the section mesh.
 */
public final class SignTextBatch {
    private static final int FULL_BRIGHT = 15728880;
    private static final int LIGHT_REFRESH_TICKS = 10;
    private static final float HALF_W = SignTextAtlas.TILE_WIDTH / 2.0F;
    private static final float HALF_H = SignTextAtlas.TILE_HEIGHT / 2.0F;

    private static final Map<SignBlockEntity, Entry> entries = new IdentityHashMap<>();
    private static final Set<SignBlockEntity> pending = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Every loaded client sign, whatever the settings, so they can all be rebuilt when an option changes. */
    private static final Set<SignBlockEntity> known = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Signs that must stay on the vanilla renderer (obfuscated text), so the recheck leaves them alone. */
    private static final Set<SignBlockEntity> rejected = Collections.newSetFromMap(new IdentityHashMap<>());
    private static int lightRefreshCountdown;
    private static int recheckCountdown;
    private static int healed;
    /** Sides grouped by atlas page, rebuilt only when signs change instead of every frame. */
    private static Int2ObjectMap<List<Side>> sidesByPage = new Int2ObjectOpenHashMap<>();
    private static boolean groupsDirty;

    private SignTextBatch() {}

    private static final class Side {
        Entry entry;
        final SignTextAtlas.Tile tile;
        final boolean glowing;
        final double[] corners = new double[12];
        final float[] us = new float[4];
        final float[] vs = new float[4];

        Side(SignTextAtlas.Tile tile, boolean glowing, BlockPos pos, Matrix4fc textToBlock) {
            this.tile = tile;
            this.glowing = glowing;
            // Same corner order as a font glyph quad: top-left, bottom-left, bottom-right, top-right (text space is y-down).
            float[][] text = {{-HALF_W, -HALF_H}, {-HALF_W, HALF_H}, {HALF_W, HALF_H}, {HALF_W, -HALF_H}};
            Vector3f p = new Vector3f();
            for (int i = 0; i < 4; i++) {
                textToBlock.transformPosition(text[i][0], text[i][1], 0.0F, p);
                corners[i * 3] = pos.getX() + p.x();
                corners[i * 3 + 1] = pos.getY() + p.y();
                corners[i * 3 + 2] = pos.getZ() + p.z();
                us[i] = tile.u0() + (text[i][0] + HALF_W) / SignTextAtlas.TILE_WIDTH * tile.uSize();
                // Render targets come out bottom-up, like the GUI picture-in-picture blit expects.
                vs[i] = 1.0F - (tile.vTop() + (text[i][1] + HALF_H) / SignTextAtlas.TILE_HEIGHT * tile.vSize());
            }
        }
    }

    private static final class Entry {
        final SignBlockEntity sign;
        final List<Side> sides;
        int light;

        Entry(SignBlockEntity sign, List<Side> sides) {
            this.sign = sign;
            this.sides = sides;
            for (Side side : sides) side.entry = this;
        }

        void refreshLight(ClientLevel level) {
            light = LightCoordsUtil.getLightCoords(level, sign.getBlockPos());
        }

        void release() {
            for (Side side : sides) SignTextAtlas.release(side.tile);
        }
    }

    public static boolean isEnabled() {
        return SettingsManager.MOD_TOGGLE.getValue() && SettingsManager.OPTIMISED_SIGNS.getValue() && SettingsManager.ATLAS_SIGN_TEXT.getValue();
    }

    /** True when this sign's text is drawn here, so the vanilla renderer must not draw it. */
    public static boolean isBatched(@Nullable Object blockEntity) {
        return blockEntity instanceof SignBlockEntity sign && entries.containsKey(sign) && isEnabled();
    }

    public static void markDirty(SignBlockEntity sign) {
        known.add(sign);
        pending.add(sign);
    }

    public static void remove(SignBlockEntity sign) {
        known.remove(sign);
        rejected.remove(sign);
        pending.remove(sign);
        Entry entry = entries.remove(sign);
        if (entry != null) {
            entry.release();
            groupsDirty = true;
        }
    }

    /** Rebuilds every sign's text, e.g. after the font or the outline option changed. */
    public static void markAllDirty() {
        pending.addAll(known);
    }

    public static void clear() {
        known.clear();
        rejected.clear();
        pending.clear();
        entries.clear();
        groupsDirty = true;
        SignTextAtlas.reset();
    }

    public static int size() {
        return entries.size();
    }

    public static String stats() {
        return "signs: known=" + known.size() + " batched=" + entries.size() + " pending=" + pending.size() + " rejected=" + rejected.size()
            + " healed=" + healed + " enabled=" + isEnabled();
    }

    private static final Map<String, int[]> UPDATE_OUTCOMES = new java.util.TreeMap<>();

    /** Per sign block class: loaded, batched, and why the others are not (debug only). */
    public static List<String> breakdown() {
        Map<String, int[]> byKind = new java.util.TreeMap<>();
        for (SignBlockEntity sign : known) {
            int[] counts = byKind.computeIfAbsent(sign.getBlockState().getBlock().getClass().getSimpleName(), k -> new int[5]);
            counts[0]++;
            if (entries.containsKey(sign)) counts[1]++;
            else if (sign.isRemoved() || sign.getLevel() == null) counts[2]++;
            else {
                List<Side> sides = buildSides(sign);
                if (sides == null) counts[3]++;
                else {
                    if (sides.isEmpty()) counts[4]++;
                    releaseAll(sides);
                }
            }
        }
        List<String> out = new ArrayList<>();
        byKind.forEach((kind, c) -> out.add(kind + ": known=" + c[0] + " batched=" + c[1] + " removed=" + c[2] + " rejected=" + c[3] + " noText=" + c[4]));
        UPDATE_OUTCOMES.forEach((kind, c) -> out.add(kind + " updates: total=" + c[0] + " gone=" + c[1] + " rejected=" + c[2] + " empty=" + c[3] + " built=" + c[4]));
        return out;
    }

    /** Main/render thread, once per client tick. */
    public static void tick(ClientLevel level) {
        if (!pending.isEmpty()) {
            SignBlockEntity[] signs = pending.toArray(SignBlockEntity[]::new);
            pending.clear();
            for (SignBlockEntity sign : signs) update(sign, level);
        }
        if (SettingsManager.SIGN_TEXT_RECHECK.getValue() && --recheckCountdown <= 0) {
            recheckCountdown = RECHECK_TICKS;
            recheck();
        }
        if (--lightRefreshCountdown <= 0) {
            lightRefreshCountdown = LIGHT_REFRESH_TICKS;
            for (Entry entry : entries.values()) entry.refreshLight(level);
        }
        SignTextAtlas.renderDirtyPages();
    }

    private static final int RECHECK_TICKS = 40;

    /**
     * Safety net: rebuilds signs with text that are neither batched, pending nor rejected, in case a mod hid it from
     * the build. {@link #healed} counts these.
     */
    private static void recheck() {
        boolean filter = Minecraft.getInstance().isTextFilteringEnabled();
        for (SignBlockEntity sign : known) {
            if (entries.containsKey(sign) || pending.contains(sign) || rejected.contains(sign)) continue;
            if (!sign.getText(SignTextSlot.FRONT).hasMessage(filter) && !sign.getText(SignTextSlot.BACK).hasMessage(filter)) continue;
            pending.add(sign);
            healed++;
        }
    }

    private static void update(SignBlockEntity sign, ClientLevel level) {
        rejected.remove(sign);
        Entry old = entries.remove(sign);
        if (old != null) old.release();
        groupsDirty = true;
        // Built regardless of the settings: they only decide whether the atlas or vanilla draws the text.
        int[] outcome = UPDATE_OUTCOMES.computeIfAbsent(sign.getBlockState().getBlock().getClass().getSimpleName(), k -> new int[5]);
        outcome[0]++;
        if (sign.isRemoved() || sign.getLevel() != level) {
            outcome[1]++;
            known.remove(sign);
            return;
        }

        List<Side> sides = buildSides(sign);
        if (sides == null) {
            outcome[2]++;
            rejected.add(sign);
        }
        else if (sides.isEmpty()) outcome[3]++;
        if (sides == null || sides.isEmpty()) return;
        outcome[4]++;
        Entry entry = new Entry(sign, sides);
        entry.refreshLight(level);
        entries.put(sign, entry);
    }

    /** Null when the sign must stay on the vanilla renderer (obfuscated text). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable List<Side> buildSides(SignBlockEntity sign) {
        BlockEntityRenderer renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(sign);
        if (!(renderer instanceof AbstractSignRenderer)) return null;
        SignRenderState state = (SignRenderState) renderer.createRenderState();
        renderer.extractRenderState(sign, state, 1.0F, Vec3.ZERO, null);

        List<Side> sides = new ArrayList<>(2);
        // Text straight from the sign: culling mods blank back-facing sides in the render state.
        if (!addSide(sides, sign, state, sign.getText(SignTextSlot.FRONT), state.transformations.frontText().getMatrix())) return releaseAll(sides);
        if (!addSide(sides, sign, state, sign.getText(SignTextSlot.BACK), state.transformations.backText().getMatrix())) return releaseAll(sides);
        return sides;
    }

    private static @Nullable List<Side> releaseAll(List<Side> sides) {
        for (Side side : sides) SignTextAtlas.release(side.tile);
        return null;
    }

    /** Mirrors {@link AbstractSignRenderer#submitSignText}. Returns false if the text cannot be pre-rendered. */
    private static boolean addSide(List<Side> sides, SignBlockEntity sign, SignRenderState state, @Nullable SignText text, Matrix4fc textToBlock) {
        if (text == null) return true;
        Font font = Minecraft.getInstance().font;
        FormattedCharSequence[] lines = text.getRenderMessages(state.isTextFilteringEnabled, input -> {
            List<FormattedCharSequence> split = font.split(input, state.maxTextLineWidth);
            return split.isEmpty() ? FormattedCharSequence.EMPTY : split.get(0);
        });

        boolean anyText = false;
        for (FormattedCharSequence line : lines) {
            boolean[] obfuscated = {false};
            boolean[] nonEmpty = {false};
            line.accept((index, style, codePoint) -> {
                nonEmpty[0] = true;
                if (style.isObfuscated()) obfuscated[0] = true;
                return true;
            });
            if (obfuscated[0]) return false;
            anyText |= nonEmpty[0];
        }
        if (!anyText) return true;

        int darkColor = AbstractSignRenderer.getDarkColor(text);
        int midpoint = 4 * state.textLineHeight / 2;
        int textColor;
        boolean outline;
        if (text.hasGlowingText()) {
            textColor = text.getColor().getTextColor();
            // Vanilla drops the glow outline past 16 blocks to save 8 glyph copies; pre-rendered text costs the same.
            outline = textColor == DyeColor.BLACK.getTextColor() || SettingsManager.SIGN_TEXT_ALWAYS_OUTLINE.getValue();
        } else {
            textColor = darkColor;
            outline = false;
        }

        List<SignTextAtlas.Line> atlasLines = new ArrayList<>(lines.length);
        for (int i = 0; i < lines.length; i++) {
            float x = -font.width(lines[i]) / 2;
            atlasLines.add(new SignTextAtlas.Line(lines[i], x, i * state.textLineHeight - midpoint, textColor, outline ? darkColor : 0));
        }
        sides.add(new Side(SignTextAtlas.allocate(atlasLines), text.hasGlowingText(), sign.getBlockPos(), textToBlock));
        return true;
    }

    public static void submit(LevelRenderContext context) {
        if (entries.isEmpty() || !isEnabled()) return;
        if (groupsDirty) {
            Int2ObjectMap<List<Side>> grouped = new Int2ObjectOpenHashMap<>();
            for (Entry entry : entries.values()) {
                for (Side side : entry.sides) grouped.computeIfAbsent(side.tile.pageIndex(), k -> new ArrayList<>()).add(side);
            }
            sidesByPage = grouped;
            groupsDirty = false;
        }

        Vec3 camera = context.levelState().cameraRenderState.pos;
        for (List<Side> page : sidesByPage.values()) {
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.textPolygonOffset(page.get(0).tile.texture()), (pose, buffer) -> {
                for (Side side : page) {
                    int light = side.glowing ? FULL_BRIGHT : side.entry.light;
                    double[] c = side.corners;
                    for (int i = 0; i < 4; i++) {
                        buffer.addVertex(pose, (float) (c[i * 3] - camera.x), (float) (c[i * 3 + 1] - camera.y), (float) (c[i * 3 + 2] - camera.z))
                            .setColor(-1)
                            .setUv(side.us[i], side.vs[i])
                            .setLight(light);
                    }
                }
            });
        }
    }
}

package io.github.nedostupn0.baked.client.renderer.map;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.util.entity.ItemFrameUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

/** Draws the maps of baked item frames: one submit per atlas page instead of a draw call and a frame render per map. */
public final class MapBatchRenderer {
    /** {@link net.minecraft.client.renderer.entity.ItemFrameRenderer#BRIGHT_MAP_LIGHT_ADJUSTMENT} applied to full bright. */
    private static final int GLOW_FRAME_MAP_LIGHT = 15728850;
    private static final int LIGHT_REFRESH_TICKS = 10;

    private static final Int2ObjectMap<Entry> entries = new Int2ObjectOpenHashMap<>();
    private static int lightRefreshCountdown;
    private static Int2ObjectMap<List<Entry>> entriesByPage = new Int2ObjectOpenHashMap<>();
    private static boolean groupsDirty;

    private MapBatchRenderer() {}

    /** Map placement captured with an item frame snapshot. {@code mapToBlock} maps 0..128 map space onto block-relative coordinates. */
    public record Geometry(int mapId, BlockPos origin, Matrix4fc mapToBlock, boolean glowFrame) {}

    private static final class Entry {
        final Entity entity;
        final Geometry geometry;
        final MapAtlas.Slot slot;
        final double[] corners = new double[12];
        final MapId mapId;
        int light;

        Entry(Entity entity, Geometry geometry, MapAtlas.Slot slot) {
            this.entity = entity;
            this.geometry = geometry;
            this.slot = slot;
            this.mapId = new MapId(geometry.mapId());
            float[][] mapCorners = {{0, 128}, {128, 128}, {128, 0}, {0, 0}};
            Vector3f p = new Vector3f();
            for (int i = 0; i < 4; i++) {
                geometry.mapToBlock().transformPosition(mapCorners[i][0], mapCorners[i][1], -0.01F, p);
                corners[i * 3] = geometry.origin().getX() + p.x();
                corners[i * 3 + 1] = geometry.origin().getY() + p.y();
                corners[i * 3 + 2] = geometry.origin().getZ() + p.z();
            }
        }

        void refreshLight(ClientLevel level) {
            light = geometry.glowFrame() ? GLOW_FRAME_MAP_LIGHT : LightCoordsUtil.getLightCoords(level, entity.blockPosition());
        }
    }

    public static void put(Entity frame, Geometry geometry) {
        remove(frame.getId());
        Entry entry = new Entry(frame, geometry, MapAtlas.acquire(geometry.mapId()));
        if (frame.level() instanceof ClientLevel level) entry.refreshLight(level);
        entries.put(frame.getId(), entry);
        groupsDirty = true;
    }

    public static void remove(int entityId) {
        Entry entry = entries.remove(entityId);
        if (entry != null) {
            MapAtlas.release(entry.geometry.mapId());
            groupsDirty = true;
        }
    }

    public static void clear() {
        entries.clear();
        groupsDirty = true;
        MapAtlas.reset();
    }

    public static int size() {
        return entries.size();
    }

    /** Light changes slowly and is cheap to sample, but not per map per frame. Main thread, once per tick. */
    public static void tick(ClientLevel level) {
        if (--lightRefreshCountdown > 0) return;
        lightRefreshCountdown = LIGHT_REFRESH_TICKS;
        for (Entry entry : entries.values()) entry.refreshLight(level);
    }

    public static void submit(LevelRenderContext context) {
        if (entries.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        MapAtlas.uploadDirty(level);

        if (groupsDirty) {
            Int2ObjectMap<List<Entry>> grouped = new Int2ObjectOpenHashMap<>();
            for (Entry entry : entries.values()) grouped.computeIfAbsent(entry.slot.pageIndex(), k -> new ArrayList<>()).add(entry);
            entriesByPage = grouped;
            groupsDirty = false;
        }

        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();

        for (List<Entry> page : entriesByPage.values()) {
            collector.submitCustomGeometry(poseStack, RenderTypes.text(page.get(0).slot.texture()), (pose, buffer) -> {
                for (Entry entry : page) {
                    if (!RenderModeManager.isPartMeshed(entry.entity, ItemFrameUtil.PART_MAP) || level.getMapData(entry.mapId) == null) continue;
                    MapAtlas.Slot slot = entry.slot;
                    float u0 = slot.u0(), v0 = slot.v0(), u1 = u0 + slot.size(), v1 = v0 + slot.size();
                    double[] c = entry.corners;
                    buffer.addVertex(pose, (float) (c[0] - camera.x), (float) (c[1] - camera.y), (float) (c[2] - camera.z)).setColor(-1).setUv(u0, v1).setLight(entry.light);
                    buffer.addVertex(pose, (float) (c[3] - camera.x), (float) (c[4] - camera.y), (float) (c[5] - camera.z)).setColor(-1).setUv(u1, v1).setLight(entry.light);
                    buffer.addVertex(pose, (float) (c[6] - camera.x), (float) (c[7] - camera.y), (float) (c[8] - camera.z)).setColor(-1).setUv(u1, v0).setLight(entry.light);
                    buffer.addVertex(pose, (float) (c[9] - camera.x), (float) (c[10] - camera.y), (float) (c[11] - camera.z)).setColor(-1).setUv(u0, v0).setLight(entry.light);
                }
            });
        }

        // Decorations are rare: look for them without allocating per map.
        for (Entry entry : entries.values()) {
            if (!RenderModeManager.isPartMeshed(entry.entity, ItemFrameUtil.PART_MAP)) continue;
            MapItemSavedData data = level.getMapData(entry.mapId);
            if (data != null && !data.decorations.isEmpty()) submitDecorations(entry, level, camera, poseStack, collector);
        }
    }

    /** Frame-visible decorations (banners, frame markers), as in {@link net.minecraft.client.renderer.MapRenderer#render} with showOnlyFrame. */
    private static void submitDecorations(Entry entry, ClientLevel level, Vec3 camera, PoseStack poseStack, SubmitNodeCollector collector) {
        MapItemSavedData data = level.getMapData(new MapId(entry.geometry.mapId()));
        if (data == null) return;
        TextureAtlas sprites = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        int light = entry.light;

        poseStack.pushPose();
        BlockPos origin = entry.geometry.origin();
        poseStack.translate(origin.getX() - camera.x, origin.getY() - camera.y, origin.getZ() - camera.z);
        poseStack.mulPose(new Matrix4f(entry.geometry.mapToBlock()));

        int count = 0;
        for (MapDecoration decoration : data.getDecorations()) {
            if (!decoration.renderOnFrame()) continue;
            poseStack.pushPose();
            poseStack.translate(decoration.x() / 2.0F + 64.0F, decoration.y() / 2.0F + 64.0F, -0.02F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(decoration.rot() * 360 / 16.0F));
            poseStack.scale(4.0F, 4.0F, 3.0F);
            poseStack.translate(-0.125F, 0.125F, 0.0F);
            TextureAtlasSprite sprite = sprites.getSprite(decoration.getSpriteLocation());
            float z = count * -0.001F;
            collector.submitCustomGeometry(poseStack, RenderTypes.text(sprite.atlasLocation()), (pose, buffer) -> {
                buffer.addVertex(pose, -1.0F, 1.0F, z).setColor(-1).setUv(sprite.getU0(), sprite.getV0()).setLight(light);
                buffer.addVertex(pose, 1.0F, 1.0F, z).setColor(-1).setUv(sprite.getU1(), sprite.getV0()).setLight(light);
                buffer.addVertex(pose, 1.0F, -1.0F, z).setColor(-1).setUv(sprite.getU1(), sprite.getV1()).setLight(light);
                buffer.addVertex(pose, -1.0F, -1.0F, z).setColor(-1).setUv(sprite.getU0(), sprite.getV1()).setLight(light);
            });
            poseStack.popPose();

            Component name = decoration.name().orElse(null);
            if (name != null) {
                Font font = Minecraft.getInstance().font;
                float width = font.width(name);
                float scale = Mth.clamp(25.0F / width, 0.0F, 6.0F / 9.0F);
                poseStack.pushPose();
                poseStack.translate(decoration.x() / 2.0F + 64.0F - width * scale / 2.0F, decoration.y() / 2.0F + 64.0F + 4.0F, -0.025F);
                poseStack.scale(scale, scale, -1.0F);
                poseStack.translate(0.0F, 0.0F, 0.1F);
                collector.order(1).submitText(poseStack, 0.0F, 0.0F, name.getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, -1, Integer.MIN_VALUE, 0);
                poseStack.popPose();
            }
            count++;
        }
        poseStack.popPose();
    }
}

package io.github.nedostupn0.baked.client.renderer.batch;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws baked entity parts whose shader animates (glint), batched per render type. In 26.x glint types draw base and
 * glint together, so these quads stay out of the section mesh.
 */
public final class DynamicPartBatch {
    private static final int LIGHT_REFRESH_TICKS = 10;
    /** x, y, z (block-relative), u, v, nx, ny, nz, color bits. */
    private static final int STRIDE = 9;

    private static final Int2ObjectMap<Entry> entries = new Int2ObjectOpenHashMap<>();
    private static int lightRefreshCountdown;
    /** Groups by render type, rebuilt only when entries change instead of every frame. */
    private static Map<RenderType, List<Placed>> placedByType = new IdentityHashMap<>();
    private static boolean groupsDirty;

    private record Placed(Entry entry, Group group) {}

    private DynamicPartBatch() {}

    /** Quads of one render type, relative to {@link Builder#origin}. */
    private record Group(RenderType renderType, float[] vertices) {}

    public static final class Builder {
        private final BlockPos origin;
        private final Map<RenderType, FloatArrayList> groups = new IdentityHashMap<>();

        public Builder(BlockPos origin) {
            this.origin = origin.immutable();
        }

        public boolean isEmpty() {
            return groups.isEmpty();
        }

        /** Block-relative positions, uvs in the render type's texture space. */
        public void addQuad(RenderType renderType, Vector3f[] positions, float[] us, float[] vs, Vector3f normal, int color) {
            FloatArrayList data = groups.computeIfAbsent(renderType, k -> new FloatArrayList());
            float colorBits = Float.intBitsToFloat(color);
            for (int i = 0; i < 4; i++) {
                data.add(positions[i].x());
                data.add(positions[i].y());
                data.add(positions[i].z());
                data.add(us[i]);
                data.add(vs[i]);
                data.add(normal.x());
                data.add(normal.y());
                data.add(normal.z());
                data.add(colorBits);
            }
        }

        /**
         * @param part  handler part these quads stand for; they are drawn while that part is meshed
         * @param fixedLight light coords to use instead of sampling the world (glow frames), or -1
         */
        public Parts build(int part, int fixedLight) {
            List<Group> built = new ArrayList<>(groups.size());
            groups.forEach((type, data) -> built.add(new Group(type, data.toFloatArray())));
            return new Parts(origin, part, fixedLight, built);
        }
    }

    /** Baked dynamic geometry of one entity, attached to its snapshot. */
    public record Parts(BlockPos origin, int part, int fixedLight, List<Group> groups) {}

    private static final class Entry {
        final Entity entity;
        final Parts parts;
        int light;

        Entry(Entity entity, Parts parts) {
            this.entity = entity;
            this.parts = parts;
        }

        void refreshLight(ClientLevel level) {
            light = parts.fixedLight() != -1 ? parts.fixedLight() : LightCoordsUtil.getLightCoords(level, entity.blockPosition());
        }
    }

    public static void put(Entity entity, Parts parts) {
        Entry entry = new Entry(entity, parts);
        if (entity.level() instanceof ClientLevel level) entry.refreshLight(level);
        entries.put(entity.getId(), entry);
        groupsDirty = true;
    }

    public static void remove(int entityId) {
        if (entries.remove(entityId) != null) groupsDirty = true;
    }

    public static void clear() {
        entries.clear();
        groupsDirty = true;
    }

    public static void tick(ClientLevel level) {
        if (--lightRefreshCountdown > 0) return;
        lightRefreshCountdown = LIGHT_REFRESH_TICKS;
        for (Entry entry : entries.values()) entry.refreshLight(level);
    }

    public static void submit(LevelRenderContext context) {
        if (entries.isEmpty() || Minecraft.getInstance().level == null) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();

        if (groupsDirty) {
            Map<RenderType, List<Placed>> grouped = new IdentityHashMap<>();
            for (Entry entry : entries.values()) {
                for (Group group : entry.parts.groups()) grouped.computeIfAbsent(group.renderType(), k -> new ArrayList<>()).add(new Placed(entry, group));
            }
            placedByType = grouped;
            groupsDirty = false;
        }

        placedByType.forEach((renderType, items) -> context.submitNodeCollector().submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (Placed item : items) {
                Entry entry = item.entry();
                if (!RenderModeManager.isPartMeshed(entry.entity, entry.parts.part())) continue;
                float[] v = item.group().vertices();
                BlockPos origin = entry.parts.origin();
                float ox = (float) (origin.getX() - camera.x);
                float oy = (float) (origin.getY() - camera.y);
                float oz = (float) (origin.getZ() - camera.z);
                for (int i = 0; i < v.length; i += STRIDE) {
                    buffer.addVertex(pose, ox + v[i], oy + v[i + 1], oz + v[i + 2])
                        .setColor(Float.floatToRawIntBits(v[i + 8]))
                        .setUv(v[i + 3], v[i + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(entry.light)
                        .setNormal(pose, v[i + 5], v[i + 6], v[i + 7]);
                }
            }
        }));
    }
}

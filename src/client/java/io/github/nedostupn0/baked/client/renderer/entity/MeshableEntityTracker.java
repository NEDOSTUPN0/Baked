package io.github.nedostupn0.baked.client.renderer.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import io.github.nedostupn0.baked.client.chunk.ChunkTaskHolder;
import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import io.github.nedostupn0.baked.client.util.entity.ArmorStandUtil;
import io.github.nedostupn0.baked.client.util.entity.ItemFrameUtil;
import io.github.nedostupn0.baked.client.util.entity.PaintingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;

/**
 * Tracks block-like entities baked into section meshes. Only rendering moves to the mesh, hitboxes and interaction stay
 * vanilla. Changes are flushed once per tick to coalesce bursts.
 */
public class MeshableEntityTracker {

    /** Every visible part of the entity is in the mesh, the vanilla renderer has nothing left to draw. */
    public static final int ALL_PARTS = -1;

    private static final Map<EntityType<?>, MeshableEntityHandler<?>> HANDLERS = new IdentityHashMap<>();

    private static final Map<SectionPos, Map<Integer, MeshableEntityData>> bySection = new ConcurrentHashMap<>();
    private static final Map<Integer, MeshableEntityData> byId = new ConcurrentHashMap<>();
    // Identity semantics: Entity#hashCode throws until the entity has been assigned an id,
    // and placement hooks fire while the spawn packet is still being applied.
    private static final Set<Entity> pending = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Entities temporarily drawn by the vanilla renderer (animating, moving), with the game time to retry at. */
    private static final Map<Entity, Suspension> suspended = new IdentityHashMap<>();
    private static final Map<Integer, MeshableEntityData> placementTracked = new ConcurrentHashMap<>();

    private static final int SETTLE_TICKS = 10;

    static {
        register(EntityTypes.ITEM_FRAME, ItemFrameUtil.HANDLER);
        register(EntityTypes.GLOW_ITEM_FRAME, ItemFrameUtil.HANDLER);
        register(EntityTypes.PAINTING, PaintingUtil.HANDLER);
        register(EntityTypes.ARMOR_STAND, ArmorStandUtil.HANDLER);
    }

    public static <T extends Entity> void register(EntityType<? extends T> type, MeshableEntityHandler<T> handler) {
        HANDLERS.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> @Nullable MeshableEntityHandler<T> getHandler(T entity) {
        return (MeshableEntityHandler<T>) HANDLERS.get(entity.getType());
    }

    public static boolean isMeshable(EntityType<?> type) {
        return HANDLERS.containsKey(type);
    }

    public static boolean isEnabled(Entity entity) {
        MeshableEntityHandler<?> handler = HANDLERS.get(entity.getType());
        return handler != null && handler.isEnabled() && SettingsManager.MOD_TOGGLE.getValue();
    }

    /** Schedules a re-snapshot of the entity on the next flush. Must be called on the main thread. */
    public static void markDirty(Entity entity) {
        if (!entity.level().isClientSide() || !((EntityExt) entity).baked$isSupported()) return;
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) {
            client.execute(() -> markDirty(entity));
            return;
        }
        pending.add(entity);
    }

    public static void remove(Entity entity) {
        if (!((EntityExt) entity).baked$isSupported()) return;
        pending.remove(entity);
        suspended.remove(entity);
        removeData(entity.getId());
        resetToEntity((EntityExt) entity);
    }

    public static void markAllDirty(ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            if (((EntityExt) entity).baked$isSupported()) pending.add(entity);
        }
    }

    /**
     * Hands the entity to vanilla for at least {@code ticks} (e.g. while animating); repeated calls extend the pause.
     */
    public static void suspend(Entity entity, int ticks) {
        if (!((EntityExt) entity).baked$isSupported() || entity.id == 0) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        long resumeAt = level.getGameTime() + ticks;
        Suspension current = suspended.get(entity);
        if (current != null) {
            current.resumeAt = Math.max(current.resumeAt, resumeAt);
            return;
        }
        suspended.put(entity, new Suspension(resumeAt, placementKeyOf(entity)));
        pending.remove(entity);
        removeData(entity.getId());
        resetToEntity((EntityExt) entity);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object placementKeyOf(Entity entity) {
        MeshableEntityHandler<Entity> handler = (MeshableEntityHandler<Entity>) HANDLERS.get(entity.getType());
        return handler == null ? null : handler.placementKey(entity);
    }

    private static final class Suspension {
        long resumeAt;
        @Nullable Object placementKey;

        Suspension(long resumeAt, @Nullable Object placementKey) {
            this.resumeAt = resumeAt;
            this.placementKey = placementKey;
        }
    }

    public static void clear() {
        suspended.clear();
        placementTracked.clear();
        pending.clear();
        byId.clear();
        bySection.clear();
    }

    public static void flush() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            pending.clear();
            suspended.clear();
            return;
        }

        if (!suspended.isEmpty()) {
            long now = level.getGameTime();
            suspended.entrySet().removeIf(entry -> {
                Suspension suspension = entry.getValue();
                // Still moving: keep it on the vanilla renderer until it has been still for a while.
                Object key = placementKeyOf(entry.getKey());
                if (key != null && !key.equals(suspension.placementKey)) {
                    suspension.placementKey = key;
                    suspension.resumeAt = Math.max(suspension.resumeAt, now + SETTLE_TICKS);
                }
                if (suspension.resumeAt > now) return false;
                pending.add(entry.getKey());
                return true;
            });
        }
        if (!placementTracked.isEmpty()) checkPlacements(level);
        if (pending.isEmpty()) return;

        Entity[] entities = pending.toArray(Entity[]::new);
        pending.clear();
        for (Entity entity : entities) {
            // Not spawned yet; ENTITY_LOAD will mark it again once it is in the level.
            if (entity.id == 0) continue;
            if (entity.level() != level || entity.isRemoved()) {
                remove(entity);
                continue;
            }
            if (suspended.containsKey(entity)) continue;
            update(entity);
        }
    }

    private static <T extends Entity> void update(T entity) {
        MeshableEntityHandler<T> handler = getHandler(entity);
        Snapshot snapshot = handler == null ? null : handler.snapshot(entity);

        if (snapshot == null) {
            remove(entity);
            return;
        }

        Set<SectionPos> sections = new HashSet<>();
        for (Piece piece : snapshot.pieces()) sections.add(SectionPos.of(piece.pos()));
        // Nothing visible (e.g. an invisible seat stand): moving or turning it changes nothing on screen.
        Object placementKey = snapshot.pieces().isEmpty() && snapshot.attachment() == null ? null : handler.placementKey(entity);
        MeshableEntityData data = new MeshableEntityData(entity.getId(), entity.level(), snapshot, handler, Set.copyOf(sections), ConcurrentHashMap.newKeySet(), placementKey);

        removeData(data.id());
        byId.put(data.id(), data);
        handler.onApplied(entity, snapshot);
        if (data.placementKey() != null) placementTracked.put(data.id(), data);
        for (SectionPos section : data.sections()) {
            bySection.computeIfAbsent(section, k -> new ConcurrentHashMap<>()).put(data.id(), data);
        }
        setDirty(data);

        // Nothing to draw at all (e.g. an empty invisible item frame): no section will report back.
        if (data.sections().isEmpty() && handler.isEnabled()) {
            EntityExt ext = (EntityExt) entity;
            ext.baked$meshedParts(snapshot.parts());
            ext.baked$fullyMeshed(snapshot.parts() == ALL_PARTS);
            ext.baked$renderMode(RenderMode.TERRAIN);
        }
    }

    private static void removeData(int id) {
        MeshableEntityData old = byId.remove(id);
        if (old == null) return;
        placementTracked.remove(id);
        old.handler().onRemoved(id);
        for (SectionPos sectionPos : old.sections()) {
            Map<Integer, MeshableEntityData> section = bySection.get(sectionPos);
            if (section != null) {
                section.remove(id);
                if (section.isEmpty()) bySection.remove(sectionPos);
            }
        }
        setDirty(old);
    }

    private static void setDirty(MeshableEntityData data) {
        for (SectionPos section : data.sections()) {
            RenderModeManager.setSectionDirty(section);
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkPlacements(ClientLevel level) {
        for (MeshableEntityData data : placementTracked.values()) {
            Entity entity = level.getEntity(data.id());
            if (entity == null) continue;
            Object key = ((MeshableEntityHandler<Entity>) data.handler()).placementKey(entity);
            if (!data.placementKey().equals(key)) suspend(entity, SETTLE_TICKS);
        }
    }

    private static void resetToEntity(EntityExt ext) {
        ext.baked$renderMode(RenderMode.ENTITY);
        ext.baked$meshedParts(0);
        ext.baked$fullyMeshed(false);
    }

    public static @Nullable MeshableEntityData getData(int id) {
        return byId.get(id);
    }

    public static boolean isPending(Entity entity) {
        return pending.contains(entity);
    }

    public static @Nullable Collection<MeshableEntityData> getMeshableEntities(SectionPos pos) {
        Map<Integer, MeshableEntityData> map = bySection.get(pos);
        return map == null ? null : map.values();
    }

    /**
     * Meshing result of a tracked entity from the section compilers. The render mode switches once the mesh is
     * uploaded, so the entity never disappears for a frame. Stale results are ignored.
     */
    public static void onCompiled(SectionPos sectionPos, MeshableEntityData data, boolean meshed) {
        ChunkTaskHolder.addTask(sectionPos, () -> {
            if (byId.get(data.id()) != data) return;
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || !(level.getEntity(data.id()) instanceof EntityExt ext)) return;
            if (!meshed) {
                resetToEntity(ext);
                return;
            }
            // Pieces may span several sections: hand over to the mesh only once all of them are uploaded.
            data.uploaded().add(sectionPos);
            if (data.uploaded().size() < data.sections().size()) return;
            ext.baked$meshedParts(data.snapshot().parts());
            ext.baked$fullyMeshed(data.snapshot().parts() == ALL_PARTS);
            ext.baked$renderMode(RenderMode.TERRAIN);
        });
    }

    /**
     * Baked geometry within one block, so it gets that block's lighting and section.
     * @param model relative to {@code pos}
     */
    public record Piece(BlockPos pos, BlockStateModel model) {}

    /**
     * @param parts bitmask of handler-defined parts drawn without the entity renderer, or {@link #ALL_PARTS}
     * @param attachment handler-specific data handed back in {@link MeshableEntityHandler#onApplied}
     */
    public record Snapshot(List<Piece> pieces, int parts, @Nullable Object attachment) {
        public Snapshot(List<Piece> pieces, int parts) {
            this(pieces, parts, null);
        }
    }

    public record MeshableEntityData(int id, Level level, Snapshot snapshot, MeshableEntityHandler<?> handler, Set<SectionPos> sections, Set<SectionPos> uploaded, @Nullable Object placementKey) {
        public boolean isEnabled() {
            return handler.isEnabled();
        }
    }
}

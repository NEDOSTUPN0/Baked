package io.github.nedostupn0.baked.client.util.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.List;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.model.QuadBaker;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityHandler;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.batch.DynamicPartBatch;
import io.github.nedostupn0.baked.client.renderer.map.MapBatchRenderer;
import net.minecraft.client.model.geom.builders.UVPair;
import org.joml.Vector3f;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.Vec3;

/**
 * Bakes item frames, mirroring {@link net.minecraft.client.renderer.entity.ItemFrameRenderer#submit}. Dynamic items
 * (maps, glint, special renderers, tints, animations) stay on the entity renderer.
 */
public class ItemFrameUtil {
    public static final int PART_FRAME = 1;
    public static final int PART_ITEM = 1 << 1;
    /** The framed map, drawn by {@link MapBatchRenderer} rather than the section mesh. */
    public static final int PART_MAP = 1 << 2;

    /** Frames holding a map whose data has not reached the client yet; re-baked once it arrives. */
    private static final Int2ObjectMap<Set<ItemFrame>> waitingForMapData = new Int2ObjectOpenHashMap<>();

    private static final int GLOW_FRAME_EMISSION = 5;
    private static final int GLOW_ITEM_EMISSION = 15;
    private static final int FULL_BRIGHT = 15728880;

    public static final MeshableEntityHandler<ItemFrame> HANDLER = new MeshableEntityHandler<>() {
        @Override
        public boolean isEnabled() {
            return SettingsManager.OPTIMISED_ITEM_FRAMES.getValue();
        }

        @Override
        public MeshableEntityTracker.@Nullable Snapshot snapshot(ItemFrame frame) {
            return ItemFrameUtil.snapshot(frame);
        }

        @Override
        public void onApplied(ItemFrame frame, MeshableEntityTracker.Snapshot snapshot) {
            if (!(snapshot.attachment() instanceof Attachment attachment)) return;
            if (attachment.map() != null) MapBatchRenderer.put(frame, attachment.map());
            if (attachment.dynamic() != null) DynamicPartBatch.put(frame, attachment.dynamic());
        }

        @Override
        public void onRemoved(int entityId) {
            MapBatchRenderer.remove(entityId);
            DynamicPartBatch.remove(entityId);
        }
    };

    /** Geometry of a frame drawn outside the section mesh. */
    private record Attachment(MapBatchRenderer.@Nullable Geometry map, DynamicPartBatch.@Nullable Parts dynamic) {}

    /** Map data arrived or changed (main thread). */
    public static void onMapData(MapId mapId) {
        Set<ItemFrame> frames = waitingForMapData.remove(mapId.id());
        if (frames == null) return;
        for (ItemFrame frame : frames) {
            if (!frame.isRemoved()) MeshableEntityTracker.markDirty(frame);
        }
    }

    public static void clearPendingMaps() {
        waitingForMapData.clear();
    }

    private static MeshableEntityTracker.@Nullable Snapshot snapshot(ItemFrame frame) {
        Minecraft client = Minecraft.getInstance();
        boolean isGlowFrame = frame.getType() == EntityTypes.GLOW_ITEM_FRAME;
        boolean isInvisible = frame.isInvisible();
        Direction direction = frame.getDirection();
        ItemStack item = frame.getItem();

        boolean hasMap = false;
        MapId mapId = item.isEmpty() ? null : frame.getFramedMapId(item);
        if (mapId != null) {
            hasMap = frame.level().getMapData(mapId) != null;
            if (!hasMap) waitingForMapData.computeIfAbsent(mapId.id(), k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(frame);
        }

        BlockPos blockPos = frame.getPos();
        Vec3 origin = frame.position().subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        PoseStack poseStack = new PoseStack();
        poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.translate(direction.getStepX() * 0.46875, direction.getStepY() * 0.46875, direction.getStepZ() * 0.46875);
        if (direction.getAxis().isHorizontal()) {
            poseStack.rotateDegrees(Axis.YP, 180.0F - direction.toYRot());
        } else {
            poseStack.rotateDegrees(Axis.XP, -90 * direction.getAxisDirection().getStep());
            poseStack.rotateDegrees(Axis.YP, 180.0F);
        }

        QuadBaker baker = new QuadBaker();
        int parts = 0;

        if (!isInvisible) {
            // Read the plain block model: BlockModelRenderState is filled through Fabric's mesh API,
            // which leaves its vanilla part list empty.
            BlockState fakeState = BlockStateDefinitions.getItemFrameFakeState(isGlowFrame, hasMap);
            BlockStateModel frameModel = client.getModelManager().getBlockStateModelSet().get(fakeState);
            List<BlockStateModelPart> frameParts = new ArrayList<>();
            frameModel.collectParts(RandomSource.create(42L), frameParts);

            poseStack.pushPose();
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            // Solid: the back texture is mostly transparent, so its distant mip levels would be
            // discarded by the cutout alpha test (the entity renderer samples without mipmaps).
            baker.addParts(frameParts, poseStack.last(), isGlowFrame ? GLOW_FRAME_EMISSION : 0, ChunkSectionLayer.SOLID);
            poseStack.popPose();
            if (baker.failed()) return null;
        }
        parts |= PART_FRAME;

        MapBatchRenderer.Geometry mapGeometry = null;
        if (hasMap && SettingsManager.BATCHED_MAPS.getValue()) {
            // Same transform as ItemFrameRenderer#submit for maps, ending in 0..128 map space.
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, isInvisible ? 0.5F : 0.4375F);
            poseStack.rotateDegrees(Axis.ZP, (frame.getRotation() % 4 * 2) * 360.0F / 8.0F);
            poseStack.rotateDegrees(Axis.ZP, 180.0F);
            poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
            poseStack.translate(-64.0F, -64.0F, 0.0F);
            poseStack.translate(0.0F, 0.0F, -1.0F);
            mapGeometry = new MapBatchRenderer.Geometry(mapId.id(), blockPos.immutable(), new Matrix4f(poseStack.last().pose()), isGlowFrame);
            poseStack.popPose();
        }

        boolean itemMeshed = item.isEmpty() || mapGeometry != null;
        DynamicPartBatch.Builder dynamic = null;
        if (!item.isEmpty() && !hasMap) {
            poseStack.translate(0.0F, 0.0F, isInvisible ? 0.5F : 0.4375F);
            poseStack.rotateDegrees(Axis.ZP, frame.getRotation() * 360.0F / 8.0F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            dynamic = new DynamicPartBatch.Builder(blockPos);
            itemMeshed = bakeItem(baker, dynamic, frame, item, poseStack, isGlowFrame ? GLOW_ITEM_EMISSION : 0);
        }
        if (itemMeshed) parts |= MeshableEntityTracker.ALL_PARTS;

        if (isInvisible && !itemMeshed) return null;
        DynamicPartBatch.Parts dynamicParts = dynamic != null && itemMeshed && !dynamic.isEmpty()
            ? dynamic.build(PART_ITEM, isGlowFrame ? FULL_BRIGHT : -1)
            : null;
        Attachment attachment = mapGeometry != null || dynamicParts != null ? new Attachment(mapGeometry, dynamicParts) : null;
        return new MeshableEntityTracker.Snapshot(baker.buildPieces(blockPos, null, false), parts, attachment);
    }

    /** Appends the item only if all of it can be meshed. Glinted layers (experimental) go to {@code dynamic}. */
    private static boolean bakeItem(QuadBaker target, DynamicPartBatch.Builder dynamic, ItemFrame frame, ItemStack item, PoseStack poseStack, int lightEmission) {
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForNonLiving(state, item, ItemDisplayContext.FIXED, frame);
        if (state.isEmpty() || state.isAnimated()) return false;

        boolean experimental = SettingsManager.EXPERIMENTAL_FRAME_EFFECTS.getValue();
        QuadBaker itemBaker = new QuadBaker(experimental);
        for (int i = 0; i < state.activeLayerCount; i++) {
            ItemStackRenderState.LayerRenderState layer = state.layers[i];
            if (!isStatic(layer, experimental)) return false;

            poseStack.pushPose();
            layer.applyTransform(poseStack.last());
            int[] tints = layer.tintLayers != null ? layer.tintLayers.toIntArray() : null;
            if (layer.foilType == ItemStackRenderState.FoilType.STANDARD) {
                addGlintQuads(dynamic, layer.quads.all(), poseStack.last(), tints);
            } else {
                itemBaker.addQuads(layer.quads.all(), poseStack.last(), lightEmission, null, tints);
            }
            poseStack.popPose();

            if (itemBaker.failed()) return false;
        }

        target.addAll(itemBaker);
        return true;
    }

    /** Glinted item quads keep their own atlas and render type, exactly what vanilla submits for them. */
    public static void addGlintQuads(DynamicPartBatch.Builder dynamic, List<BakedQuad> quads, PoseStack.Pose pose, int @Nullable [] tints) {
        Vector3f[] positions = new Vector3f[4];
        float[] us = new float[4];
        float[] vs = new float[4];
        for (BakedQuad quad : quads) {
            for (int v = 0; v < 4; v++) {
                positions[v] = pose.pose().transformPosition(quad.position(v), new Vector3f());
                us[v] = UVPair.unpackU(quad.packedUV(v));
                vs[v] = UVPair.unpackV(quad.packedUV(v));
            }
            Vector3f normal = pose.normal().transform(new Vector3f(quad.direction().getUnitVec3f())).normalize();
            int tintIndex = quad.materialInfo().tintIndex();
            int color = tints != null && tintIndex >= 0 && tintIndex < tints.length ? tints[tintIndex] : -1;
            dynamic.addQuad(quad.materialInfo().itemGlintRenderType(), positions, us, vs, normal, color);
        }
    }

    private static boolean isStatic(ItemStackRenderState.LayerRenderState layer, boolean experimental) {
        if (layer.specialRenderer != null) return false;
        // Standard glint can be batched (experimental); the special variant needs its own vertex format.
        if (layer.foilType == ItemStackRenderState.FoilType.SPECIAL) return false;
        if (layer.foilType == ItemStackRenderState.FoilType.STANDARD && !experimental) return false;

        // Translucent geometry would end up in the cutout layer of the vanilla section compiler.
        // Tints are checked by the baker: they need the experimental palette.
        for (BakedQuad quad : layer.quads.all()) {
            if (quad.materialInfo().layer().translucent()) return false;
        }
        return true;
    }
}

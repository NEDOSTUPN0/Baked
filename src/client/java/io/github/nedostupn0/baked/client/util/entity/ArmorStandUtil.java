package io.github.nedostupn0.baked.client.util.entity;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.Baked;
import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.model.CapturingSubmitCollector;
import io.github.nedostupn0.baked.client.model.QuadBaker;
import io.github.nedostupn0.baked.client.renderer.batch.DynamicPartBatch;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityHandler;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/** Bakes armor stands by capturing the vanilla renderer. Unbakeable stands (glint, dyes, trims, skins) stay vanilla. */
public class ArmorStandUtil {
    /** Hit wiggle lasts 5 ticks, see ArmorStandRenderer#setupRotations. */
    public static final int HIT_ANIMATION_TICKS = 6;

    public static final MeshableEntityHandler<ArmorStand> HANDLER = new MeshableEntityHandler<>() {
        @Override
        public boolean isEnabled() {
            return SettingsManager.OPTIMISED_ARMOR_STANDS.getValue();
        }

        @Override
        public MeshableEntityTracker.@Nullable Snapshot snapshot(ArmorStand stand) {
            return ArmorStandUtil.snapshot(stand);
        }

        @Override
        public void onApplied(ArmorStand stand, MeshableEntityTracker.Snapshot snapshot) {
            if (snapshot.attachment() instanceof DynamicPartBatch.Parts parts) DynamicPartBatch.put(stand, parts);
        }

        @Override
        public void onRemoved(int entityId) {
            DynamicPartBatch.remove(entityId);
        }

        @Override
        public Object placementKey(ArmorStand stand) {
            return new Placement(stand.position(), stand.getYRot());
        }
    };

    private record Placement(Vec3 position, float yRot) {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static MeshableEntityTracker.@Nullable Snapshot snapshot(ArmorStand stand) {
        if (stand.level().getGameTime() - stand.lastHit < HIT_ANIMATION_TICKS) return null;

        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(stand);
        BlockPos blockPos = stand.blockPosition();
        boolean experimental = SettingsManager.EXPERIMENTAL_STAND_EFFECTS.getValue();
        QuadBaker baker = new QuadBaker(experimental);
        DynamicPartBatch.Builder dynamic = experimental ? new DynamicPartBatch.Builder(blockPos) : null;
        CapturingSubmitCollector collector = new CapturingSubmitCollector(baker, dynamic);

        RenderModeManager.capturing = true;
        try {
            EntityRenderState state = renderer.createRenderState(stand, 1.0F);
            Vec3 offset = renderer.getRenderOffset(state);
            PoseStack poseStack = new PoseStack();
            poseStack.translate(stand.getX() - blockPos.getX() + offset.x, stand.getY() - blockPos.getY() + offset.y, stand.getZ() - blockPos.getZ() + offset.z);
            renderer.submit(state, poseStack, collector, new CameraRenderState());
        } catch (RuntimeException e) {
            Baked.LOGGER.warn("Failed to bake armor stand {}, keeping vanilla rendering", stand.getId(), e);
            return null;
        } finally {
            RenderModeManager.capturing = false;
        }

        if (collector.failed()) {
            lastFailure = collector.failure();
            return null;
        }
        lastFailure = null;
        DynamicPartBatch.Parts dynamicParts = dynamic != null && !dynamic.isEmpty() ? dynamic.build(MeshableEntityTracker.ALL_PARTS, -1) : null;
        return new MeshableEntityTracker.Snapshot(baker.buildPieces(blockPos, null, false), MeshableEntityTracker.ALL_PARTS, dynamicParts);
    }

    /** Why the most recent bake fell back to vanilla, for {@code /baked debug}. */
    public static @Nullable String lastFailure;
}

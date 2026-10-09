package io.github.nedostupn0.baked.client.mixin.renderer;

import java.util.SortedSet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.client.renderer.blockentity.SpecialBlockEntityRenderingManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Baked block entities are not extracted, which drops their breaking cracks: extract a cracks-only state for blocks
 * being broken. Runs after the call so it also works when Sodium replaces it.
 */
@Mixin(LevelExtractor.class)
public abstract class CrumblingPassMixin {
    @Shadow private ClientLevel level;
    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Final private LevelRenderer levelRenderer;

    @Inject(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/level/LevelRenderState;)V", shift = At.Shift.AFTER))
    private void baked$extractCrumblingOverlays(DeltaTracker deltaTracker, Camera camera, float partialTicks, CallbackInfo ci) {
        if (level == null) return;
        Long2ObjectMap<SortedSet<BlockDestructionProgress>> progresses = level.destructionProgress();
        if (progresses.isEmpty()) return;

        Vec3 cameraPos = camera.position();
        PoseStack poseStack = new PoseStack();
        for (Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry : progresses.long2ObjectEntrySet()) {
            SortedSet<BlockDestructionProgress> set = entry.getValue();
            if (set == null || set.isEmpty()) continue;
            BlockPos pos = BlockPos.of(entry.getLongKey());
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BlockEntityExt ext) || !baked$isBaked(ext, be)) continue;

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            ModelFeatureRenderer.CrumblingOverlay overlay = new ModelFeatureRenderer.CrumblingOverlay(set.last().getProgress(), poseStack.last());
            poseStack.popPose();

            RenderModeManager.crumblingPass = true;
            try {
                BlockEntityRenderState state = levelRenderer.blockEntityRenderDispatcher().tryExtractRenderState(be, partialTicks, overlay, false);
                if (state != null) {
                    ((BlockEntityRenderStateExt) state).blockEntity(be);
                    ((BlockEntityRenderStateExt) state).baked$crumblingOnly(true);
                    levelRenderState.blockEntityRenderStates.add(state);
                }
            } finally {
                RenderModeManager.crumblingPass = false;
            }
        }
    }

    /** Same condition the extraction uses to leave a block entity to the terrain mesh. */
    private static boolean baked$isBaked(BlockEntityExt ext, BlockEntity be) {
        return ext.baked$isEnabled() && (!RenderModeManager.shouldRenderEntityFast(ext) || ext.baked$shouldSkipRendering() || SpecialBlockEntityRenderingManager.shouldSkipRendering(be));
    }
}

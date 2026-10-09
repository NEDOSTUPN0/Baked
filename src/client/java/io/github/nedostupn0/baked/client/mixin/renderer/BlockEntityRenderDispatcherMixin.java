package io.github.nedostupn0.baked.client.mixin.renderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.client.model.CrumblingOnlyCollector;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow
    public abstract <E extends net.minecraft.world.level.block.entity.BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(S state);

    /** Overlay-only states (see CrumblingPassMixin): run the renderer but keep nothing except the cracks. */
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private <S extends BlockEntityRenderState> void baked$submitCrumblingOnly(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!((BlockEntityRenderStateExt) state).baked$crumblingOnly()) return;
        ci.cancel();
        BlockEntityRenderer<?, S> renderer = getRenderer(state);
        if (renderer == null) return;
        RenderModeManager.crumblingPass = true;
        try {
            renderer.submit(state, poseStack, new CrumblingOnlyCollector(collector), camera);
        } finally {
            RenderModeManager.crumblingPass = false;
        }
    }
}

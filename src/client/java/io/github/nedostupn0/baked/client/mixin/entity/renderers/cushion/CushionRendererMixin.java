package io.github.nedostupn0.baked.client.mixin.entity.renderers.cushion;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import net.minecraft.client.model.object.cushion.CushionModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CushionRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CushionRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.decoration.Cushion;

@Mixin(CushionRenderer.class)
public abstract class CushionRendererMixin extends EntityRenderer<Cushion, CushionRenderState> {

    @Shadow
    @Final
    private CushionModel model;

    protected CushionRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void baked$submit(final CushionRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera, CallbackInfo ci) {
        if(RenderModeManager.isPartMeshed(state, MeshableEntityTracker.ALL_PARTS)){
            super.submit(state, poseStack, submitNodeCollector, camera);
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void baked$cancelExtract(CallbackInfo ci, @Local CushionRenderState state, @Local Cushion entity){
        ((EntityRenderStateExt)state).entity(entity);
    }
}

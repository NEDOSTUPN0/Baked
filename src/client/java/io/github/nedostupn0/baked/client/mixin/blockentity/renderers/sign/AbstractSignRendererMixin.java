package io.github.nedostupn0.baked.client.mixin.blockentity.renderers.sign;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.sign.SignTextBatch;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.client.compat.ModCompat;
import io.github.nedostupn0.baked.client.config.SettingsManager;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;

@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
    @Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
    public void baked$cancelSignText(CallbackInfo ci, @Local PoseStack poseStack, @Local(argsOnly = true) SignRenderState state){
        // Text drawn from the sign text atlas.
        if(SignTextBatch.isBatched(((BlockEntityRenderStateExt) state).blockEntity())){
            ci.cancel();
            return;
        }
        if((ModCompat.isShadowPass() || !baked$isFacingCamera(poseStack)) && SettingsManager.SIGN_TEXT_CULLING.getValue()) ci.cancel();
    }

    @Unique
    private static boolean baked$isFacingCamera(PoseStack poseStack) {
        Matrix4f pose = poseStack.last().pose();

        PoseStack.Pose topPose = poseStack.last();
        Matrix3f normalMatrix = topPose.normal();
        Vector3f forwardVector = new Vector3f();
        normalMatrix.getColumn(2, forwardVector);

        Vector3f pos = pose.transformPosition(0, 0, 0, new Vector3f());

        return forwardVector.dot(pos) < 0.0f;
    }
}

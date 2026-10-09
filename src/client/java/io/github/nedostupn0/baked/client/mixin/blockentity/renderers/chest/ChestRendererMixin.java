package io.github.nedostupn0.baked.client.mixin.blockentity.renderers.chest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;

@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin<T extends BlockEntity & LidBlockEntity> {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    public void baked$cancelSubmit(CallbackInfo ci, @Local ChestRenderState state){
        if(!RenderModeManager.shouldRenderEntity(state) && SettingsManager.OPTIMISED_CHESTS.getValue()) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void baked$cancelExtract(CallbackInfo ci, @Local ChestRenderState state, @Local T be){
        ((BlockEntityRenderStateExt)state).blockEntity(be);
        if(!RenderModeManager.shouldRenderEntity(be) && SettingsManager.OPTIMISED_CHESTS.getValue()) ci.cancel();
    }
}

package io.github.nedostupn0.baked.client.mixin.entity.renderers.armorstand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityRenderStateExt;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;

@Mixin(ArmorStandRenderer.class)
public abstract class ArmorStandRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ArmorStand;Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;F)V", at = @At("HEAD"))
    private void baked$linkEntity(ArmorStand entity, ArmorStandRenderState state, float partialTicks, CallbackInfo ci) {
        ((EntityRenderStateExt) state).entity(entity);
    }
}

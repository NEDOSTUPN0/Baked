package io.github.nedostupn0.baked.client.mixin.entity.armorstand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.util.entity.ArmorStandUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.decoration.ArmorStand;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {

    /** The hit wiggle is animated: vanilla plays it, then the stand is baked again. */
    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void baked$onEntityEvent(byte id, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (id == EntityEvent.ARMORSTAND_WOBBLE && self.level().isClientSide()) {
            MeshableEntityTracker.suspend(self, ArmorStandUtil.HIT_ANIMATION_TICKS);
        }
    }
}

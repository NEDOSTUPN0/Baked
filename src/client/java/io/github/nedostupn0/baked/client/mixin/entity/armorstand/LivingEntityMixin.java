package io.github.nedostupn0.baked.client.mixin.entity.armorstand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /** Equipment is synced through its own packet, not entity data. */
    @Inject(method = "setItemSlot", at = @At("TAIL"))
    private void baked$onEquipmentChanged(CallbackInfo ci) {
        MeshableEntityTracker.markDirty((Entity) (Object) this);
    }

    /** Scale is an attribute, also synced through its own packet. */
    @Inject(method = "onAttributeUpdated", at = @At("TAIL"))
    private void baked$onAttributeUpdated(Holder<Attribute> attribute, CallbackInfo ci) {
        if (attribute.is(Attributes.SCALE)) MeshableEntityTracker.markDirty((Entity) (Object) this);
    }
}

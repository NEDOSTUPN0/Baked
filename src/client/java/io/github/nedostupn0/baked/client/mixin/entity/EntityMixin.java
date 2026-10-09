package io.github.nedostupn0.baked.client.mixin.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityExt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void baked$init(EntityType<?> type, Level level, CallbackInfo ci) {
        if (level.isClientSide() && MeshableEntityTracker.isMeshable(type)) {
            ((EntityExt) this).baked$isSupported(true);
        }
    }

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void baked$onDataUpdated(CallbackInfo ci) {
        MeshableEntityTracker.markDirty((Entity) (Object) this);
    }
}

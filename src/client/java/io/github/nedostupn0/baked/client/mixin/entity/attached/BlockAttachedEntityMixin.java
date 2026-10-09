package io.github.nedostupn0.baked.client.mixin.entity.attached;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;

@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityMixin {

    @Inject(method = "setPos(DDD)V", at = @At("TAIL"))
    private void baked$onMove(CallbackInfo ci) {
        MeshableEntityTracker.markDirty((Entity) (Object) this);
    }
}

package io.github.nedostupn0.baked.client.mixin.blockentity.sign;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.sign.SignTextBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/** Text arrives through block entity data (chunk load, update packets) or direct edits. */
@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityTrackingMixin {

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void baked$onLoad(CallbackInfo ci) {
        baked$markDirty();
    }

    @Inject(method = "setText", at = @At("TAIL"))
    private void baked$onSetText(CallbackInfo ci) {
        baked$markDirty();
    }

    private void baked$markDirty() {
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        // The integrated server loads its own sign block entities on another thread.
        if (Minecraft.getInstance().isSameThread() && (self.getLevel() == null || self.getLevel().isClientSide())) {
            SignTextBatch.markDirty(self);
        }
    }
}

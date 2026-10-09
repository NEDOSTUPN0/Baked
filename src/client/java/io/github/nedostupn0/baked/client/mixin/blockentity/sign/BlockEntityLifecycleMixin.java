package io.github.nedostupn0.baked.client.mixin.blockentity.sign;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.sign.SignTextBatch;
import io.github.nedostupn0.baked.client.renderer.skull.SkinHolder;
import io.github.nedostupn0.baked.client.renderer.skull.SkinPool;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

@Mixin(BlockEntity.class)
public abstract class BlockEntityLifecycleMixin {

    /** Block entities read from chunk data get their level only after loading. */
    @Inject(method = "setLevel", at = @At("TAIL"))
    private void baked$onSetLevel(CallbackInfo ci) {
        if ((Object) this instanceof SignBlockEntity sign && sign.getLevel().isClientSide() && Minecraft.getInstance().isSameThread()) {
            SignTextBatch.markDirty(sign);
        }
    }

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void baked$onRemoved(CallbackInfo ci) {
        if ((Object) this instanceof SignBlockEntity sign && Minecraft.getInstance().isSameThread()) {
            SignTextBatch.remove(sign);
        }
        // Releases the skin pool slot of a player head.
        if ((Object) this instanceof SkinHolder head && Minecraft.getInstance().isSameThread()) {
            SkinPool.hold(head, null);
        }
    }
}

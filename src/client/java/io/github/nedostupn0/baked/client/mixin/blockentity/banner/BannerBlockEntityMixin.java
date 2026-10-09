package io.github.nedostupn0.baked.client.mixin.blockentity.banner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.registry.Registry;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(BannerBlockEntity.class)
public class BannerBlockEntityMixin{

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/DyeColor;)V", at = @At("TAIL"))
    private void baked$init(CallbackInfo ci) {

        BlockEntity be = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;

        ext.baked$isSupported(Registry.isSupported("banner", be.getType()));
        ext.baked$renderBoth(true);
    }
}

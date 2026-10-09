package io.github.nedostupn0.baked.client.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.resources.ResourceUtil;

import org.spongepowered.asm.mixin.injection.At;

import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.sign.SignTextBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelManager;

@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @Inject(
        method = "apply",
        at = @At("TAIL")
    )
    private void baked$onTextureReloadComplete(CallbackInfo ci) {
        ResourceUtil.clearCache();
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) MeshableEntityTracker.markAllDirty(level);
        // Fonts may have changed with the resource packs.
        SignTextBatch.markAllDirty();
    }
}

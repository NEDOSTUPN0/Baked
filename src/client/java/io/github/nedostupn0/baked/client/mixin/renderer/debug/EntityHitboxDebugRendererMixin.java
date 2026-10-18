package io.github.nedostupn0.baked.client.mixin.renderer.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;

/**
 * The hitbox debug renderer only draws entities that pass the visibility check, which baked entities fail on purpose:
 * let them through here so F3+B still shows their hitboxes.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
    @WrapOperation(
        method = "emitGizmos",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;isEntityVisible(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDDFJ)Z")
    )
    private boolean baked$keepMeshedEntities(LevelExtractor extractor, Entity entity, Frustum frustum, double x, double y, double z, float partialTick, long time, Operation<Boolean> original) {
        RenderModeManager.hitboxPass = true;
        try {
            return original.call(extractor, entity, frustum, x, y, z, partialTick, time);
        } finally {
            RenderModeManager.hitboxPass = false;
        }
    }
}

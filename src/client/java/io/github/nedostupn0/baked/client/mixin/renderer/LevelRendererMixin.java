package io.github.nedostupn0.baked.client.mixin.renderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.SpecialBlockEntityRenderingManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.List;

@Mixin(LevelExtractor.class)
public class LevelRendererMixin {

    @WrapOperation(
        method = "extractVisibleBlockEntities",
        at = @At(
            value = "INVOKE", 
            target = "getRenderableBlockEntities" 
        )
    )
    private <T extends SectionMesh> List<BlockEntity> baked$redirectGetRenderableBlockEntities(T sectionMeshInstance, Operation<List<BlockEntity>> originalCall) {
        List<BlockEntity> original = originalCall.call(sectionMeshInstance);

        if (original == null || original.isEmpty())  return original;
        // The list belongs to the section mesh: filter a copy, or block entities would stay hidden until the next rebuild.
        original = new java.util.ArrayList<>(original);

        for (int i = original.size() - 1; i >= 0; i--) {
            if (original.get(i) instanceof BlockEntityExt ext){
                if(ext.baked$isEnabled() && (!RenderModeManager.shouldRenderEntityFast(ext) || ext.baked$shouldSkipRendering() || SpecialBlockEntityRenderingManager.shouldSkipRendering((BlockEntity)ext))){
                    original.remove(i);
                }
            }
        }

        return original;
    }

    @Inject(
        method = "isEntityVisible",
        at = @At("HEAD"),
        cancellable = true
    )
    private void baked$shouldRenderEntity(CallbackInfoReturnable<Boolean> cir, @Local Entity entity) {
        if(RenderModeManager.canSkipMeshedEntity(entity)){
            cir.setReturnValue(false);
        }
    }
}

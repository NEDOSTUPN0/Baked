package io.github.nedostupn0.baked.client.mixin.blockentity.ext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityExt {
    @Unique private RenderMode renderMode = RenderMode.ENTITY;
    @Unique private RenderMode renderModeDelayed = RenderMode.TERRAIN;
    @Unique private boolean isSupported = false;
    @Unique private boolean hasSpecialRenderer = false;
    @Unique private long timerStart = 0;
    @Unique private int timerDuration = 0;
    @Unique private boolean isEnabled = true;
    @Unique private boolean renderBoth = false;
    @Unique private boolean shouldSkipRendering = false;
    @Unique private boolean forceEntity = false;

    @Override public boolean baked$isSupported() { return isSupported; }
    @Override public void baked$isSupported(boolean bl) {
        if(bl && bl != isSupported && renderModeDelayed == RenderMode.TERRAIN){
            if(!RenderModeManager.canBeTerrain(this)) {
                renderModeDelayed = RenderMode.ENTITY;
            }
        }
        this.isSupported = bl; 
    }

    @Override public RenderMode baked$renderMode() { return renderMode; }
    @Override public void baked$renderMode(RenderMode mode) {
        if(isEnabled) renderMode = mode;
        renderModeDelayed = mode;
    }

    @Override public RenderMode baked$renderModeDelayed() { return renderModeDelayed; }
    @Override public void baked$renderModeDelayed(RenderMode mode) { renderModeDelayed = mode; }

    @Override public boolean baked$hasSpecialRenderer() { return hasSpecialRenderer; }
    @Override public void baked$hasSpecialRenderer(boolean bl) { hasSpecialRenderer = bl; }

    @Override public boolean baked$isEnabled() { return isEnabled; }
    @Override public void baked$isEnabled(boolean bl) {
        isEnabled = bl;
        if(!isEnabled) renderMode = RenderMode.ENTITY;
    }

    @Override public boolean baked$renderBoth() { return renderBoth; }
    @Override public void baked$renderBoth(boolean bl) { renderBoth = bl;}

    @Override public boolean baked$isTimerFinished(){
        if(timerStart == 0) return false;
        else return Minecraft.getInstance().level.getGameTime() - timerStart > timerDuration;
    }

    @Override public void baked$setTimer(long start, int duration){
        timerStart = start;
        timerDuration = duration;
    }

    @Override public boolean baked$shouldSkipRendering() { return shouldSkipRendering; }
    @Override public void baked$shouldSkipRendering(boolean bl) { shouldSkipRendering = bl; }
    @Override public void baked$shouldSkipRenderingAndUpdate(boolean bl) {
        if(bl != shouldSkipRendering) {
            shouldSkipRendering = bl;
            BlockPos pos = ((BlockEntity)(Object)this).getBlockPos();
            if(Minecraft.getInstance().level != null && Minecraft.getInstance().level.getChunkSource() != null) RenderModeManager.setDirty(pos);
        }
    }

    @Override public boolean baked$forceEntity() { return forceEntity; }
    @Override public void baked$forceEntity(boolean bl) { forceEntity = bl; }
}
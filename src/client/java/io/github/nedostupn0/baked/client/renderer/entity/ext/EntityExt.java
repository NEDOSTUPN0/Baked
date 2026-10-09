package io.github.nedostupn0.baked.client.renderer.entity.ext;

import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;

public interface EntityExt {
    boolean baked$isSupported();
    void baked$isSupported(boolean bl);

    RenderMode baked$renderMode();
    void baked$renderMode(RenderMode mode);

    RenderMode baked$renderModeDelayed();
    void baked$renderModeDelayed(RenderMode mode);

    boolean baked$hasSpecialRenderer();
    void baked$hasSpecialRenderer(boolean bl);

    boolean baked$isTimerFinished();
    void baked$setTimer(long start, int duration);

    boolean baked$isEnabled();
    void baked$isEnabled(boolean bl);

    boolean baked$shouldSkipRendering();
    void baked$shouldSkipRendering(boolean bl);
    void baked$shouldSkipRenderingAndUpdate(boolean bl);

    boolean baked$forceEntity();
    void baked$forceEntity(boolean bl);

    int baked$meshedParts();
    void baked$meshedParts(int parts);

    boolean baked$fullyMeshed();
    void baked$fullyMeshed(boolean bl);
}

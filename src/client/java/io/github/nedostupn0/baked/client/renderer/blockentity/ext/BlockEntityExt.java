package io.github.nedostupn0.baked.client.renderer.blockentity.ext;

import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityExt;

public interface BlockEntityExt extends EntityExt {
    boolean baked$renderBoth();
    void baked$renderBoth(boolean bl);
}

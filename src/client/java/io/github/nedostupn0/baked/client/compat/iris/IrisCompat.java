package io.github.nedostupn0.baked.client.compat.iris;

import net.irisshaders.iris.api.v0.IrisApi;

public class IrisCompat {
    public static boolean isShadowPass(){
        return IrisApi.getInstance().isRenderingShadowPass();
    }
}

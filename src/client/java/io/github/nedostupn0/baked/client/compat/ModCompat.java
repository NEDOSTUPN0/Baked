package io.github.nedostupn0.baked.client.compat;

import java.util.Arrays;
import java.util.List;

import io.github.nedostupn0.baked.Baked;
import io.github.nedostupn0.baked.client.compat.bclib.BclibCompat;
import io.github.nedostupn0.baked.client.compat.emf.EMFCompat;
import io.github.nedostupn0.baked.client.compat.iris.IrisCompat;
import io.github.nedostupn0.baked.client.compat.lootr.LootrCompat;
import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.platform.PlatformHelper;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.level.block.state.BlockState;

public class ModCompat {
    private final static boolean isIrisLoaded = PlatformHelper.isModLoaded("iris") || PlatformHelper.isModLoaded("oculus");
    private final static boolean isSodiumLoaded = PlatformHelper.isModLoaded("sodium") || PlatformHelper.isModLoaded("embeddium");
    private static boolean isEMFLoaded = PlatformHelper.isModLoaded("entity_model_features");
    private final static boolean isPunchyLoaded = PlatformHelper.isModLoaded("punchy");

    private static final List<String> incompatibleMods = Arrays.asList("optifine","embeddium","optifabric");

    public static void init(){
        if(PlatformHelper.isModLoaded("lootr")) {
            try{
                LootrCompat.init();
            }
            catch(Error e){
                Baked.LOGGER.warn("Incompatible version of Lootr used, if you are using the latest one, please report that to Baked's author.");
            }
        }
    }

    public static void onWorldLoad(){
        if(PlatformHelper.isModLoaded("bclib")) {
            try{
                BclibCompat.init();
            }
            catch(Error e){
                Baked.LOGGER.warn("Incompatible version of Bclib used, if you are using the latest one, please report that to Baked's author.");
            }
        }
    }

    public static boolean isIrisLoaded(){
        return isIrisLoaded;
    }

    public static boolean isSodiumLoaded(){
        return isSodiumLoaded;
    }

    public static boolean isEMFLoaded(){
        return isEMFLoaded;
    }

    public static boolean isPunchyLoaded(){
        return isPunchyLoaded;
    }

    public static boolean isShadowPass(){
        if(isIrisLoaded()) return IrisCompat.isShadowPass();
        else return false;
    }

    public static ModelPart applyEMFRestPose(ModelPart root, BlockState state){
        if(isEMFLoaded() && SettingsManager.EMF_COMPAT.getValue() && state != null) {
            try{
                return EMFCompat.applyRestPose(root, state);
            }
            catch(Error e){
                Baked.LOGGER.warn("Incompatible version of EMF used, if you are using the latest one, please report that to Baked's author.");
                isEMFLoaded = false;
                return root;
            }
        }
        else return root;
    }

    public static boolean isIncompatibilityDetected(){
        for(String mod : incompatibleMods){
            if(PlatformHelper.isModLoaded(mod)) return true;
        }
        return false;
    }

    public static String getIncompatibleMod(){
        for(String mod : incompatibleMods){
            if(PlatformHelper.isModLoaded(mod)) return PlatformHelper.getModName(mod);
        }
        return null;
    }
}

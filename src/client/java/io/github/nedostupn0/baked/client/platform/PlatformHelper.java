package io.github.nedostupn0.baked.client.platform;

import java.nio.file.Path;

import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import io.github.nedostupn0.baked.client.compat.ModCompat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import io.github.nedostupn0.baked.client.renderer.batch.DynamicPartBatch;
import io.github.nedostupn0.baked.client.renderer.map.MapBatchRenderer;
import io.github.nedostupn0.baked.client.renderer.sign.SignTextBatch;
import io.github.nedostupn0.baked.client.util.entity.ItemFrameUtil;
import net.fabricmc.loader.api.FabricLoader;

public class PlatformHelper {

    public static String getPlatformName(){
        return "Fabric";
    }
    
    public static boolean isModLoaded(String modId){
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static Path getConfigDir(){
        return FabricLoader.getInstance().getConfigDir();
    }

    public static String getModName(String modId){
        return FabricLoader.getInstance().getModContainer(modId).get().getMetadata().getName();
    }

    public static void registerPlatformEvents(){
        ClientEntityEvents.ENTITY_LOAD.register((entity, clientLevel) -> MeshableEntityTracker.markDirty(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, clientLevel) -> MeshableEntityTracker.remove(entity));
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            MeshableEntityTracker.flush();
            if (minecraft.level != null) {
                MapBatchRenderer.tick(minecraft.level);
                DynamicPartBatch.tick(minecraft.level);
                SignTextBatch.tick(minecraft.level);
            }
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(MapBatchRenderer::submit);
        LevelRenderEvents.COLLECT_SUBMITS.register(DynamicPartBatch::submit);
        LevelRenderEvents.COLLECT_SUBMITS.register(SignTextBatch::submit);
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((minecraft, level) -> {
            MeshableEntityTracker.clear();
            MapBatchRenderer.clear();
            DynamicPartBatch.clear();
            SignTextBatch.clear();
            ItemFrameUtil.clearPendingMaps();
            ModCompat.onWorldLoad();
        });
    }
}

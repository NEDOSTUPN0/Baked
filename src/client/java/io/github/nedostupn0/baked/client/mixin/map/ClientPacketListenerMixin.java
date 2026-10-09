package io.github.nedostupn0.baked.client.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.nedostupn0.baked.client.renderer.map.MapAtlas;
import io.github.nedostupn0.baked.client.util.entity.ItemFrameUtil;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Vanilla re-uploads the whole map texture on every map packet, even decoration-only ones, which stutters in front of
 * map walls. Only color patches are uploaded here.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @WrapOperation(method = "handleMapItemData", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/MapTextureManager;update(Lnet/minecraft/world/level/saveddata/maps/MapId;Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;)V"))
    private void baked$updateOnlyChangedPixels(MapTextureManager manager, MapId id, MapItemSavedData data, Operation<Void> original, @Local(argsOnly = true) ClientboundMapItemDataPacket packet) {
        packet.colorPatch().ifPresent(patch -> {
            original.call(manager, id, data);
            MapAtlas.markDirty(id.id(), patch.startX(), patch.startY(), patch.width(), patch.height());
        });
        ItemFrameUtil.onMapData(id);
    }
}

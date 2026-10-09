package io.github.nedostupn0.baked.client.renderer.blockentity;

import io.github.nedostupn0.baked.client.renderer.sign.SignTextBatch;

import java.util.List;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.sign.ext.SignTextExt;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public class SpecialBlockEntityRenderingManager {
    public static boolean shouldSkipRendering(BlockEntity be) {
        if(!SettingsManager.MOD_TOGGLE.getValue()) return false;
        else if(be instanceof SignBlockEntity signBe){
            return SettingsManager.OPTIMISED_SIGNS.getValue() && (isEmpty(signBe) || SignTextBatch.isBatched(signBe));
        }
        else if(be instanceof BeaconBlockEntity beaconBe){
            return SettingsManager.OPTIMISED_BEACONS.getValue() && beaconBe.getBeamSections().isEmpty();
        }
        else if(be instanceof CampfireBlockEntity campfireBe){
            return SettingsManager.OPTIMISED_CAMPFIRES.getValue() && isContainerEmpty(campfireBe.getItems());
        }
        else if(be instanceof ShelfBlockEntity shelfBe){
            return SettingsManager.OPTIMISED_SHELVES.getValue() && isContainerEmpty(shelfBe.getItems());
        }
        return false;
    }

    private static boolean isContainerEmpty(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) != null && !items.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmpty(SignBlockEntity be){
        boolean shouldFilter = Minecraft.getInstance().isTextFilteringEnabled();
        return !hasMessage(be.getFrontText(), shouldFilter) && !hasMessage(be.getBackText(), shouldFilter);
    }

    private static boolean hasMessage(SignText text, boolean filtered){
        return text != null && ((SignTextExt) text).baked$hasMessage(filtered);
    }
}

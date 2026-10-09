package io.github.nedostupn0.baked.client.config.configscreen;

import io.github.nedostupn0.baked.client.config.Option;
import io.github.nedostupn0.baked.client.config.SettingsManager;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.minecraft.network.chat.Component;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.minecraft.resources.Identifier;

public class SodiumIntegration implements ConfigEntryPoint {

    private final StorageEventHandler handler =  new StorageEventHandler() {
        public void afterSave(){
            SettingsManager.saveSettings();
        }
    };
    
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
        .setNonTintedIcon(Identifier.parse("baked:icon.png"))
        .addPage(builder.createOptionPage()
            .setName(Component.literal("Settings"))
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.general"))
                .addOption(createBooleanOption(builder, SettingsManager.MOD_TOGGLE))
                .addOption(createBooleanOption(builder, SettingsManager.EMF_COMPAT))
                .addOption(createBooleanOption(builder, SettingsManager.MODEL_OPTIMIZATION))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.chests"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_CHESTS))
                .addOption(createBooleanOption(builder, SettingsManager.CHEST_AMBIENT_OCCLUSION))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.banners"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_BANNERS))
                .addOption(createBooleanOption(builder, SettingsManager.BANNER_AMBIENT_OCCLUSION))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.signs"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_SIGNS))
                .addOption(createBooleanOption(builder, SettingsManager.SIGN_TEXT_CULLING))
                .addOption(createBooleanOption(builder, SettingsManager.ATLAS_SIGN_TEXT))
                .addOption(createBooleanOption(builder, SettingsManager.SIGN_TEXT_ALWAYS_OUTLINE))
                .addOption(createBooleanOption(builder, SettingsManager.SIGN_TEXT_RECHECK))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.shulker_boxes"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_SHULKER_BOXES))
                .addOption(createBooleanOption(builder, SettingsManager.SHULKER_BOX_AMBIENT_OCCLUSION))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.skulls"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_SKULLS))
                .addOption(createBooleanOption(builder, SettingsManager.SKULL_AMBIENT_OCCLUSION))
                .addOption(createBooleanOption(builder, SettingsManager.CUSTOM_SKULLS))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.bells"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_BELLS))
                .addOption(createBooleanOption(builder, SettingsManager.BELL_AMBIENT_OCCLUSION))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.decorated_pots"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_DECORATED_POTS))
                .addOption(createBooleanOption(builder, SettingsManager.DECORATED_POT_AMBIENT_OCCLUSION))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.copper_golems"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_COPPER_GOLEMS))
                .addOption(createBooleanOption(builder, SettingsManager.COPPER_GOLEM_AMBIENT_OCCLUSION))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.shelves"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_SHELVES))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.lecterns"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_LECTERNS))
            )
            
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.campfires"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_CAMPFIRES))
            )

            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.beacons"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_BEACONS))
            )

            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.item_frames"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_ITEM_FRAMES))
                .addOption(createBooleanOption(builder, SettingsManager.BATCHED_MAPS))
                .addOption(createBooleanOption(builder, SettingsManager.EXPERIMENTAL_FRAME_EFFECTS))
            )

            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.paintings"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_PAINTINGS))
            )

            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.translatable("baked.config.category.armor_stands"))
                .addOption(createBooleanOption(builder, SettingsManager.OPTIMISED_ARMOR_STANDS))
                .addOption(createBooleanOption(builder, SettingsManager.EXPERIMENTAL_STAND_EFFECTS))
            )
        );
    }

    public OptionBuilder createBooleanOption(ConfigBuilder builder, Option<Boolean> option){
        return builder.createBooleanOption(Identifier.parse("baked:"+option.getId()))
        .setName(Component.translatable(option.getName()))
        .setTooltip(Component.translatable(option.getDescription()))
        .setStorageHandler(this.handler)
        .setBinding(option::setValue, option::getValue)
        .setDefaultValue(option.getDefaultValue())
        .setApplyHook(configState -> option.getRunnable());
    }
}
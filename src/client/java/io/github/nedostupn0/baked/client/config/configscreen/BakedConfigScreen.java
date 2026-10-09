package io.github.nedostupn0.baked.client.config.configscreen;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.*;

import io.github.nedostupn0.baked.client.compat.ModCompat;
import io.github.nedostupn0.baked.client.config.SettingsManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class BakedConfigScreen extends Screen {
    
    private ConfigListWidget list;
    private final Screen parent;

    protected BakedConfigScreen(Screen parent) {
        super(Component.translatable("baked.config.title"));
        this.parent = parent;
    }

    public static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("bakedConfig")
                    .executes(context -> {
                        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(new BakedConfigScreen(null)));
                        return 1;
                    })
            );
        });
    }

    @Override
    protected void init() {
        super.init();
        this.list = new ConfigListWidget(this.minecraft, this.width, this.height - 80, 40, 26);

        if(ModCompat.isIncompatibilityDetected()) list.category(I18n.get("baked.config.warning.incompatibility", ModCompat.getIncompatibleMod())).style(ChatFormatting.UNDERLINE, ChatFormatting.YELLOW).build();

        list.category("baked.config.category.general").build();
        list.button(SettingsManager.MOD_TOGGLE).build();
        list.button(SettingsManager.EMF_COMPAT).build();
        list.button(SettingsManager.MODEL_OPTIMIZATION).build();

        list.category("baked.config.category.chests").build();
        list.button(SettingsManager.OPTIMISED_CHESTS).build();
        list.button(SettingsManager.CHEST_AMBIENT_OCCLUSION).isEnabled(() -> SettingsManager.OPTIMISED_CHESTS.getValue()).build();

        list.category("baked.config.category.banners").build();
        list.button(SettingsManager.OPTIMISED_BANNERS).build();
        list.button(SettingsManager.BANNER_AMBIENT_OCCLUSION).isEnabled(() -> SettingsManager.OPTIMISED_BANNERS.getValue()).build();

        list.category("baked.config.category.signs").build();
        list.button(SettingsManager.OPTIMISED_SIGNS).build();
        list.button(SettingsManager.SIGN_TEXT_CULLING).build();
        list.button(SettingsManager.ATLAS_SIGN_TEXT).build();
        list.button(SettingsManager.SIGN_TEXT_ALWAYS_OUTLINE).build();
        list.button(SettingsManager.SIGN_TEXT_RECHECK).build();

        list.category("baked.config.category.shulker_boxes").build();
        list.button(SettingsManager.OPTIMISED_SHULKER_BOXES).build();
        list.button(SettingsManager.SHULKER_BOX_AMBIENT_OCCLUSION).isEnabled(() -> SettingsManager.OPTIMISED_SHULKER_BOXES.getValue()).build();

        list.category("baked.config.category.skulls").build();
        list.button(SettingsManager.OPTIMISED_SKULLS).build();
        list.button(SettingsManager.SKULL_AMBIENT_OCCLUSION).isEnabled(() -> SettingsManager.OPTIMISED_SKULLS.getValue()).build();
        list.button(SettingsManager.CUSTOM_SKULLS).isEnabled(() -> SettingsManager.OPTIMISED_SKULLS.getValue()).build();

        list.category("baked.config.category.bells").build();
        list.button(SettingsManager.OPTIMISED_BELLS).build();
        list.button(SettingsManager.BELL_AMBIENT_OCCLUSION).isEnabled(() -> SettingsManager.OPTIMISED_BELLS.getValue()).build();

        list.category("baked.config.category.decorated_pots").build();
        list.button(SettingsManager.OPTIMISED_DECORATED_POTS).build();
        list.button(SettingsManager.DECORATED_POT_AMBIENT_OCCLUSION).isEnabled(() -> SettingsManager.OPTIMISED_DECORATED_POTS.getValue()).build();

        list.category("baked.config.category.copper_golems").build();
        list.button(SettingsManager.OPTIMISED_COPPER_GOLEMS).build();
        list.button(SettingsManager.COPPER_GOLEM_AMBIENT_OCCLUSION).isEnabled(() -> SettingsManager.OPTIMISED_COPPER_GOLEMS.getValue()).build();

        list.category("baked.config.category.shelves").build();
        list.button(SettingsManager.OPTIMISED_SHELVES).build();

        list.category("baked.config.category.lecterns").build();
        list.button(SettingsManager.OPTIMISED_LECTERNS).build();

        list.category("baked.config.category.campfires").build();
        list.button(SettingsManager.OPTIMISED_CAMPFIRES).build();

        list.category("baked.config.category.beacons").build();
        list.button(SettingsManager.OPTIMISED_BEACONS).build();

        list.category("baked.config.category.cushions").build();
        list.button(SettingsManager.OPTIMISED_CUSHIONS).build();
        list.button(SettingsManager.CUSHION_AMBIENT_OCCLUSION).build();

        list.category("baked.config.category.item_frames").build();
        list.button(SettingsManager.OPTIMISED_ITEM_FRAMES).build();
        list.button(SettingsManager.BATCHED_MAPS).build();
        list.button(SettingsManager.EXPERIMENTAL_FRAME_EFFECTS).build();

        list.category("baked.config.category.paintings").build();
        list.button(SettingsManager.OPTIMISED_PAINTINGS).build();

        list.category("baked.config.category.armor_stands").build();
        list.button(SettingsManager.OPTIMISED_ARMOR_STANDS).build();
        list.button(SettingsManager.EXPERIMENTAL_STAND_EFFECTS).build();

        Button doneButton = Button.builder(Component.translatable("baked.config.done"), b -> {
            this.minecraft.gui.setScreen(this.parent);
            SettingsManager.saveSettings();
        }).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build();

        this.addRenderableWidget(this.list);
        this.addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
        SettingsManager.saveSettings();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.list.extractRenderState(context, mouseX, mouseY, delta);
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }
}

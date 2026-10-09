package io.github.nedostupn0.baked.client.config.configscreen.entries.builders;

import java.util.function.BooleanSupplier;

import io.github.nedostupn0.baked.Baked;
import io.github.nedostupn0.baked.client.config.Option;
import io.github.nedostupn0.baked.client.config.configscreen.ConfigListWidget;
import io.github.nedostupn0.baked.client.config.configscreen.ConfigListWidget.ButtonEntry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ButtonBuilder extends AbstractEntryBuilder{

    private Button button;
    private Option<?> option;
    private String indent = "";

    public ButtonBuilder(ConfigListWidget list, Option<?> option) {
        this.parent = list;
        this.option = option;
        this.name = option.getName();
    }

    public ButtonBuilder isEnabled(BooleanSupplier supplier) {
        this.isEnabledSupplier = supplier; 
        return this;
    }

    public ButtonBuilder indent(String indent) {
        this.indent = indent; 
        return this;
    }

    public ButtonBuilder indent() {
        this.indent = getDefaultIndent(); 
        return this;
    }

    public void build() {
        button = Button.builder(Component.translatable(Baked.MOD_ID + ".config.value." + option.getValue().toString().toLowerCase()), btn -> {option.setToNextValue();}).bounds(0, 0, 100, 20).build();
        parent.addBuiltEntry(new ButtonEntry(parent, button, option, indent, isEnabledSupplier));
    }
}

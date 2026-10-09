package io.github.nedostupn0.baked.client.config.configscreen.entries.builders;

import java.util.function.BooleanSupplier;

import io.github.nedostupn0.baked.client.config.configscreen.ConfigListWidget;

public abstract class AbstractEntryBuilder{
    protected ConfigListWidget parent;
    protected String name;
    protected BooleanSupplier isEnabledSupplier = () -> true;
    
    protected String getDefaultIndent(){
        return " ⤷  ";
    }
}
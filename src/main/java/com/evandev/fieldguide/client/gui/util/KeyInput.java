package com.evandev.fieldguide.client.gui.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.events.GuiEventListener;

//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
*///?}

public record KeyInput(int key, int scanCode, int modifiers) {
    //? if >=26.1 {
    /*public KeyInput(KeyEvent event) {
        //? if <26.3 {
        this(event.key(), event.scancode(), event.modifiers());
        //?} else {
        /^this(event.key(), event.keycode(), event.modifiers());
        ^///?}
    }

    public KeyEvent event() {
        return new KeyEvent(key, scanCode, modifiers);
    }
    *///?}

    public boolean matches(KeyMapping mapping) {
        //? if <26.1 {
        return mapping.matches(key, scanCode);
        //?} else {
        /*return mapping.matches(event());
        *///?}
    }

    public boolean sendTo(GuiEventListener listener) {
        //? if <26.1 {
        return listener.keyPressed(key, scanCode, modifiers);
        //?} else {
        /*return listener.keyPressed(event());
        *///?}
    }
}

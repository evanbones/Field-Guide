package com.evandev.fieldguide.client.gui.util;

import net.minecraft.client.gui.components.events.GuiEventListener;

//? if >=26.1 {
/*import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
*///?}

public record ClickInput(double x, double y, int button, int modifiers, boolean doubleClick) {
    //? if <26.1 {
    public ClickInput(double x, double y, int button) {
        this(x, y, button, 0, false);
    }
    //?} else {
    /*public ClickInput(MouseButtonEvent event, boolean doubleClick) {
        this(event.x(), event.y(), event.button(), event.modifiers(), doubleClick);
    }

    public MouseButtonEvent event() {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
    }
    *///?}

    public boolean sendTo(GuiEventListener listener) {
        //? if <26.1 {
        return listener.mouseClicked(x, y, button);
        //?} else {
        /*return listener.mouseClicked(event(), doubleClick);
        *///?}
    }
}

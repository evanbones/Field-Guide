package com.evandev.fieldguide.compat.scholar;

import com.evandev.fieldguide.client.gui.widget.BookTextAreaWidget;
import com.evandev.fieldguide.client.gui.widget.BookTextFieldWidget;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.function.Consumer;

public class ScholarCompat {
    public static AbstractWidget createTextArea(Font font, int x, int y, int width, int height, int maxVisibleLines, int textColor, boolean scrollable, int maxCharacters, String initialText, Consumer<String> onChanged) {
        return createTextArea(font, x, y, width, height, maxVisibleLines, 9, textColor, scrollable, maxCharacters, initialText, onChanged, null);
    }

    public static AbstractWidget createTextArea(Font font, int x, int y, int width, int height, int maxVisibleLines, int lineHeight, int textColor, boolean scrollable, int maxCharacters, String initialText, Consumer<String> onChanged) {
        return createTextArea(font, x, y, width, height, maxVisibleLines, lineHeight, textColor, scrollable, maxCharacters, initialText, onChanged, null);
    }

    public static AbstractWidget createTextArea(Font font, int x, int y, int width, int height, int maxVisibleLines, int lineHeight, int textColor, boolean scrollable, int maxCharacters, String initialText, Consumer<String> onChanged, Consumer<String> onSpillover) {
        if (Services.PLATFORM.isModLoaded("scholar")) {
            return ScholarWidgetHelper.createTextArea(font, x, y, width, height, maxVisibleLines, lineHeight, textColor, scrollable, maxCharacters, initialText, onChanged, onSpillover);
        }
        BookTextAreaWidget widget = new BookTextAreaWidget(font, x, y, width, height, maxVisibleLines, lineHeight, textColor, scrollable, maxCharacters, initialText, onChanged);
        if (onSpillover != null) widget.setOnSpillover(onSpillover);
        return widget;
    }

    public static AbstractWidget createTextField(Font font, int x, int y, int width, int height, String initialText, int textColor, int maxTextWidth, int maxCharacters, Consumer<String> onChanged, boolean centered) {
        if (Services.PLATFORM.isModLoaded("scholar")) {
            return ScholarWidgetHelper.createTextField(font, x, y, width, height, initialText, textColor, maxTextWidth, maxCharacters, onChanged, centered);
        }
        return new BookTextFieldWidget(font, x, y, width, height, initialText, textColor, maxTextWidth, maxCharacters, onChanged).setCentered(centered);
    }
}

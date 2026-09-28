package com.evandev.fieldguide.client.gui.widget;

import com.evandev.fieldguide.client.gui.util.GuiCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

//? if >=26.1 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
*///?}

public class BookTextFieldWidget extends AbstractWidget {
    private final Font font;
    private final int textColor;
    private final int highlightColor = 0x550000FF;
    private final Consumer<String> onChanged;
    private final int maxTextWidth;
    private final int maxCharacters;
    private String text;
    private int cursorPos;
    private int selectionPos;
    private boolean centered = false;
    private boolean editable = true;

    //? if <26.1 {
    public BookTextFieldWidget(Font font, int x, int y, int width, int height, String text, int textColor, int maxTextWidth, int maxCharacters, Consumer<String> onChanged) {        super(x, y, width, height, Component.empty());
    //?} else {
    /*public BookTextFieldWidget(Font font, int x, int y, int width, int height, String text, int textColor, int maxTextWidth, int maxCharacters, Consumer<String> onChanged) {
        super(x, y, width, height, Component.empty());
    *///?}
        this.font = font;
        this.textColor = textColor;
        this.text = text == null ? "" : text.substring(0, Math.min(text.length(), maxCharacters));
        this.maxTextWidth = maxTextWidth;
        this.maxCharacters = maxCharacters;
        this.onChanged = onChanged;
        this.cursorPos = this.text.length();
        this.selectionPos = this.cursorPos;
    }

    public BookTextFieldWidget setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text.substring(0, Math.min(text.length(), maxCharacters));
        this.cursorPos = Math.min(this.text.length(), this.cursorPos);
        this.selectionPos = Math.min(this.text.length(), this.selectionPos);
    }

    public void setValue(String value) {
        setText(value);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    private void tryUpdateText(String newText, int newCursorPos, int newSelectionPos) {
        if (newText.length() <= maxCharacters && font.width(newText) <= maxTextWidth) {
            this.text = newText;
            this.cursorPos = newCursorPos;
            this.selectionPos = newSelectionPos;
            this.onChanged.accept(this.text);
        }
    }

    @Override
    //? if <26.1 {
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //?} else {
    /*public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
    *///?}
        if (this.isMouseOver(mouseX, mouseY)) {
            if (this.editable) {
                this.setFocused(true);
                int renderX = this.centered ? this.getX() + (this.width - this.font.width(text)) / 2 : this.getX();
                int relativeX = (int) (mouseX - renderX);
                cursorPos = this.font.plainSubstrByWidth(text, Math.max(0, relativeX)).length();
                if (!GuiCompat.hasShiftDown()) selectionPos = cursorPos;
            }
            return true;
        }
        this.setFocused(false);
        return false;
    }

    @Override
    //? if <26.1 {
    public boolean charTyped(char codePoint, int modifiers) {
    //?} else {
    /*public boolean charTyped(@NonNull CharacterEvent event) {
    *///?}
        if (!this.isFocused() || !this.editable) return false;
        String proposedText;
        int newCursor;
        if (selectionPos != cursorPos) {
            int start = Math.min(cursorPos, selectionPos);
            int end = Math.max(cursorPos, selectionPos);
            //? if <26.1 {
            proposedText = text.substring(0, start) + codePoint + text.substring(end);
            //?} else {
            /*proposedText = text.substring(0, start) + event.codepointAsString() + text.substring(end);
            *///?}
            newCursor = start + 1;
        } else {
            //? if <26.1 {
            proposedText = text.substring(0, cursorPos) + codePoint + text.substring(cursorPos);
            //?} else {
            /*proposedText = text.substring(0, cursorPos) + event.codepointAsString() + text.substring(cursorPos);
            *///?}
            newCursor = cursorPos + 1;
        }

        tryUpdateText(proposedText, newCursor, newCursor);
        return true;
    }

    @Override
    //? if <26.1 {
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?} else {
    /*public boolean keyPressed(@NonNull KeyEvent event) {
    *///?}
        if (!this.isFocused()) return false;

        //? if >=26.1 {
        /*int keyCode = event.key();
        *///?}

        if (this.editable) {
            if (GuiCompat.isSelectAll(keyCode)) {
                selectionPos = 0;
                cursorPos = text.length();
                return true;
            }
            if (GuiCompat.isCopy(keyCode)) {
                if (selectionPos != cursorPos) {
                    int start = Math.min(cursorPos, selectionPos);
                    int end = Math.max(cursorPos, selectionPos);
                    Minecraft.getInstance().keyboardHandler.setClipboard(text.substring(start, end));
                }
                return true;
            }
            if (GuiCompat.isCut(keyCode)) {
                if (selectionPos != cursorPos) {
                    int start = Math.min(cursorPos, selectionPos);
                    int end = Math.max(cursorPos, selectionPos);
                    Minecraft.getInstance().keyboardHandler.setClipboard(text.substring(start, end));
                    deleteSelection();
                }
                return true;
            }
            if (GuiCompat.isPaste(keyCode)) {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (!clipboard.isEmpty()) {
                    int start = Math.min(cursorPos, selectionPos);
                    int end = Math.max(cursorPos, selectionPos);
                    String proposedText = text.substring(0, start) + clipboard + text.substring(end);
                    tryUpdateText(proposedText, start + clipboard.length(), start + clipboard.length());
                }
                return true;
            }

            if (keyCode == InputConstants.KEY_BACKSPACE) {
                if (selectionPos != cursorPos) {
                    deleteSelection();
                } else if (cursorPos > 0) {
                    String proposedText = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                    tryUpdateText(proposedText, cursorPos - 1, cursorPos - 1);
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_DELETE) {
                if (selectionPos != cursorPos) {
                    deleteSelection();
                } else if (cursorPos < text.length()) {
                    String proposedText = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                    tryUpdateText(proposedText, cursorPos, cursorPos);
                }
                return true;
            }
        }
        if (keyCode == InputConstants.KEY_LEFT) {
            if (GuiCompat.hasControlDown()) cursorPos = getWordPosition(text, cursorPos, -1);
            else if (cursorPos > 0) cursorPos--;
            if (!GuiCompat.hasShiftDown()) selectionPos = cursorPos;
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            if (GuiCompat.hasControlDown()) cursorPos = getWordPosition(text, cursorPos, 1);
            else if (cursorPos < text.length()) cursorPos++;
            if (!GuiCompat.hasShiftDown()) selectionPos = cursorPos;
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER || keyCode == InputConstants.KEY_ESCAPE) {
            this.setFocused(false);
            return true;
        }
        return false;
    }

    private void deleteSelection() {
        if (selectionPos != cursorPos) {
            int start = Math.min(cursorPos, selectionPos);
            int end = Math.max(cursorPos, selectionPos);
            String proposedText = text.substring(0, start) + text.substring(end);
            tryUpdateText(proposedText, start, start);
        }
    }

    private int getWordPosition(String text, int cursor, int dir) {
        int pos = cursor;
        if (dir < 0) {
            while (pos > 0 && Character.isWhitespace(text.charAt(pos - 1))) pos--;
            while (pos > 0 && !Character.isWhitespace(text.charAt(pos - 1))) pos--;
        } else {
            int len = text.length();
            while (pos < len && Character.isWhitespace(text.charAt(pos))) pos++;
            while (pos < len && !Character.isWhitespace(text.charAt(pos))) pos++;
        }
        return pos;
    }

    @Override
    //? if <26.1 {
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void extractWidgetRenderState(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    *///?}
        cursorPos = Math.max(0, Math.min(cursorPos, text.length()));
        selectionPos = Math.max(0, Math.min(selectionPos, text.length()));

        int renderX = this.centered ? this.getX() + (this.width - this.font.width(text)) / 2 : this.getX();

        if (this.isFocused() && selectionPos != cursorPos) {
            int start = Math.min(cursorPos, selectionPos);
            int end = Math.max(cursorPos, selectionPos);
            int selStartX = renderX + this.font.width(text.substring(0, start));
            int selEndX = renderX + this.font.width(text.substring(0, end));
            guiGraphics.fill(selStartX, this.getY(), selEndX, this.getY() + this.font.lineHeight, highlightColor);
        }

        guiGraphics.drawString(this.font, text, renderX, this.getY(), textColor, false);

        if (this.isFocused()) {
            int cursorX = renderX + this.font.width(text.substring(0, cursorPos));
            this.renderCursor(guiGraphics, cursorX, this.getY());
        }
    }

    private void renderCursor(GuiGraphics guiGraphics, int x, int y) {
        if ((System.currentTimeMillis() / 400) % 2 == 0) {
            if (cursorPos == text.length()) {
                guiGraphics.drawString(this.font, "_", x, y, ClientConfig.get().getTextCursorColorInt(), false);
            } else {
                int cursorWidth = 1;
                int cursorHeight = this.font.lineHeight;
                guiGraphics.fill(x, y - 1, x + cursorWidth, y - 1 + cursorHeight, ClientConfig.get().getTextCursorColorInt());
            }
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}

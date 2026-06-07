package com.evandev.fieldguide.compat.scholar;

import com.evandev.fieldguide.Constants;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.scholar.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.scholar.client.gui.widget.textbox.display.FormattedStringDisplayCache;
import io.github.mortuusars.scholar.client.gui.widget.textbox.display.HorizontalAlignment;
import io.github.mortuusars.scholar.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.scholar.client.gui.widget.textbox.text.FormattedStringEditor;
import io.github.mortuusars.scholar.client.gui.widget.textbox.text.Formatting;
import io.github.mortuusars.scholar.client.util.Pos2i;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ScholarWidgetHelper {
    public static AbstractWidget createTextArea(Font font, int x, int y, int width, int height, int maxVisibleLines, int lineHeight, int textColor, boolean scrollable, int maxCharacters, String initialText, Consumer<String> onChanged, Consumer<String> onSpillover) {
        ScholarTextBox textBox = new ScholarTextBox(font, x, y, width, height, scrollable, maxVisibleLines, initialText, onChanged);
        textBox.setFontColor(textColor | 0xFF000000);
        textBox.setFontUnfocusedColor(textColor | 0xFF000000);

        Predicate<String> validator = str -> {
            if (str.length() > maxCharacters) return false;
            if (!scrollable) {
                int h = font.wordWrapHeight(str, width) + (str.endsWith("\n") ? font.lineHeight : 0);
                return h <= height;
            }
            return true;
        };

        textBox.getEditor().setValidator(validator);
        return textBox;
    }

    public static AbstractWidget createTextField(Font font, int x, int y, int width, int height, String initialText, int textColor, int maxTextWidth, int maxCharacters, Consumer<String> onChanged, boolean centered) {
        ScholarTextBox textBox = new ScholarTextBox(font, x, y, width, height, false, 1, initialText, onChanged);
        textBox.setFontColor(textColor | 0xFF000000);
        textBox.setFontUnfocusedColor(textColor | 0xFF000000);
        if (centered) textBox.setHorizontalAlignment(HorizontalAlignment.CENTER);

        Predicate<String> validator = str -> str.length() <= maxCharacters && font.width(str) <= maxTextWidth && !str.contains("\n");
        textBox.getEditor().setValidator(validator);
        return textBox;
    }

    private static class ScholarTextBox extends TextBox {
        private final boolean scrollable;
        private final int maxVisibleLines;
        private int scrollOffset = 0;
        private boolean isDraggingScrollbar = false;

        public ScholarTextBox(Font font, int x, int y, int width, int height, boolean scrollable, int maxVisibleLines, String initialText, Consumer<String> onChanged) {
            super(font, x, y, width, height);
            this.scrollable = scrollable;
            this.maxVisibleLines = maxVisibleLines > 0 ? maxVisibleLines : height / font.lineHeight;
            this.setText(FormattedString.parse(initialText));
            this.setOnTextChanged(formattedString -> onChanged.accept(formattedString.toString()));
        }

        private boolean isScrollbarHovered(double mouseX, double mouseY) {
            if (!scrollable) return false;
            int totalLines = getDisplayCache().getLines().size();
            if (totalLines <= maxVisibleLines) return false;
            int scrollbarX = this.getX() + this.width + 2;
            int scrollbarHeight = (maxVisibleLines * font.lineHeight) - 2;
            int hitPadding = 4;
            return mouseX >= scrollbarX - hitPadding && mouseX <= scrollbarX + 2 + hitPadding && mouseY >= this.getY() && mouseY <= this.getY() + scrollbarHeight;
        }

        private void updateScrollFromMouse(double mouseY) {
            int totalLines = getDisplayCache().getLines().size();
            if (totalLines > maxVisibleLines) {
                int scrollbarHeight = (maxVisibleLines * font.lineHeight) - 2;
                int thumbHeight = Math.max(4, (int) ((float) maxVisibleLines / totalLines * scrollbarHeight));
                float progress = (float) (mouseY - this.getY() - (thumbHeight / 2.0f)) / (scrollbarHeight - thumbHeight);
                progress = Math.max(0.0f, Math.min(1.0f, progress));
                scrollOffset = (int) (progress * (totalLines - maxVisibleLines) + 0.5f);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (scrollable && this.isHovered) {
                int totalLines = getDisplayCache().getLines().size();
                if (totalLines > maxVisibleLines) {
                    scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(delta), totalLines - maxVisibleLines));
                    return true;
                }
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (scrollable && button == 0 && isScrollbarHovered(mouseX, mouseY)) {
                isDraggingScrollbar = true;
                updateScrollFromMouse(mouseY);
                return true;
            }

            int scrollPixelOffset = scrollable ? scrollOffset * font.lineHeight : 0;

            if (isHovered && button == InputConstants.MOUSE_BUTTON_RIGHT) {
                int indexAtMousePos = Math.max(0, getDisplayCache().getCharIndexAtPosition(font, (int) (mouseX - getX()), (int) (mouseY + scrollPixelOffset - getY())));
                getEditor().selectWord(indexAtMousePos);
                refreshDisplayCache();
                return true;
            }

            if (isHovered && button == 0) {
                if (this.getFormattingToolbar().mouseClicked(mouseX, mouseY + scrollPixelOffset, button)) {
                    this.refreshDisplayCache();
                    this.onTextChanged().accept(this.getEditor().getString());
                    this.canDrag = false;
                } else {
                    long currentTime = System.currentTimeMillis();
                    int indexAtMousePos = Math.max(0, getDisplayCache().getCharIndexAtPosition(this.font, (int) (mouseX - getX()), (int) (mouseY + scrollPixelOffset - getY())));

                    if (Math.abs(this.lastClickPos.x - (int) mouseX) < 4 && Math.abs(this.lastClickPos.y - (int) mouseY) < 4 && currentTime - this.lastActionTime < 250L) {
                        if (!this.getEditor().isSelecting()) {
                            this.getEditor().selectWord(indexAtMousePos);
                        } else {
                            this.getEditor().selectAll();
                        }
                    } else {
                        this.getEditor().setCursorPos(indexAtMousePos, Screen.hasShiftDown());
                    }

                    this.refreshDisplayCache();
                    this.lastClickPos = new Pos2i((int) mouseX, (int) mouseY);
                    this.lastActionTime = currentTime;
                    this.canDrag = true;
                    this.setFocused(true);
                }
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scrollable && isDraggingScrollbar) {
                updateScrollFromMouse(mouseY);
                return true;
            }
            if (button == 0 && this.canDrag) {
                int scrollPixelOffset = scrollable ? scrollOffset * font.lineHeight : 0;
                int indexAtMousePos = Math.max(0, getDisplayCache().getCharIndexAtPosition(this.font, (int) (mouseX - getX()), (int) (mouseY + scrollPixelOffset - getY())));
                this.getEditor().setCursorPos(indexAtMousePos, true);
                this.refreshDisplayCache();
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (isDraggingScrollbar) {
                isDraggingScrollbar = false;
                return true;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            boolean result = super.keyPressed(keyCode, scanCode, modifiers);
            scrollToCursor();
            return result;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            boolean result = super.charTyped(codePoint, modifiers);
            scrollToCursor();
            return result;
        }

        private void scrollToCursor() {
            if (!scrollable) return;
            Pos2i cursor = getDisplayCache().getCursor();
            int cursorLine = cursor.y / font.lineHeight;
            if (cursorLine < scrollOffset) {
                scrollOffset = cursorLine;
            } else if (cursorLine >= scrollOffset + maxVisibleLines) {
                scrollOffset = cursorLine - maxVisibleLines + 1;
            }
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            FormattedStringDisplayCache displayCache = this.getDisplayCache();

            if (!scrollable) {
                this.renderLines(guiGraphics, mouseX, mouseY, partialTick, displayCache.getLines(), this.getCurrentFontColor());

                int cursorColor = this.getCurrentFontColor();
                Formatting currentFormatting = this.getEditor().getFormattingAtCursor();
                if (currentFormatting.color() != null) {
                    Integer chatColor = currentFormatting.color().asChatFormatting().getColor();
                    if (chatColor != null) {
                        cursorColor = chatColor | 0xFF000000;
                    }
                }

                this.renderCursor(guiGraphics, mouseX, mouseY, partialTick, this.getEditor(), displayCache.getCursor(), cursorColor);
                this.renderSelection(guiGraphics, mouseX, mouseY, partialTick, displayCache.getSelection(), this.getCurrentSelectionColor());
                this.getFormattingToolbar().render(guiGraphics, mouseX, mouseY, partialTick);
                return;
            }

            int totalLines = displayCache.getLines().size();
            scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalLines - maxVisibleLines)));
            int scrollPixelOffset = scrollOffset * font.lineHeight;

            guiGraphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, -scrollPixelOffset, 0);

            this.renderLines(guiGraphics, mouseX, mouseY + scrollPixelOffset, partialTick, displayCache.getLines(), this.getCurrentFontColor());

            int cursorColor = this.getCurrentFontColor();
            Formatting currentFormatting = this.getEditor().getFormattingAtCursor();
            if (currentFormatting.color() != null) {
                Integer chatColor = currentFormatting.color().asChatFormatting().getColor();
                if (chatColor != null) {
                    cursorColor = chatColor | 0xFF000000;
                }
            }

            this.renderCursorScrollable(guiGraphics, mouseX, mouseY + scrollPixelOffset, partialTick, this.getEditor(), displayCache.getCursor(), cursorColor);
            this.renderSelection(guiGraphics, mouseX, mouseY + scrollPixelOffset, partialTick, displayCache.getSelection(), this.getCurrentSelectionColor());

            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, -scrollPixelOffset, 0);
            this.getFormattingToolbar().render(guiGraphics, mouseX, mouseY + scrollPixelOffset, partialTick);
            guiGraphics.pose().popPose();

            if (totalLines > maxVisibleLines) {
                int scrollbarY = this.getY() - 1;
                int scrollbarX = this.getX() + this.width + 1;
                int scrollbarHeight = (maxVisibleLines * font.lineHeight);
                float progress = (float) scrollOffset / (totalLines - maxVisibleLines);
                int thumbHeight = Math.max(4, (int) ((float) maxVisibleLines / totalLines * scrollbarHeight));
                int thumbY = scrollbarY + (int) (progress * (scrollbarHeight - thumbHeight));

                int thumbTextureOffset = 4;
                if (isDraggingScrollbar || isScrollbarHovered(mouseX, mouseY)) {
                    thumbTextureOffset = 8;
                }
                guiGraphics.blitNineSliced(Constants.WIDGETS_TEXTURE, scrollbarX, scrollbarY, 4, scrollbarHeight, 2, 2, 4, 16, 32, 0);
                guiGraphics.blitNineSliced(Constants.WIDGETS_TEXTURE, scrollbarX, thumbY, 4, thumbHeight, 2, 3, 4, 16, 32 + thumbTextureOffset, 0);
            }
        }

        private void renderCursorScrollable(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, FormattedStringEditor editor, Pos2i cursor, int color) {
            if (this.isFocused() && !editor.isSelecting()) {
                if (System.currentTimeMillis() - this.lastActionTime <= 200L || (System.currentTimeMillis() - this.lastActionTime) % 600L <= 300L) {
                    if (editor.isCursorAtEnd()) {
                        guiGraphics.drawString(this.font, "_", this.getX() + cursor.x, this.getY() + cursor.y, color, false);
                    } else {
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(0.0F, 0.0F, 50.0F);
                        RenderSystem.disableBlend();
                        guiGraphics.fill(this.getX() + cursor.x, this.getY() + cursor.y - 1, this.getX() + cursor.x + 1, this.getY() + cursor.y + 9, color);
                        guiGraphics.pose().popPose();
                    }
                }
            }
        }
    }
}
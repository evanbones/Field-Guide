package com.evandev.fieldguide.client.gui.widget;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.gui.util.GuiCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

//? if >=26.1 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
*///?}

public class BookTextAreaWidget extends AbstractWidget {
    private final Font font;
    private final int maxVisibleLines;
    private final int textColor;
    private final boolean scrollable;
    private final int maxCharacters;
    private final Consumer<String> onChanged;
    private final List<Integer> lineStarts = new ArrayList<>();
    private final int lineHeight;
    private String text;
    private int cursorPos;
    private int selectionPos;
    private int scrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private boolean editable = true;
    private Consumer<String> onSpillover;

    public BookTextAreaWidget(Font font, int x, int y, int width, int height, int maxVisibleLines, int textColor, boolean scrollable, int maxCharacters, String initialText, Consumer<String> onChanged) {
        this(font, x, y, width, height, maxVisibleLines, 9, textColor, scrollable, maxCharacters, initialText, onChanged);
    }

    public BookTextAreaWidget(Font font, int x, int y, int width, int height, int maxVisibleLines, int lineHeight, int textColor, boolean scrollable, int maxCharacters, String initialText, Consumer<String> onChanged) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.maxVisibleLines = maxVisibleLines;
        this.lineHeight = lineHeight;
        this.textColor = textColor;
        this.scrollable = scrollable;
        this.maxCharacters = maxCharacters;
        this.text = initialText == null ? "" : initialText.substring(0, Math.min(initialText.length(), maxCharacters));
        this.computeLineStarts();
        this.onChanged = onChanged;
        this.cursorPos = this.text.length();
        this.selectionPos = this.cursorPos;
    }

    public void setOnSpillover(Consumer<String> onSpillover) {
        this.onSpillover = onSpillover;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text.substring(0, Math.min(text.length(), maxCharacters));
        this.computeLineStarts();
        this.cursorPos = Math.min(this.cursorPos, this.text.length());
        this.selectionPos = this.cursorPos;
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

    private void computeLineStarts() {
        lineStarts.clear();
        lineStarts.add(0);
        int lineStart = 0;
        int lastSpace = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lineStart = i + 1;
                lastSpace = -1;
                lineStarts.add(lineStart);
                continue;
            }
            if (c == ' ') {
                lastSpace = i;
            }
            String currentSub = text.substring(lineStart, i + 1);
            if (this.font.width(currentSub) > this.width) {
                if (lastSpace != -1 && lastSpace >= lineStart) {
                    lineStart = lastSpace + 1;
                } else {
                    lineStart = i;
                }
                lastSpace = -1;
                lineStarts.add(lineStart);
            }
        }
    }

    private void updateText(String proposedText, int newCursorPos) {
        if (proposedText.length() > maxCharacters) {
            proposedText = proposedText.substring(0, maxCharacters);
            newCursorPos = Math.min(newCursorPos, maxCharacters);
        }

        if (scrollable) {
            this.text = proposedText;
            this.computeLineStarts();
            this.cursorPos = newCursorPos;
            this.selectionPos = this.cursorPos;
            this.onChanged.accept(this.text);
            scrollToCursor();
            return;
        }

        if (this.font.split(Component.literal(proposedText), this.width).size() <= maxVisibleLines) {
            this.text = proposedText;
            this.computeLineStarts();
            this.cursorPos = newCursorPos;
            this.selectionPos = this.cursorPos;
            this.onChanged.accept(this.text);
        } else if (onSpillover != null) {
            int splitIndex = getSplitIndexForMaxLines(proposedText);
            this.text = proposedText.substring(0, splitIndex);
            this.computeLineStarts();
            String spill = proposedText.substring(splitIndex);

            boolean cursorMovedToSpill = newCursorPos > splitIndex;
            if (!cursorMovedToSpill) {
                this.cursorPos = newCursorPos;
                this.selectionPos = this.cursorPos;
            }
            this.onChanged.accept(this.text);

            if (cursorMovedToSpill) {
                this.setFocused(false);
            }
            onSpillover.accept(spill);
        }
    }

    private int getSplitIndexForMaxLines(String text) {
        int low = 0, high = text.length(), best = 0;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (this.font.split(Component.literal(text.substring(0, mid)), this.width).size() <= maxVisibleLines) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

    @Override
    //? if <26.1 {
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //?} else {
    /*public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
    *///?}
        if (scrollable && button == 0 && isScrollbarHovered(mouseX, mouseY)) {
            isDraggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        if (this.isMouseOver(mouseX, mouseY)) {
            if (this.editable) {
                this.setFocused(true);
                setCursorPosFromMouse(mouseX, mouseY);
            }
            return true;
        }
        this.setFocused(false);
        return false;
    }

    @Override
    //? if <26.1 {
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    //?} else {
    /*public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
    *///?}
        if (isDraggingScrollbar) {
            //? if <26.1 {
            updateScrollFromMouse(mouseY);
            //?} else {
            /*updateScrollFromMouse(event.y());
            *///?}
            return true;
        }
        //? if <26.1 {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        //?} else {
        /*return super.mouseDragged(event, dragX, dragY);
        *///?}
    }

    @Override
    //? if <26.1 {
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //?} else {
    /*public boolean mouseReleased(@NonNull MouseButtonEvent event) {
    *///?}
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        //? if <26.1 {
        return super.mouseReleased(mouseX, mouseY, button);
        //?} else {
        /*return super.mouseReleased(event);
        *///?}
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!scrollable || !this.isMouseOver(mouseX, mouseY)) return false;
        if (lineStarts.size() > maxVisibleLines) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(scrollY), lineStarts.size() - maxVisibleLines));
            return true;
        }
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
        updateText(proposedText, newCursor);
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
                    updateText(text.substring(0, start) + clipboard + text.substring(end), start + clipboard.length());
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_BACKSPACE) {
                if (selectionPos != cursorPos) {
                    deleteSelection();
                } else if (cursorPos > 0) {
                    updateText(text.substring(0, cursorPos - 1) + text.substring(cursorPos), cursorPos - 1);
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_DELETE) {
                if (selectionPos != cursorPos) {
                    deleteSelection();
                } else if (cursorPos < text.length()) {
                    updateText(text.substring(0, cursorPos) + text.substring(cursorPos + 1), cursorPos);
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                int start = Math.min(cursorPos, selectionPos);
                int end = Math.max(cursorPos, selectionPos);
                updateText(text.substring(0, start) + "\n" + text.substring(end), start + 1);
                return true;
            }
        }

        if (keyCode == InputConstants.KEY_LEFT) {
            if (GuiCompat.hasControlDown()) cursorPos = getWordPosition(text, cursorPos, -1);
            else if (cursorPos > 0) cursorPos--;
            if (!GuiCompat.hasShiftDown()) selectionPos = cursorPos;
            scrollToCursor();
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            if (GuiCompat.hasControlDown()) cursorPos = getWordPosition(text, cursorPos, 1);
            else if (cursorPos < text.length()) cursorPos++;
            if (!GuiCompat.hasShiftDown()) selectionPos = cursorPos;
            scrollToCursor();
            return true;
        }
        if (keyCode == InputConstants.KEY_UP) {
            moveCursorLine(-1);
            return true;
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            moveCursorLine(1);
            return true;
        }
        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.setFocused(false);
            return true;
        }
        return false;
    }

    private void deleteSelection() {
        if (selectionPos != cursorPos) {
            int start = Math.min(cursorPos, selectionPos);
            int end = Math.max(cursorPos, selectionPos);
            updateText(text.substring(0, start) + text.substring(end), start);
        }
    }

    private int getWordPosition(String text, int cPos, int dir) {
        int pos = cPos;
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

    private int[] getCoordsForIndex(int index) {
        index = Math.max(0, Math.min(index, text.length()));

        int targetLine = 0;
        for (int i = 0; i < lineStarts.size(); i++) {
            if (index >= lineStarts.get(i)) {
                targetLine = i;
            } else {
                break;
            }
        }

        int startOfTargetLine = lineStarts.get(targetLine);
        String textBeforeCursorOnLine = text.substring(startOfTargetLine, index);
        int cx = this.getX() + this.font.width(textBeforeCursorOnLine);

        return new int[]{cx, this.getY() + targetLine * this.lineHeight, targetLine};
    }

    private void moveCursorLine(int dir) {
        int[] currentCoords = getCoordsForIndex(cursorPos);
        int targetLine = currentCoords[2] + dir;

        if (targetLine < 0) {
            cursorPos = 0;
        } else if (targetLine >= lineStarts.size()) {
            cursorPos = text.length();
        } else {
            int bestPos = 0;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i <= text.length(); i++) {
                int[] coords = getCoordsForIndex(i);
                if (coords[2] == targetLine) {
                    double xDist = Math.abs(coords[0] - currentCoords[0]);
                    if (xDist < bestDist) {
                        bestDist = xDist;
                        bestPos = i;
                    }
                }
            }
            cursorPos = bestPos;
        }
        if (!GuiCompat.hasShiftDown()) selectionPos = cursorPos;
        scrollToCursor();
    }

    private void setCursorPosFromMouse(double mouseX, double mouseY) {
        int relativeY = (int) (mouseY - this.getY());
        int targetLine = (relativeY / this.lineHeight) + scrollOffset;
        targetLine = Math.max(0, targetLine);

        if (targetLine >= lineStarts.size()) {
            cursorPos = text.length();
            if (!GuiCompat.hasShiftDown()) selectionPos = cursorPos;
            return;
        }

        int bestPos = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i <= text.length(); i++) {
            int[] coords = getCoordsForIndex(i);
            if (coords[2] == targetLine) {
                double xDist = Math.abs(coords[0] - mouseX);
                if (xDist < bestDist) {
                    bestDist = xDist;
                    bestPos = i;
                }
            }
        }
        cursorPos = bestPos;
        if (!GuiCompat.hasShiftDown()) selectionPos = cursorPos;
    }

    private void scrollToCursor() {
        if (!scrollable) return;
        int targetLine = getCoordsForIndex(cursorPos)[2];
        if (targetLine < scrollOffset) scrollOffset = targetLine;
        else if (targetLine >= scrollOffset + maxVisibleLines) scrollOffset = targetLine - maxVisibleLines + 1;
    }

    private boolean isScrollbarHovered(double mouseX, double mouseY) {
        if (lineStarts.size() <= maxVisibleLines) return false;
        int scrollbarX = this.getX() + this.width + 2;
        int scrollbarHeight = (maxVisibleLines * this.lineHeight) - 2;
        int hitPadding = 4;
        return mouseX >= scrollbarX - hitPadding && mouseX <= scrollbarX + 2 + hitPadding && mouseY >= this.getY() && mouseY <= this.getY() + scrollbarHeight;
    }

    private void updateScrollFromMouse(double mouseY) {
        int totalLines = lineStarts.size();
        if (totalLines > maxVisibleLines) {
            int scrollbarHeight = (maxVisibleLines * this.lineHeight) - 2;
            int thumbHeight = Math.max(4, (int) ((float) maxVisibleLines / totalLines * scrollbarHeight));
            float progress = (float) (mouseY - this.getY() - (thumbHeight / 2.0f)) / (scrollbarHeight - thumbHeight);
            progress = Math.max(0.0f, Math.min(1.0f, progress));
            scrollOffset = (int) (progress * (totalLines - maxVisibleLines) + 0.5f);
        }
    }

    @Override
    //? if <26.1 {
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void extractWidgetRenderState(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    *///?}
        List<FormattedCharSequence> lines = this.font.split(Component.literal(text), this.width);
        int totalLines = lineStarts.size();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalLines - maxVisibleLines)));

        for (int i = 0; i < maxVisibleLines && (i + scrollOffset) < totalLines; i++) {
            if (i + scrollOffset < lines.size()) {
                guiGraphics.drawString(this.font, lines.get(i + scrollOffset), this.getX(), this.getY() + i * this.lineHeight, textColor, false);
            }
        }

        if (this.isFocused()) {
            if (selectionPos != cursorPos) {
                int start = Math.min(cursorPos, selectionPos);
                int end = Math.max(cursorPos, selectionPos);
                int[] startCoords = getCoordsForIndex(start);
                int[] endCoords = getCoordsForIndex(end);

                for (int line = startCoords[2]; line <= endCoords[2]; line++) {
                    if (scrollable && (line < scrollOffset || line >= scrollOffset + maxVisibleLines)) continue;
                    int visibleLine = line - scrollOffset;
                    int lineStartX = (line == startCoords[2]) ? startCoords[0] : this.getX();
                    int lineEndX = (line == endCoords[2]) ? endCoords[0] : this.getX() + (line < lines.size() ? this.font.width(lines.get(line)) : 0);
                    guiGraphics.fill(lineStartX, this.getY() + visibleLine * this.lineHeight, lineEndX, this.getY() + (visibleLine + 1) * this.lineHeight, 0x550000FF);
                }
            }

            int[] coords = getCoordsForIndex(cursorPos);
            int cursorLine = coords[2];
            if (!scrollable || (cursorLine >= scrollOffset && cursorLine < scrollOffset + maxVisibleLines)) {
                int visibleLine = cursorLine - scrollOffset;
                this.renderCursor(guiGraphics, coords[0], this.getY() + visibleLine * this.lineHeight);
            }
        }

        if (scrollable && totalLines > maxVisibleLines) {
            int scrollbarY = this.getY() - 1;
            int scrollbarX = this.getX() + this.width + 1;
            int scrollbarHeight = (maxVisibleLines * this.lineHeight);
            float progress = (float) scrollOffset / (totalLines - maxVisibleLines);
            int thumbHeight = Math.max(4, (int) ((float) maxVisibleLines / totalLines * scrollbarHeight));
            int thumbY = scrollbarY + (int) (progress * (scrollbarHeight - thumbHeight));

            ResourceLocation trackSprite = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/scrollbar_track");
            ResourceLocation thumbSprite = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/scrollbar_thumb" + (isDraggingScrollbar || isScrollbarHovered(mouseX, mouseY) ? "_hovered" : ""));

            GuiCompat.blitSprite(guiGraphics, trackSprite, scrollbarX, scrollbarY, 4, scrollbarHeight);
            GuiCompat.blitSprite(guiGraphics, thumbSprite, scrollbarX, thumbY, 4, thumbHeight);
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

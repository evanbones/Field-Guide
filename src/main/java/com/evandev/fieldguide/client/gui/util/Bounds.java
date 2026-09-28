package com.evandev.fieldguide.client.gui.util;

public record Bounds(int x, int y, int width, int height) {
    public int left() {
        return x;
    }

    public int right() {
        return x + width;
    }

    public int top() {
        return y;
    }

    public int bottom() {
        return y + height;
    }

    public int x_center() {
        return x + width / 2;
    }

    public int y_center() {
        return y + height / 2;
    }

    public boolean contains(int x, int y) {
        return x >= left() && x < right() && y >= top() && y < bottom();
    }

    public static boolean isMouseOver(double mouseX, double mouseY, int posX, int posY, int width, int height) {
        return mouseX >= posX && mouseX < posX + width && mouseY >= posY && mouseY < posY + height;
    }
}

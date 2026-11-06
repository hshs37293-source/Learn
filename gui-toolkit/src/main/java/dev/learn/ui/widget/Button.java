package dev.learn.ui.widget;

import dev.learn.ui.draw.DrawCommand;
import dev.learn.ui.draw.DrawList;
import dev.learn.ui.draw.UiBackend;
import dev.learn.ui.theme.Theme;

public final class Button extends Widget {
    public String text;
    public Runnable onClick = () -> {};
    private boolean hover, pressed;

    public Button(String text) { this.text = text; }

    @Override public void layout(UiBackend b, Theme t) {
        w = b.textWidth(text, t.fontSize()) + 2 * t.padding() * 1.5f;
        h = t.fontSize() + 2 * t.padding();
    }

    @Override protected void drawSelf(DrawList out, Theme t) {
        int fill = pressed ? t.accent() : hover ? t.surfaceHover() : t.surface();
        out.add(new DrawCommand.RoundedRect(absX(), absY(), w, h, t.radius(), fill, 1, t.surfaceHover()));
        out.add(new DrawCommand.Text(text, absX() + t.padding() * 1.5f, absY() + t.padding(), t.fontSize(), t.text()));
    }

    @Override public boolean onMouseMove(float px, float py) { hover = contains(px, py); return false; }
    @Override public boolean onMouseDown(float px, float py, int btn) {
        if (btn == 0 && contains(px, py)) { pressed = true; return true; }
        return false;
    }
    @Override public boolean onMouseUp(float px, float py, int btn) {
        boolean fire = pressed && btn == 0 && contains(px, py);
        pressed = false;
        if (fire) onClick.run();
        return fire;
    }
}

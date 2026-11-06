package dev.learn.ui.widget;

import dev.learn.ui.draw.DrawCommand;
import dev.learn.ui.draw.DrawList;
import dev.learn.ui.draw.UiBackend;
import dev.learn.ui.theme.Theme;

public final class Label extends Widget {
    public String text;
    public Label(String text) { this.text = text; }

    @Override public void layout(UiBackend b, Theme t) { w = b.textWidth(text, t.fontSize()); h = t.fontSize() + 2; }

    @Override protected void drawSelf(DrawList out, Theme t) {
        out.add(new DrawCommand.Text(text, absX(), absY(), t.fontSize(), t.text()));
    }
}

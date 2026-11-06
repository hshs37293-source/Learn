package dev.mcgui.widget;

import dev.mcgui.gfx.CornerRadii;
import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.input.InputEvent;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;

/** Przewijany widok jednego dziecka (pionowo). Zawartość jest przycinana, jest pasek przewijania. */
public class ScrollView extends Container {
    private float offset;
    private float contentHeight;
    public float scrollStep = 16f;
    private boolean draggingBar;
    private float dragStartY, dragStartOffset;

    @SuppressWarnings("this-escape")
    public ScrollView(Widget content) { super("scrollview"); add(content); }

    public float offset() { return offset; }
    public float maxOffset() { return Math.max(0, contentHeight - bounds.h()); }
    public void setOffset(float o) { offset = clamp(o, 0, maxOffset()); }

    private Widget content() { return children().get(0); }

    @Override protected Size measureContent(Size avail, TextMeasurer tm) {
        Size s = content().layoutMeasure(new Size(avail.w(), Float.MAX_VALUE / 4), tm);
        ResolvedStyle st = layoutStyle();
        return applyPref(new Size(s.w(), Math.min(s.h(), 120)), st);
    }

    @Override public void arrange(Rect finalBounds) {
        this.bounds = finalBounds;
        Widget c = content();
        contentHeight = Math.max(c.desired().h(), finalBounds.h());
        setOffset(offset);
        float barW = maxOffset() > 0 ? 4 : 0;
        c.arrange(new Rect(finalBounds.x(), finalBounds.y() - offset, finalBounds.w() - barW, contentHeight));
    }

    @Override protected void drawChildren(UiGraphics g) {
        g.pushClip(bounds, CornerRadii.NONE);
        super.drawChildren(g);
        g.popClip();
    }

    @Override protected void drawOverlay(UiGraphics g) {
        if (maxOffset() <= 0) return;
        ResolvedStyle bar = theme().resolve("scrollbar", states);
        Rect thumb = thumbRect();
        g.box(thumb, bar.box());
    }

    private Rect thumbRect() {
        float ratio = bounds.h() / contentHeight;
        float th = Math.max(12, bounds.h() * ratio);
        float ty = bounds.y() + (maxOffset() == 0 ? 0 : (offset / maxOffset()) * (bounds.h() - th));
        return new Rect(bounds.right() - 4, ty, 3, th);
    }

    @Override public Widget hitTest(float x, float y) {
        Widget w = super.hitTest(x, y);
        return w;
    }

    @Override public boolean onEvent(InputEvent e) {
        if (e instanceof InputEvent.Scroll s && bounds.contains(s.x(), s.y())) {
            float before = offset;
            setOffset(offset - s.dy() * scrollStep);
            return offset != before;
        }
        if (e instanceof InputEvent.MouseDown d && d.button() == InputEvent.BUTTON_LEFT && maxOffset() > 0) {
            Rect t = thumbRect();
            if (new Rect(t.x() - 2, t.y(), t.w() + 4, t.h()).contains(d.x(), d.y())) {
                draggingBar = true; dragStartY = d.y(); dragStartOffset = offset; return true;
            }
        }
        if (e instanceof InputEvent.MouseMove m && draggingBar) {
            float trackSpace = Math.max(1, bounds.h() - thumbRect().h());
            setOffset(dragStartOffset + (m.y() - dragStartY) / trackSpace * maxOffset());
            return true;
        }
        if (e instanceof InputEvent.MouseUp) { boolean was = draggingBar; draggingBar = false; return was; }
        return false;
    }
}

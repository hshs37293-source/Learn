package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;

/** Widget z dziećmi. Rysuje własne tło (jeśli styl ma wypełnienie/obramowanie/cień), potem dzieci. */
public abstract class Container extends Widget {
    protected Container(String role) { super(role); }

    public <T extends Widget> T add(T w) { addChild(w); return w; }
    public void remove(Widget w) { removeChild(w); }
    public void clear() { clearChildren(); }

    @Override protected void drawSelf(UiGraphics g) {
        ResolvedStyle st = style(g.nowMillis());
        if (st.box().fill().isVisible() || st.box().border().isVisible() || !st.box().shadows().isEmpty()) {
            boolean layered = !st.layer().isIdentity();
            if (layered) g.pushLayer(bounds, st.layer());
            g.box(bounds, st.box());
            if (layered) g.popLayer();
        }
    }

    @Override public Size measure(Size available, TextMeasurer tm) { return measureContent(available, tm); }
    protected abstract Size measureContent(Size available, TextMeasurer tm);

    protected Rect contentBox(ResolvedStyle st) { return bounds.inset(st.padding()); }
}

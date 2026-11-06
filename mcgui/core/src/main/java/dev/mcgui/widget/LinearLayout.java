package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.layout.Align;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;

import java.util.ArrayList;
import java.util.List;

/** Układ liniowy (kolumna lub wiersz) z odstępem, wagami i wyrównaniem. */
public class LinearLayout extends Container {
    public enum Axis { HORIZONTAL, VERTICAL }
    public enum Main { START, CENTER, END, SPACE_BETWEEN }

    public final Axis axis;
    public float gap = 4f;
    public Main mainAlign = Main.START;
    public Align crossAlign = Align.STRETCH;

    public LinearLayout(Axis axis, String role) { super(role); this.axis = axis; }

    private boolean horiz() { return axis == Axis.HORIZONTAL; }

    private List<Widget> visibleChildren() {
        List<Widget> l = new ArrayList<>();
        for (Widget c : children()) if (c.visible) l.add(c);
        return l;
    }

    @Override protected Size measureContent(Size avail, TextMeasurer tm) {
        ResolvedStyle st = layoutStyle();
        float padW = st.padding().horizontal(), padH = st.padding().vertical();
        float main = 0, cross = 0;
        List<Widget> kids = visibleChildren();
        for (Widget c : kids) {
            Size s = c.layoutMeasure(new Size(Math.max(0, avail.w() - padW), Math.max(0, avail.h() - padH)), tm);
            float cm = horiz() ? s.w() + c.margin.horizontal() : s.h() + c.margin.vertical();
            float cc = horiz() ? s.h() + c.margin.vertical() : s.w() + c.margin.horizontal();
            main += cm; cross = Math.max(cross, cc);
        }
        if (kids.size() > 1) main += gap * (kids.size() - 1);
        Size content = horiz() ? new Size(main + padW, cross + padH) : new Size(cross + padW, main + padH);
        return applyPref(content, st);
    }

    @Override public void arrange(Rect finalBounds) {
        super.arrange(finalBounds);
        ResolvedStyle st = layoutStyle();
        Rect box = finalBounds.inset(st.padding());
        List<Widget> kids = visibleChildren();
        int n = kids.size();
        if (n == 0) return;

        float availMain = horiz() ? box.w() : box.h();
        float availCross = horiz() ? box.h() : box.w();
        float[] mainSize = new float[n];
        float[] crossSize = new float[n];
        float used = gap * Math.max(0, n - 1), totalWeight = 0;
        for (int i = 0; i < n; i++) {
            Widget c = kids.get(i);
            Size s = c.desired();
            mainSize[i] = horiz() ? s.w() : s.h();
            crossSize[i] = horiz() ? s.h() : s.w();
            used += mainSize[i] + (horiz() ? c.margin.horizontal() : c.margin.vertical());
            totalWeight += c.weight;
        }
        float free = availMain - used;
        if (free > 0 && totalWeight > 0) {
            for (int i = 0; i < n; i++) mainSize[i] += free * (kids.get(i).weight / totalWeight);
            free = 0;
        }
        float pos = switch (mainAlign) {
            case START -> 0;
            case CENTER -> Math.max(0, free) / 2f;
            case END -> Math.max(0, free);
            case SPACE_BETWEEN -> 0;
        };
        float extraGap = (mainAlign == Main.SPACE_BETWEEN && n > 1 && free > 0) ? free / (n - 1) : 0;

        for (int i = 0; i < n; i++) {
            Widget c = kids.get(i);
            Align a = c.align != null ? c.align : crossAlign;
            float mBefore = horiz() ? c.margin.left() : c.margin.top();
            float mAfter = horiz() ? c.margin.right() : c.margin.bottom();
            float cMarginCross = horiz() ? c.margin.vertical() : c.margin.horizontal();
            float cs = a == Align.STRETCH ? Math.max(0, availCross - cMarginCross) : crossSize[i];
            float crossOff = switch (a) {
                case START, STRETCH -> 0;
                case CENTER -> (availCross - cs - cMarginCross) / 2f;
                case END -> availCross - cs - cMarginCross;
            };
            float crossBefore = horiz() ? c.margin.top() : c.margin.left();
            float x, y, w, h;
            if (horiz()) {
                x = box.x() + pos + mBefore; y = box.y() + crossOff + crossBefore; w = mainSize[i]; h = cs;
            } else {
                x = box.x() + crossOff + crossBefore; y = box.y() + pos + mBefore; w = cs; h = mainSize[i];
            }
            c.arrange(new Rect(x, y, w, h));
            pos += mBefore + mainSize[i] + mAfter + gap + extraGap;
        }
    }

}

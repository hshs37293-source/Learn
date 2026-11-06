package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;

import java.util.ArrayList;
import java.util.List;

/** Siatka o stałej liczbie kolumn; kolumny równej szerokości, wiersze wg najwyższego elementu. */
public class Grid extends Container {
    public final int columns;
    public float gapX = 4, gapY = 4;

    public Grid(int columns) { super("grid"); this.columns = Math.max(1, columns); }

    private List<Widget> kids() {
        List<Widget> l = new ArrayList<>();
        for (Widget c : children()) if (c.visible) l.add(c);
        return l;
    }

    @Override protected Size measureContent(Size avail, TextMeasurer tm) {
        ResolvedStyle st = layoutStyle();
        List<Widget> k = kids();
        float cellW = 0;
        float[] rowH = new float[(k.size() + columns - 1) / columns];
        for (int i = 0; i < k.size(); i++) {
            Size s = k.get(i).layoutMeasure(avail, tm);
            cellW = Math.max(cellW, s.w() + k.get(i).margin.horizontal());
            rowH[i / columns] = Math.max(rowH[i / columns], s.h() + k.get(i).margin.vertical());
        }
        float h = 0;
        for (float r : rowH) h += r;
        h += Math.max(0, rowH.length - 1) * gapY;
        float w = cellW * columns + (columns - 1) * gapX;
        return applyPref(new Size(w + st.padding().horizontal(), h + st.padding().vertical()), st);
    }

    @Override public void arrange(Rect finalBounds) {
        super.arrange(finalBounds);
        Rect box = finalBounds.inset(layoutStyle().padding());
        List<Widget> k = kids();
        float cellW = (box.w() - (columns - 1) * gapX) / columns;
        float y = box.y();
        for (int row = 0; row * columns < k.size(); row++) {
            float rowH = 0;
            for (int col = 0; col < columns && row * columns + col < k.size(); col++) {
                Widget c = k.get(row * columns + col);
                rowH = Math.max(rowH, c.desired().h() + c.margin.vertical());
            }
            for (int col = 0; col < columns && row * columns + col < k.size(); col++) {
                Widget c = k.get(row * columns + col);
                float x = box.x() + col * (cellW + gapX);
                c.arrange(new Rect(x + c.margin.left(), y + c.margin.top(),
                    cellW - c.margin.horizontal(), rowH - c.margin.vertical()));
            }
            y += rowH + gapY;
        }
    }
}

package dev.mcgui.math;

/** Prostokąt w jednostkach logicznych GUI (x, y = lewy górny róg). */
public record Rect(float x, float y, float w, float h) {
    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    public static Rect of(float x, float y, float w, float h) { return new Rect(x, y, w, h); }
    public float right()   { return x + w; }
    public float bottom()  { return y + h; }
    public float centerX() { return x + w * 0.5f; }
    public float centerY() { return y + h * 0.5f; }
    public boolean isEmpty() { return w <= 0 || h <= 0; }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    public Rect translate(float dx, float dy) { return new Rect(x + dx, y + dy, w, h); }

    public Rect inset(float l, float t, float r, float b) {
        return new Rect(x + l, y + t, Math.max(0, w - l - r), Math.max(0, h - t - b));
    }
    public Rect inset(float all) { return inset(all, all, all, all); }
    public Rect inset(Insets i) { return inset(i.left(), i.top(), i.right(), i.bottom()); }

    public Rect expand(float all) { return new Rect(x - all, y - all, w + 2 * all, h + 2 * all); }

    /** Część wspólna; gdy brak, zwraca prostokąt o zerowym rozmiarze. */
    public Rect intersect(Rect o) {
        float nx = Math.max(x, o.x), ny = Math.max(y, o.y);
        float nr = Math.min(right(), o.right()), nb = Math.min(bottom(), o.bottom());
        return new Rect(nx, ny, Math.max(0, nr - nx), Math.max(0, nb - ny));
    }

    public Rect union(Rect o) {
        float nx = Math.min(x, o.x), ny = Math.min(y, o.y);
        float nr = Math.max(right(), o.right()), nb = Math.max(bottom(), o.bottom());
        return new Rect(nx, ny, nr - nx, nb - ny);
    }

    /** Zaokrągla do pikseli fizycznych przy danej skali (rogi, nie przyrosty). */
    public Rect snapToPixels(float scale) {
        float x0 = Math.round(x * scale) / scale, y0 = Math.round(y * scale) / scale;
        float x1 = Math.round(right() * scale) / scale, y1 = Math.round(bottom() * scale) / scale;
        return new Rect(x0, y0, x1 - x0, y1 - y0);
    }
}

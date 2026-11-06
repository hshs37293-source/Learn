package dev.mcgui.gfx;

/** Kolory jako spakowane int ARGB (0xAARRGGBB), tak jak w Minecraft 1.21.6+. */
public final class Color {
    private Color() {}

    public static final int TRANSPARENT = 0x00000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }
    public static int rgb(int r, int g, int b) { return argb(255, r, g, b); }

    public static int a(int c) { return c >>> 24; }
    public static int r(int c) { return (c >> 16) & 0xFF; }
    public static int g(int c) { return (c >> 8) & 0xFF; }
    public static int b(int c) { return c & 0xFF; }

    public static int withAlpha(int c, int a) { return (clamp(a) << 24) | (c & 0xFFFFFF); }

    /** Mnoży alfę koloru przez współczynnik 0..1. */
    public static int multiplyAlpha(int c, float f) {
        return withAlpha(c, Math.round(a(c) * Math.max(0f, Math.min(1f, f))));
    }

    /** Liniowa interpolacja kanałów (bez premultiplikacji). */
    public static int mix(int c0, int c1, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return argb(
            Math.round(a(c0) + (a(c1) - a(c0)) * t),
            Math.round(r(c0) + (r(c1) - r(c0)) * t),
            Math.round(g(c0) + (g(c1) - g(c0)) * t),
            Math.round(b(c0) + (b(c1) - b(c0)) * t));
    }

    /** Parsuje "#rgb", "#rrggbb" lub "#rrggbbaa" (alfa na końcu, jak w CSS). */
    public static int parse(String s) {
        if (s == null || !s.startsWith("#")) throw new IllegalArgumentException("Kolor musi zaczynać się od #: " + s);
        String h = s.substring(1);
        switch (h.length()) {
            case 3: {
                int r = Integer.parseInt(h.substring(0, 1), 16), g = Integer.parseInt(h.substring(1, 2), 16),
                    b = Integer.parseInt(h.substring(2, 3), 16);
                return rgb(r * 17, g * 17, b * 17);
            }
            case 6: return 0xFF000000 | Integer.parseInt(h, 16);
            case 8: {
                int rgb = Integer.parseInt(h.substring(0, 6), 16);
                int a = Integer.parseInt(h.substring(6, 8), 16);
                return (a << 24) | rgb;
            }
            default: throw new IllegalArgumentException("Zła długość koloru: " + s);
        }
    }

    public static String toHex(int c) {
        return String.format("#%02x%02x%02x%02x", r(c), g(c), b(c), a(c));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}

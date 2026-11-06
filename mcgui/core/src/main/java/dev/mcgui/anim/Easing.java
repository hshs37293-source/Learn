package dev.mcgui.anim;

import java.util.function.DoubleUnaryOperator;

/** Funkcje łagodzenia czasu t ∈ [0,1] → [0,1]. */
public enum Easing {
    LINEAR(t -> t),
    EASE_IN(t -> t * t * t),
    EASE_OUT(t -> 1 - Math.pow(1 - t, 3)),
    EASE_IN_OUT(t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2),
    /** Odpowiednik CSS "ease". */
    EASE(t -> cubicBezier(0.25, 0.1, 0.25, 1.0, t)),
    BACK_OUT(t -> { double c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2); }),
    SMOOTHSTEP(t -> t * t * (3 - 2 * t));

    private final DoubleUnaryOperator fn;
    Easing(DoubleUnaryOperator fn) { this.fn = fn; }

    public float apply(float t) {
        double c = Math.max(0, Math.min(1, t));
        return (float) fn.applyAsDouble(c);
    }

    public static Easing parse(String s) {
        if (s == null) return EASE;
        return switch (s.toLowerCase().replace('-', '_')) {
            case "linear" -> LINEAR;
            case "ease_in", "easein" -> EASE_IN;
            case "ease_out", "easeout" -> EASE_OUT;
            case "ease_in_out", "easeinout" -> EASE_IN_OUT;
            case "back_out", "backout" -> BACK_OUT;
            case "smoothstep" -> SMOOTHSTEP;
            default -> EASE;
        };
    }

    /** Krzywa Béziera jak w CSS: punkty kontrolne (x1,y1), (x2,y2); rozwiązuje x(t)=x metodą Newtona/bisekcji. */
    static double cubicBezier(double x1, double y1, double x2, double y2, double x) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;
        double lo = 0, hi = 1, t = x;
        for (int i = 0; i < 24; i++) {
            double cx = bez(x1, x2, t);
            if (Math.abs(cx - x) < 1e-6) break;
            if (cx < x) lo = t; else hi = t;
            t = (lo + hi) / 2;
        }
        return bez(y1, y2, t);
    }

    private static double bez(double a, double b, double t) {
        double u = 1 - t;
        return 3 * u * u * t * a + 3 * u * t * t * b + t * t * t;
    }
}

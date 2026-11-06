package dev.mcgui.gfx;

import dev.mcgui.math.Rect;

/**
 * Kontrakt rysowania 2D, na którym opierają się wszystkie widgety.
 * Nie zawiera żadnych typów Minecrafta, więc ten sam kod działa na każdej linii gry.
 * Wszystkie metody to POLECENIA (model extract → draw): nic nie jest rysowane natychmiast.
 * Współrzędne są w jednostkach logicznych; {@link #scale()} zwraca skalę do pikseli fizycznych.
 */
public interface UiGraphics extends TextMeasurer {
    /** Prostokąt ze stylem (wypełnienie, rogi, obramowanie, cienie). */
    void box(Rect r, BoxStyle style);

    /** Tekst z lewym-górnym rogiem w (x, y). Wyrównanie względem {@code width} (0 = bez wyrównania). */
    void text(CharSequence text, float x, float y, float width, TextStyle style);

    /** Obraz w prostokącie {@code dst}; {@code src} = region tekstury w pikselach (null = cała). */
    void image(TextureRef tex, Rect dst, Rect src, ImageStyle style);

    /** Dowolna ścieżka wektorowa. */
    void path(Path path, PathStyle style);

    /** Odcinek (wygodny skrót do ścieżki). */
    void line(float x0, float y0, float x1, float y1, float width, int argb);

    // --- przycinanie ---
    void pushClip(Rect r, CornerRadii radii);
    void popClip();

    // --- transformacje ---
    void pushTranslate(float dx, float dy);
    void pushScale(float sx, float sy, float pivotX, float pivotY);
    void pushRotate(float radians, float pivotX, float pivotY);
    void popTransform();

    // --- warstwy (kompozycja) ---
    void pushLayer(Rect bounds, LayerStyle style);
    void popLayer();

    /** Wyjście awaryjne: własny shader w prostokącie. */
    void custom(Rect r, ShaderEffect effect);

    /** Współczynnik pikseli fizycznych na jednostkę logiczną (np. 2.0 przy GUI scale 2). */
    float scale();

    /** Czas rzeczywisty w milisekundach (do animacji). */
    long nowMillis();

    default void text(CharSequence text, float x, float y, TextStyle style) { text(text, x, y, 0, style); }
    default void clip(Rect r) { pushClip(r, CornerRadii.NONE); }
}

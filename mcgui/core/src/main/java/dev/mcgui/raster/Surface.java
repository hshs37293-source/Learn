package dev.mcgui.raster;

import dev.mcgui.gfx.ImageStyle;
import dev.mcgui.gfx.TextureRef;
import dev.mcgui.math.Rect;

/**
 * Najprostsza powierzchnia rysowania — to JEDYNE, co adapter gry musi zaimplementować,
 * aby dostać pełne {@link dev.mcgui.gfx.UiGraphics} przez {@link SimpleBackend}.
 * Wszystkie współrzędne są w PIKSELACH FIZYCZNYCH (adapter ustawia macierz 1/guiScale).
 */
public interface Surface {
    void fillRect(float x0, float y0, float x1, float y1, int argb);

    /** Gradient pionowy (góra → dół). */
    void fillGradientV(float x0, float y0, float x1, float y1, int topArgb, int bottomArgb);

    /** Tekst z lewym-górnym rogiem w (x,y); {@code scale} = mnożnik rozmiaru fontu gry. */
    void drawText(CharSequence text, float x, float y, int argb, boolean shadow, float scale);
    float textWidth(CharSequence text, float scale);
    float lineHeight(float scale);

    /** Obraz: {@code src} w pikselach tekstury (null = cała), {@code dst} w pikselach ekranu. */
    void drawImage(TextureRef tex, Rect dst, Rect src, ImageStyle style, int tintArgb, float texW, float texH);

    void pushClip(float x0, float y0, float x1, float y1);
    void popClip();

    void pushMatrix();
    void popMatrix();
    void translate(float dx, float dy);
    void scale(float sx, float sy);
    void rotateAbout(float radians, float px, float py);

    /** Czy powierzchnia umie rysować elementy SDF (gładkie krawędzie shaderem). Domyślnie nie → paski. */
    default boolean supportsSdf() { return false; }

    /** Rysuje element SDF (tylko gdy {@link #supportsSdf()}). */
    default void drawSdf(SdfElement element) {}

    /** Mnożnik GUI (piksele fizyczne na jednostkę logiczną). */
    float guiScale();
    long nowMillis();
}

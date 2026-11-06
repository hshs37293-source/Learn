package dev.learn.ui.draw;

import dev.learn.ui.math.Mat4;

/**
 * Wszystko, co toolkit potrafi narysować, jako dane. Backend (1.21.11 / 26.x) tylko tłumaczy te
 * polecenia na wywołania silnika. Dzięki temu nowe GUI nie dotyka klas Minecrafta.
 * Kolory: ARGB. Współrzędne: piksele fizyczne okna.
 */
public sealed interface DrawCommand {
    record RoundedRect(float x, float y, float w, float h, float radius, int fill,
                       float borderWidth, int borderColor) implements DrawCommand {}
    /** Cień: rozmyty zaokrąglony prostokąt (blur w shaderze, nie w CPU). */
    record Shadow(float x, float y, float w, float h, float radius, float blur, int color) implements DrawCommand {}
    record Text(String text, float x, float y, float size, int color) implements DrawCommand {}
    record Image(String textureId, float x, float y, float w, float h,
                 float u0, float v0, float u1, float v1, int tint) implements DrawCommand {}
    record PushClip(float x, float y, float w, float h) implements DrawCommand {}
    record PopClip() implements DrawCommand {}
    /** Rozmycie tego, co jest pod prostokątem (szkło / frosted glass). */
    record BackdropBlur(float x, float y, float w, float h, float radius, float cornerRadius) implements DrawCommand {}
    /** Siatka 3D w prostokącie GUI (render-to-texture) lub w świecie, zależnie od {@link Space}. */
    record Mesh3D(String meshId, Mat4 model, Mat4 view, Mat4 projection, Space space) implements DrawCommand {}
    /**
     * Wyjście awaryjne: backend sam rysuje coś, czego powyższe nie opisują (własny shader, własny
     * render pass). To gwarantuje, że toolkit nigdy nie jest „sufitem" możliwości.
     */
    record Custom(String backendKey, Object payload) implements DrawCommand {}

    enum Space { GUI_RECT, WORLD }
}

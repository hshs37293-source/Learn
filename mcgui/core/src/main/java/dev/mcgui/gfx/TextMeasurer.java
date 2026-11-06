package dev.mcgui.gfx;

import dev.mcgui.math.Size;

/** Pomiar tekstu dostarczany przez backend (zależy od fontu). Potrzebny do layoutu. */
public interface TextMeasurer {
    Size measure(CharSequence text, TextStyle style);

    /** Przybliżony pomiar bez fontu: stała szerokość znaku (do testów i trybu bez gry). */
    static TextMeasurer monospace() {
        return (text, style) -> new Size(text.length() * style.size() * 0.6f, style.size() * 1.2f);
    }
}

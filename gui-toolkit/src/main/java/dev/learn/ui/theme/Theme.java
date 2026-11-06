package dev.learn.ui.theme;

/** Motyw jako dane (tokeny), podmienialny w locie. Kolory ARGB. */
public record Theme(int background, int surface, int surfaceHover, int accent, int text,
                    int textMuted, float radius, float padding, float fontSize) {
    public static Theme dark() {
        return new Theme(0xF0101114, 0xFF1B1D22, 0xFF262A31, 0xFF5B8CFF, 0xFFE8EAF0,
                0xFF8A90A0, 10f, 8f, 9f);
    }
}

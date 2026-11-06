package dev.mcgui.gfx;

/** Odwołanie do fontu po identyfikatorze; backend decyduje, jak go wczytać. "default" = font gry. */
public record FontRef(String id) {
    public static final FontRef DEFAULT = new FontRef("default");
    public static FontRef of(String id) { return new FontRef(id); }
}

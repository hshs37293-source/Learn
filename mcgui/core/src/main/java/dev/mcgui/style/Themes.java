package dev.mcgui.style;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Wbudowane motywy (zasoby w JAR-ze) oraz ładowanie z napisu. */
public final class Themes {
    private Themes() {}

    public static final String FLAT = "flat", GLASS = "glass", NEON = "neon", PIXEL = "pixel";

    /** Ładuje wbudowany motyw o danej nazwie. */
    public static Theme builtin(String name) {
        String path = "/dev/mcgui/themes/" + name + ".json";
        try (InputStream in = Themes.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("Brak wbudowanego motywu: " + name);
            return ThemeLoader.load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Nie można wczytać motywu " + name, e);
        }
    }

    public static Theme fromJson(String json) { return ThemeLoader.load(json); }
}

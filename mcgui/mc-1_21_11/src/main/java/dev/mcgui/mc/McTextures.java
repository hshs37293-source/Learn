package dev.mcgui.mc;

import dev.mcgui.gfx.TextureRef;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Rejestr: {@link TextureRef} (id tekstowe z rdzenia) → {@link Identifier} + rozmiar tekstury. */
public final class McTextures {
    public record Entry(Identifier id, int width, int height) {}

    private static final Map<String, Entry> MAP = new HashMap<>();

    private McTextures() {}

    /** Rejestruje teksturę, np. register("icon", "mymod:textures/gui/icon.png", 16, 16). */
    public static TextureRef register(String ref, String identifier, int width, int height) {
        int colon = identifier.indexOf(':');
        Identifier id = colon < 0 ? Identifier.withDefaultNamespace(identifier)
            : Identifier.fromNamespaceAndPath(identifier.substring(0, colon), identifier.substring(colon + 1));
        MAP.put(ref, new Entry(id, width, height));
        return TextureRef.of(ref);
    }

    public static Entry get(TextureRef ref) { return MAP.get(ref.id()); }
}

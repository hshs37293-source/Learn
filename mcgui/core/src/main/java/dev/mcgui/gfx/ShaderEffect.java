package dev.mcgui.gfx;

import java.util.Map;

/**
 * Wyjście awaryjne: własny efekt (shader) rysowany w prostokącie.
 * Backend mapuje {@code id} na zarejestrowany pipeline; parametry trafiają do uniformów.
 */
public record ShaderEffect(String id, Map<String, Float> params) {
    public ShaderEffect { params = params == null ? Map.of() : Map.copyOf(params); }
    public static ShaderEffect of(String id) { return new ShaderEffect(id, Map.of()); }
    public float param(String name, float def) { return params.getOrDefault(name, def); }
}

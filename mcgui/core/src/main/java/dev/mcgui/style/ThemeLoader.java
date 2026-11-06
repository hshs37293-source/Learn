package dev.mcgui.style;

import dev.mcgui.gfx.Border;
import dev.mcgui.gfx.Color;
import dev.mcgui.gfx.CornerRadii;
import dev.mcgui.gfx.Paint;
import dev.mcgui.gfx.Shadow;
import dev.mcgui.math.Insets;
import dev.mcgui.util.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wczytuje motyw z JSON. Format:
 * <pre>
 * { "name": "flat",
 *   "tokens": { "color.accent": "#4677ff", "radius.m": 5 },
 *   "rules": {
 *     "button": { "fill": "$color.accent", "radius": "$radius.m", "padding": [6, 10],
 *                 "border": { "width": 1, "color": "#ffffff44" },
 *                 "shadow": [ { "dx": 0, "dy": 2, "blur": 8, "color": "#00000066" } ],
 *                 "text": { "color": "#fff", "size": 9, "shadow": true },
 *                 "transition": 200, "easing": "ease",
 *                 "states": { "hover": { "fill": "#5a86ff" } } } } }
 * </pre>
 * Wartości tekstowe zaczynające się od "$" odwołują się do tokenów.
 */
public final class ThemeLoader {
    private final Map<String, Object> tokens = new LinkedHashMap<>();

    private ThemeLoader() {}

    public static Theme load(String json) { return new ThemeLoader().build(Json.parseObject(json)); }

    @SuppressWarnings("unchecked")
    private Theme build(Map<String, Object> root) {
        String name = String.valueOf(root.getOrDefault("name", "unnamed"));
        Object t = root.get("tokens");
        if (t instanceof Map<?, ?> tm) tokens.putAll((Map<String, Object>) tm);

        Map<String, RuleData> rules = new HashMap<>();
        Map<String, Map<State, RuleData>> stateRules = new HashMap<>();
        Object r = root.get("rules");
        if (r instanceof Map<?, ?> rm) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) rm).entrySet()) {
                Map<String, Object> rule = (Map<String, Object>) e.getValue();
                rules.put(e.getKey(), rule(rule));
                Object st = rule.get("states");
                if (st instanceof Map<?, ?> sm) {
                    Map<State, RuleData> per = new HashMap<>();
                    for (State s : State.values()) {
                        Object so = ((Map<String, Object>) sm).get(s.key);
                        if (so != null) per.put(s, rule((Map<String, Object>) so));
                    }
                    stateRules.put(e.getKey(), per);
                }
            }
        }
        return new Theme(name, tokens, rules, stateRules);
    }

    @SuppressWarnings("unchecked")
    private RuleData rule(Map<String, Object> m) {
        RuleData d = new RuleData();
        if (m.containsKey("fill")) d.fill = paint(m.get("fill"));
        if (m.containsKey("radius")) d.radii = radii(m.get("radius"));
        if (m.containsKey("border")) d.border = border(m.get("border"));
        if (m.containsKey("shadow")) d.shadows = shadows(m.get("shadow"));
        if (m.get("text") instanceof Map<?, ?> tm) {
            Map<String, Object> text = (Map<String, Object>) tm;
            if (text.containsKey("color")) d.textColor = color(text.get("color"));
            if (text.containsKey("size")) d.textSize = num(text.get("size"));
            if (text.containsKey("shadow")) d.textShadow = Boolean.TRUE.equals(resolve(text.get("shadow")));
        }
        if (m.containsKey("padding")) d.padding = insets(m.get("padding"));
        if (m.containsKey("minWidth")) d.minWidth = num(m.get("minWidth"));
        if (m.containsKey("minHeight")) d.minHeight = num(m.get("minHeight"));
        if (m.containsKey("transition")) d.transitionMs = Math.round(num(m.get("transition")));
        if (m.containsKey("easing")) d.easing = String.valueOf(resolve(m.get("easing")));
        if (m.get("layer") instanceof Map<?, ?> lm) {
            Map<String, Object> layer = (Map<String, Object>) lm;
            if (layer.containsKey("alpha")) d.layerAlpha = num(layer.get("alpha"));
            if (layer.containsKey("backdropBlur")) d.backdropBlur = num(layer.get("backdropBlur"));
        }
        return d;
    }

    // --- konwersje ---

    private Object resolve(Object v) {
        int guard = 0;
        while (v instanceof String s && s.startsWith("$")) {
            if (!tokens.containsKey(s.substring(1))) throw new IllegalArgumentException("Nieznany token: " + s);
            v = tokens.get(s.substring(1));
            if (++guard > 16) throw new IllegalArgumentException("Cykl w tokenach: " + s);
        }
        return v;
    }

    private float num(Object v) {
        Object r = resolve(v);
        if (r instanceof Number n) return n.floatValue();
        throw new IllegalArgumentException("Oczekiwano liczby, jest: " + r);
    }

    private int color(Object v) {
        Object r = resolve(v);
        if (r instanceof String s) return Color.parse(s);
        throw new IllegalArgumentException("Oczekiwano koloru (#rrggbb), jest: " + r);
    }

    @SuppressWarnings("unchecked")
    private Paint paint(Object v) {
        Object r = resolve(v);
        if (r instanceof String) return Paint.solid(color(r));
        if (r instanceof Map<?, ?> mm) {
            Map<String, Object> m = (Map<String, Object>) mm;
            String type = String.valueOf(m.getOrDefault("type", "linear"));
            if (type.equals("radial")) return Paint.radial(color(m.get("inner")), color(m.get("outer")));
            // linear: "angle" w stopniach (0 = od lewej do prawej, 90 = od góry do dołu) lub "direction": vertical/horizontal/diagonal
            int c0 = color(m.get("from")), c1 = color(m.get("to"));
            if (m.containsKey("angle")) {
                double a = Math.toRadians(num(m.get("angle")));
                float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
                return new Paint.Linear(0.5f - dx / 2, 0.5f - dy / 2, 0.5f + dx / 2, 0.5f + dy / 2, c0, c1);
            }
            String dir = String.valueOf(m.getOrDefault("direction", "vertical"));
            return switch (dir) {
                case "horizontal" -> Paint.horizontal(c0, c1);
                case "diagonal" -> Paint.diagonal(c0, c1);
                default -> Paint.vertical(c0, c1);
            };
        }
        throw new IllegalArgumentException("Zły format wypełnienia: " + r);
    }

    @SuppressWarnings("unchecked")
    private CornerRadii radii(Object v) {
        Object r = resolve(v);
        if (r instanceof Number n) return CornerRadii.all(n.floatValue());
        if (r instanceof List<?> l && l.size() == 4)
            return new CornerRadii(num(l.get(0)), num(l.get(1)), num(l.get(2)), num(l.get(3)));
        throw new IllegalArgumentException("Promień: liczba lub [tl,tr,br,bl], jest: " + r);
    }

    @SuppressWarnings("unchecked")
    private Border border(Object v) {
        Object r = resolve(v);
        if (r == null) return Border.NONE;
        Map<String, Object> m = (Map<String, Object>) r;
        Border.Placement pl = Border.Placement.valueOf(String.valueOf(m.getOrDefault("placement", "inside")).toUpperCase());
        return new Border(num(m.get("width")), color(m.get("color")), pl);
    }

    @SuppressWarnings("unchecked")
    private List<Shadow> shadows(Object v) {
        Object r = resolve(v);
        List<Shadow> out = new ArrayList<>();
        if (r == null) return out;
        for (Object o : (List<Object>) r) {
            Map<String, Object> m = (Map<String, Object>) o;
            out.add(new Shadow(m.containsKey("dx") ? num(m.get("dx")) : 0, m.containsKey("dy") ? num(m.get("dy")) : 0,
                m.containsKey("blur") ? num(m.get("blur")) : 0, m.containsKey("spread") ? num(m.get("spread")) : 0,
                color(m.get("color")), Boolean.TRUE.equals(m.get("inset"))));
        }
        return out;
    }

    private Insets insets(Object v) {
        Object r = resolve(v);
        if (r instanceof Number n) return Insets.all(n.floatValue());
        if (r instanceof List<?> l) {
            if (l.size() == 2) return Insets.xy(num(l.get(1)), num(l.get(0)));          // [pion, poziom] jak w CSS
            if (l.size() == 4) return new Insets(num(l.get(3)), num(l.get(0)), num(l.get(1)), num(l.get(2))); // t,r,b,l jak w CSS
        }
        throw new IllegalArgumentException("Padding: liczba, [pion,poziom] lub [t,r,b,l], jest: " + r);
    }
}

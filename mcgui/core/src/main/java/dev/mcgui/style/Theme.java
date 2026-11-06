package dev.mcgui.style;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Motyw: reguły stylu dla ról widgetów + tokeny. Widget zna tylko swoją ROLĘ (np. "button.primary"),
 * a wygląd pochodzi z motywu. Zmiana stylu całego GUI = podmiana obiektu {@code Theme}.
 *
 * Rozwiązywanie: rola "a.b.c" dziedziczy po "a.b", potem po "a", potem po "widget".
 * Na końcu nakładane są nadpisania stanów w kolejności {@link State}.
 */
public final class Theme {
    public final String name;
    private final Map<String, Object> tokens;
    private final Map<String, RuleData> rules;
    private final Map<String, Map<State, RuleData>> stateRules;
    private final Map<String, ResolvedStyle> cache = new HashMap<>();

    public Theme(String name, Map<String, Object> tokens, Map<String, RuleData> rules,
                 Map<String, Map<State, RuleData>> stateRules) {
        this.name = name;
        this.tokens = Collections.unmodifiableMap(tokens);
        this.rules = rules;
        this.stateRules = stateRules;
    }

    public Map<String, Object> tokens() { return tokens; }
    public boolean hasRule(String role) { return rules.containsKey(role) || stateRules.containsKey(role); }

    public ResolvedStyle resolve(String role, Set<State> states) {
        String key = role + "|" + (states.isEmpty() ? "" : EnumSet.copyOf(states).toString());
        return cache.computeIfAbsent(key, k -> ResolvedStyle.from(resolveData(role, states)));
    }

    private RuleData resolveData(String role, Set<State> states) {
        // łańcuch dziedziczenia od ogółu do szczegółu
        java.util.List<String> chain = new java.util.ArrayList<>();
        chain.add("widget");
        String[] parts = role.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append('.');
            sb.append(p);
            chain.add(sb.toString());
        }
        RuleData d = new RuleData();
        for (String r : chain) { RuleData x = rules.get(r); if (x != null) d = d.overlay(x); }
        for (State st : State.values()) {
            if (!states.contains(st)) continue;
            for (String r : chain) {
                Map<State, RuleData> m = stateRules.get(r);
                if (m != null && m.get(st) != null) d = d.overlay(m.get(st));
            }
        }
        return d;
    }
}

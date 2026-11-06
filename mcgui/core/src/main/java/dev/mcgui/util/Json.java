package dev.mcgui.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimalny parser JSON bez zależności (obiekty → Map, tablice → List, liczby → Double,
 * napisy → String, true/false → Boolean, null → null). Wystarczy do motywów i konfiguracji.
 */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) throw p.err("Nadmiarowe znaki po wartości");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) throw new IllegalArgumentException("Oczekiwano obiektu JSON");
        return (Map<String, Object>) v;
    }

    private Object value() {
        if (i >= s.length()) throw err("Nieoczekiwany koniec");
        char c = s.charAt(i);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> lit("true", Boolean.TRUE);
            case 'f' -> lit("false", Boolean.FALSE);
            case 'n' -> lit("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            String k = string();
            ws(); expect(':'); ws();
            m.put(k, value());
            ws();
            char c = next();
            if (c == '}') return m;
            if (c != ',') throw err("Oczekiwano , lub }");
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++; ws();
        if (peek() == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = next();
            if (c == ']') return l;
            if (c != ',') throw err("Oczekiwano , lub ]");
        }
    }

    private String string() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw err("Niezamknięty napis");
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case '"', '\\', '/' -> b.append(e);
                    case 'n' -> b.append('\n');
                    case 't' -> b.append('\t');
                    case 'r' -> b.append('\r');
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> throw err("Zła sekwencja ucieczki \\" + e);
                }
            } else b.append(c);
        }
    }

    private Object number() {
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (st == i) throw err("Nieoczekiwany znak '" + s.charAt(i) + "'");
        return Double.parseDouble(s.substring(st, i));
    }

    private Object lit(String word, Object v) {
        if (!s.startsWith(word, i)) throw err("Oczekiwano " + word);
        i += word.length();
        return v;
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\t' || c == '\r') i++;
            else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') {   // komentarz // (wygoda w motywach)
                while (i < s.length() && s.charAt(i) != '\n') i++;
            } else break;
        }
    }

    private char peek() { return i < s.length() ? s.charAt(i) : '\0'; }
    private char next() { if (i >= s.length()) throw err("Nieoczekiwany koniec"); return s.charAt(i++); }
    private void expect(char c) { if (next() != c) throw err("Oczekiwano '" + c + "'"); }
    private IllegalArgumentException err(String m) { return new IllegalArgumentException("JSON: " + m + " (pozycja " + i + ")"); }
}

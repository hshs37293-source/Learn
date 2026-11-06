package dev.mcgui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Prosta obserwowalna wartość, łącząca widgety z danymi (np. ustawienia modułu). */
public final class Property<T> {
    private T value;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    public Property(T initial) { this.value = initial; }

    public T get() { return value; }

    /** Ustawia wartość i powiadamia słuchaczy, jeśli się zmieniła. */
    public void set(T v) {
        if (Objects.equals(value, v)) return;
        value = v;
        for (Consumer<T> l : List.copyOf(listeners)) l.accept(v);
    }

    public Runnable listen(Consumer<T> l) {
        listeners.add(l);
        return () -> listeners.remove(l);
    }
}

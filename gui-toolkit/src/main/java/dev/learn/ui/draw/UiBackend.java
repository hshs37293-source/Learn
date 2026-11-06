package dev.learn.ui.draw;

/**
 * Kontrakt backendu. Jedna implementacja na wersję gry. Backend odpowiada za: pipeline'y/shadery,
 * batching, scissor, atlas tekstu, tekstury offscreen, głębię dla 3D.
 */
public interface UiBackend {
    /** Rozmiar okna w pikselach fizycznych. */
    int windowWidth();
    int windowHeight();

    /** Szerokość tekstu (potrzebna layoutowi). */
    float textWidth(String text, float size);

    /** Wykonaj polecenia jednej klatki. Polecenie nieznane backendowi: wyjątek, nie ciche pominięcie. */
    void execute(java.util.List<DrawCommand> commands);
}

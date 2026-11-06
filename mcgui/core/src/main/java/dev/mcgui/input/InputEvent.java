package dev.mcgui.input;

/** Zdarzenia wejścia przekazywane do drzewa widgetów. Współrzędne w jednostkach logicznych GUI. */
public sealed interface InputEvent {
    int BUTTON_LEFT = 0, BUTTON_RIGHT = 1, BUTTON_MIDDLE = 2;

    // Kody klawiszy jak w GLFW (używa ich Minecraft).
    int KEY_ESCAPE = 256, KEY_ENTER = 257, KEY_TAB = 258, KEY_BACKSPACE = 259, KEY_DELETE = 261,
        KEY_RIGHT = 262, KEY_LEFT = 263, KEY_HOME = 268, KEY_END = 269, KEY_A = 65, KEY_C = 67, KEY_V = 86, KEY_X = 88;
    int MOD_SHIFT = 1, MOD_CONTROL = 2, MOD_ALT = 4;

    record MouseMove(float x, float y) implements InputEvent {}
    record MouseDown(float x, float y, int button) implements InputEvent {}
    record MouseUp(float x, float y, int button) implements InputEvent {}
    record Scroll(float x, float y, float dx, float dy) implements InputEvent {}
    record KeyDown(int key, int mods) implements InputEvent {}
    record CharTyped(int codePoint) implements InputEvent {}
}

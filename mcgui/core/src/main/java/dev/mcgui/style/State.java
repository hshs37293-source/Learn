package dev.mcgui.style;

/** Stany widgetu, które mogą zmieniać styl. Kolejność = kolejność nakładania nadpisań. */
public enum State {
    FOCUSED("focused"), ACTIVE("active"), HOVER("hover"), PRESSED("pressed"), DISABLED("disabled");

    public final String key;
    State(String key) { this.key = key; }
}

package dev.mcgui.widget;

/** Poziomy układ liniowy. */
public class Row extends LinearLayout {
    public Row() { this("row"); }
    public Row(String role) { super(Axis.HORIZONTAL, role); }
}

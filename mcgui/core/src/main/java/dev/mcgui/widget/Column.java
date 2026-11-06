package dev.mcgui.widget;

/** Pionowy układ liniowy. */
public class Column extends LinearLayout {
    public Column() { this("column"); }
    public Column(String role) { super(Axis.VERTICAL, role); }
}

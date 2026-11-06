package dev.mcgui.demo;

import dev.mcgui.gfx.Color;
import dev.mcgui.gfx3d.DrawMode3;
import dev.mcgui.math.Box3;
import dev.mcgui.math.Vec3;
import dev.mcgui.style.Theme;
import dev.mcgui.style.Themes;
import dev.mcgui.util.Property;
import dev.mcgui.widget.Button;
import dev.mcgui.widget.Column;
import dev.mcgui.widget.Label;
import dev.mcgui.widget.Panel;
import dev.mcgui.widget.ProgressBar;
import dev.mcgui.widget.Row;
import dev.mcgui.widget.ScrollView;
import dev.mcgui.widget.Slider;
import dev.mcgui.widget.Spacer;
import dev.mcgui.widget.TextField;
import dev.mcgui.widget.Toggle;
import dev.mcgui.widget.UiRoot;
import dev.mcgui.widget.Viewport3D;

/**
 * Przykładowe GUI pokazujące wszystkie widgety (2D) i podgląd 3D.
 * Używane w testach i w ekranie demo adapterów. Dane są w {@link Model}, żeby je można było sprawdzać.
 */
public final class DemoUi {
    private DemoUi() {}

    public static final class Model {
        public final Property<Boolean> enabled = new Property<>(true);
        public final Property<Double> volume = new Property<>(0.5);
        public final Property<String> name = new Property<>("Steve");
        public final Property<Double> progress = new Property<>(0.35);
        public int clicks;
    }

    public static UiRoot build(Theme theme, Model m) {
        UiRoot[] holder = new UiRoot[1];
        Panel panel = new Panel();
        ((Column) panel).gap = 6;

        Row header = new Row();
        header.add(new Label("Demo GUI"));
        header.add(new Spacer());
        Button primary = new Button("Zapisz", "button.primary", () -> m.clicks++);
        header.add(primary);
        panel.add(header);

        Row themes = new Row();
        for (String name : new String[] { Themes.FLAT, Themes.GLASS, Themes.NEON, Themes.PIXEL }) {
            Button b = new Button(name, () -> holder[0].setTheme(Themes.builtin(name)));
            b.weight = 1f;
            themes.add(b);
        }
        panel.add(themes);

        Row toggleRow = new Row();
        toggleRow.add(new Label("Włączony"));
        toggleRow.add(new Spacer());
        toggleRow.add(new Toggle(m.enabled));
        panel.add(toggleRow);

        panel.add(new Label("Głośność"));
        Slider slider = new Slider(m.volume, 0, 1);
        slider.step = 0.05;
        panel.add(slider);

        TextField tf = new TextField(m.name);
        tf.placeholder = "Nazwa gracza";
        panel.add(tf);

        ProgressBar bar = new ProgressBar(m.progress);
        bar.weight = 1f;
        Row meters = new Row();
        meters.crossAlign = dev.mcgui.layout.Align.CENTER;
        meters.add(bar);
        meters.add(new RadialMeter(m.progress));
        panel.add(meters);

        Viewport3D vp = new Viewport3D(g -> {
            g.grid(Vec3.ZERO, 6, 1, Color.argb(90, 255, 255, 255));
            g.box(Box3.centered(new Vec3(0, 0.5f, 0), 1, 1, 1), Color.argb(200, 70, 119, 255), DrawMode3.BOTH);
            g.sphere(new Vec3(2, 0.6f, 0), 0.6f, Color.argb(200, 255, 120, 80), DrawMode3.FILL, 12);
            g.arrow(Vec3.ZERO, new Vec3(0, 2, 0), Color.rgb(80, 255, 120), 2f);
        });
        vp.autoRotate = true;
        panel.add(vp);

        Column list = new Column();
        for (int i = 1; i <= 8; i++) list.add(new Button("Element " + i, () -> {}));
        ScrollView sv = new ScrollView(list);
        sv.prefHeight = 70f;
        panel.add(sv);

        Column root = new Column("column");
        root.add(panel);
        UiRoot ui = new UiRoot(theme, root);
        ui.setSize(240, 400);
        holder[0] = ui;
        return ui;
    }
}

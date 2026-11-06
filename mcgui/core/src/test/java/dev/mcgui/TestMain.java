package dev.mcgui;

import dev.mcgui.demo.DemoUi;
import dev.mcgui.gfx.*;
import dev.mcgui.gfx3d.*;
import dev.mcgui.input.InputEvent;
import dev.mcgui.math.*;
import dev.mcgui.style.*;
import dev.mcgui.raster.Rasterizer;
import dev.mcgui.raster.SdfElement;
import dev.mcgui.raster.SdfMath;
import dev.mcgui.raster.SdfEmitter;
import dev.mcgui.raster.SdfPacking;
import dev.mcgui.raster.SimpleBackend;
import dev.mcgui.raster.Surface;
import dev.mcgui.util.Json;
import dev.mcgui.util.Property;
import dev.mcgui.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Minimalny runner testów (bez JUnit): uruchom klasę, kod wyjścia != 0 przy błędzie. */
public final class TestMain {
    static int passed, failed;
    static final List<String> failures = new ArrayList<>();

    interface T { void run() throws Exception; }

    static void test(String name, T t) {
        try { t.run(); passed++; System.out.println("  ok   " + name); }
        catch (Throwable e) { failed++; failures.add(name + ": " + e); System.out.println("  FAIL " + name + " -> " + e); }
    }

    static void eq(Object exp, Object act) { if (!java.util.Objects.equals(exp, act)) throw new AssertionError("oczekiwano " + exp + ", jest " + act); }
    static void near(double exp, double act, double eps) { if (Math.abs(exp - act) > eps) throw new AssertionError("oczekiwano ~" + exp + " (±" + eps + "), jest " + act); }
    static void yes(boolean c, String msg) { if (!c) throw new AssertionError(msg); }
    static void throwsEx(Class<? extends Throwable> k, Runnable r) {
        try { r.run(); } catch (Throwable e) { if (k.isInstance(e)) return; throw new AssertionError("zły wyjątek: " + e); }
        throw new AssertionError("brak wyjątku " + k.getSimpleName());
    }

    static UiRoot ui(Widget w, float width, float height) {
        UiRoot r = new UiRoot(Themes.builtin("flat"), w);
        r.setSize(width, height);
        r.layout(TextMeasurer.monospace());
        return r;
    }

    public static void main(String[] args) {
        System.out.println("== math/gfx ==");
        test("Color.parse formaty", () -> {
            eq(0xFF112233, Color.parse("#112233")); eq(0x80112233, Color.parse("#11223380")); eq(0xFFFFAA00, Color.parse("#fa0"));
            throwsEx(IllegalArgumentException.class, () -> Color.parse("112233"));
        });
        test("Color.mix i alfa", () -> { eq(0xFF808080, Color.mix(0xFF000000, 0xFFFFFFFF, 0.5f) | 0x00000000); eq(0x80FFFFFF, Color.multiplyAlpha(0xFFFFFFFF, 0.5f)); });
        test("Rect: intersect/union/snap", () -> {
            Rect r = Rect.of(0, 0, 10, 10).intersect(Rect.of(5, 5, 10, 10));
            eq(Rect.of(5, 5, 5, 5), r);
            eq(Rect.of(0, 0, 15, 15), Rect.of(0, 0, 10, 10).union(Rect.of(5, 5, 10, 10)));
            eq(true, Rect.of(0, 0, 10, 10).intersect(Rect.of(20, 20, 5, 5)).isEmpty());
            near(0.5, Rect.of(0.3f, 0, 1, 1).snapToPixels(2).x(), 1e-6);
        });
        test("CornerRadii.clampTo", () -> eq(5f, CornerRadii.all(100).clampTo(10, 20).tl()));
        test("Path.flatten okrąg", () -> {
            var c = Path.circle(0, 0, 10).flatten(0.1f);
            eq(1, c.size()); yes(c.get(0).closed(), "zamknięty");
            for (Vec2 p : c.get(0).points()) near(10, p.length(), 0.2);
        });
        test("Tessellator: pole prostokąta/L/okręgu", () -> {
            near(200, Tessellator.area(Tessellator.fillPolygon(Path.rect(0, 0, 10, 20).flatten(1).get(0).points())), 1e-3);
            var l = Path.polygon(0, 0, 20, 0, 20, 10, 10, 10, 10, 20, 0, 20).flatten(1).get(0).points();   // L: 300
            near(300, Tessellator.area(Tessellator.fillPolygon(l)), 1e-3);
            var circ = Path.circle(0, 0, 10).flatten(0.05f).get(0).points();
            near(Math.PI * 100, Tessellator.area(Tessellator.fillPolygon(circ)), 3);
        });
        test("Tessellator: zaokrąglony prostokąt", () -> {
            double r = 4, w = 20, h = 12;
            double expect = w * h - (4 - Math.PI) * r * r;
            var poly = Tessellator.roundedRect(0, 0, 20, 12, CornerRadii.all(4), 16);
            near(expect, Tessellator.area(Tessellator.fillPolygon(poly)), 0.5);
        });
        test("Tessellator.strokePolyline: długość*szerokość", () -> {
            var tris = Tessellator.strokePolyline(List.of(new Vec2(0, 0), new Vec2(10, 0)), false, 2);
            near(20, Tessellator.area(tris), 1e-4);
        });
        test("Json", () -> {
            Map<String, Object> m = Json.parseObject("{\"a\": [1, 2.5, true, null], // komentarz\n \"b\": {\"c\": \"x\\ny\"}}");
            eq(java.util.Arrays.asList(1.0, 2.5, true, null), m.get("a"));
            eq("x\ny", ((Map<?, ?>) m.get("b")).get("c"));
            throwsEx(IllegalArgumentException.class, () -> Json.parse("{\"a\": }"));
        });
        test("Easing: granice i monotoniczność", () -> {
            for (Easing_ e : Easing_.all()) { near(0, e.f(0), 1e-6); near(1, e.f(1), 1e-3); }
        });

        System.out.println("== styl/motywy ==");
        test("wszystkie wbudowane motywy się ładują", () -> {
            for (String n : new String[]{"flat", "glass", "neon", "pixel"}) {
                Theme t = Themes.builtin(n);
                eq(n, t.name);
                yes(t.hasRule("button") && t.hasRule("panel") && t.hasRule("slider.knob"), "brak reguł w " + n);
            }
        });
        test("stany nadpisują styl (hover, disabled)", () -> {
            Theme t = Themes.builtin("flat");
            var base = t.resolve("button", java.util.EnumSet.noneOf(State.class));
            var hover = t.resolve("button", java.util.EnumSet.of(State.HOVER));
            var dis = t.resolve("button", java.util.EnumSet.of(State.DISABLED));
            yes(!base.box().fill().equals(hover.box().fill()), "hover powinien zmienić wypełnienie");
            yes(!base.text().equals(dis.text()), "disabled powinien zmienić tekst");
        });
        test("dziedziczenie roli: button.primary dziedziczy po button", () -> {
            Theme t = Themes.builtin("flat");
            var p = t.resolve("button.primary", java.util.EnumSet.noneOf(State.class));
            var b = t.resolve("button", java.util.EnumSet.noneOf(State.class));
            eq(b.box().radii(), p.box().radii());                       // odziedziczone
            yes(!b.box().fill().equals(p.box().fill()), "primary ma własny kolor");
        });
        test("tokeny i błędy w motywie", () -> {
            Theme t = Themes.fromJson("{\"tokens\":{\"c\":\"#ff0000\",\"alias\":\"$c\"},\"rules\":{\"widget\":{\"fill\":\"$alias\"}}}");
            eq(0xFFFF0000, ((Paint.Solid) t.resolve("x", java.util.EnumSet.noneOf(State.class)).box().fill()).argb());
            throwsEx(IllegalArgumentException.class, () -> Themes.fromJson("{\"rules\":{\"widget\":{\"fill\":\"$nie_ma\"}}}"));
            throwsEx(IllegalArgumentException.class, () -> Themes.fromJson("{\"tokens\":{\"a\":\"$b\",\"b\":\"$a\"},\"rules\":{\"widget\":{\"fill\":\"$a\"}}}"));
        });
        test("ResolvedStyle.lerp interpoluje kolor, promień i cień", () -> {
            Theme t = Themes.builtin("neon");
            var a = t.resolve("button", java.util.EnumSet.noneOf(State.class));
            var b = t.resolve("button", java.util.EnumSet.of(State.HOVER));
            var mid = a.lerp(b, 0.5f);
            eq(1, mid.box().shadows().size());
            yes(mid.box().shadows().get(0).blur() > a.box().shadows().get(0).blur()
                && mid.box().shadows().get(0).blur() < b.box().shadows().get(0).blur(), "blur pośredni");
        });

        System.out.println("== layout ==");
        test("Column: sumuje wysokości i odstępy", () -> {
            Column c = new Column("column"); c.gap = 4;
            Spacer s1 = c.add(new Spacer(50, 20)); s1.weight = 0;
            Spacer s2 = c.add(new Spacer(50, 30)); s2.weight = 0;
            UiRoot r = ui(c, 200, 200);
            near(20, s1.bounds().h(), 1e-4); near(24, s2.bounds().y(), 1e-4); near(30, s2.bounds().h(), 1e-4);
            near(200, s1.bounds().w(), 1e-4);                             // STRETCH
        });
        test("Row: wagi dzielą wolne miejsce", () -> {
            Row row = new Row("row"); row.gap = 0;
            Spacer a1 = row.add(new Spacer(10, 10)); a1.weight = 1;
            Spacer a2 = row.add(new Spacer(10, 10)); a2.weight = 3;
            ui(row, 110, 10);
            near(10 + 90 / 4.0, a1.bounds().w(), 1e-3); near(10 + 90 * 3 / 4.0, a2.bounds().w(), 1e-3);
        });
        test("Row: SPACE_BETWEEN i CENTER w osi poprzecznej", () -> {
            Row row = new Row("row"); row.gap = 0; row.mainAlign = LinearLayout.Main.SPACE_BETWEEN; row.crossAlign = dev.mcgui.layout.Align.CENTER;
            Spacer a1 = row.add(new Spacer(10, 10)); a1.weight = 0;
            Spacer a2 = row.add(new Spacer(10, 10)); a2.weight = 0;
            ui(row, 100, 50);
            near(0, a1.bounds().x(), 1e-4); near(90, a2.bounds().x(), 1e-4); near(20, a1.bounds().y(), 1e-4);
        });
        test("Stack: wyśrodkowanie", () -> {
            Stack s = new Stack(); s.horizontal = dev.mcgui.layout.Align.CENTER; s.vertical = dev.mcgui.layout.Align.CENTER;
            Spacer in = s.add(new Spacer(20, 10)); in.weight = 0;
            ui(s, 100, 100);
            near(40, in.bounds().x(), 1e-4); near(45, in.bounds().y(), 1e-4);
        });
        test("Grid: 2 kolumny", () -> {
            Grid g = new Grid(2); g.gapX = 0; g.gapY = 0;
            Spacer[] s = new Spacer[4];
            for (int i = 0; i < 4; i++) { s[i] = g.add(new Spacer(10, 10)); s[i].weight = 0; }
            ui(g, 100, 100);
            near(50, s[1].bounds().x(), 1e-4); near(10, s[2].bounds().y(), 1e-4);
        });
        test("ScrollView: offset ograniczony do zakresu", () -> {
            Column list = new Column("column");
            for (int i = 0; i < 10; i++) { Spacer sp = list.add(new Spacer(10, 20)); sp.weight = 0; }
            ScrollView sv = new ScrollView(list); sv.prefHeight = 50f;
            Column root = new Column("column"); root.add(sv); 
            UiRoot r = ui(root, 100, 100);
            sv.setOffset(9999);
            near(sv.maxOffset(), sv.offset(), 1e-4);
            yes(sv.maxOffset() > 0, "jest co przewijać");
        });

        System.out.println("== wejście ==");
        test("Button: klik wywołuje akcję tylko gdy puszczony nad nim", () -> {
            int[] n = {0};
            Button b = new Button("OK", () -> n[0]++);
            Column c = new Column("column"); c.add(b);
            UiRoot r = ui(c, 100, 100);
            float cx = b.bounds().centerX(), cy = b.bounds().centerY();
            r.dispatch(new InputEvent.MouseMove(cx, cy));
            yes(b.hasState(State.HOVER), "hover");
            r.dispatch(new InputEvent.MouseDown(cx, cy, 0));
            yes(b.hasState(State.PRESSED), "pressed");
            r.dispatch(new InputEvent.MouseUp(cx, cy, 0));
            eq(1, n[0]); yes(!b.hasState(State.PRESSED), "po puszczeniu");
            r.dispatch(new InputEvent.MouseDown(cx, cy, 0));
            r.dispatch(new InputEvent.MouseUp(500, 500, 0));              // puszczony poza przyciskiem
            eq(1, n[0]);
        });
        test("Button wyłączony nie reaguje", () -> {
            int[] n = {0};
            Button b = new Button("OK", () -> n[0]++); b.setEnabled(false);
            Column c = new Column("column"); c.add(b);
            UiRoot r = ui(c, 100, 100);
            r.dispatch(new InputEvent.MouseDown(b.bounds().centerX(), b.bounds().centerY(), 0));
            r.dispatch(new InputEvent.MouseUp(b.bounds().centerX(), b.bounds().centerY(), 0));
            eq(0, n[0]);
        });
        test("Toggle przełącza Property", () -> {
            Property<Boolean> p = new Property<>(false);
            Toggle t = new Toggle(p);
            Column c = new Column("column"); c.crossAlign = dev.mcgui.layout.Align.START; c.add(t);
            UiRoot r = ui(c, 100, 100);
            r.dispatch(new InputEvent.MouseDown(t.bounds().centerX(), t.bounds().centerY(), 0));
            r.dispatch(new InputEvent.MouseUp(t.bounds().centerX(), t.bounds().centerY(), 0));
            eq(true, p.get());
        });
        test("Slider: przeciąganie ustawia wartość z krokiem", () -> {
            Property<Double> p = new Property<>(0.0);
            Slider s = new Slider(p, 0, 10); s.step = 1;
            Column c = new Column("column"); c.add(s);
            UiRoot r = ui(c, 200, 100);
            Rect b = s.bounds();
            r.dispatch(new InputEvent.MouseDown(b.x() + b.w() / 2, b.centerY(), 0));
            near(5, p.get(), 1e-9);
            r.dispatch(new InputEvent.MouseMove(b.right() + 50, b.centerY()));      // poza prawą krawędzią → max
            near(10, p.get(), 1e-9);
            r.dispatch(new InputEvent.MouseUp(0, 0, 0));
            r.dispatch(new InputEvent.MouseMove(b.x() + 10, b.centerY()));         // po puszczeniu bez efektu
            near(10, p.get(), 1e-9);
        });
        test("TextField: wpisywanie, backspace, strzałki", () -> {
            Property<String> p = new Property<>("");
            TextField tf = new TextField(p);
            Column c = new Column("column"); c.add(tf);
            UiRoot r = ui(c, 200, 100);
            r.dispatch(new InputEvent.MouseDown(tf.bounds().centerX(), tf.bounds().centerY(), 0));
            r.dispatch(new InputEvent.MouseUp(tf.bounds().centerX(), tf.bounds().centerY(), 0));
            yes(tf.hasState(State.FOCUSED), "fokus po kliknięciu");
            for (char ch : "abc".toCharArray()) r.dispatch(new InputEvent.CharTyped(ch));
            eq("abc", p.get());
            r.dispatch(new InputEvent.KeyDown(InputEvent.KEY_LEFT, 0));
            r.dispatch(new InputEvent.CharTyped('X'));
            eq("abXc", p.get());
            r.dispatch(new InputEvent.KeyDown(InputEvent.KEY_BACKSPACE, 0));
            eq("abc", p.get());
            r.dispatch(new InputEvent.KeyDown(InputEvent.KEY_HOME, 0));
            r.dispatch(new InputEvent.KeyDown(InputEvent.KEY_DELETE, 0));
            eq("bc", p.get());
        });
        test("Tab przenosi fokus między kontrolkami", () -> {
            Button b1 = new Button("1", () -> {}), b2 = new Button("2", () -> {});
            Column c = new Column("column"); c.add(b1); c.add(b2);
            UiRoot r = ui(c, 100, 100);
            r.dispatch(new InputEvent.KeyDown(InputEvent.KEY_TAB, 0)); yes(b1.hasState(State.FOCUSED), "1");
            r.dispatch(new InputEvent.KeyDown(InputEvent.KEY_TAB, 0)); yes(b2.hasState(State.FOCUSED) && !b1.hasState(State.FOCUSED), "2");
            r.dispatch(new InputEvent.KeyDown(InputEvent.KEY_TAB, InputEvent.MOD_SHIFT)); yes(b1.hasState(State.FOCUSED), "wstecz");
        });
        test("ScrollView: kółko przewija", () -> {
            Column list = new Column("column");
            for (int i = 0; i < 10; i++) { Spacer sp = list.add(new Spacer(10, 20)); sp.weight = 0; }
            ScrollView sv = new ScrollView(list); sv.prefHeight = 50f;
            Column root = new Column("column"); root.add(sv);
            UiRoot r = ui(root, 100, 100);
            r.dispatch(new InputEvent.Scroll(10, 10, 0, -1));
            yes(sv.offset() > 0, "offset po przewinięciu w dół");
        });

        System.out.println("== rysowanie ==");
        test("DisplayList: zbalansowane push/pop i replay", () -> {
            DisplayList dl = new DisplayList();
            dl.pushClip(Rect.of(0, 0, 5, 5), CornerRadii.NONE); dl.pushTranslate(1, 1); dl.line(0, 0, 1, 1, 1, 0xFFFFFFFF);
            dl.popTransform(); dl.popClip();
            yes(dl.isBalanced(), "zbalansowana");
            DisplayList copy = new DisplayList(); dl.replay(copy);
            eq(dl.commands(), copy.commands());
        });
        for (String th : new String[]{"flat", "glass", "neon", "pixel"}) {
            test("pełne demo GUI w motywie " + th + ": brak wyjątków, zbalansowane, jest 2D i 3D", () -> {
                DemoUi.Model m = new DemoUi.Model();
                UiRoot r = DemoUi.build(Themes.builtin(th), m);
                DisplayList dl = new DisplayList(TextMeasurer.monospace(), 2f, () -> 1234L);
                r.render(dl);
                yes(dl.isBalanced(), "niezbalansowane");
                yes(dl.count(DrawCommand.Box.class) > 8, "mało boxów: " + dl.count(DrawCommand.Box.class));
                yes(dl.count(DrawCommand.Text.class) > 3, "mało tekstów");
                yes(dl.count(DrawCommand.PathCmd.class) + dl.count(DrawCommand.Line.class) > 10, "brak rzutu 3D");
            });
        }
        test("zmiana motywu w locie zmienia styl", () -> {
            Button b = new Button("A", () -> {});
            Column c = new Column("column"); c.add(b);
            UiRoot r = ui(c, 100, 100);
            DisplayList d1 = new DisplayList(); r.render(d1);
            r.setTheme(Themes.builtin("neon"));
            DisplayList d2 = new DisplayList(); r.render(d2);
            yes(!d1.commands().equals(d2.commands()), "inny wygląd po zmianie motywu");
        });
        test("przejście stylu hover trwa i kończy się", () -> {
            Button b = new Button("A", () -> {});
            Column c = new Column("column"); c.add(b);
            UiRoot r = ui(c, 100, 100);
            long[] now = {1000};
            DisplayList probe = new DisplayList(TextMeasurer.monospace(), 1f, () -> now[0]);
            r.render(probe);
            r.dispatch(new InputEvent.MouseMove(b.bounds().centerX(), b.bounds().centerY()));
            now[0] = 1050; r.render(new DisplayList(TextMeasurer.monospace(), 1f, () -> now[0]));   // tu widget zauważa zmianę stanu
            now[0] = 1100; DisplayList mid = new DisplayList(TextMeasurer.monospace(), 1f, () -> now[0]); r.render(mid);
            now[0] = 5000; DisplayList end = new DisplayList(TextMeasurer.monospace(), 1f, () -> now[0]); r.render(end);
            Paint pm = firstBoxFill(mid), pe = firstBoxFill(end), p0 = firstBoxFill(probe);
            yes(!pm.equals(p0) && !pm.equals(pe), "fill w trakcie przejścia różni się od początku i końca");
        });

        System.out.println("== 3D ==");
        test("Flattener3D: box FILL = 12 trójkątów, OUTLINE = 12 linii", () -> {
            Flattener3D f = new Flattener3D(); f.box(Box3.of(0, 0, 0, 1, 1, 1), 0xFFFFFFFF, DrawMode3.FILL);
            eq(12, f.result().triangles.size()); eq(0, f.result().lines.size());
            Flattener3D g = new Flattener3D(); g.box(Box3.of(0, 0, 0, 1, 1, 1), 0xFFFFFFFF, DrawMode3.OUTLINE);
            eq(12, g.result().lines.size()); eq(0, g.result().triangles.size());
        });
        test("Flattener3D: pose (translate/scale) i pop", () -> {
            Flattener3D f = new Flattener3D();
            f.pushPose(); f.translate(10, 0, 0); f.scale(2, 2, 2);
            f.line(Vec3.ZERO, new Vec3(1, 0, 0), 0xFFFFFFFF, 1);
            f.popPose();
            f.line(Vec3.ZERO, new Vec3(1, 0, 0), 0xFFFFFFFF, 1);
            var l = f.result().lines;
            near(10, l.get(0).a().x(), 1e-5); near(12, l.get(0).b().x(), 1e-5);
            near(0, l.get(1).a().x(), 1e-5);
            throwsEx(IllegalStateException.class, () -> new Flattener3D().popPose());
        });
        test("Flattener3D: rotacja o 90° wokół Y", () -> {
            Flattener3D f = new Flattener3D();
            f.rotate((float) Math.PI / 2, 0, 1, 0);
            f.line(Vec3.ZERO, new Vec3(1, 0, 0), 0xFFFFFFFF, 1);
            Vec3 b = f.result().lines.get(0).b();
            near(0, b.x(), 1e-5); near(-1, b.z(), 1e-5);
        });
        test("Shapes3D: sfera ma punkty na promieniu, okrąg w płaszczyźnie", () -> {
            for (Vec3[] t : Shapes3D.sphereTriangles(new Vec3(1, 2, 3), 2, 12)) for (Vec3 p : t) near(2, p.sub(new Vec3(1, 2, 3)).length(), 1e-4);
            for (Vec3 p : Shapes3D.circlePoints(Vec3.ZERO, new Vec3(0, 0, 1), 3, 16)) { near(0, p.z(), 1e-4); near(3, p.length(), 1e-4); }
        });
        test("Camera3D: cel rzutowany na środek widoku, punkt za kamerą = null", () -> {
            Camera3D c = new Camera3D(); c.eye = new Vec3(0, 0, 5); c.target = Vec3.ZERO;
            Rect vp = Rect.of(0, 0, 200, 100);
            var p = c.project(Vec3.ZERO, vp);
            near(100, p.screen().x(), 1e-2); near(50, p.screen().y(), 1e-2);
            eq(null, c.project(new Vec3(0, 0, 10), vp));
            var up = c.project(new Vec3(0, 1, 0), vp);
            yes(up.screen().y() < 50, "wyżej w świecie = wyżej na ekranie");
        });
        test("Projector2D: rysuje trójkąty i linie, zbalansowany", () -> {
            DisplayList3D l = new DisplayList3D();
            l.box(Box3.centered(Vec3.ZERO, 1, 1, 1), 0xFFFFFFFF, DrawMode3.BOTH);
            Camera3D c = new Camera3D().orbit(0.5f, 0.4f, 5);
            DisplayList dl = new DisplayList();
            Projector2D.draw(Flattener3D.flatten(l), c, Rect.of(0, 0, 100, 100), dl);
            yes(dl.count(DrawCommand.PathCmd.class) > 0 && dl.count(DrawCommand.Line.class) > 0, "polecenia");
            yes(dl.isBalanced(), "balans");
        });
        test("Viewport3D: obrót myszą i zoom", () -> {
            Viewport3D vp = new Viewport3D(g -> g.box(Box3.centered(Vec3.ZERO, 1, 1, 1), 0xFFFFFFFF, DrawMode3.BOTH));
            Column c = new Column("column"); c.add(vp);
            UiRoot r = ui(c, 300, 300);
            DisplayList before = new DisplayList(); r.render(before);
            r.dispatch(new InputEvent.MouseDown(vp.bounds().centerX(), vp.bounds().centerY(), 0));
            r.dispatch(new InputEvent.MouseMove(vp.bounds().centerX() + 40, vp.bounds().centerY()));
            r.dispatch(new InputEvent.MouseUp(0, 0, 0));
            DisplayList after = new DisplayList(); r.render(after);
            yes(!before.commands().equals(after.commands()), "widok zmienił się po obrocie");
        });

        System.out.println("== raster / prosty backend ==");
        test("Rasterizer.roundedRect: pole ~ w*h - (4-pi)r^2 (z antyaliasingiem)", () -> {
            double[] area = {0};
            Rasterizer.roundedRect(0.3f, 0.2f, 20f, 12.5f, CornerRadii.all(4), false, (x0, y0, x1, y1, c) -> area[0] += (x1 - x0) * (y1 - y0) * c);
            double expect = 20.0 * 12.5 - (4 - Math.PI) * 16;
            near(expect, area[0], 1.2);
        });
        test("Rasterizer.roundedRect: scalanie wierszy zmniejsza liczbę wywołań, nie pole", () -> {
            int[] merged = {0}, rows = {0}; double[] a1 = {0}, a2 = {0};
            Rasterizer.roundedRect(0, 0, 40, 30, CornerRadii.all(3), false, (x0, y0, x1, y1, c) -> { merged[0]++; a1[0] += (x1 - x0) * (y1 - y0) * c; });
            Rasterizer.roundedRect(0, 0, 40, 30, CornerRadii.all(3), true, (x0, y0, x1, y1, c) -> { rows[0]++; a2[0] += (x1 - x0) * (y1 - y0) * c; });
            yes(merged[0] < rows[0], "scalone " + merged[0] + " < wierszowe " + rows[0]);
            near(a1[0], a2[0], 1e-3);
        });
        test("Rasterizer.ring: pole obrysu = pole zewn - pole wewn", () -> {
            double[] ring = {0}, outer = {0}, inner = {0};
            Rasterizer.ring(0, 0, 30, 20, CornerRadii.all(5), 2, (x0, y0, x1, y1, c) -> ring[0] += (x1 - x0) * (y1 - y0) * c);
            Rasterizer.roundedRect(0, 0, 30, 20, CornerRadii.all(5), false, (x0, y0, x1, y1, c) -> outer[0] += (x1 - x0) * (y1 - y0) * c);
            Rasterizer.roundedRect(2, 2, 26, 16, CornerRadii.all(3), false, (x0, y0, x1, y1, c) -> inner[0] += (x1 - x0) * (y1 - y0) * c);
            near(outer[0] - inner[0], ring[0], 1.0);
        });
        test("Rasterizer.triangle: pole zgodne z geometrią", () -> {
            double[] a = {0};
            Rasterizer.triangle(0, 0, 20, 0, 0, 10, (x0, y0, x1, y1, c) -> a[0] += (x1 - x0) * (y1 - y0) * c);
            near(100, a[0], 1.5);
        });
        test("Ribbons: wstęga prostopadła do kierunku, szerokość = zadana", () -> {
            Vec3[] q = Ribbons.lineQuad(new Vec3(0, 0, 0), new Vec3(10, 0, 0), new Vec3(5, 0, 10), 2);
            near(2, q[0].sub(q[1]).length(), 1e-4);
            near(0, q[1].sub(q[0]).dot(new Vec3(1, 0, 0)), 1e-5);
            Geometry3D g = new Geometry3D(); g.lines.add(new Geometry3D.LineSeg(Vec3.ZERO, new Vec3(1, 0, 0), 0xFFFFFFFF, 2, true));
            Geometry3D t = Ribbons.linesToTriangles(g, new Vec3(0, 0, 5), 1f, 1000f);
            eq(2, t.triangles.size()); eq(0, t.lines.size());
            near(Ribbons.worldWidth(2, 5, 1f, 1000f), t.triangles.get(0).a().sub(t.triangles.get(0).b()).length(), 1e-4);
        });
        test("SimpleBackend: box zaokrąglony/obrys/cień rysuje przez Surface, pole pokrycia sensowne", () -> {
            RecordingSurface rs = new RecordingSurface(2f);
            SimpleBackend b = new SimpleBackend(rs);
            b.box(Rect.of(0, 0, 20, 10), BoxStyle.builder().fill(0xFFFF0000).radius(3).border(1, 0xFF00FF00)
                .shadow(Shadow.drop(0, 2, 6, 0xFF000000)).build());
            yes(rs.fills > 10, "dużo wypełnień: " + rs.fills);
            yes(rs.maxX <= 20 * 2 + 14 && rs.minX >= -14, "cień mieści się w rozsądnych granicach: " + rs.minX + ".." + rs.maxX);
        });
        test("SimpleBackend: 9-slice daje 9 wywołań drawImage z poprawnymi regionami", () -> {
            RecordingSurface rs = new RecordingSurface(1f);
            SimpleBackend b = new SimpleBackend(rs);
            b.image(TextureRef.of("t"), Rect.of(0, 0, 40, 30), null, ImageStyle.nineSlice(new Insets(4, 4, 4, 4), 16, 16));
            eq(9, rs.images);
            near(256, rs.imageSrcArea, 1e-3);                // regiony źródłowe 9-slice tworzą całą teksturę 16x16
        });
        test("SimpleBackend: warstwa alfa mnoży kolory; zbalansowane clip/transform", () -> {
            RecordingSurface rs = new RecordingSurface(1f);
            SimpleBackend b = new SimpleBackend(rs);
            b.pushLayer(Rect.ZERO, LayerStyle.alpha(0.5f));
            b.box(Rect.of(0, 0, 4, 4), BoxStyle.builder().fill(0xFFFFFFFF).build());
            b.popLayer();
            yes(rs.lastColorAlpha > 120 && rs.lastColorAlpha < 135, "alfa ~128, jest " + rs.lastColorAlpha);
            b.pushClip(Rect.of(0, 0, 1, 1), CornerRadii.NONE); b.pushTranslate(1, 1); b.popTransform(); b.popClip();
            yes(rs.balanced(), "clip/macierz niezbalansowane");
        });
        test("SimpleBackend: pełne demo GUI działa na powierzchni (wszystkie motywy)", () -> {
            for (String th : new String[]{"flat", "glass", "neon", "pixel"}) {
                RecordingSurface rs = new RecordingSurface(2f);
                UiRoot r = DemoUi.build(Themes.builtin(th), new DemoUi.Model());
                r.render(new SimpleBackend(rs));
                yes(rs.fills > 50 && rs.texts > 2 && rs.balanced(), th + ": fills=" + rs.fills + " texts=" + rs.texts);
            }
        });

        System.out.println("== SDF (gładkie krawędzie) ==");
        test("SdfPacking: obieg rozmiaru i promieni, ograniczenia zakresu", () -> {
            CornerRadii r = new CornerRadii(3.5f, 0f, 12f, 63.5f);
            eq(r, SdfPacking.unpackRadii(SdfPacking.radiiX(r), SdfPacking.radiiY(r)));
            near(100.25, SdfPacking.decodeSize(SdfPacking.encodeSize(100.25f)), 1e-6);
            near(63.5, SdfPacking.decodeRadius(SdfPacking.encodeRadius(500f)), 1e-6);          // obcięte do maksimum
            eq(32767, SdfPacking.encodeSize(1e9f));
            yes(SdfPacking.radiiX(CornerRadii.all(63.5f)) <= 32767, "mieści się w 16 bitach ze znakiem");
        });
        test("SdfEmitter: kolejność cień → wypełnienie → obrys, geometria cienia i obrysu", () -> {
            BoxStyle st = BoxStyle.builder().fill(0xFFFF0000).radius(4).border(2, 0xFF00FF00)
                .shadow(new Shadow(1, 2, 6, 3, 0xFF000000, false)).build();
            var els = SdfEmitter.emit(Rect.of(10, 20, 40, 30), st, 2f, 1f);
            eq(3, els.size());
            eq(SdfElement.Kind.SHADOW, els.get(0).kind()); eq(SdfElement.Kind.FILL, els.get(1).kind()); eq(SdfElement.Kind.OUTLINE, els.get(2).kind());
            SdfElement sh = els.get(0);
            near(12, sh.param(), 1e-5);                                                          // blur 6 * scale 2
            near(40 + 2 * 6 + 2 * 12, sh.w(), 1e-4);                                             // szerokość + 2*spread(6) + 2*blur(12)
            near(40 + 2 * 6, sh.shapeW(), 1e-4);                                                 // sam kształt: bez blura
            near(10 + 2 - 6 - 12, sh.x(), 1e-4);                                                 // x + dx*scale - spread - blur
            near(8 + 6, sh.radii().tl(), 1e-4);                                                  // promień 4*2 + spread
            near(4, els.get(2).param(), 1e-5);                                                   // obrys 2*scale
        });
        test("SdfEmitter: położenie obrysu inside/center/outside", () -> {
            for (var c : new Object[][]{ {Border.Placement.INSIDE, 0f}, {Border.Placement.CENTER, 2f}, {Border.Placement.OUTSIDE, 4f} }) {
                BoxStyle st = BoxStyle.builder().border(new Border(4, 0xFFFFFFFF, (Border.Placement) c[0])).build();
                var o = SdfEmitter.emit(Rect.of(0, 0, 20, 20), st, 1f, 1f).get(0);
                near(-(float) c[1], o.x(), 1e-5); near(20 + 2 * (float) c[1], o.w(), 1e-5);
            }
        });
        test("SdfEmitter: gradienty → kolory narożników (bilinearnie)", () -> {
            int a = 0xFFFF0000, b = 0xFF0000FF;
            var v = SdfEmitter.corners(Paint.vertical(a, b), 1f);
            eq(a, v[0]); eq(a, v[1]); eq(b, v[2]); eq(b, v[3]);
            var h = SdfEmitter.corners(Paint.horizontal(a, b), 1f);
            eq(a, h[0]); eq(b, h[1]); eq(b, h[2]); eq(a, h[3]);
            var d = SdfEmitter.corners(Paint.diagonal(a, b), 1f);
            eq(a, d[0]); eq(b, d[2]); eq(Color.mix(a, b, 0.5f), d[1]); eq(d[1], d[3]);
            eq(0x80FF0000, SdfEmitter.corners(Paint.solid(a), 0.5f)[0]);                         // alfa warstwy
        });
        test("SimpleBackend + powierzchnia SDF: box idzie do shadera, nie paskami; skala logiczne→fizyczne", () -> {
            SdfSurface sf = new SdfSurface(2f);
            new SimpleBackend(sf).box(Rect.of(1, 2, 10, 5), BoxStyle.builder().fill(0xFFFFFFFF).radius(2).border(1, 0xFF000000).build());
            eq(0, sf.fills); eq(2, sf.sdf.size());
            near(2, sf.sdf.get(0).x(), 1e-5); near(20, sf.sdf.get(0).w(), 1e-5);
            near(4, sf.sdf.get(0).radii().tl(), 1e-5);
            var big = new SdfSurface(2f);
            new SimpleBackend(big).box(Rect.of(0, 0, 10, 5), BoxStyle.builder().fill(0xFFFFFFFF).radius(3).build());
            near(5, big.sdf.get(0).radii().tl(), 1e-5);                       // 6 ograniczone do połowy krótszego boku (10px/2)
        });
        test("pełne demo GUI na powierzchni SDF: wszystkie boxy to elementy SDF, brak pasków dla boxów", () -> {
            for (String th : new String[]{"flat", "glass", "neon", "pixel"}) {
                SdfSurface sf = new SdfSurface(2f);
                UiRoot r = DemoUi.build(Themes.builtin(th), new DemoUi.Model());
                r.render(new SimpleBackend(sf));
                yes(sf.sdf.size() > 10, th + ": mało elementów SDF " + sf.sdf.size());
                yes(sf.balanced(), "niezbalansowane");
            }
        });
        test("ostry tekst: skala co 0.5, pozycja w pełnych pikselach (można wyłączyć)", () -> {
            SdfSurface sf = new SdfSurface(2f);
            TextStyle ts = TextStyle.DEFAULT.withSize(10);
            new SimpleBackend(sf).text("x", 3.3f, 1.2f, ts);
            near(2.0, sf.lastTextScale, 1e-6); near(7, sf.lastTextX, 1e-6); near(2, sf.lastTextY, 1e-6);   // 2.22→2.0; 6.6→7; 2.4→2
            SdfSurface sf2 = new SdfSurface(2f);
            SimpleBackend raw = new SimpleBackend(sf2); raw.crispText = false;
            raw.text("x", 3.3f, 1.2f, ts);
            near(10 / 9.0 * 2, sf2.lastTextScale, 1e-5); near(6.6, sf2.lastTextX, 1e-4);
        });

        test("SdfMath: pokrycie pikseli zaokrąglonego prostokąta ≈ pole geometryczne (gładkie krawędzie)", () -> {
            float w = 40.3f, h = 24.7f, r = 7f;
            double sum = 0;
            for (int y = -4; y < 30; y++) for (int x = -4; x < 46; x++) {
                float d = SdfMath.distance(x + 0.5f - w / 2f, y + 0.5f - h / 2f, w / 2f, h / 2f, CornerRadii.all(r));
                sum += SdfMath.fillCoverage(d, 1f);
            }
            near(w * h - (4 - Math.PI) * r * r, sum, 1.0);
        });
        test("SdfMath: cztery różne promienie trafiają w właściwe rogi", () -> {
            CornerRadii r = new CornerRadii(10, 0, 0, 0);                    // tylko lewy-górny
            float d0 = SdfMath.distance(-19.5f, -9.5f, 20, 10, r);           // narożnik tl: odcięty łukiem → na zewnątrz
            float d1 = SdfMath.distance(19.5f, -9.5f, 20, 10, r);            // narożnik tr: ostry → wewnątrz
            yes(d0 > 0.5f && d1 < 0, "tl zaokrąglony (d=" + d0 + "), tr ostry (d=" + d1 + ")");
            CornerRadii rb = new CornerRadii(0, 0, 10, 0);                   // tylko prawy-dolny
            yes(SdfMath.distance(19.5f, 9.5f, 20, 10, rb) > 0.5f && SdfMath.distance(-19.5f, 9.5f, 20, 10, rb) < 0, "br");
        });
        test("SdfMath: obrys ma pole ≈ obwód * grubość, wnętrze puste", () -> {
            float w = 30f, h = 20f, line = 2f; double ring = 0;
            for (int y = -3; y < 24; y++) for (int x = -3; x < 34; x++) {
                float d = SdfMath.distance(x + 0.5f - w / 2f, y + 0.5f - h / 2f, w / 2f, h / 2f, CornerRadii.NONE);
                ring += SdfMath.outlineCoverage(d, line, 1f);
            }
            near(w * h - (w - 2 * line) * (h - 2 * line), ring, 1.0);
            near(0, SdfMath.outlineCoverage(SdfMath.distance(0, 0, 15, 10, CornerRadii.NONE), line, 1f), 1e-6);
        });
        test("SdfMath: cień maleje monotonicznie od krawędzi, ~0.5 na krawędzi, 0 daleko", () -> {
            float prev = 2f;
            for (float d = -10; d <= 10; d += 0.5f) {
                float a = SdfMath.shadowCoverage(d, 8f);
                yes(a <= prev + 1e-6f, "monotoniczność przy d=" + d); prev = a;
            }
            near(0.5, SdfMath.shadowCoverage(0, 8f), 1e-5);
            near(0, SdfMath.shadowCoverage(10, 8f), 1e-6); near(1, SdfMath.shadowCoverage(-10, 8f), 1e-6);
        });
        test("SDF: krawędź na ułamku piksela daje pokrycie częściowe (płynna, bez schodków)", () -> {
            // półpłaszczyzna wypełniona dla y >= edge; środek piksela y=0.5, odległość ze znakiem d = edge - 0.5 (ujemna = wewnątrz)
            float[] edges = { 0.0f, 0.25f, 0.5f, 0.75f, 1.0f };
            float prev = 2f;
            for (float edge : edges) {
                float c = SdfMath.fillCoverage(edge - 0.5f, 1f);
                near(1.0 - edge, c, 1e-6);                        // pokrycie = ułamek piksela pod krawędzią
                yes(c < prev, "pokrycie maleje wraz z przesuwaniem krawędzi w dół");
                prev = c;
            }
        });

        System.out.println("\nWynik: " + passed + " zaliczonych, " + failed + " niezaliczonych");
        if (failed > 0) { failures.forEach(f -> System.out.println(" - " + f)); System.exit(1); }
    }

    static Paint firstBoxFill(DisplayList dl) {
        for (DrawCommand c : dl.commands()) if (c instanceof DrawCommand.Box b && b.style().fill().isVisible()) return b.style().fill();
        throw new AssertionError("brak boxa z wypełnieniem");
    }

    /** Adapter do testów Easing bez zależności od jego wewnętrznych pól. */
    record Easing_(dev.mcgui.anim.Easing e) {
        float f(float t) { return e.apply(t); }
        static List<Easing_> all() { List<Easing_> l = new ArrayList<>(); for (var e : dev.mcgui.anim.Easing.values()) l.add(new Easing_(e)); return l; }
    }

    /** Powierzchnia testowa: liczy wywołania i zakres współrzędnych. */
    static final class RecordingSurface implements Surface {
        final float gui; int fills, texts, images, clip, matrix; float minX = 1e9f, maxX = -1e9f; int lastColorAlpha; float imageSrcArea;
        RecordingSurface(float gui) { this.gui = gui; }
        boolean balanced() { return clip == 0 && matrix == 0; }
        public void fillRect(float x0, float y0, float x1, float y1, int argb) { fills++; minX = Math.min(minX, x0); maxX = Math.max(maxX, x1); lastColorAlpha = Color.a(argb); }
        public void fillGradientV(float x0, float y0, float x1, float y1, int t, int b) { fills++; minX = Math.min(minX, x0); maxX = Math.max(maxX, x1); }
        public void drawText(CharSequence t, float x, float y, int c, boolean sh, float sc) { texts++; }
        public float textWidth(CharSequence t, float sc) { return t.length() * 5f * sc; }
        public float lineHeight(float sc) { return 9f * sc; }
        public void drawImage(TextureRef t, Rect dst, Rect src, ImageStyle st, int tint, float tw, float th) {
            images++; if (src != null) imageSrcArea += src.w() * src.h();
        }
        public void pushClip(float a, float b, float c, float d) { clip++; }
        public void popClip() { clip--; }
        public void pushMatrix() { matrix++; }
        public void popMatrix() { matrix--; }
        public void translate(float dx, float dy) {}
        public void scale(float sx, float sy) {}
        public void rotateAbout(float r, float px, float py) {}
        public float guiScale() { return gui; }
        public long nowMillis() { return 1000; }
    }

    /** Powierzchnia testowa z obsługą SDF: zbiera elementy SDF i ostatnie parametry tekstu. */
    static final class SdfSurface implements Surface {
        final float gui; int fills, clip, matrix; final List<SdfElement> sdf = new ArrayList<>();
        float lastTextScale, lastTextX, lastTextY;
        SdfSurface(float gui) { this.gui = gui; }
        boolean balanced() { return clip == 0 && matrix == 0; }
        public boolean supportsSdf() { return true; }
        public void drawSdf(SdfElement e) { sdf.add(e); }
        public void fillRect(float x0, float y0, float x1, float y1, int argb) { fills++; }
        public void fillGradientV(float x0, float y0, float x1, float y1, int t, int b) { fills++; }
        public void drawText(CharSequence t, float x, float y, int c, boolean sh, float sc) { lastTextX = x; lastTextY = y; lastTextScale = sc; }
        public float textWidth(CharSequence t, float sc) { return t.length() * 5f * sc; }
        public float lineHeight(float sc) { return 9f * sc; }
        public void drawImage(TextureRef t, Rect dst, Rect src, ImageStyle st, int tint, float tw, float th) {}
        public void pushClip(float a, float b, float c, float d) { clip++; }
        public void popClip() { clip--; }
        public void pushMatrix() { matrix++; }
        public void popMatrix() { matrix--; }
        public void translate(float dx, float dy) {}
        public void scale(float sx, float sy) {}
        public void rotateAbout(float r, float px, float py) {}
        public float guiScale() { return gui; }
        public long nowMillis() { return 1000; }
    }
}

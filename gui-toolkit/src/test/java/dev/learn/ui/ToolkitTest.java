package dev.learn.ui;

import static dev.learn.ui.Check.*;
import dev.learn.ui.Check.Test;

import dev.learn.ui.anim.Spring;
import dev.learn.ui.draw.*;
import dev.learn.ui.math.*;
import dev.learn.ui.widget.*;
import java.util.ArrayList;
import java.util.List;

public class ToolkitTest {

    /** Backend testowy: zapamiętuje polecenia, tekst ma stałą szerokość 6 px/znak. */
    static final class FakeBackend implements UiBackend {
        List<DrawCommand> last = new ArrayList<>();
        public int windowWidth() { return 800; }
        public int windowHeight() { return 600; }
        public float textWidth(String t, float s) { return t.length() * 6f; }
        public void execute(List<DrawCommand> c) { last = new ArrayList<>(c); }
    }

    @Test public void sdfInsideOutsideAndCorner() {
        assertTrue(Sdf.roundedBox(0, 0, 50, 20, 8) < 0);          // środek: wewnątrz
        assertEquals(0f, Sdf.roundedBox(50, 0, 50, 20, 8), 1e-4); // środek krawędzi: na granicy
        assertTrue(Sdf.roundedBox(60, 0, 50, 20, 8) > 0);         // poza
        // róg zaokrąglony: punkt w kwadratowym rogu jest już POZA kształtem
        assertTrue(Sdf.roundedBox(49.5f, 19.5f, 50, 20, 8) > 0);
        assertEquals(1f, Sdf.coverage(-5), 0); assertEquals(0f, Sdf.coverage(5), 0);
    }

    @Test public void matrixInverseRoundTrip() {
        Mat4 m = Mat4.translation(1, 2, 3).mul(Mat4.rotationY(0.7f)).mul(Mat4.scale(2, 2, 2));
        Mat4 id = m.mul(m.inverse());
        for (int i = 0; i < 16; i++) assertEquals(i % 5 == 0 ? 1f : 0f, id.m[i], 1e-4);
    }

    @Test public void pickingHitsPanelInWorld() {
        Mat4 view = Mat4.lookAt(new Vec3(0, 0, 5), new Vec3(0, 0, 0), new Vec3(0, 1, 0));
        Mat4 vp = Mat4.perspective((float) Math.toRadians(70), 800f / 600f, 0.1f, 100f).mul(view);
        Ray center = Ray.fromScreen(400, 300, 800, 600, vp);
        float[] uv = center.hitPanel(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(0, 1, 0), 2, 1);
        assertNotNull(uv);
        assertEquals(0.5f, uv[0], 1e-3); assertEquals(0.5f, uv[1], 1e-3);
        Ray corner = Ray.fromScreen(0, 0, 800, 600, vp);
        assertNull(corner.hitPanel(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(0, 1, 0), 2, 1));
    }

    @Test public void layoutHitTestAndClick() {
        Screen s = new Screen();
        FakeBackend b = new FakeBackend();
        int[] clicks = {0};
        Stack col = (Stack) s.root;
        col.add(new Label("Hello"));
        Button btn = col.add(new Button("Click"));
        btn.onClick = () -> clicks[0]++;
        s.frame(b, new DrawList());

        assertTrue(btn.y > 0);                       // button pod labelem
        float cx = btn.absX() + 2, cy = btn.absY() + 2;
        assertSame(btn, s.root.hitTest(cx, cy));
        assertTrue(s.mouseDown(cx, cy, 0));
        assertTrue(s.mouseUp(cx, cy, 0));
        assertEquals(1, clicks[0]);

        s.mouseDown(cx, cy, 0);                       // wciśnięcie i puszczenie POZA = brak kliku
        s.mouseUp(-50, -50, 0);
        assertEquals(1, clicks[0]);
    }

    @Test public void frameProducesRoundedRectAndText() {
        Screen s = new Screen();
        FakeBackend b = new FakeBackend();
        ((Stack) s.root).add(new Button("OK"));
        s.frame(b, new DrawList());
        assertTrue(b.last.stream().anyMatch(c -> c instanceof DrawCommand.RoundedRect));
        assertTrue(b.last.stream().anyMatch(c -> c instanceof DrawCommand.Text));
    }

    @Test public void sliderDrag() {
        Screen s = new Screen();
        Slider sl = ((Stack) s.root).add(new Slider(0, 100, 0, 100));
        s.frame(new FakeBackend(), new DrawList());
        float y = sl.absY() + 5;
        s.mouseDown(sl.absX() + 50, y, 0);
        assertEquals(50f, sl.value, 0.01);
        s.mouseMove(sl.absX() + 500, y);              // przeciągnięcie poza: clamp
        assertEquals(100f, sl.value, 0.01);
    }

    @Test public void unbalancedClipIsRejected() {
        DrawList l = new DrawList().add(new DrawCommand.PushClip(0, 0, 1, 1));
        assertThrows(IllegalStateException.class, l::commands);
        assertThrows(IllegalStateException.class, () -> new DrawList().add(new DrawCommand.PopClip()));
    }

    @Test public void customCommandPassesThrough() {
        DrawList l = new DrawList().add(new DrawCommand.Custom("my-shader", "payload"));
        assertEquals("payload", ((DrawCommand.Custom) l.commands().get(0)).payload());
    }

    @Test public void springSettles() {
        Spring sp = new Spring(0, 120, 18);
        sp.target = 1;
        for (int i = 0; i < 300; i++) sp.update(1 / 60f);
        assertTrue(sp.settled()); assertEquals(1f, sp.value, 1e-2);
    }
}

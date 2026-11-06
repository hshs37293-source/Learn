package dev.learn.ui;

import java.lang.annotation.*;
import java.lang.reflect.Method;

/** Minimalne asercje i runner bez zależności (sieć Mavena bywa niedostępna/limitowana). */
public final class Check {
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface Test {}

    public static void assertTrue(boolean c) { if (!c) throw new AssertionError("expected true"); }
    public static void assertNotNull(Object o) { if (o == null) throw new AssertionError("expected non-null"); }
    public static void assertNull(Object o) { if (o != null) throw new AssertionError("expected null"); }
    public static void assertSame(Object a, Object b) { if (a != b) throw new AssertionError("not same"); }
    public static void assertEquals(Object a, Object b) { if (!a.equals(b)) throw new AssertionError(a + " != " + b); }
    public static void assertEquals(int a, int b) { if (a != b) throw new AssertionError(a + " != " + b); }
    public static void assertEquals(float a, float b, double eps) {
        if (Math.abs(a - b) > eps) throw new AssertionError(a + " != " + b + " (eps " + eps + ")");
    }
    public static void assertThrows(Class<? extends Throwable> t, Runnable r) {
        try { r.run(); } catch (Throwable e) { if (t.isInstance(e)) return; throw new AssertionError("wrong exception " + e); }
        throw new AssertionError("expected " + t.getSimpleName());
    }

    public static void main(String[] args) throws Exception {
        int ok = 0, fail = 0;
        Class<?> cls = Class.forName("dev.learn.ui.ToolkitTest");
        for (Method m : cls.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(Test.class)) continue;
            m.setAccessible(true);
            try { m.invoke(cls.getDeclaredConstructor().newInstance()); ok++; System.out.println("PASS " + m.getName()); }
            catch (Exception e) { fail++; System.out.println("FAIL " + m.getName() + ": " + e.getCause()); }
        }
        System.out.println(ok + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}

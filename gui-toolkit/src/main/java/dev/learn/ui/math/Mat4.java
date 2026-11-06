package dev.learn.ui.math;

/** Macierz 4x4 (column-major, jak w OpenGL/GLSL). Bez zależności od Minecrafta. */
public final class Mat4 {
    public final float[] m = new float[16];

    public static Mat4 identity() {
        Mat4 r = new Mat4();
        r.m[0] = r.m[5] = r.m[10] = r.m[15] = 1;
        return r;
    }

    /** this * o (najpierw o, potem this). */
    public Mat4 mul(Mat4 o) {
        Mat4 r = new Mat4();
        for (int c = 0; c < 4; c++)
            for (int row = 0; row < 4; row++) {
                float s = 0;
                for (int k = 0; k < 4; k++) s += m[k * 4 + row] * o.m[c * 4 + k];
                r.m[c * 4 + row] = s;
            }
        return r;
    }

    public static Mat4 translation(float x, float y, float z) {
        Mat4 r = identity();
        r.m[12] = x; r.m[13] = y; r.m[14] = z;
        return r;
    }

    public static Mat4 scale(float x, float y, float z) {
        Mat4 r = identity();
        r.m[0] = x; r.m[5] = y; r.m[10] = z;
        return r;
    }

    public static Mat4 rotationY(float rad) {
        Mat4 r = identity();
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        r.m[0] = c; r.m[8] = s; r.m[2] = -s; r.m[10] = c;
        return r;
    }

    /** Perspektywa OpenGL (głębia NDC -1..1). fovY w radianach. */
    public static Mat4 perspective(float fovY, float aspect, float near, float far) {
        float f = (float) (1.0 / Math.tan(fovY / 2));
        Mat4 r = new Mat4();
        r.m[0] = f / aspect;
        r.m[5] = f;
        r.m[10] = (far + near) / (near - far);
        r.m[11] = -1;
        r.m[14] = 2 * far * near / (near - far);
        return r;
    }

    /** Rzut ortograficzny (do 2D w pikselach: ortho(0,w,h,0,-1,1)). */
    public static Mat4 ortho(float l, float r_, float b, float t, float n, float f) {
        Mat4 r = identity();
        r.m[0] = 2 / (r_ - l);
        r.m[5] = 2 / (t - b);
        r.m[10] = -2 / (f - n);
        r.m[12] = -(r_ + l) / (r_ - l);
        r.m[13] = -(t + b) / (t - b);
        r.m[14] = -(f + n) / (f - n);
        return r;
    }

    public static Mat4 lookAt(Vec3 eye, Vec3 target, Vec3 up) {
        Vec3 f = target.sub(eye).normalize();
        Vec3 s = f.cross(up).normalize();
        Vec3 u = s.cross(f);
        Mat4 r = identity();
        r.m[0] = s.x(); r.m[4] = s.y(); r.m[8] = s.z();
        r.m[1] = u.x(); r.m[5] = u.y(); r.m[9] = u.z();
        r.m[2] = -f.x(); r.m[6] = -f.y(); r.m[10] = -f.z();
        r.m[12] = -s.dot(eye); r.m[13] = -u.dot(eye); r.m[14] = f.dot(eye);
        return r;
    }

    /** Punkt (w=1) -> po dzieleniu perspektywicznym. */
    public Vec3 transformPoint(Vec3 p) {
        float x = m[0] * p.x() + m[4] * p.y() + m[8] * p.z() + m[12];
        float y = m[1] * p.x() + m[5] * p.y() + m[9] * p.z() + m[13];
        float z = m[2] * p.x() + m[6] * p.y() + m[10] * p.z() + m[14];
        float w = m[3] * p.x() + m[7] * p.y() + m[11] * p.z() + m[15];
        return new Vec3(x / w, y / w, z / w);
    }

    /** Odwrotność ogólna (Gauss-Jordan). Zwraca null, gdy macierz osobliwa. */
    public Mat4 inverse() {
        double[][] a = new double[4][8];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) a[r][c] = m[c * 4 + r];
            a[r][4 + r] = 1;
        }
        for (int i = 0; i < 4; i++) {
            int p = i;
            for (int r = i + 1; r < 4; r++) if (Math.abs(a[r][i]) > Math.abs(a[p][i])) p = r;
            if (Math.abs(a[p][i]) < 1e-12) return null;
            double[] t = a[i]; a[i] = a[p]; a[p] = t;
            double d = a[i][i];
            for (int c = 0; c < 8; c++) a[i][c] /= d;
            for (int r = 0; r < 4; r++) {
                if (r == i) continue;
                double f = a[r][i];
                for (int c = 0; c < 8; c++) a[r][c] -= f * a[i][c];
            }
        }
        Mat4 out = new Mat4();
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) out.m[c * 4 + r] = (float) a[r][4 + c];
        return out;
    }
}

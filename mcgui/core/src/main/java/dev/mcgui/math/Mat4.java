package dev.mcgui.math;

/** Macierz 4x4 (kolumnowa, jak w OpenGL: m[col*4 + row]). Niemutowalna. */
public final class Mat4 {
    public final float[] m;

    private Mat4(float[] m) { this.m = m; }

    public static Mat4 identity() {
        float[] a = new float[16];
        a[0] = a[5] = a[10] = a[15] = 1;
        return new Mat4(a);
    }

    public static Mat4 translation(float x, float y, float z) {
        Mat4 r = identity();
        r.m[12] = x; r.m[13] = y; r.m[14] = z;
        return r;
    }

    public static Mat4 scaling(float x, float y, float z) {
        Mat4 r = identity();
        r.m[0] = x; r.m[5] = y; r.m[10] = z;
        return r;
    }

    /** Obrót wokół osi (x,y,z) o kąt w radianach. */
    public static Mat4 rotation(float angle, float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return identity();
        x /= len; y /= len; z /= len;
        float c = (float) Math.cos(angle), s = (float) Math.sin(angle), t = 1 - c;
        Mat4 r = identity();
        r.m[0] = t * x * x + c;     r.m[4] = t * x * y - s * z; r.m[8]  = t * x * z + s * y;
        r.m[1] = t * x * y + s * z; r.m[5] = t * y * y + c;     r.m[9]  = t * y * z - s * x;
        r.m[2] = t * x * z - s * y; r.m[6] = t * y * z + s * x; r.m[10] = t * z * z + c;
        return r;
    }

    /** Projekcja perspektywiczna (głębia NDC -1..1, kamera patrzy w -Z). */
    public static Mat4 perspective(float fovYRad, float aspect, float near, float far) {
        float f = (float) (1.0 / Math.tan(fovYRad / 2));
        float[] a = new float[16];
        a[0] = f / aspect;
        a[5] = f;
        a[10] = (far + near) / (near - far);
        a[11] = -1;
        a[14] = (2 * far * near) / (near - far);
        return new Mat4(a);
    }

    public static Mat4 lookAt(Vec3 eye, Vec3 target, Vec3 up) {
        Vec3 f = target.sub(eye).normalized();
        Vec3 s = f.cross(up).normalized();
        Vec3 u = s.cross(f);
        Mat4 r = identity();
        r.m[0] = s.x(); r.m[4] = s.y(); r.m[8]  = s.z();
        r.m[1] = u.x(); r.m[5] = u.y(); r.m[9]  = u.z();
        r.m[2] = -f.x(); r.m[6] = -f.y(); r.m[10] = -f.z();
        r.m[12] = -s.dot(eye); r.m[13] = -u.dot(eye); r.m[14] = f.dot(eye);
        return r;
    }

    public Mat4 mul(Mat4 o) {
        float[] r = new float[16];
        for (int c = 0; c < 4; c++)
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) sum += m[k * 4 + row] * o.m[c * 4 + k];
                r[c * 4 + row] = sum;
            }
        return new Mat4(r);
    }

    /** Transformuje punkt (w = 1); zwraca {x, y, z, w} przed dzieleniem perspektywicznym. */
    public float[] transform(float x, float y, float z) {
        return new float[] {
            m[0] * x + m[4] * y + m[8]  * z + m[12],
            m[1] * x + m[5] * y + m[9]  * z + m[13],
            m[2] * x + m[6] * y + m[10] * z + m[14],
            m[3] * x + m[7] * y + m[11] * z + m[15]
        };
    }

    public Vec3 transformPoint(Vec3 p) {
        float[] r = transform(p.x(), p.y(), p.z());
        float w = r[3] == 0 ? 1 : r[3];
        return new Vec3(r[0] / w, r[1] / w, r[2] / w);
    }
}

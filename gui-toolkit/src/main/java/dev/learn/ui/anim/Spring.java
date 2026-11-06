package dev.learn.ui.anim;

/** Sprężyna do płynnych animacji niezależnych od liczby klatek (stały krok wewnętrzny). */
public final class Spring {
    public float value, velocity, target;
    private final float stiffness, damping;

    public Spring(float initial, float stiffness, float damping) {
        this.value = this.target = initial;
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void update(float dtSeconds) {
        float remaining = Math.min(dtSeconds, 0.25f);
        final float step = 1f / 240f;
        while (remaining > 0) {
            float dt = Math.min(step, remaining);
            velocity += (stiffness * (target - value) - damping * velocity) * dt;
            value += velocity * dt;
            remaining -= dt;
        }
    }

    public boolean settled() { return Math.abs(target - value) < 1e-3f && Math.abs(velocity) < 1e-3f; }
}

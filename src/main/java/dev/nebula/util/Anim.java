package dev.nebula.util;

/** Frame-rate independent exponential smoothing toward a target. */
public final class Anim {
    private float value;
    private long last = System.nanoTime();

    public Anim(float initial) {
        value = initial;
    }

    public float update(float target, float speed) {
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1e9f, 0.1f);
        last = now;
        value += (target - value) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.0004f) value = target;
        return value;
    }

    public float get() {
        return value;
    }

    public void set(float v) {
        value = v;
        last = System.nanoTime();
    }
}

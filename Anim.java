package pl.novabomba;

import net.minecraft.util.Mth;

public final class Anim {
    private static long last = System.nanoTime();
    public static float dt = 0.016f;

    public static void frame() {
        long n = System.nanoTime();
        dt = Math.min(0.05f, (n - last) / 1_000_000_000f);
        last = n;
    }

    /** wygładzone dążenie do celu (exp) */
    public static float approach(float cur, float target, float speed) {
        Config c = Config.get();
        if (!c.animations) return target;
        float k = 1f - (float) Math.exp(-speed * c.animSpeed * dt);
        return cur + (target - cur) * k;
    }

    /** liniowe dążenie do celu (fade in/out) */
    public static float linear(float cur, float target, float speed) {
        Config c = Config.get();
        if (!c.animations) return target;
        float s = speed * c.animSpeed * dt;
        if (cur < target) return Math.min(target, cur + s);
        return Math.max(target, cur - s);
    }

    public static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }
}

package dev.nebula.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/** Shared combat/session state fed by client events. */
public final class Combat {
    public static LivingEntity lastTarget;
    public static long lastHitTime;
    public static double lastReach;
    public static int kills;
    public static int deaths;
    public static int hits;
    public static int combo;
    public static final long SESSION_START = System.currentTimeMillis();

    private Combat() {}

    public static void onHit(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        combo = now - lastHitTime < 1500 && target == lastTarget ? combo + 1 : 1;
        lastTarget = target;
        lastHitTime = now;
        hits++;
        if (mc.player != null) {
            var eye = mc.player.getEyePosition();
            var bb = target.getBoundingBox();
            double cx = Math.max(bb.minX, Math.min(eye.x, bb.maxX));
            double cy = Math.max(bb.minY, Math.min(eye.y, bb.maxY));
            double cz = Math.max(bb.minZ, Math.min(eye.z, bb.maxZ));
            lastReach = eye.distanceTo(new net.minecraft.world.phys.Vec3(cx, cy, cz));
        }
    }

    public static long sinceHit() {
        return System.currentTimeMillis() - lastHitTime;
    }
}

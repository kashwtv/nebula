package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Targets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * While you draw a bow, crossbow or trident, eases your aim onto the spot where the
 * shot will actually connect: arrow drop and drag are simulated, and the target's
 * movement is led by the projectile's flight time.
 */
public final class BowAim extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.5, 20, 6, 0.5));
    public final NumberSetting fov = add(new NumberSetting("FOV", 10, 180, 70, 5, "°"));
    public final NumberSetting range = add(new NumberSetting("Range", 8, 120, 60, 2, "m"));
    public final BoolSetting predict = add(new BoolSetting("Lead Moving Targets", true));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", false));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting passives = add(new BoolSetting("Passive Mobs", true));

    private LivingEntity target;
    private long last = System.nanoTime();

    public BowAim() {
        super("Bow Aim", "Aims bows and crossbows with drop and lead prediction.", Category.COMBAT);
    }

    public LivingEntity target() {
        return target;
    }

    /** Launch speed in blocks/tick for what you're holding, or -1 if not a ranged weapon. */
    static double launchSpeed(ItemStack st, int ticksUsing) {
        if (st.getItem() instanceof BowItem) return Math.max(0.1f, BowItem.getPowerForTime(ticksUsing)) * 3.0;
        if (st.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(st)) return 3.15;
        if (st.getItem() instanceof TridentItem) return 2.5;
        return -1;
    }

    @SubscribeEvent
    public void onFrame(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1e9f, 0.05f);
        last = now;
        var p = mc().player;
        if (!isEnabled() || p == null || mc().level == null || mc().screen != null) { target = null; return; }
        ItemStack held = p.getMainHandItem();
        boolean drawing = p.isUsingItem() && (held.getItem() instanceof BowItem || held.getItem() instanceof TridentItem);
        boolean crossbow = held.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(held);
        if (!drawing && !crossbow) { target = null; return; }
        double v = launchSpeed(held, p.getTicksUsingItem());
        if (v <= 0) return;

        if (target == null || !target.isAlive() || p.distanceTo(target) > range.value) target = pick();
        if (target == null) return;

        float pt = e.renderTickTime;
        Vec3 eye = p.getEyePosition(pt).subtract(0, 0.1, 0);
        Vec3 aim = new Vec3(Mth.lerp(pt, target.xOld, target.getX()), Mth.lerp(pt, target.yOld, target.getY()) + target.getBbHeight() * 0.55,
                Mth.lerp(pt, target.zOld, target.getZ()));
        Vec3 vel = target.getDeltaMovement();
        float[] sol = null;
        for (int it = 0; it < 3; it++) {
            sol = solve(eye, aim, v);
            if (sol == null || !predict.get()) break;
            Vec3 base = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ());
            aim = base.add(vel.x * sol[2], target.onGround() ? 0 : vel.y * sol[2] * 0.5, vel.z * sol[2]);
        }
        if (sol == null) return;
        float dYaw = Mth.wrapDegrees(sol[0] - p.getYRot());
        float dPitch = sol[1] - p.getXRot();
        float k = 1f - (float) Math.exp(-speed.f() * dt);
        p.turn(dYaw * k / 0.15, dPitch * k / 0.15);
    }

    /** Finds {yaw, pitch, flightTicks} that lands an arrow at the point, via bisection on pitch. */
    private float[] solve(Vec3 from, Vec3 to, double v) {
        double dx = to.x - from.x, dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        double lo = -60, hi = 40; // degrees of elevation, low-arc solution
        double bestErr = Double.MAX_VALUE;
        float bestPitch = 0, bestTicks = 0;
        for (int i = 0; i < 24; i++) {
            double mid = (lo + hi) / 2;
            double[] r = simulate(v, Math.toRadians(mid), horiz);
            double err = r[0] - (to.y - from.y);
            if (Math.abs(err) < bestErr) { bestErr = Math.abs(err); bestPitch = (float) -mid; bestTicks = (float) r[1]; }
            if (err > 0) hi = mid; else lo = mid;
        }
        return bestErr > 1.5 ? null : new float[]{yaw, bestPitch, bestTicks};
    }

    /** Height reached when the arrow has travelled {@code horiz} blocks, and ticks taken. */
    private static double[] simulate(double v, double elev, double horiz) {
        double vh = Math.cos(elev) * v, vy = Math.sin(elev) * v;
        double x = 0, y = 0;
        for (int t = 1; t < 200; t++) {
            double nx = x + vh, ny = y + vy;
            if (nx >= horiz) {
                double f = (horiz - x) / Math.max(1e-6, vh);
                return new double[]{y + vy * f, t - 1 + f};
            }
            x = nx;
            y = ny;
            vh *= 0.99;
            vy = vy * 0.99 - 0.05;
        }
        return new double[]{-999, 200};
    }

    private LivingEntity pick() {
        var p = mc().player;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity en : mc().level.entitiesForRendering()) {
            if (!Targets.valid(en, players.get(), hostiles.get(), passives.get(), false)) continue;
            LivingEntity le = (LivingEntity) en;
            if (p.distanceTo(le) > range.value) continue;
            float f = Targets.fovTo(le.getBoundingBox().getCenter());
            if (f > fov.value / 2) continue;
            if (!throughWalls.get() && !p.hasLineOfSight(le)) continue;
            if (f < bestScore) { bestScore = f; best = le; }
        }
        return best;
    }
}

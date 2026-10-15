package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fights hostile mobs for you, the way a person would: eases your aim onto a point that
 * drifts around the mob's body, only swings once your crosshair is actually on it, at full
 * cooldown with a little timing variation, and ignores anything outside your view.
 */
public final class KillAura extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 2.5, 4.5, 3.2, 0.1, "m"));
    public final NumberSetting fov = add(new NumberSetting("FOV", 30, 180, 100, 5, "°"));
    public final NumberSetting speed = add(new NumberSetting("Aim Speed", 1, 15, 6, 0.5));
    public final NumberSetting charge = add(new NumberSetting("Min Charge", 0.8, 1, 0.92, 0.01));
    public final BoolSetting requireAim = add(new BoolSetting("Swing Only When Aimed", true));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", false));

    private LivingEntity current;
    private Vec3 aimOffset = Vec3.ZERO, aimTarget = Vec3.ZERO;
    private long nextDrift;
    private float speedJitter = 1;
    private long last = System.nanoTime();
    private double nextCharge = 0.95;

    public KillAura() {
        super("Kill Aura", "Fights hostile mobs around you with natural-looking aim.", Category.COMBAT);
    }

    public LivingEntity target() {
        return current;
    }

    private boolean valid(Entity e) {
        var p = mc().player;
        return e instanceof Enemy && e instanceof LivingEntity le && le.isAlive() && !le.isDeadOrDying()
                && !e.isInvisible() && p.distanceTo(e) <= range.value
                && dev.nebula.util.Targets.fovTo(e.getBoundingBox().getCenter()) <= fov.value / 2
                && (throughWalls.get() || p.hasLineOfSight(e));
    }

    private LivingEntity pick() {
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : mc().level.entitiesForRendering()) {
            if (!valid(e)) continue;
            // prefer what's closest to where you're already looking
            double score = dev.nebula.util.Targets.fovTo(e.getBoundingBox().getCenter()) + mc().player.distanceTo(e) * 8;
            if (score < bestScore) { bestScore = score; best = (LivingEntity) e; }
        }
        return best;
    }

    /** Smooth aim every frame so the camera never jumps. */
    @SubscribeEvent
    public void onFrame(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1e9f, 0.05f);
        last = now;
        var p = mc().player;
        if (!isEnabled() || p == null || current == null || mc().screen != null) return;
        long ms = System.currentTimeMillis();
        var r = ThreadLocalRandom.current();
        if (ms > nextDrift) {
            // pick a new spot on the body to drift toward, and a slightly different hand speed
            aimTarget = new Vec3(r.nextDouble(-0.25, 0.25), r.nextDouble(0.35, 0.85), r.nextDouble(-0.25, 0.25));
            speedJitter = (float) r.nextDouble(0.75, 1.2);
            nextDrift = ms + r.nextLong(250, 650);
        }
        aimOffset = aimOffset.add(aimTarget.subtract(aimOffset).scale(Math.min(1, dt * 4)));
        float pt = e.renderTickTime;
        double x = Mth.lerp(pt, current.xOld, current.getX()), y = Mth.lerp(pt, current.yOld, current.getY()), z = Mth.lerp(pt, current.zOld, current.getZ());
        Vec3 point = new Vec3(x + aimOffset.x * current.getBbWidth(), y + aimOffset.y * current.getBbHeight(), z + aimOffset.z * current.getBbWidth());
        float[] a = dev.nebula.util.Targets.angles(p.getEyePosition(pt), point);
        float dy = Mth.wrapDegrees(a[0] - p.getYRot()), dp = a[1] - p.getXRot();
        float k = (1f - (float) Math.exp(-speed.f() * speedJitter * dt));
        p.turn(dy * k / 0.15, dp * k / 0.15);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null || mc().screen != null || p.isSpectator()) { current = null; return; }
        if (current == null || !valid(current)) current = pick();
        if (current == null) return;
        if (p.getAttackStrengthScale(0.5f) < nextCharge) return;
        if (requireAim.get() && !(mc().hitResult instanceof EntityHitResult eh && eh.getEntity() == current)) return;

        ModuleManager.get(AutoWeapon.class).select();
        ModuleManager.get(Criticals.class).beforeAttack();
        mc().gameMode.attack(p, current);
        p.swing(InteractionHand.MAIN_HAND);
        nextCharge = Math.min(1.0, charge.value + ThreadLocalRandom.current().nextDouble(0, 0.08));
    }

    @Override
    public void onDisable() {
        current = null;
    }
}

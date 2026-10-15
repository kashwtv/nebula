package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Targets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Smoothly pulls your aim toward the best target inside a cone, every frame. */
public final class AimAssist extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.5, 20, 5, 0.5));
    public final NumberSetting fov = add(new NumberSetting("FOV", 10, 180, 60, 5, "°"));
    public final NumberSetting range = add(new NumberSetting("Range", 2, 8, 4.5, 0.1, "m"));
    public final ModeSetting aimAt = add(new ModeSetting("Aim At", "Closest", "Closest", "Head", "Chest", "Feet"));
    public final ModeSetting priority = add(new ModeSetting("Priority", "Angle", "Angle", "Distance", "Health"));
    public final BoolSetting vertical = add(new BoolSetting("Vertical", true));
    public final BoolSetting onClick = add(new BoolSetting("Only While Clicking", true));
    public final BoolSetting weaponOnly = add(new BoolSetting("Weapon Only", false));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", false));
    public final BoolSetting sticky = add(new BoolSetting("Sticky Target", true));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting passives = add(new BoolSetting("Passive Mobs", true));
    public final BoolSetting invisibles = add(new BoolSetting("Invisibles", false));

    private LivingEntity locked;
    private long last = System.nanoTime();

    public AimAssist() {
        super("Aim Assist", "Smoothly drags your aim onto targets. Tune speed and FOV.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return fov.i() + "°";
    }

    public LivingEntity target() {
        return locked;
    }

    @SubscribeEvent
    public void onFrame(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1e9f, 0.05f);
        last = now;
        var p = mc().player;
        if (!isEnabled() || p == null || mc().screen != null || mc().level == null) { locked = null; return; }
        if (onClick.get() && !mc().options.keyAttack.isDown()) { locked = null; return; }
        if (weaponOnly.get() && !(p.getMainHandItem().getItem() instanceof SwordItem || p.getMainHandItem().getItem() instanceof AxeItem)) return;

        float pt = e.renderTickTime;
        if (!(sticky.get() && stillValid(locked))) locked = pick();
        if (locked == null) return;

        Vec3 eye = p.getEyePosition(pt);
        Vec3 aim = aimPoint(locked, eye, pt);
        float[] want = Targets.angles(eye, aim);
        float dYaw = Mth.wrapDegrees(want[0] - p.getYRot());
        float dPitch = vertical.get() ? want[1] - p.getXRot() : 0;
        // When the crosshair is already on the hitbox, ease off so it doesn't feel robotic.
        boolean onTarget = mc().hitResult instanceof net.minecraft.world.phys.EntityHitResult h && h.getEntity() == locked;
        float k = (1f - (float) Math.exp(-speed.f() * dt)) * (onTarget ? 0.35f : 1f);
        float stepYaw = dYaw * k, stepPitch = dPitch * k;
        p.turn(stepYaw / 0.15, stepPitch / 0.15);
    }

    private boolean stillValid(LivingEntity e) {
        var p = mc().player;
        return e != null && Targets.valid(e, players.get(), hostiles.get(), passives.get(), invisibles.get())
                && p.distanceTo(e) <= range.value + 0.5
                && Targets.fovTo(e.getBoundingBox().getCenter()) <= fov.value * 0.75
                && (throughWalls.get() || p.hasLineOfSight(e));
    }

    private LivingEntity pick() {
        var p = mc().player;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity en : mc().level.entitiesForRendering()) {
            if (!Targets.valid(en, players.get(), hostiles.get(), passives.get(), invisibles.get())) continue;
            LivingEntity le = (LivingEntity) en;
            if (p.distanceTo(le) > range.value) continue;
            float f = Targets.fovTo(le.getBoundingBox().getCenter());
            if (f > fov.value / 2) continue;
            if (!throughWalls.get() && !p.hasLineOfSight(le)) continue;
            double score = switch (priority.get()) {
                case "Distance" -> p.distanceTo(le);
                case "Health" -> le.getHealth() + le.getAbsorptionAmount();
                default -> f;
            };
            if (score < bestScore) { bestScore = score; best = le; }
        }
        return best;
    }

    private Vec3 aimPoint(LivingEntity t, Vec3 eye, float pt) {
        double x = Mth.lerp(pt, t.xOld, t.getX()), y = Mth.lerp(pt, t.yOld, t.getY()), z = Mth.lerp(pt, t.zOld, t.getZ());
        double h = t.getBbHeight();
        return switch (aimAt.get()) {
            case "Head" -> new Vec3(x, y + t.getEyeHeight(), z);
            case "Chest" -> new Vec3(x, y + h * 0.65, z);
            case "Feet" -> new Vec3(x, y + h * 0.15, z);
            default -> {
                // closest point on the hitbox to where we're already looking
                double cy = Mth.clamp(eye.y, y + 0.1, y + h - 0.1);
                yield new Vec3(x, cy, z);
            }
        };
    }
}

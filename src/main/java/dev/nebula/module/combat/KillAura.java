package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Friends;
import dev.nebula.util.Targets;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fights for you the way a person would: picks a target by your filters and priority, eases
 * your aim onto a point that drifts around its body, and swings at full cooldown (or at a
 * set CPS) with a little timing variation. Friends are never targeted.
 */
public final class KillAura extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 2.5, 6, 3.2, 0.1, "m"));
    public final NumberSetting fov = add(new NumberSetting("FOV", 30, 360, 100, 5, "°"));
    public final ModeSetting priority = add(new ModeSetting("Priority", "Angle", "Angle", "Distance", "Health"));
    public final NumberSetting speed = add(new NumberSetting("Aim Speed", 1, 20, 6, 0.5));
    public final ModeSetting timing = add(new ModeSetting("Timing", "Cooldown", "Cooldown", "CPS"));
    public final NumberSetting charge = add(new NumberSetting("Min Charge", 0.8, 1, 0.92, 0.01));
    public final NumberSetting cps = add(new NumberSetting("CPS", 1, 20, 10, 0.5));
    public final NumberSetting randomness = add(new NumberSetting("Randomness", 0, 0.3, 0.08, 0.01));
    public final BoolSetting players = add(new BoolSetting("Players", false));
    public final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
    public final BoolSetting neutrals = add(new BoolSetting("Neutrals", false));
    public final BoolSetting passives = add(new BoolSetting("Animals", false));
    public final BoolSetting invisibles = add(new BoolSetting("Invisibles", false));
    public final BoolSetting requireWeapon = add(new BoolSetting("Require Weapon", false));
    public final BoolSetting requireAim = add(new BoolSetting("Swing Only When Aimed", true));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", false));
    public final BoolSetting rotate = add(new BoolSetting("Rotate", true));

    private LivingEntity current;
    private Vec3 aimOffset = Vec3.ZERO, aimTarget = Vec3.ZERO;
    private long nextDrift, nextCpsHit;
    private float speedJitter = 1;
    private long last = System.nanoTime();
    private double nextCharge = 0.95;

    public KillAura() {
        super("Kill Aura", "Fights mobs and players around you with natural-looking aim.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return priority.get();
    }

    /**
     * Reach on: the distance you can actually hit (your entity_reach attribute, which Reach sets).
     * Reach off: the Range setting. Either way never beyond your current entity reach limit.
     */
    public double effectiveRange() {
        var reach = ModuleManager.get(Reach.class);
        var p = mc().player;
        double limit = p != null ? p.getEntityReach() : (reach != null && reach.isEnabled() ? reach.entity.value : 3.0);
        double want = reach != null && reach.isEnabled() ? limit : range.value;
        return Math.min(want, limit);
    }

    public LivingEntity target() {
        return isEnabled() ? current : null;
    }

    private boolean typeAllowed(Entity e) {
        var me = mc().player;
        if (e instanceof Player pl) return players.get() && !pl.isSpectator() && !pl.isCreative() && !Friends.is(e);
        if (e instanceof TamableAnimal t && t.isOwnedBy(me)) return false;
        if (e instanceof Enemy) return hostiles.get();
        if (e instanceof NeutralMob) return neutrals.get();
        return e instanceof net.minecraft.world.entity.Mob && passives.get();
    }

    private boolean valid(Entity e) {
        var p = mc().player;
        return e instanceof LivingEntity le && e != p && !(e instanceof ArmorStand) && le.isAlive() && !le.isDeadOrDying()
                && (invisibles.get() || !e.isInvisible()) && typeAllowed(e)
                && p.distanceTo(e) <= effectiveRange()
                && Targets.fovTo(e.getBoundingBox().getCenter()) <= fov.value / 2
                && (throughWalls.get() || p.hasLineOfSight(e));
    }

    private double score(LivingEntity e) {
        var p = mc().player;
        return switch (priority.get()) {
            case "Distance" -> p.distanceTo(e);
            case "Health" -> e.getHealth() + e.getAbsorptionAmount();
            default -> Targets.fovTo(e.getBoundingBox().getCenter()) + p.distanceTo(e) * 8;
        };
    }

    private LivingEntity pick() {
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : mc().level.entitiesForRendering()) {
            if (!valid(e)) continue;
            double s = score((LivingEntity) e);
            if (s < bestScore) { bestScore = s; best = (LivingEntity) e; }
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
        if (!isEnabled() || !rotate.get() || p == null || current == null || mc().screen != null) return;
        long ms = System.currentTimeMillis();
        var r = ThreadLocalRandom.current();
        if (ms > nextDrift) {
            aimTarget = new Vec3(r.nextDouble(-0.25, 0.25), r.nextDouble(0.35, 0.85), r.nextDouble(-0.25, 0.25));
            speedJitter = (float) r.nextDouble(0.75, 1.2);
            nextDrift = ms + r.nextLong(250, 650);
        }
        aimOffset = aimOffset.add(aimTarget.subtract(aimOffset).scale(Math.min(1, dt * 4)));
        float pt = e.renderTickTime;
        double x = Mth.lerp(pt, current.xOld, current.getX()), y = Mth.lerp(pt, current.yOld, current.getY()), z = Mth.lerp(pt, current.zOld, current.getZ());
        Vec3 point = new Vec3(x + aimOffset.x * current.getBbWidth(), y + aimOffset.y * current.getBbHeight(), z + aimOffset.z * current.getBbWidth());
        float[] a = Targets.angles(p.getEyePosition(pt), point);
        float dy = Mth.wrapDegrees(a[0] - p.getYRot()), dp = a[1] - p.getXRot();
        // exponential easing: fast when far off, gentle as it settles
        float k = (1f - (float) Math.exp(-speed.f() * speedJitter * dt));
        p.turn(dy * k / 0.15, dp * k / 0.15);
    }

    private boolean holdingWeapon() {
        var it = mc().player.getMainHandItem().getItem();
        return it instanceof SwordItem || it instanceof AxeItem || it instanceof TridentItem;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null || mc().screen != null || p.isSpectator()) { current = null; return; }
        if (current == null || !valid(current)) current = pick();
        if (current == null) return;
        if (requireWeapon.get() && !holdingWeapon()) return;
        if (p.isUsingItem()) return;
        var r = ThreadLocalRandom.current();
        if (timing.is("CPS")) {
            long now = System.currentTimeMillis();
            if (now < nextCpsHit) return;
            double interval = 1000.0 / cps.value;
            nextCpsHit = now + (long) (interval * (1 + r.nextDouble(-randomness.value, randomness.value)));
        } else if (p.getAttackStrengthScale(0.5f) < nextCharge) return;
        if (requireAim.get() && rotate.get() && !(mc().hitResult instanceof EntityHitResult eh && eh.getEntity() == current)) return;
        if (!rotate.get() && !requireAim.get() && p.distanceTo(current) > effectiveRange()) return;

        ModuleManager.get(AutoWeapon.class).select();
        ModuleManager.get(Criticals.class).beforeAttack();
        mc().gameMode.attack(p, current);
        p.swing(InteractionHand.MAIN_HAND);
        nextCharge = Math.min(1.0, charge.value + r.nextDouble(0, Math.max(1e-4, randomness.value)));
    }

    @Override
    public void onDisable() {
        current = null;
    }
}

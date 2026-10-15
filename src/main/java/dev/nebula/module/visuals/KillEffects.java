package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.util.Combat;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

/** Something dramatic happens when the thing you were fighting dies (client side only). */
public final class KillEffects extends Module {
    public final ModeSetting mode = add(new ModeSetting("Effect", "Lightning", "Lightning", "Totem", "Souls", "Firework", "All"));
    public final BoolSetting sound = add(new BoolSetting("Sound", true));

    private LivingEntity handled;
    private int fakeId = -42000;

    public KillEffects() {
        super("Kill Effects", "Lightning, totem bursts or souls when you get a kill.", Category.VISUALS);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    /** Runs every tick to count kills even when the effect is off. */
    @Override
    public void update() {
        LivingEntity t = Combat.lastTarget;
        if (t == null || t == handled || Combat.sinceHit() > 3000) return;
        if (t.isDeadOrDying() || t.getHealth() <= 0) {
            handled = t;
            Combat.kills++;
            if (isEnabled()) play(t);
        }
    }

    private void play(LivingEntity t) {
        var level = mc().level;
        if (level == null) return;
        String m = mode.get();
        boolean all = m.equals("All");
        if (all || m.equals("Lightning")) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(t.getX(), t.getY(), t.getZ());
                bolt.setVisualOnly(true);
                level.putNonPlayerEntity(fakeId--, bolt);
            }
        }
        if (all || m.equals("Totem")) {
            mc().particleEngine.createTrackingEmitter(t, ParticleTypes.TOTEM_OF_UNDYING, 30);
            if (sound.get()) mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.TOTEM_USE, 1.3f, 0.35f));
        }
        if (all || m.equals("Souls")) {
            for (int i = 0; i < 40; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 0.6;
                level.addParticle(ParticleTypes.SOUL, t.getX() + Math.cos(a) * r, t.getY() + Math.random() * t.getBbHeight(), t.getZ() + Math.sin(a) * r, 0, 0.05 + Math.random() * 0.08, 0);
            }
            if (sound.get()) mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SOUL_ESCAPE, 1f, 0.8f));
        }
        if (all || m.equals("Firework")) {
            for (int i = 0; i < 60; i++) {
                double vx = (Math.random() - 0.5) * 0.5, vy = Math.random() * 0.5, vz = (Math.random() - 0.5) * 0.5;
                level.addParticle(ParticleTypes.FIREWORK, t.getX(), t.getY() + t.getBbHeight(), t.getZ(), vx, vy, vz);
            }
            if (sound.get()) mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 0.6f));
        }
    }
}

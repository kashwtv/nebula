package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Col;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Extra particle bursts every time you hit something. */
public final class HitParticles extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Accent", "Accent", "Crit", "Magic", "Both", "Blood", "Hearts"));
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final NumberSetting amount = add(new NumberSetting("Amount", 1, 5, 2, 1));

    public HitParticles() {
        super("Hit Particles", "Bigger, juicier hit effects.", Category.VISUALS);
        color.visibleWhen(() -> mode.is("Accent"));
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    public void spawn(Entity target) {
        if (!isEnabled() || mc().level == null) return;
        var pe = mc().particleEngine;
        int n = amount.i();
        switch (mode.get()) {
            case "Crit" -> { for (int i = 0; i < n; i++) pe.createTrackingEmitter(target, ParticleTypes.CRIT); }
            case "Magic" -> { for (int i = 0; i < n; i++) pe.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT); }
            case "Both" -> {
                for (int i = 0; i < n; i++) {
                    pe.createTrackingEmitter(target, ParticleTypes.CRIT);
                    pe.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
                }
            }
            case "Hearts" -> burst(target, n * 4, null, true);
            case "Blood" -> burst(target, n * 18, new DustParticleOptions(new Vector3f(0.75f, 0.02f, 0.06f), 1.3f), false);
            default -> burst(target, n * 18, null, false);
        }
    }

    private void burst(Entity t, int count, DustParticleOptions fixed, boolean hearts) {
        var level = mc().level;
        double cx = t.getX(), cy = t.getY() + t.getBbHeight() * 0.6, cz = t.getZ();
        for (int i = 0; i < count; i++) {
            double vx = (Math.random() - 0.5) * 0.6, vy = Math.random() * 0.4, vz = (Math.random() - 0.5) * 0.6;
            double x = cx + (Math.random() - 0.5) * t.getBbWidth(), y = cy + (Math.random() - 0.5) * t.getBbHeight() * 0.5, z = cz + (Math.random() - 0.5) * t.getBbWidth();
            if (hearts) {
                level.addParticle(ParticleTypes.HEART, x, y + 0.3, z, vx, vy, vz);
            } else {
                DustParticleOptions o = fixed;
                if (o == null) {
                    int c = color.get(i * 0.3f);
                    o = new DustParticleOptions(new Vector3f(Col.r(c), Col.g(c), Col.b(c)), 0.6f + (float) Math.random() * 0.8f);
                }
                level.addParticle(o, x, y, z, vx, vy, vz);
            }
        }
    }
}

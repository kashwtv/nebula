package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Col;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;

/** Particle trail behind you while you move. */
public final class Trails extends Module {
    public final ModeSetting mode = add(new ModeSetting("Particle", "Dust", "Dust", "Sparks", "Soul", "Glow", "Cherry", "Notes"));
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final NumberSetting density = add(new NumberSetting("Density", 1, 8, 3, 1));
    public final NumberSetting dustSize = add(new NumberSetting("Dust Size", 0.4, 2, 1, 0.1));
    public final BoolSetting onlyMoving = add(new BoolSetting("Only Moving", true));

    public Trails() {
        super("Trails", "Particle trail that follows your footsteps.", Category.VISUALS);
        color.visibleWhen(() -> mode.is("Dust"));
        dustSize.visibleWhen(() -> mode.is("Dust"));
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var level = mc().level;
        if (p == null || level == null) return;
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        if (onlyMoving.get() && dx * dx + dz * dz < 0.0004) return;
        for (int i = 0; i < density.i(); i++) {
            double ox = (Math.random() - 0.5) * 0.4, oz = (Math.random() - 0.5) * 0.4;
            double x = p.getX() - dx * Math.random() + ox, y = p.getY() + 0.1 + Math.random() * 0.15, z = p.getZ() - dz * Math.random() + oz;
            ParticleOptions opt = switch (mode.get()) {
                case "Sparks" -> ParticleTypes.END_ROD;
                case "Soul" -> ParticleTypes.SOUL_FIRE_FLAME;
                case "Glow" -> ParticleTypes.GLOW;
                case "Cherry" -> ParticleTypes.CHERRY_LEAVES;
                case "Notes" -> ParticleTypes.NOTE;
                default -> {
                    int c = color.get(i * 0.7f);
                    yield new DustParticleOptions(new Vector3f(Col.r(c), Col.g(c), Col.b(c)), dustSize.f());
                }
            };
            double vy = mode.is("Sparks") || mode.is("Soul") ? 0.02 : 0;
            level.addParticle(opt, x, y, z, 0, vy, 0);
        }
    }
}

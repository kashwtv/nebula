package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Particle multiplier for hits: extra crit and magic-crit sparks every time you hit something. */
public final class CritParticles extends Module {
    public final NumberSetting multiplier = add(new NumberSetting("Multiplier", 1, 10, 3, 1, "x"));
    public final BoolSetting crit = add(new BoolSetting("Crit", true));
    public final BoolSetting magic = add(new BoolSetting("Sharpness", true));
    public final BoolSetting always = add(new BoolSetting("Every Hit", true));

    public CritParticles() {
        super("Particle Multiplier", "More hit particles every time you attack.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getEntity() != p) return;
        boolean wasCrit = p.fallDistance > 0 && !p.onGround();
        if (!always.get() && !wasCrit) return;
        for (int i = 0; i < multiplier.i(); i++) {
            if (crit.get()) mc().particleEngine.createTrackingEmitter(e.getTarget(), ParticleTypes.CRIT);
            if (magic.get()) mc().particleEngine.createTrackingEmitter(e.getTarget(), ParticleTypes.ENCHANTED_HIT);
        }
    }
}

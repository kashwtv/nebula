package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.world.effect.MobEffects;

/** Ignores levitation and slow falling on your side so you keep normal control. */
public final class AntiLevitation extends Module {
    public AntiLevitation() {
        super("Anti Levitation", "Cancel levitation effects locally.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p.hasEffect(MobEffects.LEVITATION)) p.removeEffectNoUpdate(MobEffects.LEVITATION);
    }
}

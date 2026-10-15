package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.world.effect.MobEffects;

/** Removes the screen effects of blindness, darkness and nausea on your side only. */
public final class NoBlindness extends Module {
    public final BoolSetting blindness = add(new BoolSetting("Blindness", true));
    public final BoolSetting darkness = add(new BoolSetting("Darkness", true));
    public final BoolSetting nausea = add(new BoolSetting("Nausea", true));

    public NoBlindness() {
        super("Anti Blind", "Removes blindness, darkness and nausea visuals.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (blindness.get() && p.hasEffect(MobEffects.BLINDNESS)) p.removeEffectNoUpdate(MobEffects.BLINDNESS);
        if (darkness.get() && p.hasEffect(MobEffects.DARKNESS)) p.removeEffectNoUpdate(MobEffects.DARKNESS);
        if (nausea.get() && p.hasEffect(MobEffects.CONFUSION)) { p.removeEffectNoUpdate(MobEffects.CONFUSION); p.spinningEffectIntensity = 0; p.oSpinningEffectIntensity = 0; }
    }
}

package dev.nebula.module.visuals;

import dev.nebula.Nebula;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/** Smoothly fades gamma up past the vanilla cap, or uses an invisible night vision effect. */
public final class Fullbright extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Gamma", "Gamma", "Night Vision"));
    public final NumberSetting brightness = add(new NumberSetting("Brightness", 1, 15, 12, 0.5));

    private static Field gammaValue;
    private Double original;
    private double current = -1;

    public Fullbright() {
        super("Fullbright", "See everything, even in caves at midnight.", Category.VISUALS);
        brightness.visibleWhen(() -> mode.is("Gamma"));
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    private void setGamma(double v) {
        try {
            if (gammaValue == null) gammaValue = ObfuscationReflectionHelper.findField(OptionInstance.class, "f_231481_");
            gammaValue.set(mc().options.gamma(), v);
        } catch (Throwable t) {
            Nebula.LOG.warn("Fullbright could not set gamma", t);
        }
    }

    @Override
    public void update() {
        if (mc().options == null) return;
        boolean gammaOn = isEnabled() && mode.is("Gamma");
        if (gammaOn && original == null) {
            original = mc().options.gamma().get();
            current = original;
        }
        if (original == null) return;
        double target = gammaOn ? brightness.value : original;
        current += (target - current) * 0.18;
        if (!gammaOn && Math.abs(current - original) < 0.01) {
            setGamma(original);
            original = null;
            return;
        }
        setGamma(current);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        if (mode.is("Night Vision")) {
            MobEffectInstance e = p.getEffect(MobEffects.NIGHT_VISION);
            if (e == null) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, true, false, false));
        } else {
            removeOurEffect();
        }
    }

    @Override
    public void onDisable() {
        removeOurEffect();
    }

    private void removeOurEffect() {
        var p = mc().player;
        if (p == null) return;
        MobEffectInstance e = p.getEffect(MobEffects.NIGHT_VISION);
        if (e != null && e.isInfiniteDuration() && e.isAmbient() && !e.isVisible()) p.removeEffect(MobEffects.NIGHT_VISION);
    }
}

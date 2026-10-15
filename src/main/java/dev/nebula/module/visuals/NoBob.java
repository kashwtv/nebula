package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;

/** Turns off view bobbing and/or the hurt camera tilt, restoring your settings after. */
public final class NoBob extends Module {
    public final BoolSetting bob = add(new BoolSetting("No View Bob", true));
    public final BoolSetting hurt = add(new BoolSetting("No Hurt Tilt", true));

    private Boolean savedBob;
    private Double savedTilt;

    public NoBob() {
        super("No Bob", "Removes view bobbing and hurt camera shake.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        var o = mc().options;
        if (bob.get()) { if (savedBob == null) savedBob = o.bobView().get(); o.bobView().set(false); }
        if (hurt.get()) { if (savedTilt == null) savedTilt = o.damageTiltStrength().get(); o.damageTiltStrength().set(0.0); }
    }

    @Override
    public void onDisable() {
        var o = mc().options;
        if (savedBob != null) o.bobView().set(savedBob);
        if (savedTilt != null) o.damageTiltStrength().set(savedTilt);
        savedBob = null;
        savedTilt = null;
    }
}

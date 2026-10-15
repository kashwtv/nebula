package dev.nebula.module.hud;

import dev.nebula.module.setting.BoolSetting;
import dev.nebula.util.Clicks;

public final class CpsHud extends StatHud {
    public final BoolSetting right = add(new BoolSetting("Right Click", true));

    public CpsHud() {
        super("CPS", "Clicks per second.", 0.0f, 0.36f);
    }

    @Override
    protected String label() {
        return "CPS";
    }

    @Override
    protected String value(boolean editor) {
        return right.get() ? Clicks.left() + " | " + Clicks.right() : String.valueOf(Clicks.left());
    }
}

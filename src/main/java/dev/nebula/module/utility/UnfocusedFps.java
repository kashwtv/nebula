package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;

/** Caps your FPS while the game window isn't focused, saving power and heat. */
public final class UnfocusedFps extends Module {
    public final NumberSetting fps = add(new NumberSetting("Background FPS", 5, 60, 15, 5));

    private Integer saved;

    public UnfocusedFps() {
        super("Unfocused FPS", "Lower FPS when tabbed out.", Category.CLIENT);
    }

    @Override
    public void update() {
        if (mc().options == null) return;
        boolean limit = isEnabled() && !mc().isWindowActive();
        var opt = mc().options.framerateLimit();
        if (limit && saved == null) { saved = opt.get(); opt.set(fps.i()); mc().getWindow().setFramerateLimit(fps.i()); }
        else if (!limit && saved != null) { opt.set(saved); mc().getWindow().setFramerateLimit(saved); saved = null; }
    }
}

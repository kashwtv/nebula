package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;

/** Shows vanilla entity hitboxes (same as F3+B) while enabled. */
public final class HitboxView extends Module {
    public HitboxView() {
        super("Hitboxes", "Show entity hitboxes like F3+B.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        mc().getEntityRenderDispatcher().setRenderHitBoxes(true);
    }

    @Override
    public void onDisable() {
        mc().getEntityRenderDispatcher().setRenderHitBoxes(false);
    }
}

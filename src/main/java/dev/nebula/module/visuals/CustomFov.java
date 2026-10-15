package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Any field of view (past the 110 cap), and optionally no FOV change from sprinting or potions. */
public final class CustomFov extends Module {
    public final NumberSetting fov = add(new NumberSetting("FOV", 30, 150, 100, 1, "°"));
    public final BoolSetting staticFov = add(new BoolSetting("Static (No Sprint Zoom)", true));

    public CustomFov() {
        super("Custom FOV", "Set any field of view.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onModifier(ComputeFovModifierEvent e) {
        if (isEnabled() && staticFov.get()) e.setNewFovModifier(1f);
    }

    @SubscribeEvent
    public void onFov(ViewportEvent.ComputeFov e) {
        if (!isEnabled() || !e.usedConfiguredFov()) return;
        double vanilla = mc().options.fov().get();
        e.setFOV(e.getFOV() * fov.value / vanilla);
    }
}

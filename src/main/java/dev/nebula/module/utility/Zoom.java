package dev.nebula.module.utility;

import dev.nebula.NebulaClient;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Anim;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Smooth animated zoom on a key (default Z). Scroll while zooming to adjust. */
public final class Zoom extends Module {
    public final NumberSetting factor = add(new NumberSetting("Zoom", 1.5, 15, 4, 0.5, "x"));
    public final ModeSetting activation = add(new ModeSetting("Key Mode", "Hold", "Hold", "Toggle"));
    public final NumberSetting smoothness = add(new NumberSetting("Animation Speed", 2, 30, 12, 1));
    public final BoolSetting cinematic = add(new BoolSetting("Smooth Camera", true));
    public final BoolSetting scroll = add(new BoolSetting("Scroll Adjust", true));

    private final Anim anim = new Anim(1);
    private double scrollZoom = -1;
    private boolean toggled;
    private boolean savedSmooth;
    private boolean applied;

    public Zoom() {
        super("Zoom", "Smooth zoom on a key. Scroll to zoom further.", Category.VISUALS);
        setEnabled(true);
    }

    @Override
    public boolean listed() {
        return false;
    }

    public boolean zooming() {
        if (!isEnabled() || mc().screen != null) return false;
        return activation.is("Toggle") ? toggled : NebulaClient.ZOOM.isDown();
    }

    @Override
    public void update() {
        if (activation.is("Toggle")) {
            while (NebulaClient.ZOOM.consumeClick()) toggled = !toggled;
        }
        boolean z = zooming();
        if (!z) scrollZoom = -1;
        if (z && cinematic.get() && !applied) {
            savedSmooth = mc().options.smoothCamera;
            mc().options.smoothCamera = true;
            applied = true;
        } else if (!z && applied) {
            mc().options.smoothCamera = savedSmooth;
            applied = false;
        }
    }

    @SubscribeEvent
    public void onFov(ViewportEvent.ComputeFov e) {
        if (!e.usedConfiguredFov()) return;
        double target = zooming() ? (scrollZoom > 0 ? scrollZoom : factor.value) : 1;
        float z = anim.update((float) target, smoothness.f());
        if (z > 1.001f) e.setFOV(e.getFOV() / z);
    }

    @SubscribeEvent
    public void onScroll(InputEvent.MouseScrollingEvent e) {
        if (!zooming() || !scroll.get()) return;
        if (scrollZoom < 0) scrollZoom = factor.value;
        scrollZoom = Math.max(1.2, Math.min(50, scrollZoom * (e.getScrollDelta() > 0 ? 1.25 : 0.8)));
        e.setCanceled(true);
    }
}

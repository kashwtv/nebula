package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Removes overlays that block your view. */
public final class CleanView extends Module {
    public final BoolSetting fire = add(new BoolSetting("No Fire", true));
    public final BoolSetting water = add(new BoolSetting("No Water", true));
    public final BoolSetting suffocation = add(new BoolSetting("No Suffocation", true));
    public final BoolSetting pumpkin = add(new BoolSetting("No Pumpkin", true));
    public final BoolSetting vignette = add(new BoolSetting("No Vignette", true));
    public final BoolSetting portal = add(new BoolSetting("No Portal", true));
    public final BoolSetting spyglass = add(new BoolSetting("No Spyglass", false));

    public CleanView() {
        super("Clean View", "No fire, pumpkin, water or portal overlays in your face.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockScreenEffectEvent e) {
        if (!isEnabled()) return;
        switch (e.getOverlayType()) {
            case FIRE -> { if (fire.get()) e.setCanceled(true); }
            case WATER -> { if (water.get()) e.setCanceled(true); }
            case BLOCK -> { if (suffocation.get()) e.setCanceled(true); }
        }
    }

    @SubscribeEvent
    public void onGuiOverlay(RenderGuiOverlayEvent.Pre e) {
        if (!isEnabled()) return;
        var id = e.getOverlay().id();
        if ((pumpkin.get() && id.equals(VanillaGuiOverlay.HELMET.id()))
                || (vignette.get() && id.equals(VanillaGuiOverlay.VIGNETTE.id()))
                || (portal.get() && id.equals(VanillaGuiOverlay.PORTAL.id()))
                || (spyglass.get() && id.equals(VanillaGuiOverlay.SPYGLASS.id()))) {
            e.setCanceled(true);
        }
    }
}

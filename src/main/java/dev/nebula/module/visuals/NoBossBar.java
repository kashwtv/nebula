package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Hide screen clutter: boss bars, the sidebar scoreboard, title text, potion icons. */
public final class NoBossBar extends Module {
    public final BoolSetting bossBars = add(new BoolSetting("Boss Bars", true));
    public final BoolSetting scoreboard = add(new BoolSetting("Scoreboard", false));
    public final BoolSetting titles = add(new BoolSetting("Titles", false));
    public final BoolSetting potionIcons = add(new BoolSetting("Potion Icons", false));

    public NoBossBar() {
        super("Hide HUD Clutter", "Hide boss bars, scoreboard and more.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onBoss(CustomizeGuiOverlayEvent.BossEventProgress e) {
        if (isEnabled() && bossBars.get()) e.setCanceled(true);
    }

    @SubscribeEvent
    public void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (!isEnabled()) return;
        var id = e.getOverlay().id();
        if ((scoreboard.get() && id.equals(VanillaGuiOverlay.SCOREBOARD.id()))
                || (titles.get() && id.equals(VanillaGuiOverlay.TITLE_TEXT.id()))
                || (potionIcons.get() && id.equals(VanillaGuiOverlay.POTION_ICONS.id()))) e.setCanceled(true);
    }
}

package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Hides your first-person hand(s) for clean screenshots and videos. */
public final class HideHand extends Module {
    public final BoolSetting main = add(new BoolSetting("Main Hand", true));
    public final BoolSetting off = add(new BoolSetting("Off Hand", true));

    public HideHand() {
        super("Hide Hand", "Hide your first-person hands.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onHand(RenderHandEvent e) {
        if (!isEnabled()) return;
        if ((e.getHand() == InteractionHand.MAIN_HAND && main.get()) || (e.getHand() == InteractionHand.OFF_HAND && off.get())) e.setCanceled(true);
    }
}

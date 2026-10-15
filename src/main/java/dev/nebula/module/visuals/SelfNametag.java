package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Shows your own name tag in third person. */
public final class SelfNametag extends Module {
    public SelfNametag() {
        super("Self Nametag", "See your own name in third person.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onTag(RenderNameTagEvent e) {
        if (isEnabled() && e.getEntity() == mc().player && !mc().options.getCameraType().isFirstPerson()) e.setResult(Event.Result.ALLOW);
    }
}

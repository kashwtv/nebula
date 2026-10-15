package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.util.Place;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/** Middle-click to throw an ender pearl from your hotbar without switching to it. */
public final class MiddleClickPearl extends Module {
    public MiddleClickPearl() {
        super("Middle Click Pearl", "Throw a pearl with middle click.", Category.PLAYER);
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseButton.Pre e) {
        var p = mc().player;
        if (!isEnabled() || p == null || mc().screen != null || e.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || e.getAction() != GLFW.GLFW_PRESS) return;
        if (mc().hitResult instanceof net.minecraft.world.phys.EntityHitResult) return; // keep middle-click friend toggling
        int slot = Place.findItem(Items.ENDER_PEARL);
        if (slot < 0 || p.getCooldowns().isOnCooldown(Items.ENDER_PEARL)) return;
        int prev = p.getInventory().selected;
        p.getInventory().selected = slot;
        mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
        p.getInventory().selected = prev;
        e.setCanceled(true);
    }
}

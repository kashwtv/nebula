package dev.nebula.module.player;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nebula.gui.ClickGui;
import dev.nebula.gui.HudEditor;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/** Keep walking, jumping and sprinting while menus are open. */
public final class InventoryMove extends Module {
    public final BoolSetting jump = add(new BoolSetting("Jump", true));

    public InventoryMove() {
        super("Inventory Move", "Walk around with your inventory open.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var s = mc().screen;
        if (s == null || s instanceof ChatScreen || s instanceof AbstractSignEditScreen || s instanceof AnvilScreen
                || s instanceof BookEditScreen || s instanceof ClickGui || s instanceof HudEditor) return;
        if (s instanceof CreativeModeInventoryScreen c && c.isInventoryOpen() == false) return;
        var o = mc().options;
        KeyMapping[] keys = jump.get()
                ? new KeyMapping[]{o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keySprint}
                : new KeyMapping[]{o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keySprint};
        long win = mc().getWindow().getWindow();
        for (KeyMapping k : keys) {
            var key = k.getKey();
            if (key.getType() != InputConstants.Type.KEYSYM) continue;
            k.setDown(InputConstants.isKeyDown(win, key.getValue()));
        }
    }
}

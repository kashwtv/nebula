package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Your main inventory (not the hotbar) always on screen. */
public final class InventoryHud extends HudModule {
    public final BoolSetting slots = add(new BoolSetting("Slot Backgrounds", true));

    public InventoryHud() {
        super("Inventory", "Shows the 27 main inventory slots.", 1.0f, 0.75f);
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        var p = mc().player;
        w = 9 * 18 + 6;
        h = 3 * 18 + 6;
        if (p == null) return;
        Skin.plate(g, 0, 0, w, h);
        for (int i = 0; i < 27; i++) {
            int col = i % 9, row = i / 9;
            float x = 3 + col * 18, y = 3 + row * 18;
            if (slots.get()) R2D.rect(g, x, y, 17, 17, 1.5f, 0x50000000);
            ItemStack st = p.getInventory().getItem(9 + i);
            if (st.isEmpty()) continue;
            g.renderItem(st, (int) x, (int) y);
            g.renderItemDecorations(R2D.font(), st, (int) x, (int) y);
        }
    }
}

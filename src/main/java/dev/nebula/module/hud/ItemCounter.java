package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Counts PvP essentials across your whole inventory. */
public final class ItemCounter extends HudModule {
    public final ModeSetting layout = add(new ModeSetting("Layout", "Horizontal", "Horizontal", "Vertical"));
    public final BoolSetting hideZero = add(new BoolSetting("Hide Empty", true));
    public final BoolSetting totems = add(new BoolSetting("Totems", true));
    public final BoolSetting gapples = add(new BoolSetting("Golden Apples", true));
    public final BoolSetting pearls = add(new BoolSetting("Ender Pearls", true));
    public final BoolSetting arrows = add(new BoolSetting("Arrows", true));
    public final BoolSetting xp = add(new BoolSetting("XP Bottles", true));
    public final BoolSetting crystals = add(new BoolSetting("Crystals", true));
    public final BoolSetting obsidian = add(new BoolSetting("Obsidian", false));

    public ItemCounter() {
        super("Item Counter", "Totems, gapples, pearls, arrows at a glance.", 0.5f, 0.8f);
    }

    private int count(Item... items) {
        var p = mc().player;
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            for (Item it : items) if (st.is(it)) n += st.getCount();
        }
        return n;
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        var p = mc().player;
        if (p == null) return;
        List<Object[]> e = new ArrayList<>();
        if (totems.get()) e.add(new Object[]{Items.TOTEM_OF_UNDYING, count(Items.TOTEM_OF_UNDYING)});
        if (gapples.get()) e.add(new Object[]{Items.GOLDEN_APPLE, count(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)});
        if (pearls.get()) e.add(new Object[]{Items.ENDER_PEARL, count(Items.ENDER_PEARL)});
        if (arrows.get()) e.add(new Object[]{Items.ARROW, count(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW)});
        if (xp.get()) e.add(new Object[]{Items.EXPERIENCE_BOTTLE, count(Items.EXPERIENCE_BOTTLE)});
        if (crystals.get()) e.add(new Object[]{Items.END_CRYSTAL, count(Items.END_CRYSTAL)});
        if (obsidian.get()) e.add(new Object[]{Items.OBSIDIAN, count(Items.OBSIDIAN)});
        if (hideZero.get() && !editor) e.removeIf(o -> (int) o[1] == 0);
        boolean vert = layout.is("Vertical");
        float cell = vert ? 18 : 30;
        w = vert ? 44 : Math.max(30, e.size() * cell + 4);
        h = vert ? Math.max(18, e.size() * cell + 4) : 26;
        if (e.isEmpty()) return;
        Skin.plate(g, 0, 0, w, h);
        for (int i = 0; i < e.size(); i++) {
            Item it = (Item) e.get(i)[0];
            int n = (int) e.get(i)[1];
            float x = vert ? 3 : 2 + i * cell + (cell - 16) / 2, y = vert ? 2 + i * cell : 2;
            g.renderItem(new ItemStack(it), (int) x, (int) y);
            String s = String.valueOf(n);
            int c = n == 0 ? Theme.RED : Theme.TEXT;
            if (vert) R2D.text(g, s, x + 20, y + 4.5f, c);
            else R2D.textCentered(g, s, x + 8, y + 16.5f, c);
        }
    }
}

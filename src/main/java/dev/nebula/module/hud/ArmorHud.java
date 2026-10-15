package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Armor and held items with durability bars that flash when nearly broken. */
public final class ArmorHud extends HudModule {
    public final ModeSetting layout = add(new ModeSetting("Layout", "Vertical", "Vertical", "Horizontal"));
    public final ModeSetting durability = add(new ModeSetting("Durability", "Percent", "Percent", "Value", "Off"));
    public final BoolSetting hands = add(new BoolSetting("Held Items", true));
    public final BoolSetting bars = add(new BoolSetting("Bars", true));
    public final BoolSetting warn = add(new BoolSetting("Low Warning", true));

    public ArmorHud() {
        super("Armor", "Your armor and items with durability.", 0.98f, 0.5f);
        setEnabled(true);
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        var p = mc().player;
        if (p == null) return;
        List<ItemStack> items = new ArrayList<>();
        items.add(p.getItemBySlot(EquipmentSlot.HEAD));
        items.add(p.getItemBySlot(EquipmentSlot.CHEST));
        items.add(p.getItemBySlot(EquipmentSlot.LEGS));
        items.add(p.getItemBySlot(EquipmentSlot.FEET));
        if (hands.get()) {
            items.add(p.getMainHandItem());
            items.add(p.getOffhandItem());
        }
        items.removeIf(ItemStack::isEmpty);
        if (items.isEmpty() && editor) items.add(new ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE));
        if (items.isEmpty()) { w = 20; h = 20; return; }

        boolean vert = layout.is("Vertical");
        boolean showText = !durability.is("Off");
        float textW = 0;
        if (showText) for (ItemStack st : items) textW = Math.max(textW, R2D.width(label(st)));
        float cellW = vert ? 20 + (showText ? textW + 4 : 0) : Math.max(20, showText ? textW + 4 : 0);
        float cellH = vert ? 19 : 19 + (showText ? 9 : 0);
        w = vert ? cellW + 4 : items.size() * cellW + 4;
        h = vert ? items.size() * cellH + 4 : cellH + 4;
        if (background.get()) Skin.plate(g, 0, 0, w, h);

        long now = System.currentTimeMillis();
        for (int i = 0; i < items.size(); i++) {
            ItemStack st = items.get(i);
            float cx = vert ? 2 : 2 + i * cellW, cy = vert ? 2 + i * cellH : 2;
            float ix = vert ? cx + 2 : cx + (cellW - 16) / 2;
            float frac = st.isDamageableItem() ? 1f - (float) st.getDamageValue() / st.getMaxDamage() : 1f;
            boolean low = warn.get() && st.isDamageableItem() && frac < 0.15f;
            if (low) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(now / 140.0));
                R2D.rect(g, ix - 1, cy, 18, 18, 3, Col.alpha(Theme.RED, 0.28f * pulse));
            }
            g.renderItem(st, (int) ix, (int) (cy + 1));
            if (!st.isDamageableItem() && st.getCount() > 1) g.renderItemDecorations(R2D.font(), st, (int) ix, (int) (cy + 1));
            if (st.isDamageableItem()) {
                int c = Col.health(frac);
                if (bars.get()) Skin.bar(g, ix, cy + 17.5f, 16, 1.2f, frac, c);
                if (showText) {
                    String s = label(st);
                    if (vert) R2D.text(g, s, ix + 20, cy + 5.5f, c);
                    else R2D.textCentered(g, s, cx + cellW / 2, cy + 20, c);
                }
            }
        }
    }

    private String label(ItemStack st) {
        if (!st.isDamageableItem()) return st.getCount() > 1 ? String.valueOf(st.getCount()) : "";
        int left = st.getMaxDamage() - st.getDamageValue();
        return durability.is("Value") ? String.valueOf(left) : Math.round(100f * left / st.getMaxDamage()) + "%";
    }
}

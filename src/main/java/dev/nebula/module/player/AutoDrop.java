package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

/** Inventory cleaner: throws out junk items (pick which) so your inventory stays tidy. */
public final class AutoDrop extends Module {
    public final BoolSetting flesh = add(new BoolSetting("Rotten Flesh", true));
    public final BoolSetting seeds = add(new BoolSetting("Seeds", false));
    public final BoolSetting stone = add(new BoolSetting("Cobble/Diorite/Granite/Andesite", false));
    public final BoolSetting dirt = add(new BoolSetting("Dirt/Gravel", false));
    public final BoolSetting misc = add(new BoolSetting("Poison Potato/Spider Eye", true));

    private int cooldown;

    public AutoDrop() {
        super("Auto Drop", "Throws away junk items.", Category.PLAYER);
    }

    private Set<Item> junk() {
        Set<Item> s = new HashSet<>();
        if (flesh.get()) s.add(Items.ROTTEN_FLESH);
        if (seeds.get()) { s.add(Items.WHEAT_SEEDS); s.add(Items.BEETROOT_SEEDS); }
        if (stone.get()) { s.add(Items.COBBLESTONE); s.add(Items.DIORITE); s.add(Items.GRANITE); s.add(Items.ANDESITE); s.add(Items.COBBLED_DEEPSLATE); s.add(Items.TUFF); }
        if (dirt.get()) { s.add(Items.DIRT); s.add(Items.GRAVEL); }
        if (misc.get()) { s.add(Items.POISONOUS_POTATO); s.add(Items.SPIDER_EYE); }
        return s;
    }

    @Override
    public void onTick() {
        if (cooldown-- > 0 || mc().screen != null) return;
        var p = mc().player;
        Set<Item> j = junk();
        for (int i = 9; i < 36; i++) {
            if (j.contains(p.getInventory().getItem(i).getItem())) {
                mc().gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, i, 1, ClickType.THROW, p);
                cooldown = 3;
                return;
            }
        }
    }
}

package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;

/** Boxes your feet in with obsidian so crystals can't hit you from the side. */
public final class Surround extends Module {
    public final NumberSetting perTick = add(new NumberSetting("Blocks Per Tick", 1, 4, 2, 1));
    public final BoolSetting floor = add(new BoolSetting("Floor", true));
    public final BoolSetting disableOnMove = add(new BoolSetting("Off When You Move", true));

    private BlockPos start;

    public Surround() {
        super("Surround", "Puts obsidian around your feet.", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        start = mc().player == null ? null : mc().player.blockPosition();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        BlockPos feet = p.blockPosition();
        if (disableOnMove.get() && start != null && !feet.equals(start)) {
            setEnabled(false);
            return;
        }
        int slot = Place.findItem(Items.OBSIDIAN);
        if (slot < 0) slot = Place.findItem(Items.ENDER_CHEST);
        if (slot < 0) return;
        int placed = 0;
        if (floor.get() && Place.replaceable(feet.below()) && Place.place(feet.below(), slot, false)) placed++;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (placed >= perTick.i()) break;
            BlockPos pos = feet.relative(d);
            if (Place.replaceable(pos) && Place.place(pos, slot, false)) placed++;
        }
    }
}

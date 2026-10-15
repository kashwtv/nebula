package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Friends;
import dev.nebula.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Traps the nearest player in obsidian, or drops a cobweb on their feet. */
public final class AutoTrap extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Obsidian", "Obsidian", "Web"));
    public final NumberSetting range = add(new NumberSetting("Range", 2, 5, 4, 0.5, "m"));
    public final NumberSetting perTick = add(new NumberSetting("Blocks Per Tick", 1, 4, 2, 1));

    public AutoTrap() {
        super("Auto Trap", "Cages the closest player in blocks or webs.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().level == null) return;
        Player target = null;
        double best = range.value;
        for (Player o : mc().level.players()) {
            if (o == p || o.isSpectator() || o.isCreative() || Friends.is(o)) continue;
            double d = p.distanceTo(o);
            if (d < best) { best = d; target = o; }
        }
        if (target == null) return;
        BlockPos feet = target.blockPosition();
        if (mode.is("Web")) {
            int slot = Place.findItem(Items.COBWEB);
            if (slot >= 0 && Place.replaceable(feet)) Place.place(feet, slot, true);
            return;
        }
        int slot = Place.findItem(Items.OBSIDIAN);
        if (slot < 0) return;
        int placed = 0;
        BlockPos head = feet.above();
        BlockPos[] spots = {
                feet.relative(Direction.NORTH), feet.relative(Direction.SOUTH), feet.relative(Direction.EAST), feet.relative(Direction.WEST),
                head.relative(Direction.NORTH), head.relative(Direction.SOUTH), head.relative(Direction.EAST), head.relative(Direction.WEST),
                head.above()};
        for (BlockPos pos : spots) {
            if (placed >= perTick.i()) break;
            if (Place.replaceable(pos) && Place.place(pos, slot, false)) placed++;
        }
    }
}

package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Place;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

/** The water-bucket clutch: places water under you just before a dangerous landing, then picks it back up. */
public final class AutoMlg extends Module {
    public final NumberSetting minFall = add(new NumberSetting("Min Fall", 4, 30, 6, 1, "m"));

    private int pickupIn = -1, slot;

    public AutoMlg() {
        super("Auto MLG", "Water-bucket clutch on big falls.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (pickupIn > 0 && --pickupIn == 0) { use(slot); pickupIn = -1; return; }
        if (p.onGround() || p.fallDistance < minFall.value || p.isFallFlying() || p.isInWater()) return;
        var from = p.position();
        var hit = p.level().clip(new ClipContext(from, from.add(0, -4, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        if (hit.getType() != HitResult.Type.BLOCK || from.y - hit.getLocation().y > 3) return;
        slot = Place.findItem(Items.WATER_BUCKET);
        if (slot < 0) return;
        use(slot);
        pickupIn = 10;
    }

    private void use(int s) {
        var p = mc().player;
        int prev = p.getInventory().selected;
        float pitch = p.getXRot();
        p.getInventory().selected = s;
        p.setXRot(90f);
        p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90f, p.onGround()));
        mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
        p.setXRot(pitch);
        p.getInventory().selected = prev;
    }
}

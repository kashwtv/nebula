package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Place;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;

/** Places a torch from your hotbar at your feet whenever it gets dark enough for mobs. */
public final class AutoTorch extends Module {
    public final NumberSetting light = add(new NumberSetting("Max Light", 0, 7, 0, 1));

    private int cooldown;

    public AutoTorch() {
        super("Auto Torch", "Lights up dark spots as you walk.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (cooldown-- > 0 || !p.onGround()) return;
        var pos = p.blockPosition();
        if (p.level().getBrightness(LightLayer.BLOCK, pos) > light.value) return;
        int slot = Place.findItem(Items.TORCH);
        if (slot >= 0 && Place.place(pos, slot, false)) cooldown = 10;
    }
}

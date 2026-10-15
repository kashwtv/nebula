package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Picks the fastest tool in your hotbar while you mine, then switches back. */
public final class AutoTool extends Module {
    public final BoolSetting swapBack = add(new BoolSetting("Swap Back", true));
    public final BoolSetting saveTools = add(new BoolSetting("Save Low Tools", true));

    private int previous = -1;

    public AutoTool() {
        super("Auto Tool", "Switches to the best tool for the block you're mining.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().level == null || p.isCreative()) return;
        boolean mining = mc().options.keyAttack.isDown() && mc().hitResult != null && mc().hitResult.getType() == HitResult.Type.BLOCK;
        if (!mining) {
            if (swapBack.get() && previous >= 0) p.getInventory().selected = previous;
            previous = -1;
            return;
        }
        BlockState st = mc().level.getBlockState(((BlockHitResult) mc().hitResult).getBlockPos());
        int best = p.getInventory().selected;
        float bestSpeed = speed(p.getInventory().getItem(best), st);
        for (int i = 0; i < 9; i++) {
            float s = speed(p.getInventory().getItem(i), st);
            if (s > bestSpeed + 0.01f) { bestSpeed = s; best = i; }
        }
        if (best != p.getInventory().selected) {
            if (previous < 0) previous = p.getInventory().selected;
            p.getInventory().selected = best;
        }
    }

    private float speed(ItemStack st, BlockState state) {
        if (saveTools.get() && st.isDamageableItem() && st.getMaxDamage() - st.getDamageValue() < 4) return 0;
        float s = st.getDestroySpeed(state);
        if (s > 1) {
            int eff = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, st);
            if (eff > 0) s += eff * eff + 1;
        }
        return s;
    }
}

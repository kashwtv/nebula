package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/**
 * Longer attack and block reach, done with Forge's reach attributes on your own player so
 * crosshair picking, Triggerbot and Kill Aura all see the new distance. The server checks
 * reach with its own copy of these attributes, so on your server match them with
 * /attribute (see CHANGES.md) or hits past vanilla range get dropped.
 */
public final class Reach extends Module {
    private static final UUID ENTITY_ID = UUID.fromString("6c1b0f3e-6f2c-4d1e-9a51-6e6562756c61");
    private static final UUID BLOCK_ID = UUID.fromString("6c1b0f3e-6f2c-4d1e-9a51-6e6562756c62");

    public final NumberSetting entity = add(new NumberSetting("Entity Reach", 3, 6, 3.5, 0.1, "m"));
    public final NumberSetting block = add(new NumberSetting("Block Reach", 3, 6, 5, 0.1, "m"));
    public final BoolSetting blocks = add(new BoolSetting("Change Block Reach", true));

    public Reach() {
        super("Reach", "Hit and build from further away.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return entity.display();
    }

    /** Current entity reach to use for range checks; vanilla 3 when off. */
    public double entityReach() {
        return isEnabled() ? entity.value : 3.0;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        apply(ForgeMod.ENTITY_REACH.get(), ENTITY_ID, entity.value, true);
        apply(ForgeMod.BLOCK_REACH.get(), BLOCK_ID, block.value, blocks.get());
    }

    private void apply(Attribute attr, UUID id, double target, boolean on) {
        AttributeInstance inst = mc().player.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (!on) {
            if (cur != null) inst.removeModifier(id);
            return;
        }
        double amount = target - inst.getBaseValue();
        if (cur != null && Math.abs(cur.getAmount() - amount) < 1e-6) return;
        if (cur != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, "Nebula reach", amount, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public void onDisable() {
        var p = mc().player;
        if (p == null) return;
        var e = p.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (e != null) e.removeModifier(ENTITY_ID);
        var b = p.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (b != null) b.removeModifier(BLOCK_ID);
    }
}

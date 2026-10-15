package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.util.Friends;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Better name tags: always shown, with health, distance and friend marker added. */
public final class Nametags extends Module {
    public final BoolSetting health = add(new BoolSetting("Health", true));
    public final BoolSetting distance = add(new BoolSetting("Distance", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs Too", false));
    public final BoolSetting always = add(new BoolSetting("Always Show", true));

    public Nametags() {
        super("Nametags", "Name tags with health and distance.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onTag(RenderNameTagEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || !(e.getEntity() instanceof LivingEntity le) || le == p) return;
        if (!(le instanceof Player) && !mobs.get()) return;
        if (always.get()) e.setResult(Event.Result.ALLOW);
        var c = Component.empty();
        if (Friends.is(le)) c.append(Component.literal("★ ").withStyle(ChatFormatting.AQUA));
        c.append(e.getContent());
        if (health.get()) {
            float hp = le.getHealth() + le.getAbsorptionAmount();
            float f = hp / Math.max(1, le.getMaxHealth());
            ChatFormatting col = f > 0.66f ? ChatFormatting.GREEN : f > 0.33f ? ChatFormatting.YELLOW : ChatFormatting.RED;
            c.append(Component.literal(String.format(" %.1f", hp)).withStyle(col));
        }
        if (distance.get()) c.append(Component.literal(String.format(" %.0fm", p.distanceTo(le))).withStyle(ChatFormatting.GRAY));
        e.setContent(c);
    }
}

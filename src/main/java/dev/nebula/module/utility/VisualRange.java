package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tells you (only you) when players enter or leave your render distance. */
public final class VisualRange extends Module {
    public final BoolSetting leave = add(new BoolSetting("Leave Messages", true));
    public final BoolSetting sound = add(new BoolSetting("Sound", true));

    private final Set<UUID> known = new HashSet<>();

    public VisualRange() {
        super("Visual Range", "Alerts when players come into view.", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        known.clear();
        if (mc().level != null) for (Player pl : mc().level.players()) known.add(pl.getUUID());
    }

    @Override
    public void onTick() {
        var p = mc().player;
        Set<UUID> now = new HashSet<>();
        for (Player pl : mc().level.players()) {
            if (pl == p) continue;
            now.add(pl.getUUID());
            if (!known.contains(pl.getUUID())) {
                p.displayClientMessage(Component.literal(pl.getGameProfile().getName() + " entered visual range").withStyle(ChatFormatting.YELLOW), false);
                if (sound.get()) p.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            }
        }
        if (leave.get()) for (UUID id : known) if (!now.contains(id) && !id.equals(p.getUUID())) {
            var info = mc().getConnection().getPlayerInfo(id);
            String name = info != null ? info.getProfile().getName() : "A player";
            p.displayClientMessage(Component.literal(name + " left visual range").withStyle(ChatFormatting.GRAY), false);
        }
        known.clear();
        known.addAll(now);
        known.add(p.getUUID());
    }
}

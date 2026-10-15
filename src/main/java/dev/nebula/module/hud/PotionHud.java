package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Active effects with icons, time left and a draining bar. */
public final class PotionHud extends HudModule {
    public final BoolSetting bars = add(new BoolSetting("Bars", true));
    public final BoolSetting hideVanilla = add(new BoolSetting("Hide Vanilla", true));
    public final BoolSetting blinkEnding = add(new BoolSetting("Blink When Ending", true));

    private final Map<MobEffect, Integer> maxSeen = new HashMap<>();

    public PotionHud() {
        super("Potions", "Your active effects with timers.", 1.0f, 0.3f);
        setEnabled(true);
    }

    @SubscribeEvent
    public void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (isEnabled() && hideVanilla.get() && e.getOverlay().id().equals(VanillaGuiOverlay.POTION_ICONS.id())) e.setCanceled(true);
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        var p = mc().player;
        if (p == null) return;
        List<MobEffectInstance> list = new ArrayList<>(p.getActiveEffects());
        if (list.isEmpty() && editor) {
            list.add(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1));
            list.add(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
        }
        list.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        float maxText = 60;
        for (MobEffectInstance e : list) maxText = Math.max(maxText, R2D.width(name(e)) + R2D.width(time(e)) + 8);
        w = 22 + maxText;
        float row = 20;
        h = Math.max(row, list.size() * row);
        if (list.isEmpty()) return;
        boolean right = rightSide();
        long now = System.currentTimeMillis();
        for (int i = 0; i < list.size(); i++) {
            MobEffectInstance e = list.get(i);
            float y = i * row;
            int dur = e.getDuration();
            maxSeen.merge(e.getEffect(), dur, Math::max);
            float frac = e.isInfiniteDuration() ? 1 : Math.min(1, dur / (float) Math.max(1, maxSeen.get(e.getEffect())));
            float prev = R2D.alpha;
            if (blinkEnding.get() && !e.isInfiniteDuration() && dur < 200) R2D.alpha *= 0.55f + 0.45f * (float) Math.abs(Math.sin(now / 160.0));
            Skin.plate(g, 0, y, w, row - 2);
            int ec = 0xFF000000 | e.getEffect().getColor();
            float ix = right ? w - 19 : 3;
            var sprite = mc().getMobEffectTextures().get(e.getEffect());
            g.blit((int) ix, (int) (y + 1), 0, 16, 16, sprite);
            float tx = right ? 4 : 22;
            R2D.text(g, name(e), tx, y + 2.5f, Theme.TEXT);
            String t = time(e);
            if (right) R2D.text(g, t, w - 22 - R2D.width(t), y + 2.5f, Theme.MUTED);
            else R2D.textRight(g, t, w - 4, y + 2.5f, Theme.MUTED);
            if (bars.get()) Skin.bar(g, tx, y + 13.5f, maxText - 6, 1.2f, frac, Col.lerp(ec, 0xFFFFFFFF, 0.25f));
            R2D.alpha = prev;
        }
    }

    private static String name(MobEffectInstance e) {
        String n = e.getEffect().getDisplayName().getString();
        int amp = e.getAmplifier();
        String[] roman = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
        return n + (amp > 0 && amp < roman.length ? roman[amp] : amp >= roman.length ? " " + (amp + 1) : "");
    }

    private static String time(MobEffectInstance e) {
        if (e.isInfiniteDuration()) return "∞";
        int s = e.getDuration() / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}

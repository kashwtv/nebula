package dev.nebula.module.visuals;

import com.mojang.math.Axis;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Move, scale and tilt your first-person hands, plus a few swing animation styles. */
public final class ViewModel extends Module {
    public final NumberSetting x = add(new NumberSetting("X", -1, 1, 0, 0.05));
    public final NumberSetting y = add(new NumberSetting("Y", -1, 1, 0, 0.05));
    public final NumberSetting z = add(new NumberSetting("Z", -1, 1, 0, 0.05));
    public final NumberSetting scale = add(new NumberSetting("Scale", 0.3, 1.5, 1, 0.05, "x"));
    public final NumberSetting tilt = add(new NumberSetting("Tilt", -45, 45, 0, 1, "°"));
    public final ModeSetting swing = add(new ModeSetting("Swing", "Vanilla", "Vanilla", "Spin", "Stab", "Smooth"));

    public ViewModel() {
        super("View Model", "Customise your hand position and swing.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onHand(RenderHandEvent e) {
        if (!isEnabled()) return;
        var ps = e.getPoseStack();
        float side = e.getHand() == InteractionHand.MAIN_HAND ? 1 : -1;
        ps.translate(x.f() * side, y.f(), z.f());
        ps.mulPose(Axis.ZP.rotationDegrees(tilt.f() * side));
        ps.scale(scale.f(), scale.f(), scale.f());
        float sw = e.getSwingProgress();
        if (sw > 0 && e.getHand() == InteractionHand.MAIN_HAND) {
            switch (swing.get()) {
                case "Spin" -> ps.mulPose(Axis.YP.rotationDegrees(sw * 360f));
                case "Stab" -> ps.translate(0, 0, -Math.sin(sw * Math.PI) * 0.4);
                case "Smooth" -> ps.mulPose(Axis.XP.rotationDegrees((float) -Math.sin(sw * Math.PI) * 30f));
                default -> {}
            }
        }
    }
}

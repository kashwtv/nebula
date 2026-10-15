package dev.nebula.module.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.render.World3D;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows exactly where your bow, crossbow, trident, pearl, snowball or potion will land,
 * with a marker on the block face and a red box if it will hit someone.
 */
public final class Trajectories extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final BoolSetting marker = add(new BoolSetting("Landing Marker", true));
    public final BoolSetting entities = add(new BoolSetting("Highlight Hit", true));

    public Trajectories() {
        super("Trajectories", "Shows where your arrows, pearls and potions will land.", Category.VISUALS);
        setEnabled(true);
    }

    /** {speed, gravity, drag, pitchOffset} for the held item, or null. */
    private static double[] params(Player p, ItemStack st) {
        Item it = st.getItem();
        if (it instanceof BowItem) {
            float power = p.isUsingItem() ? BowItem.getPowerForTime(p.getTicksUsingItem()) : 1f;
            return power < 0.1f ? null : new double[]{power * 3.0, 0.05, 0.99, 0};
        }
        if (it instanceof CrossbowItem) return CrossbowItem.isCharged(st) ? new double[]{3.15, 0.05, 0.99, 0} : null;
        if (it instanceof TridentItem) return new double[]{2.5, 0.05, 0.99, 0};
        if (it instanceof EnderpearlItem || it instanceof SnowballItem || it instanceof EggItem) return new double[]{1.5, 0.03, 0.99, 0};
        if (it instanceof ThrowablePotionItem) return new double[]{0.5, 0.05, 0.99, -20};
        if (it instanceof ExperienceBottleItem) return new double[]{0.7, 0.07, 0.99, -20};
        return null;
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var p = mc().player;
        var level = mc().level;
        if (p == null || level == null) return;
        ItemStack st = p.getMainHandItem();
        double[] pr = params(p, st);
        if (pr == null) {
            st = p.getOffhandItem();
            pr = params(p, st);
        }
        if (pr == null) return;

        float pt = e.getPartialTick();
        double yaw = Math.toRadians(p.getViewYRot(pt)), pitch = Math.toRadians(p.getViewXRot(pt) + pr[3]);
        Vec3 pos = new Vec3(Mth.lerp(pt, p.xo, p.getX()), Mth.lerp(pt, p.yo, p.getY()) + p.getEyeHeight() - 0.1, Mth.lerp(pt, p.zo, p.getZ()));
        Vec3 vel = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)).normalize().scale(pr[0]);
        Vec3 pv = p.getDeltaMovement();
        vel = vel.add(pv.x, p.onGround() ? 0 : pv.y, pv.z);

        List<Vec3> pts = new ArrayList<>();
        pts.add(pos);
        HitResult hit = null;
        Entity hitEntity = null;
        for (int i = 0; i < 300; i++) {
            Vec3 next = pos.add(vel);
            BlockHitResult bh = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 end = bh.getType() != HitResult.Type.MISS ? bh.getLocation() : next;
            double best = Double.MAX_VALUE;
            for (Entity en : level.getEntities(p, new AABB(pos, end).inflate(1), en -> en.isPickable() && !en.isSpectator())) {
                var clip = en.getBoundingBox().inflate(0.3).clip(pos, end);
                if (clip.isPresent() && clip.get().distanceToSqr(pos) < best) {
                    best = clip.get().distanceToSqr(pos);
                    hitEntity = en;
                    end = clip.get();
                }
            }
            pts.add(end);
            if (hitEntity != null) break;
            if (bh.getType() != HitResult.Type.MISS) { hit = bh; break; }
            double drag = level.getFluidState(net.minecraft.core.BlockPos.containing(next)).isEmpty() ? pr[2] : 0.6;
            vel = vel.scale(drag).subtract(0, pr[1], 0);
            pos = next;
            if (pos.y < level.getMinBuildHeight() - 16) break;
        }

        Vec3 cam = e.getCamera().getPosition();
        Matrix4f m = e.getPoseStack().last().pose();
        RenderSystem.lineWidth(2.5f);
        BufferBuilder b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, true);
        int n = pts.size();
        for (int i = 2; i < n; i++) {
            Vec3 a = pts.get(i - 1), c = pts.get(i);
            float fade = Math.min(1, (i - 1) / 4f);
            int col = color.get(i * 0.03f);
            World3D.v(b, m, a.x - cam.x, a.y - cam.y, a.z - cam.z, col, fade);
            World3D.v(b, m, c.x - cam.x, c.y - cam.y, c.z - cam.z, col, fade);
        }
        if (hitEntity != null && entities.get()) {
            World3D.boxEdges(b, m, hitEntity.getBoundingBox().move(-cam.x, -cam.y, -cam.z), 0xFFFF4B4B, 1);
        }
        World3D.end(b);

        if (marker.get() && hit instanceof BlockHitResult bh) {
            Vec3 at = bh.getLocation();
            var face = bh.getDirection();
            double r = 0.35;
            BufferBuilder q = World3D.begin(VertexFormat.Mode.QUADS, true);
            int col = color.get();
            double ox = at.x - cam.x + face.getStepX() * 0.01, oy = at.y - cam.y + face.getStepY() * 0.01, oz = at.z - cam.z + face.getStepZ() * 0.01;
            double[][] c;
            switch (face.getAxis()) {
                case Y -> c = new double[][]{{-r, 0, -r}, {r, 0, -r}, {r, 0, r}, {-r, 0, r}};
                case X -> c = new double[][]{{0, -r, -r}, {0, r, -r}, {0, r, r}, {0, -r, r}};
                default -> c = new double[][]{{-r, -r, 0}, {r, -r, 0}, {r, r, 0}, {-r, r, 0}};
            }
            for (double[] v : c) World3D.v(q, m, ox + v[0], oy + v[1], oz + v[2], col, 0.35f);
            World3D.end(q);
        }
    }
}

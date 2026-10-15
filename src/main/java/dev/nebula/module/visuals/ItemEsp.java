package dev.nebula.module.visuals;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Boxes and name/count labels on dropped items, visible through walls. */
public final class ItemEsp extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.14f));
    public final BoolSetting labels = add(new BoolSetting("Labels", true));
    public final BoolSetting rareOnly = add(new BoolSetting("Rare Only", false));
    public final NumberSetting range = add(new NumberSetting("Range", 8, 128, 48, 4, "m"));

    public ItemEsp() {
        super("Item ESP", "Highlights dropped items through walls.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var cam = e.getCamera().getPosition();
        var m = e.getPoseStack().last().pose();
        float pt = e.getPartialTick();
        java.util.List<ItemEntity> list = new java.util.ArrayList<>();
        for (Entity en : mc().level.entitiesForRendering())
            if (en instanceof ItemEntity it && it.distanceTo(p) <= range.value
                    && (!rareOnly.get() || it.getItem().getRarity() != net.minecraft.world.item.Rarity.COMMON || it.getItem().isEnchanted())) list.add(it);
        if (list.isEmpty()) return;
        var b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, false);
        for (ItemEntity it : list) {
            double dx = Mth.lerp(pt, it.xOld, it.getX()) - it.getX(), dy = Mth.lerp(pt, it.yOld, it.getY()) - it.getY(), dz = Mth.lerp(pt, it.zOld, it.getZ()) - it.getZ();
            World3D.boxEdges(b, m, it.getBoundingBox().move(dx - cam.x, dy - cam.y, dz - cam.z), color.get(), 0.9f);
        }
        World3D.end(b);
        if (!labels.get()) return;
        for (ItemEntity it : list) {
            var st = it.getItem();
            String t = st.getHoverName().getString() + (st.getCount() > 1 ? " x" + st.getCount() : "");
            World3D.label(e.getPoseStack(), e.getCamera(), Mth.lerp(pt, it.xOld, it.getX()) - cam.x, Mth.lerp(pt, it.yOld, it.getY()) - cam.y + 0.6,
                    Mth.lerp(pt, it.zOld, it.getZ()) - cam.z, t, 0xFFFFFFFF, 0.8f);
        }
    }
}

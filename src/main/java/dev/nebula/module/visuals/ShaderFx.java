package dev.nebula.module.visuals;

import dev.nebula.Nebula;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

/** Full-screen vanilla post shaders (the old "Super Secret Settings"): invert, retro, blur and more. */
public final class ShaderFx extends Module {
    public final ModeSetting effect = add(new ModeSetting("Effect", "Desaturate", "Desaturate", "Invert", "Blur", "Bits", "Pencil",
            "Phosphor", "Creeper", "Spider", "Wobble", "NTSC", "Sobel", "Deconverge", "Green", "Art", "Notch", "Flip"));

    private static Method loadEffect;
    private String loaded;

    public ShaderFx() {
        super("Shader FX", "Full-screen retro and artsy shaders.", Category.VISUALS);
    }

    @Override
    public String suffix() {
        return effect.get();
    }

    @Override
    public void onTick() {
        String want = effect.get().toLowerCase(java.util.Locale.ROOT);
        if (want.equals(loaded) && mc().gameRenderer.currentEffect() != null) return;
        try {
            if (loadEffect == null) loadEffect = ObfuscationReflectionHelper.findMethod(GameRenderer.class, "m_109128_", ResourceLocation.class);
            loadEffect.invoke(mc().gameRenderer, new ResourceLocation("minecraft", "shaders/post/" + want + ".json"));
            loaded = want;
        } catch (Throwable t) {
            Nebula.LOG.warn("Shader FX could not load {}", want, t);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        loaded = null;
        mc().gameRenderer.shutdownEffect();
    }
}

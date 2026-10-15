package dev.nebula;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Nebula.MODID)
public final class Nebula {
    public static final String MODID = "nebula";
    public static final String VERSION = "1.0";
    public static final Logger LOG = LogUtils.getLogger();

    public Nebula() {
        // Client-only mod: never ask servers to have it.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NebulaClient.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}

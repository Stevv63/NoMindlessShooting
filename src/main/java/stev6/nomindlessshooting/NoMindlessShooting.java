package stev6.nomindlessshooting;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NoMindlessShooting.MODID)
public class NoMindlessShooting {
  public static final String MODID = "nomindlessshooting";
  public static final Logger LOGGER = LogUtils.getLogger();

  public NoMindlessShooting(FMLJavaModLoadingContext ctx) {
    MinecraftForge.EVENT_BUS.register(this);
    MinecraftForge.EVENT_BUS.register(new TacZListener());
    ctx.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

    LOGGER.info("NoMindlessShooting started.");
  }
}

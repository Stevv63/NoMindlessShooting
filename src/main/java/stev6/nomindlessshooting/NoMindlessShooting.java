/*
   This file is part of NoMindlessShooting.
   NoMindlessShooting is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
   NoMindlessShooting is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
   You should have received a copy of the GNU General Public License along with NoMindlessShooting. If not, see <https://www.gnu.org/licenses/>.
*/

package stev6.nomindlessshooting;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.UUID;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NoMindlessShooting.MODID)
public class NoMindlessShooting {
  public static final String MODID = "nomindlessshooting";
  public static final Logger LOGGER = LogUtils.getLogger();
  static LoadingCache<UUID, TacZListener.ShotsData> SHOTS;

  public NoMindlessShooting(FMLJavaModLoadingContext ctx) {
    MinecraftForge.EVENT_BUS.register(new TacZListener());
    IEventBus modBus = ctx.getModEventBus();
    modBus.addListener(this::setup);
    ctx.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

    LOGGER.debug("NoMindlessShooting started.");
  }

  @SubscribeEvent
  public void setup(FMLCommonSetupEvent e) {
    e.enqueueWork(
        () -> {
          SHOTS =
              CacheBuilder.newBuilder()
                  .expireAfterAccess(Duration.ofSeconds(Config.timeLimit))
                  .removalListener(
                      (RemovalNotification<UUID, TacZListener.ShotsData> u) -> {
                        TacZListener.ShotsData data = u.getValue();
                        LOGGER.debug(
                            "Entry for UUID {} was removed with count {}. Due to {} ",
                            u.getKey(),
                            data != null ? data.count : 0,
                            u.getCause());
                      })
                  .build(CacheLoader.from(k -> new TacZListener.ShotsData()));
        });
  }
}

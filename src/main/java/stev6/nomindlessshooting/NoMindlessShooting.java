/*
    This file is part of NoMindlessShooting.
    NoMindlessShooting is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    NoMindlessShooting is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
    You should have received a copy of the GNU General Public License along with NoMindlessShooting. If not, see <https://www.gnu.org/licenses/>.
 */

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

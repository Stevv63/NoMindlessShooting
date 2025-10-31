package stev6.nomindlessshooting;

import static stev6.nomindlessshooting.NoMindlessShooting.LOGGER;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.smileycorp.hordes.hordeevent.capability.HordeEvent;
import net.smileycorp.hordes.hordeevent.capability.HordeSavedData;
import net.smileycorp.hordes.hordeevent.data.HordeTableLoader;

public class TheHordesIntegration {

  public static void tryStartHorde(ServerPlayer p) {
    if (p.level().isClientSide) return;
    LOGGER.debug("Attempting to start a horde for player {}.", p.getName().getString());
    ServerLevel sl = (ServerLevel) p.level();
    HordeSavedData data = HordeSavedData.getData(sl);
    HordeEvent event = data.getEvent(p);
    if (Config.hordeSpawnTable != null)
      event.setSpawntable(HordeTableLoader.INSTANCE.getTable(Config.hordeSpawnTable));
    event.tryStartEvent(p, Config.duration, true);
    LOGGER.info(
        "Hordes integration horde started, spawnTable is: {} with duration {}",
        event.getSpawntable().getName(),
        Config.duration);
  }

  public static boolean isHordeOngoing(ServerPlayer p) {
    ServerLevel sl = (ServerLevel) p.level();
    HordeSavedData data = HordeSavedData.getData(sl);
    HordeEvent event = data.getEvent(p);
    return event.isActive(p);
  }
}

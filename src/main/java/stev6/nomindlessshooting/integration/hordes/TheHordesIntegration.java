package stev6.nomindlessshooting.integration.hordes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.smileycorp.hordes.hordeevent.HordeSpawnTable;
import net.smileycorp.hordes.hordeevent.capability.HordeEvent;
import net.smileycorp.hordes.hordeevent.capability.HordeSavedData;
import net.smileycorp.hordes.hordeevent.data.HordeTableLoader;
import stev6.nomindlessshooting.Config;

import static stev6.nomindlessshooting.NoMindlessShooting.LOGGER;

public class TheHordesIntegration {

    public static void tryStartHorde(ServerPlayer p) {
        if (p.level().isClientSide) return;
        LOGGER.debug("Attempting to start a horde for player {}.", p.getName().getString());

        ServerLevel sl = (ServerLevel) p.level();
        HordeSavedData data = HordeSavedData.getData(sl);
        HordeEvent event = data.getEvent(p);

        Integration i = Integration.INSTANCE;

        if (Config.hordeSpawnTable != null)
            i.setSpawnTable(event, HordeTableLoader.INSTANCE.getTable(Config.hordeSpawnTable));

        event.tryStartEvent(p, Config.duration, true);

        HordeSpawnTable table = i.getSpawnTable(event);
        LOGGER.info(
                "Hordes integration horde started, spawnTable is: {} with duration {}",
                table != null ? table.getName() : "None",
                Config.duration);
    }

    public static boolean isHordeOngoing(ServerPlayer p) {
        ServerLevel sl = (ServerLevel) p.level();
        HordeSavedData data = HordeSavedData.getData(sl);
        HordeEvent event = data.getEvent(p);
        return Integration.INSTANCE.isActive(event);
    }
}

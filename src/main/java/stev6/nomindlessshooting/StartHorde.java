package stev6.nomindlessshooting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.smileycorp.hordes.hordeevent.capability.HordeEvent;
import net.smileycorp.hordes.hordeevent.capability.HordeSavedData;

public class StartHorde {

  public void tryStartHorde(ServerPlayer p) {
    if (p.level().isClientSide) {
      return;
    }
    ServerLevel sl = (ServerLevel) p.level();
    HordeSavedData data = HordeSavedData.getData(sl);
    HordeEvent event = data.getEvent(p);
    event.tryStartEvent(p, 2400, true);
  }

  public boolean isHordeOngoing(ServerPlayer p) {
    ServerLevel sl = (ServerLevel) p.level();
    HordeSavedData data = HordeSavedData.getData(sl);
    HordeEvent event = data.getEvent(p);

    return event.isActive(p);
  }
}

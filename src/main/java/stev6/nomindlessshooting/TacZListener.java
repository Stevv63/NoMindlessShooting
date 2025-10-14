package stev6.nomindlessshooting;

import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TacZListener {

  private static final StartHorde HORDE = new StartHorde();
  private static final Map<ServerPlayer, ShotsData> shots = new WeakHashMap<>();

  @SubscribeEvent
  public void onFire(GunShootEvent e) {
    if (e.getLogicalSide().isClient()) return;
    if (!(e.getShooter() instanceof ServerPlayer p)) return;
    if (HORDE.isHordeOngoing(p)) return;
    IGun gun = IGun.getIGunOrNull(e.getGunItemStack());
    if (gun == null) return;
    ResourceLocation muzzleId = gun.getAttachmentId(e.getGunItemStack(), AttachmentType.MUZZLE);
    boolean isSuppressed = !Config.SILENCER_IDS.isEmpty() && Config.SILENCER_IDS.contains(muzzleId);
    if (isSuppressed) return;

    Instant now = Instant.now();
    Vec3 pos = p.position();
    ShotsData data = shots.computeIfAbsent(p, k -> new ShotsData());
    if (data.lastPos == null
        || data.lastTime == null
        || Duration.between(data.lastTime, now).toSeconds() > Config.timeLimit
        || data.lastPos.distanceToSqr(pos) > Config.radius * Config.radius) {
      data.count = 0;
      data.lastPos = pos;
    }
    data.count++;
    data.lastTime = now;

    if (data.count == Config.threshold / 2) {
      if (!Config.noiseWarningMessage.isEmpty()) {
        p.displayClientMessage(
            Component.literal(Config.noiseWarningMessage).withStyle(ChatFormatting.DARK_RED), true);
      }
    }

    if (data.count >= Config.threshold) {
      if (!Config.hordeStartMessage.isEmpty()) {
        p.displayClientMessage(
            Component.literal(Config.hordeStartMessage)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
            false);
      }
      HORDE.tryStartHorde(p);
      data.count = 0;
    }
  }

  private static class ShotsData {
    Vec3 lastPos;
    int count;
    Instant lastTime;
  }
}

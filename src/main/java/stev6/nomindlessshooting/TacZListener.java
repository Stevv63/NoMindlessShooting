package stev6.nomindlessshooting;

import static stev6.nomindlessshooting.NoMindlessShooting.LOGGER;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TacZListener {

  private static final TriggerHandler TRIGGER = new TriggerHandler();
  private static final Map<UUID, ShotsData> SHOTS = new ConcurrentHashMap<>();


  @SubscribeEvent
  public void onFire(GunShootEvent e) {
    if (e.getLogicalSide().isClient()) return;
    if (!(e.getShooter() instanceof ServerPlayer p)) return;
    if (shouldIgnore(e.getGunItemStack())) {
      LOGGER.debug("Shot by {} ignored", p.getName());
      return;
    }

    if (Config.ignoreEncased && isEncased(p)) {
      LOGGER.debug("{} shot but is encased", p.getName());
      return;
    }

    if (Config.useHorde && TheHordesIntegration.isHordeOngoing(p)) return;

    Instant now = Instant.now();
    Vec3 pos = p.position();
    ShotsData data = SHOTS.computeIfAbsent(p.getUUID(), k -> new ShotsData());

    if (data.lastTrigger != null && Duration.between(data.lastTrigger, now).toSeconds() < Config.cooldown) {
      return;
    }

    if (data.lastPos == null || data.lastPos.distanceToSqr(pos) > Config.hearingRadius * Config.hearingRadius) {
      data.shots.clear();
      if (data.lastPos == null) {
        LOGGER.debug("No last position data available for {}, resetting shot count.", p.getName());
      } else {
        LOGGER.debug("Player {} moved out of their shot counting radius, resetting shot count.", p.getName());
      }
      data.lastPos = pos;
    }

    data.shots.removeIf(t -> Duration.between(t, now).toSeconds() > Config.timeLimit);
    data.shots.addLast(now);

    int count = data.shots.size();

    if (count == Config.threshold / 2 && Config.noiseWarningMessage != null) {
      p.displayClientMessage(Config.noiseWarningMessage, !Config.noiseWarningMessageInChat);
    }

    if (count >= Config.threshold) {
      TRIGGER.trigger(p);
      data.lastTrigger = now;
      data.shots.clear();
    }

    LOGGER.debug("Player {} now has {} shots in the time window.", p.getName(), count);
  }

  @SubscribeEvent
  public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) SHOTS.remove(p.getUUID());
  }

  private static boolean shouldIgnore(ItemStack i) {
    var iGun = IGun.getIGunOrNull(i);
    if (iGun == null) return false;

    var gunId = iGun.getGunId(i);

    if (Config.excludedWeapons.contains(gunId)) return true;

    if (!Config.ignoreSilencer) return false;

    var gunData = TimelessAPI.getCommonGunIndex(gunId)
            .map(CommonGunIndex::getGunData)
            .orElse(null);
    if (gunData == null) return false;

    var cache = new AttachmentCacheProperty();
    cache.eval(i, gunData);

    var silenceData = cache.getCache(SilenceModifier.ID);
    return silenceData instanceof Pair<?, ?> pair && Boolean.TRUE.equals(pair.right());
  }

  private static boolean isEncased(ServerPlayer p) {
    BlockPos pos = p.blockPosition();
    return p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ())
            > pos.getY() + 1
        && p.level().getBrightness(LightLayer.SKY, pos) == 0;
  }

  private static final class ShotsData {
    Vec3 lastPos;
    Deque<Instant> shots = new ArrayDeque<>();
    Instant lastTrigger;
  }
}

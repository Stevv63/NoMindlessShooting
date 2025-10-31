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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TacZListener {

  private static final TriggerHandler trigger = new TriggerHandler();

  @SubscribeEvent
  public void onFire(GunShootEvent e) {
    if (e.getLogicalSide().isClient()) return;
    if (!(e.getShooter() instanceof ServerPlayer p)) return;
    if (isSilenced(e.getGunItemStack()) && Config.ignoreSilencer) {
      LOGGER.debug("Silenced by {}", p.getName());
      return;
    }
    if (Config.ignoreEncased && isEncased(p)) {
      LOGGER.debug("{} shot but is encased", p.getName());
      return;
    }
    if (Config.useHorde && TheHordesIntegration.isHordeOngoing(p)) return;

    Instant now = Instant.now();
    Vec3 pos = p.position();
    ShotsData data = NoMindlessShooting.SHOTS.getUnchecked(p.getUUID());

    if (data.lastTrigger != null
        && Duration.between(data.lastTrigger, now).toSeconds() < Config.cooldown) return;

    if (data.lastPos == null || data.lastPos.distanceToSqr(pos) > Config.radius * Config.radius) {
      data.count = 0;

      if (data.lastPos == null) {
        LOGGER.debug("No last position data available for {}, resetting shot count.", p.getName());
      } else {
        LOGGER.debug(
            "Player {} moved out of their shot counting radius, resetting shot count.",
            p.getName());
      }
      data.lastPos = pos;
    }

    data.count++;

    if (data.count == Config.threshold / 2 && !Config.noiseWarningMessage.isEmpty()) {
      p.displayClientMessage(
          Component.literal(Config.noiseWarningMessage).withStyle(ChatFormatting.DARK_RED),
          !Config.noiseWarningMessageInChat);
    }

    if (data.count >= Config.threshold) {
      trigger.trigger(p);
      data.lastTrigger = now;
      data.count = 0;
    }

    LOGGER.debug("Player {} now has {} shots.", p.getName(), data.count);
  }

  private static boolean isSilenced(ItemStack i) {
    var iGun = IGun.getIGunOrNull(i);
    if (iGun == null) return false;
    var gunData =
        TimelessAPI.getCommonGunIndex(iGun.getGunId(i))
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

  static final class ShotsData {
    Vec3 lastPos;
    int count;
    Instant lastTrigger;
  }
}

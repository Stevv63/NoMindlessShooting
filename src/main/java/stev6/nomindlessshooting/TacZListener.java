package stev6.nomindlessshooting;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TacZListener {

  private static final TriggerHandler trigger = new TriggerHandler();
  private static final Map<ServerPlayer, ShotsData> shots = new WeakHashMap<>();

  @SubscribeEvent
  public void onFire(GunShootEvent e) {
    if (e.getLogicalSide().isClient()) return;
    if (!(e.getShooter() instanceof ServerPlayer p)) return;
    if (trigger.isHordeOngoing(p)) return;

    ItemStack gun = e.getGunItemStack();
    var iGun = IGun.getIGunOrNull(gun);
    if (iGun == null) return;

    var gunData =
        TimelessAPI.getCommonGunIndex(iGun.getGunId(gun))
            .map(CommonGunIndex::getGunData)
            .orElse(null);
    if (gunData == null) return;

    var cache = new AttachmentCacheProperty();
    cache.eval(gun, gunData);

    var silenceData = cache.getCache(SilenceModifier.ID);
    boolean isSuppressed =
        silenceData instanceof Pair<?, ?> pair && pair.right() instanceof Boolean b && b;
    if (isSuppressed && Config.ignoreSilencer) return;

    Instant now = Instant.now();
    Vec3 pos = p.position();
    ShotsData data = shots.computeIfAbsent(p, k -> new ShotsData());

    if (data.lastTrigger != null
        && Duration.between(data.lastTrigger, now).toSeconds() < Config.cooldown) return;

    if (data.lastPos == null
        || data.lastTime == null
        || Duration.between(data.lastTime, now).toSeconds() > Config.timeLimit
        || data.lastPos.distanceToSqr(pos) > Config.radius * Config.radius) {
      data.count = 0;
      data.lastPos = pos;
    }

    data.count++;
    data.lastTime = now;

    if (data.count == (Config.threshold + 1) / 2 && !Config.noiseWarningMessage.isEmpty()) {
      p.displayClientMessage(
          Component.literal(Config.noiseWarningMessage).withStyle(ChatFormatting.DARK_RED),
          !Config.noiseWarningMessageInChat);
    }

    if (data.count >= Config.threshold) {
      if (!Config.hordeStartMessage.isEmpty()) {
        p.displayClientMessage(
            Component.literal(Config.hordeStartMessage)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
            !Config.hordeStartMessageInChat);
      }
      trigger.trigger(p);
      data.lastTrigger = now;
      data.count = 0;
    }
  }

  private static class ShotsData {
    Vec3 lastPos;
    int count;
    Instant lastTrigger;
    Instant lastTime;
  }
}

package stev6.nomindlessshooting;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import stev6.nomindlessshooting.integration.hordes.TheHordesIntegration;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static stev6.nomindlessshooting.NoMindlessShooting.LOGGER;

public class TacZListener {

    private static final TriggerHandler TRIGGER = new TriggerHandler();
    private static final Map<UUID, ShotsData> SHOTS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onFire(GunShootEvent e) {
        if (e.getLogicalSide().isClient()) return;
        if (!(e.getShooter() instanceof ServerPlayer p)) return;

        ItemStack gunItem = e.getGunItemStack();
        IGun iGun = IGun.getIGunOrNull(gunItem);

        if (iGun == null) return;

        ResourceLocation gunId = iGun.getGunId(gunItem);

        Optional<GunData> maybeGunData = TimelessAPI.getCommonGunIndex(gunId)
                .map(CommonGunIndex::getGunData);

        if (maybeGunData.isEmpty()) return;

        GunData gunData = maybeGunData.get();

        if (shouldIgnore(gunId)) {
            LOGGER.debug("Shot by {} ignored", p.getName().getString());
            return;
        }

        if (Config.ignoreEncased && isEncased(p)) {
            LOGGER.debug("{} shot but is encased", p.getName().getString());
            return;
        }

        if (Config.useHorde && TheHordesIntegration.isHordeOngoing(p)) return;

        long now = System.currentTimeMillis();
        Vec3 pos = p.position();
        ShotsData data = SHOTS.computeIfAbsent(p.getUUID(), k -> new ShotsData());

        if (data.lastTriggerTime != 0 && now - data.lastTriggerTime < Config.cooldown) {
            return;
        }

        if (data.lastShotPos == null || data.lastShotPos.distanceToSqr(pos) > Config.hearingRadius * Config.hearingRadius) {
            data.resetShots();
            if (data.lastShotPos == null) {
                LOGGER.debug("No last position data available for {}, resetting point count.", p.getName().getString());
            } else {
                LOGGER.debug("Player {} moved out of their shot counting radius, resetting point count.", p.getName().getString());
            }
            data.lastShotPos = pos;
        }

        data.cleanBefore(now);

        double gunPoints = getThresholdPoints(gunId, gunData);
        double points = applySilencerReduction(gunPoints, gunItem, iGun, gunData);

        if (points <= 0) return;
        data.addShot(now, points);

        boolean shouldTrigger = data.totalPoints >= Config.threshold;

        if (!data.sentWarning && data.totalPoints >= Config.threshold / 2 && !shouldTrigger) {
            if (Config.noiseWarningMessage != null)
                p.displayClientMessage(Config.noiseWarningMessage, !Config.noiseWarningMessageInChat);

            data.sentWarning = true;
        }

        if (shouldTrigger) {
            TRIGGER.trigger(p);
            data.lastTriggerTime = now;
            data.resetShots();
        }

        LOGGER.debug("Player {} now has {} counted points towards the threshold in the time window.", p.getName().getString(), data.totalPoints);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SHOTS.remove(p.getUUID());
    }

    private static double applySilencerReduction(double points, ItemStack i, IGun iGun, GunData gunData) {
        if (!Config.ignoreSilencer) return points;

        ResourceLocation silencerId = getSilencerAttachmentIdOrNull(i, iGun, gunData);
        if (silencerId == null) return points;

        int modifier = Config.silencerModifiers.getOrDefault(silencerId, Config.silencerModifier);
        return points * (100 - modifier) / 100.0;
    }

    private static double getThresholdPoints(ResourceLocation gunId, GunData gunData) {
        return Config.gunPoints.getOrDefault(gunId, Config.gunPoints.getOrDefault(gunData.getAmmoId(), 1.0));
    }

    private static ResourceLocation getSilencerAttachmentIdOrNull(ItemStack i, IGun iGun, GunData gunData) {
        ResourceLocation muzzle = iGun.getAttachmentId(i, AttachmentType.MUZZLE);
        if (DefaultAssets.isEmptyAttachmentId(muzzle)) return null;

        AttachmentData data = gunData.getExclusiveAttachments().getOrDefault(
                muzzle,
                TimelessAPI.getCommonAttachmentIndex(muzzle)
                        .map(CommonAttachmentIndex::getData)
                        .orElse(null)
        );

        if (data != null && data.getModifier().containsKey(SilenceModifier.ID)) return muzzle;

        return null;
    }

    private static boolean shouldIgnore(ResourceLocation gunId) {
        return Config.excludedWeapons.contains(gunId);
    }

    private static boolean isEncased(ServerPlayer p) {
        BlockPos pos = p.blockPosition();
        return p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ())
                > pos.getY() + 1
                && p.level().getBrightness(LightLayer.SKY, pos) == 0;
    }

    private static final class ShotsData {
        boolean sentWarning;
        Shots shots = Shots.create(64);
        double totalPoints;
        Vec3 lastShotPos;
        long lastTriggerTime = 0;

        public void addShot(long time, double points) {
            shots.times().add(time);
            shots.points().add(points);
            totalPoints += points;
        }

        public void resetShots() {
            shots.clear();
            totalPoints = 0;
            sentWarning = false;
        }

        public void cleanBefore(long now) {
            int expired = 0;

            while (expired < shots.points().size() && now - shots.times().getLong(expired) > Config.timeLimit) {
                totalPoints -= shots.points().getDouble(expired);
                expired++;
            }

            if (expired > 0) {
                shots.times().removeElements(0, expired);
                shots.points().removeElements(0, expired);

                if (totalPoints < Config.threshold / 2) sentWarning = false;
            }
        }
    }

    private record Shots(LongArrayList times, DoubleArrayList points) {
        public static Shots create(int size) {
            return new Shots(new LongArrayList(size), new DoubleArrayList(size));
        }

        public void clear() {
            times.clear();
            points.clear();
        }
    }
}

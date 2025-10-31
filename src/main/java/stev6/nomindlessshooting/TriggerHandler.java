package stev6.nomindlessshooting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import static stev6.nomindlessshooting.NoMindlessShooting.LOGGER;

public class TriggerHandler {

  public void trigger(ServerPlayer p) {
    if (Config.useHorde) {
      TheHordesIntegration.tryStartHorde(p);
    } else {
      spawnEntities(p.serverLevel(), p, Config.spawnCount);
    }
    if (!Config.hordeStartMessage.isEmpty()) {
      p.displayClientMessage(
          Component.literal(Config.hordeStartMessage)
              .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
          !Config.hordeStartMessageInChat);
    }
  }

  public void spawnEntities(ServerLevel level, ServerPlayer p, int count) {
    RandomSource rand = level.random;
    int maxTries = count * 10;
    int spawnedCount = 0;
    List<EntityType<?>> mobList = new ArrayList<>(Config.mobs);
    for (int tries = 0; spawnedCount < count && tries < maxTries; tries++) {
      Vec3 spawnPos = calculateSpawn(p, rand, level);
      if (spawnPos == null) continue;
      EntityType<?> type = mobList.get(rand.nextInt(mobList.size()));
      Entity spawnedEntity = type.spawn(level, BlockPos.containing(spawnPos), MobSpawnType.EVENT);
      if (spawnedEntity instanceof Mob mob) {
        mobSetup(mob, p);
        spawnedCount++;
        LOGGER.debug("Mob number {} out of {} spawned after {} tries", spawnedCount, count, tries);
      }
    }
  }

  private Vec3 calculateSpawn(ServerPlayer p, RandomSource rand, ServerLevel level) {
    double angle = rand.nextDouble() * 2 * Math.PI;
    double distance = 50.0 + rand.nextDouble() * 10.0;
    double x = Math.cos(angle) * distance;
    double z = Math.sin(angle) * distance;

    Vec3 pos = p.position().add(x, 0, z);
    BlockPos posBlock = BlockPos.containing(pos);
    int y =
        level.getHeight(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, posBlock.getX(), posBlock.getZ());
    BlockPos spawnPos = new BlockPos(posBlock.getX(), y, posBlock.getZ());

    if (level.getBlockState(spawnPos.below()).getFluidState().isEmpty()) {
      return Vec3.atCenterOf(spawnPos);
    }
    return null;
  }

  private void mobSetup(Mob mob, ServerPlayer target) {
    if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
      Objects.requireNonNull(mob.getAttribute(Attributes.FOLLOW_RANGE))
          .addPermanentModifier(
              new AttributeModifier("attracted", 70.0, AttributeModifier.Operation.ADDITION));
    }

    if (mob instanceof PathfinderMob pathfinder) {
      pathfinder.targetSelector.getAvailableGoals().forEach(WrappedGoal::stop);
      pathfinder.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinder));
      pathfinder.targetSelector.addGoal(
          2, new NearestAttackableTargetGoal<>(pathfinder, ServerPlayer.class, true));
    }

    mob.setTarget(target);
  }
}

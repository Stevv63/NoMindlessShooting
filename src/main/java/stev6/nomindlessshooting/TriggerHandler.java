package stev6.nomindlessshooting;

import com.google.common.base.Predicates;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.levelgen.Heightmap;
import stev6.nomindlessshooting.integration.hordes.TheHordesIntegration;

import java.util.List;

import static stev6.nomindlessshooting.NoMindlessShooting.LOGGER;

public class TriggerHandler {

    private static final ResourceLocation ATTRACTED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(NoMindlessShooting.MODID, "attracted");

    private static final AttributeModifier ATTRACTED_MODIFIER = new AttributeModifier(
            ATTRACTED_MODIFIER_ID,
            70.0,
            AttributeModifier.Operation.ADD_VALUE
    );

    public void trigger(ServerPlayer p) {
        if (Config.useHorde) {
            TheHordesIntegration.tryStartHorde(p);
        } else {
            spawnEntities(p.serverLevel(), p, Config.spawnCount);
        }
        if (Config.hordeStartMessage != null) {
            p.displayClientMessage(Config.hordeStartMessage, !Config.hordeStartMessageInChat);
        }
    }

    public void spawnEntities(ServerLevel level, ServerPlayer p, int count) {
        RandomSource rand = level.random;
        int maxTries = count * 10;
        int spawnedCount = 0;
        List<EntityType<?>> mobs = Config.mobs;

        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int tries = 0; spawnedCount < count && tries < maxTries; tries++) {
            BlockPos spawnPos = calculateSpawn(p, rand, level, checkPos);
            if (spawnPos == null) continue;

            EntityType<?> type = mobs.get(rand.nextInt(mobs.size()));
            Entity spawnedEntity = type.spawn(level, spawnPos, MobSpawnType.EVENT);
            if (spawnedEntity instanceof Mob mob) {
                setupHostileMob(mob, p);
                spawnedCount++;
                LOGGER.debug("Mob number {} out of {} spawned after {} tries", spawnedCount, count, tries);
            }
        }
    }

    private BlockPos calculateSpawn(ServerPlayer p, RandomSource rand, ServerLevel level, BlockPos.MutableBlockPos checkPos) {
        double angle = rand.nextDouble() * (2 * Math.PI);
        double distance = Config.spawnRadius + rand.nextDouble() * 10.0;

        int x = Mth.floor(p.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(p.getZ() + Math.sin(angle) * distance);

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        checkPos.set(x, y - 1, z);
        if (level.getBlockState(checkPos).getFluidState().isEmpty()) return new BlockPos(x, y, z);

        return null;
    }

    private void setupHostileMob(Mob mob, ServerPlayer target) {
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) followRange.addPermanentModifier(ATTRACTED_MODIFIER);


        if (mob instanceof PathfinderMob pathfinder) {
            pathfinder.targetSelector.removeAllGoals(Predicates.alwaysTrue());
            pathfinder.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinder));
            pathfinder.targetSelector.addGoal(
                    2,
                    new NearestAttackableTargetGoal<>(pathfinder, ServerPlayer.class, true)
            );
        }

        mob.setTarget(target);
    }
}

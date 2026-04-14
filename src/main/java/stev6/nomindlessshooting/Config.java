package stev6.nomindlessshooting;

import static stev6.nomindlessshooting.NoMindlessShooting.LOGGER;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = NoMindlessShooting.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
  private static final List<String> DEFAULT_MOBS = List.of("minecraft:zombie");

  static {
    BUILDER.push("Messages");

    NOISE_WARNING_MESSAGE =
        BUILDER
            .comment(
                "Action bar gun noise warning message, displayed when half of the threshold is reached.")
            .define("noiseWarningMsg", "&4This gun is too loud..");

    NOISE_WARNING_MESSAGE_IN_CHAT =
        BUILDER
            .comment(
                "Whether to send the noise warning message in the chat or the action bar, true will send it to the chat and false will use the action bar")
            .define("noiseWarningMessageInChat", false);

    HORDE_START_MESSAGE =
        BUILDER
            .comment(
                "Message for when the horde is alerted by your gun's noise, sent when the threshold is reached.")
            .define("hordeStartMsg", "&4The loud noise is attracting zombies..");

    HORDE_START_MESSAGE_IN_CHAT =
        BUILDER
            .comment(
                "Whether to send the horde start message in the chat or the action bar, true will send it to the chat and false will use the action bar")
            .define("hordeStartMessageInChat", false);

    BUILDER.pop();
    BUILDER.push("Zombie_Attraction");

    MOBS =
        BUILDER
            .comment("Choose which mobs to spawn")
            .defineList("mobs", DEFAULT_MOBS, Config::validateResourceLocationString);

    TIME_LIMIT =
        BUILDER
            .comment("Time window for noise (in seconds).")
            .defineInRange("timeLimit", 300, 0, Long.MAX_VALUE);

    COOLDOWN =
        BUILDER
            .comment("Cooldown between each trigger (in seconds)")
            .defineInRange("cooldown", 50, 0, Long.MAX_VALUE);

    HEARING_RADIUS =
            BUILDER
                    .comment("The radius of the area where zombies can hear your shots, moving out of that radius will reset your shots")
                    .defineInRange("hearingRadius", 50, 0, Double.MAX_VALUE);

    SPAWN_RADIUS =
            BUILDER
                    .comment("The radius around the player where zombies can spawn. Only works with hordeless mode.")
                    .defineInRange("spawnRadius", 60, 0, Double.MAX_VALUE);

    THRESHOLD =
        BUILDER
            .comment("How many shots until a horde is triggered")
            .defineInRange("threshold", 60, 1, Integer.MAX_VALUE);

    SPAWN_COUNT =
        BUILDER
            .comment("How many zombies to spawn, only works with use horde set to false")
            .defineInRange("spawnCount", 5, 1, Integer.MAX_VALUE);

    USE_HORDE =
        BUILDER
            .comment("HORDELESS MODE MAY HAVE ISSUES, PLEASE REPORT THEM")
            .comment(
                "Whether to start a horde or use the hordeless mode, useful for multiplayer servers and difficulty handling as the hordeless mode allows you to select the amount of entities")
            .define("useHorde", false);

    HORDE_SPAWNTABLE =
        BUILDER
            .comment(
                "Sets the spawntable for the horde started by the mod. only if you're using the hordes. set it to empty to use the default, usage is hordes:name")
            .define("hordeSpawnTable", "");

    DURATION =
        BUILDER
            .comment("Duration of the horde (in ticks) (only works with use horde set to true)")
            .defineInRange("hordeDuration", 2400, 1, Integer.MAX_VALUE);

    BUILDER.pop();
    BUILDER.push("Gun");

    EXCLUDED_WEAPONS =
            BUILDER
                    .comment("List of TaCZ Gun IDs to ignore, e.g. tacz:ak47")
                    .defineList("excludedWeapons", List.of(), Config::validateResourceLocationString);

    IGNORE_ENCASED =
        BUILDER
            .comment(
                "Whether to not count shots if the player is encased or not (For simplicity, this works by checking if you are under a block and have no sky light coming to you.)")
            .define("ignoreEncased", true);

    IGNORE_SILENCER =
        BUILDER
            .comment("Whether to ignore shots if the gun has a silencer on or not.")
            .define("ignoreSilencer", true);

    BUILDER.pop();
  }

  private static final ForgeConfigSpec.ConfigValue<String> NOISE_WARNING_MESSAGE;
  private static final ForgeConfigSpec.ConfigValue<String> HORDE_SPAWNTABLE;
  private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOBS;
  private static final ForgeConfigSpec.BooleanValue NOISE_WARNING_MESSAGE_IN_CHAT;
  private static final ForgeConfigSpec.ConfigValue<String> HORDE_START_MESSAGE;
  private static final ForgeConfigSpec.BooleanValue HORDE_START_MESSAGE_IN_CHAT;
  private static final ForgeConfigSpec.BooleanValue IGNORE_SILENCER;
  private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_WEAPONS;
  private static final ForgeConfigSpec.LongValue TIME_LIMIT;
  private static final ForgeConfigSpec.LongValue COOLDOWN;
  private static final ForgeConfigSpec.IntValue SPAWN_COUNT;
  private static final ForgeConfigSpec.DoubleValue HEARING_RADIUS;
  private static final ForgeConfigSpec.DoubleValue SPAWN_RADIUS;
  private static final ForgeConfigSpec.IntValue THRESHOLD;
  private static final ForgeConfigSpec.IntValue DURATION;
  private static final ForgeConfigSpec.BooleanValue IGNORE_ENCASED;
  private static final ForgeConfigSpec.BooleanValue USE_HORDE;

  public static int threshold;
  public static double hearingRadius;
  public static double spawnRadius;
  public static long cooldown;
  public static long timeLimit;
  public static Set<ResourceLocation> excludedWeapons;
  public static ResourceLocation hordeSpawnTable;
  public static Component noiseWarningMessage;
  public static Set<EntityType<?>> mobs;
  public static Component hordeStartMessage;
  public static int duration;
  public static boolean ignoreSilencer;
  public static boolean ignoreEncased;
  public static boolean useHorde;
  public static int spawnCount;
  public static boolean noiseWarningMessageInChat;
  public static boolean hordeStartMessageInChat;

  public static final ForgeConfigSpec SPEC = BUILDER.build();

  private static boolean validateResourceLocationString(final Object obj) {

    if (obj instanceof final String string) {

      return ResourceLocation.tryParse(string) != null;
    }

    return false;
  }

  @SubscribeEvent
  static void onLoad(final ModConfigEvent event) {
    threshold = THRESHOLD.get();
    hearingRadius = HEARING_RADIUS.get();
    spawnRadius = SPAWN_RADIUS.get();
    hordeSpawnTable = ResourceLocation.tryParse(HORDE_SPAWNTABLE.get());
    duration = DURATION.get();
    cooldown = COOLDOWN.get();
    timeLimit = TIME_LIMIT.get();
    ignoreSilencer = IGNORE_SILENCER.get();
    ignoreEncased = IGNORE_ENCASED.get();
    useHorde = USE_HORDE.get();
    spawnCount = SPAWN_COUNT.get();
    var rawNoiseWarning = NOISE_WARNING_MESSAGE.get();
    noiseWarningMessage = rawNoiseWarning.isEmpty() ? null : Util.parseMessage(rawNoiseWarning);
    noiseWarningMessageInChat = NOISE_WARNING_MESSAGE_IN_CHAT.get();
    var rawHordeStart = HORDE_START_MESSAGE.get();
    hordeStartMessage = rawHordeStart.isEmpty() ? null : Util.parseMessage(rawHordeStart);
    hordeStartMessageInChat = HORDE_START_MESSAGE_IN_CHAT.get();
    excludedWeapons = EXCLUDED_WEAPONS.get().stream()
            .map(ResourceLocation::tryParse)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    mobs =
        MOBS.get().stream()
            .map(ResourceLocation::parse)
            .map(ForgeRegistries.ENTITY_TYPES::getValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (useHorde && !ModList.get().isLoaded("hordes")) {
      useHorde = false;
      LOGGER.warn(
          "The hordes mod wasn't found but the config is set to use it. Defaulting to not use the hordes.");
    }
  }
}

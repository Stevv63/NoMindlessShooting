package stev6.nomindlessshooting;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = NoMindlessShooting.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

  static {
    BUILDER.push("Messages");

    NOISE_WARNING_MESSAGE =
        BUILDER
            .comment(
                "Action bar gun noise warning message, displayed when half of the threshold is reached.")
            .define("noiseWarningMsg", "This gun is too loud..");

    NOISE_WARNING_MESSAGE_IN_CHAT =
        BUILDER
            .comment(
                "Whether to send the noise warning message in the chat or the action bar, true will send it to the chat and false will use the action bar")
            .define("noiseWarningMessageInChat", true);

    HORDE_START_MESSAGE =
        BUILDER
            .comment(
                "Message for when the horde is alerted by your gun's noise, sent when the threshold is reached.")
            .define(
                "hordeStartMsg",
                "The loud noise attracts a horde of zombies.. You should probably run away.");

    HORDE_START_MESSAGE_IN_CHAT =
        BUILDER
            .comment(
                "Whether to send the horde start message in the chat or the action bar, true will send it to the chat and false will use the action bar")
            .define("hordeStartMessageInChat", true);

    BUILDER.pop();
    BUILDER.push("Zombie_Attraction");

    TIME_LIMIT =
        BUILDER
            .comment("Time window for noise (in seconds).")
            .defineInRange("timeLimit", 300, 0, Long.MAX_VALUE);

    COOLDOWN =
        BUILDER
            .comment("Cooldown between each trigger (in seconds)")
            .defineInRange("cooldown", 300, 0, Long.MAX_VALUE);

    RADIUS =
        BUILDER
            .comment("The radius of the area where zombies can hear your shots")
            .defineInRange("radius", 50, 0, Double.MAX_VALUE);
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
            .comment("EXPERIMENTAL")
            .comment(
                "Whether to start a horde or use the hordeless mode, useful for multiplayer servers and difficulty handling as the hordeless mode allows you to select the amount of entities")
            .define("useHorde", false);

    DURATION =
        BUILDER
            .comment("Duration of the horde (in ticks) (only works with use horde set to true)")
            .defineInRange("hordeDuration", 2400, 1, Integer.MAX_VALUE);

    BUILDER.pop();
    BUILDER.push("Gun");

    IGNORE_SILENCER =
        BUILDER
            .comment("Whether to ignore shots if the gun has a silencer on or not.")
            .define("ignoreSilencer", true);

    BUILDER.pop();
  }

  private static final ForgeConfigSpec.ConfigValue<String> NOISE_WARNING_MESSAGE;
  private static final ForgeConfigSpec.ConfigValue<Boolean> NOISE_WARNING_MESSAGE_IN_CHAT;
  private static final ForgeConfigSpec.ConfigValue<String> HORDE_START_MESSAGE;
  private static final ForgeConfigSpec.ConfigValue<Boolean> HORDE_START_MESSAGE_IN_CHAT;
  private static final ForgeConfigSpec.ConfigValue<Boolean> IGNORE_SILENCER;
  private static final ForgeConfigSpec.LongValue TIME_LIMIT;
  private static final ForgeConfigSpec.LongValue COOLDOWN;
  private static final ForgeConfigSpec.IntValue SPAWN_COUNT;
  private static final ForgeConfigSpec.DoubleValue RADIUS;
  private static final ForgeConfigSpec.IntValue THRESHOLD;
  private static final ForgeConfigSpec.IntValue DURATION;
  private static final ForgeConfigSpec.ConfigValue<Boolean> USE_HORDE;

  public static int threshold;
  public static double radius;
  public static long cooldown;
  public static long timeLimit;
  public static String noiseWarningMessage;
  public static String hordeStartMessage;
  public static int duration;
  public static boolean ignoreSilencer;
  public static boolean useHorde;
  public static int spawnCount;
  public static boolean noiseWarningMessageInChat;
  public static boolean hordeStartMessageInChat;

  public static final ForgeConfigSpec SPEC = BUILDER.build();

  @SubscribeEvent
  static void onLoad(final ModConfigEvent event) {
    threshold = THRESHOLD.get();
    radius = RADIUS.get();
    duration = DURATION.get();
    cooldown = COOLDOWN.get();
    timeLimit = TIME_LIMIT.get();
    ignoreSilencer = IGNORE_SILENCER.get();
    useHorde = USE_HORDE.get();
    spawnCount = SPAWN_COUNT.get();
    noiseWarningMessage = NOISE_WARNING_MESSAGE.get();
    noiseWarningMessageInChat = NOISE_WARNING_MESSAGE_IN_CHAT.get();
    hordeStartMessage = HORDE_START_MESSAGE.get();
    hordeStartMessageInChat = HORDE_START_MESSAGE_IN_CHAT.get();
  }

  @SubscribeEvent
  static void onReload(final ModConfigEvent.Reloading event) {
    onLoad(event);
  }
}

package stev6.nomindlessshooting;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = NoMindlessShooting.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
  public static final ForgeConfigSpec.ConfigValue<String> NOISE_WARNING_MESSAGE =
      BUILDER
          .comment("Action bar gun noise warning message, displayed when half of the threshold is reached.")
          .define("noiseWarningMsg", "This gun is too loud..");
  public static final ForgeConfigSpec.ConfigValue<String> HORDE_START_MESSAGE =
      BUILDER
          .comment("Message for when the horde is alerted by your gun's noise, sent when the threshold is reached.")
          .define(
              "hordeStartMsg",
              "The loud noise attracts a horde of zombies.. You should probably run away.");

  private static final List<String> DEFAULT_SILENCERS =
      List.of(
          "tacz:muzzle_silencer_vulture",
          "tacz:muzzle_silencer_phantom_s1",
          "tacz:muzzle_silencer_ursus",
          "tacz:muzzle_silencer_knight_qd",
          "tacz:muzzle_silencer_mirage",
          "tacz:muzzle_silencer_ptilopsis",
          "tacz:muzzle_suppressor_salvo_12g",
          "tacz:muzzle_suppressor_large_caliber");
  private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SILENCER_IDS_STRINGS =
      BUILDER
          .comment(
              "List of muzzle attachment ID's that are silencers",
              "Any shots fired with any one of these muzzle attachments will be ignored.")
          .defineListAllowEmpty(
              "silencer_ids", DEFAULT_SILENCERS, Config::validateResourceLocationString);
  private static final ForgeConfigSpec.LongValue TIME_LIMIT =
      BUILDER
          .comment("Time window for noise (in seconds).")
          .defineInRange("timeLimit", 300, 0, Long.MAX_VALUE);
  private static final ForgeConfigSpec.DoubleValue RADIUS =
      BUILDER
          .comment("The radius of the area where zombies can hear your shots")
          .defineInRange("radius", 50, 0, Double.MAX_VALUE);
  private static final ForgeConfigSpec.IntValue THRESHOLD =
      BUILDER
          .comment("How many shots until a horde is triggered")
          .defineInRange("threshold", 60, 1, Integer.MAX_VALUE);
  private static final ForgeConfigSpec.IntValue DURATION =
      BUILDER
          .comment("Duration of the horde (in ticks)")
          .defineInRange("hordeDuration", 2400, 1, Integer.MAX_VALUE);
  public static int threshold;
  public static double radius;
  public static long timeLimit;
  public static String noiseWarningMessage;
  public static String hordeStartMessage;
  public static int duration;
  public static Set<ResourceLocation> SILENCER_IDS;

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
    radius = RADIUS.get();
    duration = DURATION.get();
    timeLimit = TIME_LIMIT.get();
    noiseWarningMessage = NOISE_WARNING_MESSAGE.get();
    hordeStartMessage = HORDE_START_MESSAGE.get();
    SILENCER_IDS =
        SILENCER_IDS_STRINGS.get().stream()
            .map(ResourceLocation::parse)
            .collect(Collectors.toSet());
  }
}

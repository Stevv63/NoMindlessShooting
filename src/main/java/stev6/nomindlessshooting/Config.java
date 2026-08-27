package stev6.nomindlessshooting;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

import static stev6.nomindlessshooting.NoMindlessShooting.LOGGER;

@Mod.EventBusSubscriber(modid = NoMindlessShooting.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final List<String> DEFAULT_MOBS = List.of("minecraft:zombie");
    private static final List<String> DEFAULT_THRESHOLD_POINTS = List.of(
            "tacz:50bmg=1"
    );

    private static final List<String> DEFAULT_SILENCER_MODIFIERS = List.of("tacz:muzzle_silencer_wraith=100");

    static {
        BUILDER.push("Messages");

        NOISE_WARNING_MESSAGE =
                BUILDER
                        .comment(
                                "Gun noise warning message, displayed when half of the threshold is reached.")
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
                        .comment("Choose which mobs to spawn when useHorde is off")
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
                        .comment("The radius around the player where zombies can spawn when useHorde is off.")
                        .defineInRange("spawnRadius", 60, 0, Double.MAX_VALUE);

        THRESHOLD =
                BUILDER
                        .comment("How many noise points are needed to trigger a horde.",
                                "Each shot will add its gun's (if specified, else it defaults to 1) noise points.",
                                "If \"ignoreSilencer\" is enabled, the silencer reduction (and if specified, any overrides) will apply")
                        .defineInRange("threshold", 60, 1, Double.MAX_VALUE);

        SPAWN_COUNT =
                BUILDER
                        .comment("How many mobs to spawn when useHorde is off")
                        .defineInRange("spawnCount", 5, 1, Integer.MAX_VALUE);

        USE_HORDE =
                BUILDER
                        .comment(
                                "HORDELESS MODE MAY HAVE ISSUES, PLEASE REPORT THEM",
                                "Whether to start a horde or use the hordeless mode, useful for multiplayer servers and difficulty handling as the hordeless mode allows you to select the amount of entities")
                        .define("useHorde", false);

        HORDE_SPAWNTABLE =
                BUILDER
                        .comment(
                                "Sets the spawntable for the horde started by the mod. only if you're using the hordes. set it to empty to use the default, usage is hordes:name")
                        .define("hordeSpawnTable", "");

        DURATION =
                BUILDER
                        .comment("Duration of the horde (in ticks) (only works with useHorde set to true)")
                        .defineInRange("hordeDuration", 2400, 1, Integer.MAX_VALUE);

        BUILDER.pop();
        BUILDER.push("Gun");

        THRESHOLD_POINTS =
                BUILDER
                        .comment(
                                "Define how many noise points count towards the threshold for an AmmoId or GunId.",
                                "The GunId is checked first before the AmmoId. Any GunId/AmmoId not present here will default to 1 point",
                                "Format: \"namespace:id=points\" (e.g. \"tacz:50bmg=1\")"
                        )
                        .defineList("gunPoints", DEFAULT_THRESHOLD_POINTS, Config::validateStringDoubleValueString);


        SILENCER_MODIFIER =
                BUILDER
                        .comment("How much a silencer reduces a shot's noise points, from 0% (no reduction) to 100% (no points count).",
                                "Example: If an AKM shot gives 1 noise point and the modifier is 50%, 0.5 noise points will be added")
                        .defineInRange("silencerModifier", 100, 0, 100);

        SILENCER_MODIFIERS =
                BUILDER
                        .comment("Define the silence modifier for a specific silencer AttachmentId",
                                "Must be a valid silencer's AttachmentId, otherwise it will not count even if it is a valid AttachmentId")
                        .defineList("silencerModifiers", DEFAULT_SILENCER_MODIFIERS, Config::validateStringDoubleValueString);

        EXCLUDED_WEAPONS =
                BUILDER
                        .comment("List of TaCZ Gun IDs to ignore, e.g. tacz:ak47")
                        .defineList("excludedWeapons", List.of(), Config::validateResourceLocationString);


        IGNORE_ENCASED =
                BUILDER
                        .comment(
                                "Whether to not count shots if the player is encased (Works by checking if you are under a solid block with no sky light.)")
                        .define("ignoreEncased", true);

        IGNORE_SILENCER =
                BUILDER
                        .comment("Whether to apply any silencer modifiers.")
                        .define("ignoreSilencer", true);

        BUILDER.pop();
    }

    private static final ForgeConfigSpec.ConfigValue<String> NOISE_WARNING_MESSAGE;
    private static final ForgeConfigSpec.ConfigValue<String> HORDE_SPAWNTABLE;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOBS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> THRESHOLD_POINTS;
    private static final ForgeConfigSpec.BooleanValue NOISE_WARNING_MESSAGE_IN_CHAT;
    private static final ForgeConfigSpec.ConfigValue<String> HORDE_START_MESSAGE;
    private static final ForgeConfigSpec.BooleanValue HORDE_START_MESSAGE_IN_CHAT;
    private static final ForgeConfigSpec.BooleanValue IGNORE_SILENCER;
    private static final ForgeConfigSpec.IntValue SILENCER_MODIFIER;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_WEAPONS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SILENCER_MODIFIERS;
    private static final ForgeConfigSpec.LongValue TIME_LIMIT;
    private static final ForgeConfigSpec.LongValue COOLDOWN;
    private static final ForgeConfigSpec.IntValue SPAWN_COUNT;
    private static final ForgeConfigSpec.DoubleValue HEARING_RADIUS;
    private static final ForgeConfigSpec.DoubleValue SPAWN_RADIUS;
    private static final ForgeConfigSpec.DoubleValue THRESHOLD;
    private static final ForgeConfigSpec.IntValue DURATION;
    private static final ForgeConfigSpec.BooleanValue IGNORE_ENCASED;
    private static final ForgeConfigSpec.BooleanValue USE_HORDE;

    public static double threshold;
    public static double hearingRadius;
    public static double spawnRadius;
    public static long cooldown;
    public static long timeLimit;
    public static Set<ResourceLocation> excludedWeapons;
    public static Map<ResourceLocation, Double> gunPoints = new HashMap<>();
    public static Map<ResourceLocation, Integer> silencerModifiers = new HashMap<>();
    public static int silencerModifier;
    public static ResourceLocation hordeSpawnTable;
    public static Component noiseWarningMessage;
    public static List<EntityType<?>> mobs;
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
        if (obj instanceof final String string) return ResourceLocation.tryParse(string) != null;
        return false;
    }

    private static boolean validateStringDoubleValueString(final Object obj) {
        if (!(obj instanceof final String string)) return false;
        return parseStringDoubleValueString(string) != null;
    }

    private static Map.Entry<ResourceLocation, Double> parseStringDoubleValueString(String str) {
        int idx = str.indexOf('=');
        if (idx <= 0 || idx == str.length() - 1) return null;

        String idStr = str.substring(0, idx).trim();
        String valStr = str.substring(idx + 1).trim();
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) return null;

        double val;
        try {
            val = Double.parseDouble(valStr);
        } catch (NumberFormatException e) {
            return null;
        }

        if (val < 0) return null;
        return Map.entry(id, val);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {

        gunPoints = THRESHOLD_POINTS.get().stream()
                .map(Config::parseStringDoubleValueString)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));

        silencerModifiers = SILENCER_MODIFIERS.get().stream()
                .map(Config::parseStringDoubleValueString)
                .filter(Objects::nonNull)
                .map(e -> Map.entry(e.getKey(), e.getValue().intValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));

        silencerModifier = SILENCER_MODIFIER.get();

        threshold = THRESHOLD.get();
        hearingRadius = HEARING_RADIUS.get();
        spawnRadius = SPAWN_RADIUS.get();
        hordeSpawnTable = ResourceLocation.tryParse(HORDE_SPAWNTABLE.get());
        duration = DURATION.get();
        cooldown = COOLDOWN.get() * 1000;
        timeLimit = TIME_LIMIT.get() * 1000;
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

        List<EntityType<?>> parsedMobs = MOBS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .map(ForgeRegistries.ENTITY_TYPES::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!parsedMobs.isEmpty()) mobs = parsedMobs;
        else {
            LOGGER.warn("No valid mobs to spawn found in config. Falling back to minecraft:zombie");
            mobs = List.of(EntityType.ZOMBIE);
        }

        if (useHorde && !ModList.get().isLoaded("hordes")) {
            useHorde = false;
            LOGGER.warn(
                    "The hordes mod wasn't found but the config is set to use it. Defaulting to not use the hordes.");
        }
    }
}

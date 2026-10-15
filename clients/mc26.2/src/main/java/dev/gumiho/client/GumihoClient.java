package dev.gumiho.client;

import dev.gumiho.client.config.GumihoConfigManager;
import dev.gumiho.client.gui.GumihoScreen;
import dev.gumiho.client.module.ModuleManager;
import dev.gumiho.client.module.combat.AttackCooldownHudModule;
import dev.gumiho.client.module.combat.AutoEatModule;
import dev.gumiho.client.module.combat.AutoShieldModule;
import dev.gumiho.client.module.combat.AutoTotemModule;
import dev.gumiho.client.module.combat.BowAimAssistModule;
import dev.gumiho.client.module.combat.BowStatusHudModule;
import dev.gumiho.client.module.combat.CooldownAttackModule;
import dev.gumiho.client.module.combat.DirectionalIndicatorsModule;
import dev.gumiho.client.module.combat.MeleeAimModule;
import dev.gumiho.client.module.combat.MaceComboAimModule;
import dev.gumiho.client.module.combat.MeleeActions;
import dev.gumiho.client.module.combat.WindMaceSwapModule;
import dev.gumiho.client.module.combat.AutoWeaponModule;
import dev.gumiho.client.module.combat.MaceSmashModule;
import dev.gumiho.client.module.combat.MaceStatusHudModule;
import dev.gumiho.client.module.combat.PlayerBowAimAssistModule;
import dev.gumiho.client.module.combat.PlayerTrackerHudModule;
import dev.gumiho.client.module.combat.ProjectileWarningHudModule;
import dev.gumiho.client.module.combat.TargetHudModule;
import dev.gumiho.client.module.movement.AutoSprintModule;
import dev.gumiho.client.module.movement.AutoWalkModule;
import dev.gumiho.client.module.movement.BridgeAssistModule;
import dev.gumiho.client.module.utility.CooldownHudModule;
import dev.gumiho.client.module.utility.CombatResourcesHudModule;
import dev.gumiho.client.module.utility.EffectsHudModule;
import dev.gumiho.client.module.utility.NetworkHudModule;
import dev.gumiho.client.module.utility.ProjectilePathsModule;
import dev.gumiho.client.module.utility.SurvivalAlertsHudModule;
import dev.gumiho.client.module.settings.BooleanSettingModule;
import dev.gumiho.client.module.visual.BrandHudModule;
import dev.gumiho.client.module.visual.CoordinatesHudModule;
import dev.gumiho.client.module.visual.FullbrightModule;
import dev.gumiho.client.module.visual.NoWeatherModule;
import dev.gumiho.client.security.ServerPolicy;
import dev.gumiho.client.profile.ProfileManager;
import dev.gumiho.client.tracking.GumihoClientCommands;
import dev.gumiho.client.tracking.PlayerGroupManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import dev.gumiho.client.tracking.PlayerGroup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = GumihoClient.MOD_ID, dist = Dist.CLIENT)
public final class GumihoClient {
    public static final String MOD_ID = "gumiho";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final GumihoConfigManager CONFIG = new GumihoConfigManager();
    public static final ServerPolicy SERVER_POLICY = new ServerPolicy(CONFIG);
    public static final PlayerGroupManager PLAYER_GROUPS = new PlayerGroupManager(CONFIG);
    public static final ModuleManager MODULES = new ModuleManager(CONFIG, SERVER_POLICY);
    public static final ProfileManager PROFILES = new ProfileManager(CONFIG, MODULES);
    public static final WindMaceSwapModule WIND_MACE_SWAP = new WindMaceSwapModule();
    public static final MaceComboAimModule MACE_COMBO_AIM = new MaceComboAimModule();
    public static final ProjectilePathsModule PROJECTILE_PATHS = new ProjectilePathsModule();
    public static final AutoShieldModule AUTO_SHIELD = new AutoShieldModule();
    public static final BridgeAssistModule BRIDGE_ASSIST = new BridgeAssistModule();

    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static final KeyMapping RANGED_AIM_KEY = new KeyMapping(
            "key.gumiho.ranged_aim", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY);

    private final KeyMapping menuKey = new KeyMapping(
            "key.gumiho.menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            KEY_CATEGORY
    );
    private final KeyMapping pinTargetKey = new KeyMapping(
            "key.gumiho.pin_target", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, KEY_CATEGORY);
    private final KeyMapping emergencyKey = new KeyMapping(
            "key.gumiho.emergency", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_END, KEY_CATEGORY);

    public GumihoClient(IEventBus modEventBus) {
        CONFIG.load();

        MODULES.register(new BrandHudModule());
        MODULES.register(new CoordinatesHudModule());
        MODULES.register(new dev.gumiho.client.module.visual.CompassHudModule());
        MODULES.register(new dev.gumiho.client.module.utility.EquipmentHudModule());
        MODULES.register(new BowAimAssistModule());
        MODULES.register(new PlayerBowAimAssistModule());
        MODULES.register(new PlayerTrackerHudModule());
        MODULES.register(new DirectionalIndicatorsModule());
        MODULES.register(new MaceStatusHudModule());
        MODULES.register(new ProjectileWarningHudModule());
        MODULES.register(new BowStatusHudModule());
        MODULES.register(new TargetHudModule());
        MODULES.register(new AttackCooldownHudModule());
        MODULES.register(new AutoTotemModule());
        MODULES.register(WIND_MACE_SWAP);
        MODULES.register(new AutoEatModule());
        MODULES.register(AUTO_SHIELD);
        MODULES.register(new MeleeAimModule());
        if (BuildInfo.NATIVE_SPEARS) {
            MODULES.register(new dev.gumiho.client.module.combat.SpearAssistModule());
            MODULES.register(new dev.gumiho.client.module.combat.SpearStatusHudModule());
        }
        MODULES.register(MACE_COMBO_AIM);
        MODULES.register(new AutoWeaponModule());
        MODULES.register(new CooldownAttackModule());
        MODULES.register(new MaceSmashModule());
        MODULES.register(new AutoSprintModule());
        MODULES.register(new AutoWalkModule());
        MODULES.register(BRIDGE_ASSIST);
        MODULES.register(new SurvivalAlertsHudModule());
        MODULES.register(new EffectsHudModule());
        MODULES.register(new CooldownHudModule());
        MODULES.register(new CombatResourcesHudModule());
        MODULES.register(new NetworkHudModule());
        MODULES.register(PROJECTILE_PATHS);
        MODULES.register(new BooleanSettingModule(
                "contextual_hud", "Contextual HUD", "상황별 HUD",
                "Hide awareness panels when they have nothing useful to show",
                "표시할 유용한 정보가 없을 때 인식 패널을 숨깁니다", true));
        MODULES.register(new BooleanSettingModule(
                "show_neutral_players", "Show Neutral Players", "중립 플레이어 표시",
                "Include unlisted neutral players in the tracker",
                "추적기에 목록에 없는 중립 플레이어를 포함합니다", true));
        MODULES.register(new BooleanSettingModule(
                "prioritize_weak_enemies", "Prioritize Weak Enemies", "약한 적 우선",
                "Prefer low-health marked enemies when target scores are close",
                "대상 점수가 비슷할 때 체력이 낮은 지정 적을 우선합니다", true));
        MODULES.register(new FullbrightModule());
        MODULES.register(new NoWeatherModule());

        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(AUTO_SHIELD::beforeTick);
        NeoForge.EVENT_BUS.addListener(AUTO_SHIELD::onMouse);
        NeoForge.EVENT_BUS.addListener(AUTO_SHIELD::onKey);
        NeoForge.EVENT_BUS.addListener(BRIDGE_ASSIST::onInteraction);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
        NeoForge.EVENT_BUS.addListener(this::onRenderFrame);
        NeoForge.EVENT_BUS.addListener(WIND_MACE_SWAP::onUse);
        NeoForge.EVENT_BUS.addListener(PROJECTILE_PATHS::onExtract);
        NeoForge.EVENT_BUS.addListener(dev.gumiho.client.module.utility.ProjectilePathRenderType::onSubmit);
        NeoForge.EVENT_BUS.addListener(MACE_COMBO_AIM::onExtract);
        NeoForge.EVENT_BUS.addListener(MACE_COMBO_AIM::onInput);
        NeoForge.EVENT_BUS.addListener(this::registerClientCommands);

        LOGGER.info("GUMIHO | 구미호 initialized");
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(menuKey);
        event.register(RANGED_AIM_KEY);
        event.register(pinTargetKey);
        event.register(emergencyKey);
    }

    private void registerClientCommands(RegisterClientCommandsEvent event) {
        GumihoClientCommands.register(event);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        while (menuKey.consumeClick()) {
            client.gui.setScreen(new GumihoScreen());
        }
        while (pinTargetKey.consumeClick()) {
            togglePinnedTarget(client);
        }
        while (emergencyKey.consumeClick()) {
            int disabled = MODULES.disableAutomation(client);
            PROFILES.markCustom();
            if (client.player != null) {
                client.player.sendOverlayMessage(Component.literal(
                        "GUMIHO emergency: disabled " + disabled + " automation modules"));
            }
        }
        MeleeActions.beginTick(client);
        MODULES.tick(client);
    }

    private void onRenderFrame(RenderFrameEvent.Pre event) {
        MODULES.frame(Minecraft.getInstance(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        MODULES.renderHud(event.getGuiGraphics(), Minecraft.getInstance());
    }

    private static void togglePinnedTarget(Minecraft client) {
        if (client.player == null || !(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player player) || player == client.player) {
            PLAYER_GROUPS.clearPin();
            if (client.player != null) {
                client.player.sendOverlayMessage(Component.literal("GUMIHO target pin cleared"));
            }
            return;
        }
        String name = player.getScoreboardName();
        if (PLAYER_GROUPS.groupOf(name) == PlayerGroup.TEAMMATE) {
            client.player.sendOverlayMessage(Component.literal("GUMIHO: teammates cannot be pinned as targets"));
            return;
        }
        if (PLAYER_GROUPS.isPinned(name)) {
            PLAYER_GROUPS.clearPin();
            client.player.sendOverlayMessage(Component.literal("GUMIHO target pin cleared"));
            return;
        }
        PLAYER_GROUPS.setGroup(name, PlayerGroup.ENEMY);
        PLAYER_GROUPS.pin(name);
        client.player.sendOverlayMessage(Component.literal("GUMIHO pinned enemy: " + name));
    }
}

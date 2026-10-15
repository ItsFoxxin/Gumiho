package dev.gumiho.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.gumiho.client.GumihoClient;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.util.Mth;

public final class GumihoConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path = FMLPaths.CONFIGDIR.get().resolve("gumiho.json");
    private GumihoConfig config = new GumihoConfig();

    public void load() {
        if (!Files.exists(path)) {
            normalize();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            GumihoConfig loaded = GSON.fromJson(reader, GumihoConfig.class);
            if (loaded != null) {
                if (loaded.schemaVersion < 1) {
                    Path backup = path.resolveSibling("gumiho.pre-0.8.0.json");
                    if (!Files.exists(backup)) Files.copy(path, backup);
                }
                config = loaded;
                normalize();
            }
        } catch (IOException | RuntimeException error) {
            GumihoClient.LOGGER.error("Could not load {}", path, error);
        }
    }

    public void save() {
        try { saveChecked(); }
        catch (IOException error) { GumihoClient.LOGGER.error("Could not save {}", path, error); }
    }

    public void saveChecked() throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "gumiho-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary)) { GSON.toJson(config, writer); }
            try { Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }

    public void replaceWith(GumihoConfig replacement) {
        config = replacement;
        normalize();
    }

    public GumihoConfig config() {
        return config;
    }

    private void normalize() {
        if (config.allowedServers == null) {
            config.allowedServers = new java.util.ArrayList<>();
        }
        if (config.modules == null) {
            config.modules = new java.util.HashMap<>();
        }
        if (config.hudX == null) {
            config.hudX = new java.util.HashMap<>();
        }
        if (config.hudY == null) {
            config.hudY = new java.util.HashMap<>();
        }
        if (config.hudScales == null) config.hudScales = new java.util.HashMap<>();
        config.hudScales.replaceAll((id, value) -> dev.gumiho.client.gui.HudSizing.scale(value == null ? 1 : value));
        if (config.playerGroups == null) {
            config.playerGroups = new java.util.HashMap<>();
        }
        if (config.activeProfile == null) {
            config.activeProfile = "CUSTOM";
        }
        config.bowAimRange = Mth.clamp(config.bowAimRange, 16.0D, 96.0D);
        config.bowAimSmoothness = Mth.clamp(config.bowAimSmoothness, 0.5D, 10.0D);
        config.autoTotemHealth = Mth.clamp(config.autoTotemHealth, 2.0D, 20.0D);
        config.autoEatHunger = Mth.clamp(config.autoEatHunger, 1, 19);
        config.lowHealthWarning = Mth.clamp(config.lowHealthWarning, 2.0D, 20.0D);
        if (config.pinnedTarget == null) {
            config.pinnedTarget = "";
        }
        config.bridgeMode = dev.gumiho.client.module.movement.BridgeRules.mode(config.bridgeMode).name();
        config.bridgePlaceDelay = dev.gumiho.client.module.movement.BridgeRules.delay(config.bridgePlaceDelay);
        CombatConfigMigration.normalize(config);
    }
}

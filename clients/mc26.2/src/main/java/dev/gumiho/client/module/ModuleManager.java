package dev.gumiho.client.module;

import dev.gumiho.client.config.GumihoConfig;
import dev.gumiho.client.config.GumihoConfigManager;
import dev.gumiho.client.security.ServerPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class ModuleManager {
    private final List<GumihoModule> modules = new ArrayList<>();
    private final GumihoConfigManager configManager;
    private final ServerPolicy serverPolicy;

    public ModuleManager(GumihoConfigManager configManager, ServerPolicy serverPolicy) {
        this.configManager = configManager;
        this.serverPolicy = serverPolicy;
    }

    public void register(GumihoModule module) {
        modules.add(module);
        Boolean savedState = configManager.config().modules.get(module.id());
        if (savedState != null) {
            module.restoreEnabled(savedState);
        }
        if (module instanceof HudModule hudModule) {
            hudModule.setScale(configManager.config().hudScales.getOrDefault(module.id(),1.0));
            Integer savedX = configManager.config().hudX.get(module.id());
            Integer savedY = configManager.config().hudY.get(module.id());
            if (savedX != null && savedY != null) {
                hudModule.setPosition(savedX, savedY);
            }
        }
    }

    public boolean toggle(GumihoModule module, Minecraft client) {
        boolean changed = module.toggle(client, serverPolicy);
        if (changed) {
            GumihoConfig config = configManager.config();
            config.modules.put(module.id(), module.enabled());
            configManager.save();
        }
        return changed;
    }

    public void tick(Minecraft client) {
        if (!GumihoModule.runtimeReady(client)) return;
        for (GumihoModule module : modules) {
            if (module.activate(client, serverPolicy)) {
                module.tick(client);
            } else if (module.enabled()) {
                module.suspend(client);
            }
        }
    }

    public void frame(Minecraft client, float partialTick) {
        if (!GumihoModule.runtimeReady(client)) return;
        for (GumihoModule module : modules) {
            if (!module.enabled()) continue;
            if (module.activate(client, serverPolicy)) module.frame(client, partialTick);
            else module.suspend(client);
        }
    }

    public void renderHud(GuiGraphicsExtractor context, Minecraft client) {
        if (!GumihoModule.runtimeReady(client)) return;
        // The editor draws its own scaled previews after the world blur. A second
        // gameplay pass underneath would darken enabled panels and leave ghosts.
        if (client.gui.screen() instanceof dev.gumiho.client.gui.HudEditorScreen) return;
        for (GumihoModule module : modules) {
            if (module.activate(client, serverPolicy)) {
                if (module instanceof HudModule hud) hud.prepareLayout(client,
                        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                if (module instanceof HudModule hud) hud.renderScaled(context,client);
                else module.renderHud(context, client);
            }
        }
    }

    public void saveHudPosition(HudModule module) {
        configManager.config().hudX.put(module.id(), module.preferredX());
        configManager.config().hudY.put(module.id(), module.preferredY());
        configManager.config().hudScales.put(module.id(),module.scale());
        configManager.save();
    }

    public void resetHudPositions() {
        for (GumihoModule module : modules) {
            if (module instanceof HudModule hudModule) {
                hudModule.resetPosition();
                hudModule.setScale(1);
                configManager.config().hudScales.put(module.id(),1.0);
                configManager.config().hudX.put(module.id(), hudModule.preferredX());
                configManager.config().hudY.put(module.id(), hudModule.preferredY());
            }
        }
        configManager.save();
    }

    public void applyProfile(Set<String> enabledIds, Minecraft client) {
        GumihoConfig config = configManager.config();
        for (GumihoModule module : modules) {
            boolean target = enabledIds.contains(module.id());
            module.setEnabled(target, client, serverPolicy);
            config.modules.put(module.id(), module.enabled());
        }
        configManager.save();
    }

    public List<GumihoModule> modules() {
        return Collections.unmodifiableList(modules);
    }

    public boolean isEnabled(String id) {
        return modules.stream().anyMatch(module -> module.id().equals(id) && module.enabled());
    }

    public int disableAutomation(Minecraft client) {
        int disabled = 0;
        GumihoConfig config = configManager.config();
        for (GumihoModule module : modules) {
            if (module.enabled() && module.requiresAuthorizedServer()) {
                module.setEnabled(false, client, serverPolicy);
                config.modules.put(module.id(), false);
                disabled++;
            }
        }
        configManager.save();
        return disabled;
    }
}

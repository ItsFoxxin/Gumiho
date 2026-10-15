package dev.gumiho.client.module;

import dev.gumiho.client.security.ServerPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class GumihoModule {
    private final String id;
    private final String englishName;
    private final String koreanName;
    private final String englishDescription;
    private final String koreanDescription;
    private final ModuleCategory category;
    private final boolean requiresAuthorizedServer;
    private boolean enabled;
    private boolean runtimeEnabled;

    protected GumihoModule(
            String id,
            String englishName,
            String koreanName,
            String englishDescription,
            String koreanDescription,
            ModuleCategory category,
            boolean enabledByDefault,
            boolean requiresAuthorizedServer
    ) {
        this.id = id;
        this.englishName = englishName;
        this.koreanName = koreanName;
        this.englishDescription = englishDescription;
        this.koreanDescription = koreanDescription;
        this.category = category;
        this.enabled = enabledByDefault;
        this.requiresAuthorizedServer = requiresAuthorizedServer;
    }

    /** Restore intent only. Mod construction may precede Minecraft's singleton/options/font. */
    final void restoreEnabled(boolean target) {
        enabled = target;
        runtimeEnabled = false;
    }

    protected static boolean runtimeReady(Minecraft client) {
        return client != null && client.options != null && client.font != null;
    }

    final boolean activate(Minecraft client, ServerPolicy policy) {
        if (!runtimeReady(client) || !enabled || !canRun(client, policy)) return false;
        if (!runtimeEnabled) {
            onEnable(client);
            runtimeEnabled = true;
        }
        return true;
    }

    public final boolean setEnabled(boolean target, Minecraft client, ServerPolicy policy) {
        if (!runtimeReady(client)) return false;
        if (target && requiresAuthorizedServer && !policy.canUseExperimentalModules(client)) {
            return false;
        }

        if (enabled == target) {
            if (target) activate(client, policy);
            return true;
        }

        enabled = target;
        if (enabled) {
            activate(client, policy);
        } else {
            onDisable(client);
            runtimeEnabled = false;
        }
        return true;
    }

    public final boolean toggle(Minecraft client, ServerPolicy policy) {
        return setEnabled(!enabled, client, policy);
    }

    public void tick(Minecraft client) {
    }

    public void frame(Minecraft client, float partialTick) {
    }

    /** Release transient input/state without changing the saved toggle. */
    public void suspend(Minecraft client) {
    }

    public void renderHud(GuiGraphicsExtractor context, Minecraft client) {
    }

    protected void onEnable(Minecraft client) {
    }

    protected void onDisable(Minecraft client) {
    }

    public final String id() {
        return id;
    }

    public final String name() {
        return englishName + " | " + koreanName;
    }

    public final String description() {
        return englishDescription + " | " + koreanDescription;
    }

    public final ModuleCategory category() {
        return category;
    }

    public final boolean enabled() {
        return enabled;
    }

    public final boolean canRun(Minecraft client, ServerPolicy policy) {
        return !requiresAuthorizedServer || policy.canUseExperimentalModules(client);
    }

    public final boolean requiresAuthorizedServer() {
        return requiresAuthorizedServer;
    }
}

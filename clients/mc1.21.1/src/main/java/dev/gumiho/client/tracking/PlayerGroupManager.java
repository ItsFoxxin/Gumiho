package dev.gumiho.client.tracking;

import dev.gumiho.client.config.GumihoConfigManager;

import java.util.Locale;
import java.util.Map;

public final class PlayerGroupManager {
    private final GumihoConfigManager configManager;

    public PlayerGroupManager(GumihoConfigManager configManager) {
        this.configManager = configManager;
    }

    public PlayerGroup groupOf(String playerName) {
        String saved = configManager.config().playerGroups.get(normalize(playerName));
        if (saved == null) return PlayerGroup.NEUTRAL;
        try {
            return PlayerGroup.valueOf(saved);
        } catch (IllegalArgumentException ignored) {
            return PlayerGroup.NEUTRAL;
        }
    }

    public void setGroup(String playerName, PlayerGroup group) {
        configManager.config().playerGroups.put(normalize(playerName), group.name());
        configManager.save();
    }

    public boolean remove(String playerName) {
        boolean changed = configManager.config().playerGroups.remove(normalize(playerName)) != null;
        if (changed) configManager.save();
        return changed;
    }

    public Map<String, String> entries() {
        return Map.copyOf(configManager.config().playerGroups);
    }

    public void pin(String playerName) {
        configManager.config().pinnedTarget = normalize(playerName);
        configManager.save();
    }

    public void clearPin() {
        configManager.config().pinnedTarget = "";
        configManager.save();
    }

    public boolean isPinned(String playerName) {
        return normalize(playerName).equals(configManager.config().pinnedTarget);
    }

    public String pinnedName() {
        return configManager.config().pinnedTarget;
    }

    private static String normalize(String playerName) {
        return playerName.strip().toLowerCase(Locale.ROOT);
    }
}

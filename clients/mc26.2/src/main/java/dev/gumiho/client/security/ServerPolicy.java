package dev.gumiho.client.security;

import dev.gumiho.client.config.GumihoConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

public final class ServerPolicy {
    private final GumihoConfigManager configManager;

    public ServerPolicy(GumihoConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean canUseExperimentalModules(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return false;
        if (client.hasSingleplayerServer()) {
            return configManager.config().allowSingleplayer;
        }

        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null) {
            return false;
        }

        String connectedAddress = normalize(server.ip);
        if (connectedAddress.isEmpty()) return false;
        return configManager.config().allowedServers.stream()
                .map(ServerPolicy::normalize)
                .anyMatch(connectedAddress::equals);
    }

    private static String normalize(String address) {
        return address == null ? "" : address.strip().toLowerCase(Locale.ROOT);
    }
}

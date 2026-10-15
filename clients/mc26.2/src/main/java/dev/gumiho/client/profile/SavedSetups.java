package dev.gumiho.client.profile;

import dev.gumiho.client.BuildInfo;
import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.config.GumihoConfig;
import dev.gumiho.client.module.HudModule;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.util.UUID;

public final class SavedSetups {
    public static final PresetStore STORE = new PresetStore(FMLPaths.CONFIGDIR.get().resolve("gumiho-setups"));
    private SavedSetups() { }
    public static GumihoConfig snapshot() {
        GumihoConfig result = PresetCodec.copy(GumihoClient.CONFIG.config());
        for (var module : GumihoClient.MODULES.modules()) {
            result.modules.put(module.id(), module.enabled());
            if (module instanceof HudModule hud) {
                result.hudX.put(module.id(), hud.preferredX());
                result.hudY.put(module.id(), hud.preferredY());
                result.hudScales.put(module.id(),hud.scale());
            }
        }
        return result;
    }
    public static String export(PresetCodec.Kind kind) {
        return PresetCodec.encode(snapshot(), kind, BuildInfo.MINECRAFT);
    }
    public static void save(String name, PresetCodec.Kind kind) throws IOException {
        STORE.save(name, kind, export(kind));
    }
    public static String load(String json, PresetCodec.Kind kind, Minecraft client) throws IOException {
        GumihoConfig before = snapshot();
        GumihoConfig next = PresetCodec.merge(json, kind, before);
        String backup = "Before-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0,8);
        STORE.save(backup, PresetCodec.Kind.KIT, PresetCodec.encode(before, PresetCodec.Kind.KIT, BuildInfo.MINECRAFT));
        try {
            int blocked = apply(next, kind, client);
            return blocked == 0 ? "Loaded; backup saved | 불러오기 완료" : "Loaded; " + blocked + " restricted toggles skipped";
        } catch (IOException | RuntimeException error) {
            try { apply(before, kind, client); }
            catch (IOException | RuntimeException recovery) { error.addSuppressed(recovery); }
            throw error;
        }
    }
    private static int apply(GumihoConfig next, PresetCodec.Kind kind, Minecraft client) throws IOException {
        int blocked = 0;
        if (kind == PresetCodec.Kind.KIT) {
            // Release old automation and brightness ownership before installing new options.
            for (var module : GumihoClient.MODULES.modules()) {
                module.suspend(client);
                module.setEnabled(false, client, GumihoClient.SERVER_POLICY);
            }
            next.activeProfile = "CUSTOM";
            GumihoClient.CONFIG.replaceWith(next);
            for (var module : GumihoClient.MODULES.modules()) {
                boolean requested = Boolean.TRUE.equals(next.modules.get(module.id()));
                if (!module.setEnabled(requested, client, GumihoClient.SERVER_POLICY)) blocked++;
                next.modules.put(module.id(), module.enabled());
            }
        } else {
            // A HUD-only import must never change options, toggles or permissions.
            GumihoClient.CONFIG.config().hudX = next.hudX;
            GumihoClient.CONFIG.config().hudY = next.hudY;
            GumihoClient.CONFIG.config().hudScales = next.hudScales;
        }
        for (var module : GumihoClient.MODULES.modules()) {
            if (module instanceof HudModule hud) {
                Integer x = next.hudX.get(module.id()), y = next.hudY.get(module.id());
                if (x != null && y != null) hud.setPosition(x, y);
                hud.setScale(next.hudScales.getOrDefault(module.id(),1.0));
            }
        }
        GumihoClient.CONFIG.saveChecked();
        return blocked;
    }
}

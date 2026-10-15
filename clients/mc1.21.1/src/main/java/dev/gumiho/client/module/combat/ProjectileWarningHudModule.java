package dev.gumiho.client.module.combat;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.HudEditorScreen;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class ProjectileWarningHudModule extends HudModule {
    private static final int WIDTH = 176;
    private Object cachedLevel;
    private int cachedTick = Integer.MIN_VALUE;
    private Warning cachedWarning;

    public ProjectileWarningHudModule() {
        super("projectile_warning", "Projectile Warning", "투사체 경고",
                "Warn about nearby projectiles moving toward you", "당신을 향해 움직이는 근처 투사체를 경고합니다",
                ModuleCategory.COMBAT, true, 6, 199);
    }

    @Override
    public void renderHud(GuiGraphics graphics, Minecraft client) {
        int tick = client.player == null ? Integer.MIN_VALUE : client.player.tickCount;
        if (cachedLevel != client.level || tick != cachedTick) {
            cachedLevel = client.level;
            cachedTick = tick;
            cachedWarning = nearestWarning(client);
        }
        Warning warning = cachedWarning;
        if (warning == null && GumihoClient.MODULES.isEnabled("contextual_hud")
                && !(client.screen instanceof HudEditorScreen)) return;
        String title = warning == null ? "Projectile | 투사체: Clear" : "INCOMING | 접근 " + warning.direction;
        String detail = warning == null ? "No projectile threat | 위협 없음" : String.format(
                Locale.ROOT, "%s  %.1fm  ~%.1fs", warning.name, warning.distance, warning.seconds);
        graphics.fill(x(), y(), x() + WIDTH, y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        graphics.fill(x(), y(), x() + 3, y() + hudHeight(), 0xFFFF4D36);
        graphics.drawString(client.font, title, x() + 7, y() + 4, 0xFFFF7664, false);
        graphics.drawString(client.font, detail, x() + 7, y() + 15, 0xFFFFE8C2, false);
    }

    @Override public int hudWidth(Minecraft client) { return WIDTH; }
    @Override public int hudHeight() { return 27; }

    private static Warning nearestWarning(Minecraft client) {
        if (client.player == null || client.level == null) return null;
        Warning best = null;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Projectile projectile) || projectile.getOwner() == client.player) continue;
            Vec3 motion = projectile.getDeltaMovement();
            double speed = motion.length();
            double distance = client.player.distanceTo(projectile);
            if (speed < 0.05D || distance > 32.0D) continue;
            Vec3 towardPlayer = client.player.getEyePosition().subtract(projectile.position()).normalize();
            if (motion.normalize().dot(towardPlayer) < 0.65D) continue;
            Warning candidate = new Warning(projectile.getName().getString(), distance,
                    distance / speed / 20.0D, CombatAwareness.direction(client, projectile.position()));
            if (best == null || candidate.seconds < best.seconds) best = candidate;
        }
        return best;
    }

    private record Warning(String name, double distance, double seconds, String direction) {
    }
}

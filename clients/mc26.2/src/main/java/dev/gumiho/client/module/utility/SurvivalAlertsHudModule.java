package dev.gumiho.client.module.utility;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.HudEditorScreen;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import dev.gumiho.client.module.combat.CombatAwareness;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class SurvivalAlertsHudModule extends HudModule {
    private static final int WIDTH = 176;
    private int previousHurtTime;
    private int hitDirectionTicks;
    private String hitDirection = "";
    private Object trackedPlayer, trackedLevel;

    public SurvivalAlertsHudModule() {
        super("survival_alerts", "Survival Alerts", "생존 경고",
                "Warn about low health, armor, missing totems, and hit direction",
                "낮은 체력, 방어구, 토템 부족과 피격 방향을 경고합니다",
                ModuleCategory.UTILITY, true, 6, 166);
    }

    @Override
    public void tick(Minecraft client) {
        if (trackedPlayer != client.player || trackedLevel != client.level) {
            trackedPlayer = client.player; trackedLevel = client.level;
            previousHurtTime = 0; hitDirectionTicks = 0; hitDirection = "";
        }
        if (client.player == null) return;
        if (client.player.hurtTime > previousHurtTime) {
            DamageSource source = client.player.getLastDamageSource();
            Entity attacker = source == null ? null : source.getEntity();
            if (attacker != null) {
                hitDirection = CombatAwareness.direction(client, attacker.position());
                hitDirectionTicks = 50;
            }
        }
        previousHurtTime = client.player.hurtTime;
        if (hitDirectionTicks > 0) hitDirectionTicks--;
    }

    @Override
    public void renderHud(GuiGraphicsExtractor graphics, Minecraft client) {
        List<String> alerts = alerts(client);
        if (alerts.isEmpty() && GumihoClient.MODULES.isEnabled("contextual_hud")
                && !(client.gui.screen() instanceof HudEditorScreen)) return;
        String line = alerts.isEmpty() ? "Status clear | 정상" : String.join("  •  ", alerts);
        graphics.fill(x(), y(), x() + WIDTH, y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        graphics.fill(x(), y(), x() + 3, y() + hudHeight(), HudTheme.ORANGE);
        graphics.text(client.font, "Survival | 생존", x() + 7, y() + 4,
                HudTheme.ORANGE, false);
        graphics.text(client.font, fit(client, line), x() + 7, y() + 15, 0xFFFFE8C2, false);
    }

    @Override public int hudWidth(Minecraft client) { return WIDTH; }
    @Override public int hudHeight() { return 27; }

    private List<String> alerts(Minecraft client) {
        List<String> alerts = new ArrayList<>();
        if (client.player == null) return alerts;
        if (client.player.getHealth() <= GumihoClient.CONFIG.config().lowHealthWarning) {
            alerts.add("LOW HP " + Math.round(client.player.getHealth()));
        }
        int armor = lowestArmorPercent(client);
        if (armor >= 0 && armor <= 25) alerts.add("ARMOR " + armor + "%");
        if (client.player.getHealth() <= 14.0F && !hasTotem(client)) alerts.add("NO TOTEM");
        if (hitDirectionTicks > 0) alerts.add("HIT " + hitDirection);
        return alerts;
    }

    private static int lowestArmorPercent(Minecraft client) {
        int lowest = 101;
        boolean found = false;
        for (ItemStack stack : java.util.List.of(client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD), client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST), client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS), client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET))) {
            if (!stack.isDamageableItem()) continue;
            found = true;
            int remaining = Math.round(100.0F * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
            lowest = Math.min(lowest, remaining);
        }
        return found ? lowest : -1;
    }

    private static boolean hasTotem(Minecraft client) {
        if (client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return true;
        for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++) {
            if (client.player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) return true;
        }
        return false;
    }

    private static String fit(Minecraft client, String text) {
        if (client.font.width(text) <= WIDTH - 14) return text;
        return client.font.plainSubstrByWidth(text, WIDTH - 14 - client.font.width("…")) + "…";
    }

    @Override protected void onDisable(Minecraft client) {
        trackedPlayer = null; trackedLevel = null; previousHurtTime = 0;
        hitDirectionTicks = 0; hitDirection = "";
    }
}

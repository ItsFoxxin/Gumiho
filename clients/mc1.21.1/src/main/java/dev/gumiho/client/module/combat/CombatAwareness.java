package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.tracking.PlayerGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class CombatAwareness {
    private static Minecraft cachedClient;
    private static Object cachedLevel;
    private static int cachedTick = Integer.MIN_VALUE;
    private static List<AbstractClientPlayer> cachedPlayers = List.of();

    private CombatAwareness() {
    }

    public static AbstractClientPlayer nearestEnemy(Minecraft client, double range) {
        if (client.player == null || client.level == null) return null;
        return loadedPlayers(client).stream()
                .filter(player -> GumihoClient.PLAYER_GROUPS.groupOf(player.getScoreboardName()) == PlayerGroup.ENEMY)
                .filter(player -> client.player.distanceTo(player) <= range)
                .min(Comparator
                        .comparingInt((AbstractClientPlayer player) ->
                                GumihoClient.PLAYER_GROUPS.isPinned(player.getScoreboardName()) ? 0 : 1)
                        .thenComparingDouble(client.player::distanceTo))
                .orElse(null);
    }

    public static List<AbstractClientPlayer> loadedPlayers(Minecraft client) {
        if (client.player == null || client.level == null) return List.of();
        int tick = client.player.tickCount;
        if (cachedClient != client || cachedLevel != client.level || cachedTick != tick) {
            cachedClient = client;
            cachedLevel = client.level;
            cachedTick = tick;
            cachedPlayers = client.level.players().stream()
                    .filter(player -> player != client.player && player.isAlive() && !player.isSpectator())
                    .toList();
        }
        return cachedPlayers;
    }

    public static int threatScore(Minecraft client, AbstractClientPlayer enemy) {
        double distance = client.player.distanceTo(enemy);
        int score = (int) Mth.clamp(70.0D - distance, 0.0D, 70.0D);
        ItemStack weapon = enemy.getMainHandItem();
        if (weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem) score += 12;
        if (weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem) score += 10;
        if (enemy.isUsingItem()) score += 8;
        if (isLookingAt(enemy, client.player.position())) score += 10;
        return Mth.clamp(score, 0, 100);
    }

    public static String direction(Minecraft client, Vec3 position) {
        double dx = position.x - client.player.getX();
        double dz = position.z - client.player.getZ();
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float relative = Mth.wrapDegrees(targetYaw - client.player.getYRot());
        if (relative > 135 || relative < -135) return "v";
        if (relative > 45) return ">";
        if (relative < -45) return "<";
        return "^";
    }

    public static String intent(AbstractClientPlayer enemy) {
        ItemStack used = enemy.getUseItem();
        if (enemy.isUsingItem() && used.getItem() instanceof BowItem) return "AIMING BOW";
        if (enemy.isUsingItem() && used.getItem() instanceof CrossbowItem) return "CHARGING";
        if (enemy.isUsingItem() && used.getItem() instanceof ShieldItem) return "SHIELDING";
        ItemStack weapon = enemy.getMainHandItem();
        if (weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem) return "MELEE READY";
        return "WATCHING";
    }

    public static String armorStatus(AbstractClientPlayer player) {
        int pieces = 0;
        int worstPercent = 100;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty()) continue;
            pieces++;
            if (stack.isDamageableItem()) {
                int remaining = Math.round(100.0F * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
                worstPercent = Math.min(worstPercent, remaining);
            }
        }
        return pieces == 0 ? "A0" : "A" + pieces + " " + worstPercent + "%";
    }

    private static boolean isLookingAt(AbstractClientPlayer player, Vec3 target) {
        Vec3 toTarget = target.subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().normalize().dot(toTarget) > 0.90D;
    }
}

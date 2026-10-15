package dev.gumiho.client.module.combat;

import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;

public final class AutoWeaponModule extends GumihoModule {
    private Object lastPlayer;
    private int lastSwitchTick = Integer.MIN_VALUE;
    public AutoWeaponModule() {
        super("auto_weapon", "Auto Weapon", "자동 무기",
                "Hotbar sword preference; use an axe against a raised shield. Keeps a held mace/trident",
                "핫바의 검을 선택하고 방패에는 도끼를 사용합니다. 철퇴는 유지합니다",
                ModuleCategory.COMBAT, false, true);
    }

    @Override public void tick(Minecraft client) {
        var target = MeleeActions.crosshairTarget(client);
        if (target == null || !MeleeActions.weapon(client.player.getMainHandItem())
                || client.player.getMainHandItem().getItem() instanceof MaceItem
                || client.player.getMainHandItem().is(net.minecraft.world.item.Items.TRIDENT)) return;
        int tick = client.player.tickCount;
        if (lastPlayer == client.player && (long) tick - lastSwitchTick >= 0 && (long) tick - lastSwitchTick < 10) return;
        boolean axe = MeleeActions.facingShield(client, target);
        int best = client.player.getInventory().selected;
        double score = score(client.player.getMainHandItem(), axe);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            double candidate = score(stack, axe);
            if (candidate > score) { score = candidate; best = slot; }
        }
        if (best != client.player.getInventory().selected) {
            client.player.getInventory().selected = best;
            client.player.resetAttackStrengthTicker();
            lastPlayer = client.player;
            lastSwitchTick = tick;
        }
    }

    private static double score(ItemStack stack, boolean preferAxe) {
        boolean sword = stack.getItem() instanceof SwordItem;
        boolean axe = stack.getItem() instanceof AxeItem;
        if (stack.isEmpty() || (!sword && !axe)) return -1;
        double[] damage = {1};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                damage[0] += modifier.amount();
            }
        });
        // Category preference first, then base attack damage; enchantment matchups are not predicted.
        return (preferAxe == axe ? 100 : 0) + damage[0];
    }
}

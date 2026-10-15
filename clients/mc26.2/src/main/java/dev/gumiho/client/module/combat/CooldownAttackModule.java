package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.MaceItem;

public final class CooldownAttackModule extends GumihoModule {
    public CooldownAttackModule() {
        super("cooldown_attack", "Cooldown Attack", "재사용 공격",
                "Attack the eligible crosshair target at full charge; release manual attack to avoid extra swings",
                "충전 완료 시 조준 대상을 공격합니다. 수동 공격 버튼은 놓으세요",
                ModuleCategory.COMBAT, false, true);
    }
    @Override public void tick(Minecraft client) {
        if (!MeleeActions.active(client)) return;
        if (client.player.getMainHandItem().getItem() instanceof MaceItem
                && GumihoClient.MODULES.isEnabled("mace_smash")) return;
        MeleeActions.attack(client, MeleeActions.crosshairTarget(client), false);
    }
}

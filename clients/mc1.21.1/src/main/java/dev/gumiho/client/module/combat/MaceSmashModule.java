package dev.gumiho.client.module.combat;

import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class MaceSmashModule extends GumihoModule {
    public MaceSmashModule() {
        super("mace_smash", "Mace Smash Timing", "철퇴 강타 타이밍",
                "Strike a crosshair target while descending with a charged mace; does not jump or prevent falls",
                "추락 중 충전된 철퇴로 조준 대상을 공격합니다. 낙하 피해를 막지는 않습니다",
                ModuleCategory.COMBAT, false, true);
    }
    @Override public void tick(Minecraft client) {
        if (MeleeActions.active(client) && MeleeActions.smashReady(client)) {
            MeleeActions.attack(client, MeleeActions.crosshairTarget(client), true);
        }
    }
}

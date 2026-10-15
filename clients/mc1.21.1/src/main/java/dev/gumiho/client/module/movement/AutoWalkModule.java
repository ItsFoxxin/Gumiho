package dev.gumiho.client.module.movement;

import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class AutoWalkModule extends GumihoModule {
    public AutoWalkModule() {
        super(
                "auto_walk",
                "Auto Walk",
                "자동 걷기",
                "Hold forward automatically until disabled",
                "끌 때까지 자동으로 앞으로 이동합니다",
                ModuleCategory.MOVEMENT,
                false,
                false
        );
    }

    @Override
    public void tick(Minecraft client) {
        boolean canWalk = client.player != null && client.screen == null;
        client.options.keyUp.setDown(canWalk);
    }

    @Override
    protected void onDisable(Minecraft client) {
        client.options.keyUp.setDown(false);
    }
}

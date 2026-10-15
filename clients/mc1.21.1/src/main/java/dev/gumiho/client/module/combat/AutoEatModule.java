package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;

public final class AutoEatModule extends GumihoModule {
    private boolean eating;
    private int previousSlot = -1;

    public AutoEatModule() {
        super(
                "auto_eat",
                "Auto Eat",
                "자동 먹기",
                "Eat the best hotbar food when hunger drops below seven bars",
                "배고픔이 일곱 칸 아래로 내려가면 단축바의 가장 좋은 음식을 먹습니다",
                ModuleCategory.COMBAT,
                false,
                true
        );
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null || client.screen != null) {
            stop(client);
            return;
        }

        if (eating) {
            if (client.player.getFoodData().getFoodLevel() >= 20
                    || food(client.player.getMainHandItem()) == null) {
                stop(client);
            } else {
                client.options.keyUse.setDown(true);
            }
            return;
        }

        if (client.player.getFoodData().getFoodLevel() > GumihoClient.CONFIG.config().autoEatHunger
                || client.player.isUsingItem() || client.options.keyUse.isDown()
                || client.player.getMainHandItem().is(Items.WIND_CHARGE)
                || GumihoClient.WIND_MACE_SWAP.launchGrace(client)
                || (client.player.getMainHandItem().getItem() instanceof MaceItem
                    && !client.player.onGround() && GumihoClient.MODULES.isEnabled("mace_smash"))) {
            return;
        }

        int foodSlot = bestFoodSlot(client);
        if (foodSlot < 0) {
            return;
        }

        previousSlot = client.player.getInventory().selected;
        selectSlot(client, foodSlot);
        eating = true;
        client.options.keyUse.setDown(true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        stop(client);
    }

    private void stop(Minecraft client) {
        if (!eating) return;
        client.options.keyUse.setDown(false);
        if (client.player != null && previousSlot >= 0 && previousSlot < 9) {
            selectSlot(client, previousSlot);
        }
        previousSlot = -1;
        eating = false;
    }

    private static int bestFoodSlot(Minecraft client) {
        int bestSlot = -1;
        int bestNutrition = -1;
        for (int slot = 0; slot < 9; slot++) {
            FoodProperties properties = food(client.player.getInventory().getItem(slot));
            if (properties != null && properties.nutrition() > bestNutrition) {
                bestNutrition = properties.nutrition();
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static FoodProperties food(ItemStack stack) {
        return stack.get(DataComponents.FOOD);
    }

    private static void selectSlot(Minecraft client, int slot) {
        client.player.getInventory().selected = slot;
        if (client.getConnection() != null) {
            client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
    }
}

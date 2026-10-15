package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

public final class AutoTotemModule extends GumihoModule {
    public AutoTotemModule() {
        super(
                "auto_totem",
                "Auto Totem",
                "자동 토템",
                "Equip a totem from inventory when health drops to five hearts",
                "체력이 다섯 칸 이하가 되면 인벤토리의 토템을 장착합니다",
                ModuleCategory.COMBAT,
                false,
                true
        );
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.screen != null) {
            return;
        }
        if (client.player.getHealth() + client.player.getAbsorptionAmount() > GumihoClient.CONFIG.config().autoTotemHealth
                || client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        int inventorySlot = findTotem(client);
        if (inventorySlot < 0) {
            return;
        }

        int menuSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        client.gameMode.handleInventoryMouseClick(
                client.player.inventoryMenu.containerId,
                menuSlot,
                40,
                ClickType.SWAP,
                client.player
        );
    }

    private static int findTotem(Minecraft client) {
        for (int slot = 0; slot < 36; slot++) {
            if (client.player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
                return slot;
            }
        }
        return -1;
    }
}

package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class WindMaceSwapModule extends GumihoModule {
    private final WindSwapTrigger trigger = new WindSwapTrigger();
    private Object swappedPlayer;
    private int swappedTick;
    public WindMaceSwapModule() {
        super("wind_mace_swap", "Wind Charge → Mace", "바람 탄환 → 철퇴",
                "After throwing a mainhand wind charge, select a hotbar mace; normal cooldown applies",
                "주 손의 바람 탄환을 던진 뒤 핫바 철퇴로 전환합니다. 재사용 대기는 유지됩니다",
                ModuleCategory.COMBAT, false, true);
    }
    private boolean active(Minecraft client) {
        return enabled() && runtimeReady(client) && client.player != null && client.level != null && client.gameMode != null
                && client.player.isAlive() && !client.player.isSpectator() && client.screen == null
                && client.isWindowActive() && !client.isPaused()
                && GumihoClient.SERVER_POLICY.canUseExperimentalModules(client);
    }
    public void onUse(PlayerInteractEvent.RightClickItem event) {
        Minecraft client = Minecraft.getInstance();
        if (event.getEntity() != client.player || !event.getLevel().isClientSide()
                || event.isCanceled() || !active(client) || event.getHand() != InteractionHand.MAIN_HAND
                || !client.player.getMainHandItem().is(Items.WIND_CHARGE)
                || client.player.getCooldowns().isOnCooldown(Items.WIND_CHARGE)) return;
        trigger.arm(client.player, client.player.tickCount, client.player.getInventory().selected);
    }
    @Override public void tick(Minecraft client) { update(client); }
    @Override public void frame(Minecraft client, float partialTick) { update(client); }
    private void update(Minecraft client) {
        if (!active(client)) { suspend(client); return; }
        var held = client.player.getMainHandItem();
        if ((!held.is(Items.WIND_CHARGE) && !held.isEmpty()) || client.player.isUsingItem()) {
            trigger.reset(); return;
        }
        if (!trigger.consume(client.player, client.player.tickCount, client.player.getInventory().selected,
                client.player.getCooldowns().isOnCooldown(Items.WIND_CHARGE))) return;
        for (int slot = 0; slot < 9; slot++) {
            if (!(client.player.getInventory().getItem(slot).getItem() instanceof MaceItem)) continue;
            client.player.getInventory().selected = slot;
            if (client.getConnection() != null) client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            client.player.resetAttackStrengthTicker();
            swappedPlayer = client.player;
            swappedTick = client.player.tickCount;
            return;
        }
    }
    public boolean launchGrace(Minecraft client) {
        return enabled() && swappedPlayer == client.player && client.player != null
                && client.player.tickCount >= swappedTick && client.player.tickCount - swappedTick < 10;
    }
    @Override public void suspend(Minecraft client) { trigger.reset(); swappedPlayer = null; }
    @Override protected void onDisable(Minecraft client) { suspend(client); }
}

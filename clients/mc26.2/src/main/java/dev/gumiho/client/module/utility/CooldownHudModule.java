package dev.gumiho.client.module.utility;
import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.SnapshotHudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import java.util.List;
public final class CooldownHudModule extends SnapshotHudModule {
    public CooldownHudModule() {
        super("cooldown_hud","Item Cooldowns","아이템 재사용",
                "Pearl/shield cooldown remaining, or Ready. Not attack charge or shield startup time",
                "진주와 방패의 남은 재사용 시간을 표시합니다. 공격 충전이나 방패 준비 시간은 아닙니다",
                ModuleCategory.UTILITY,true,178,210,"Cooldowns | 재사용",2);
    }
    @Override protected List<Row> readRows(Minecraft c) {
        if(c.player==null) return List.of();
        float pearl=c.player.getCooldowns().getCooldownPercent(new net.minecraft.world.item.ItemStack(Items.ENDER_PEARL),0);
        float shield=c.player.getCooldowns().getCooldownPercent(new net.minecraft.world.item.ItemStack(Items.SHIELD),0);
        if(pearl==0&&shield==0&&GumihoClient.MODULES.isEnabled("contextual_hud")) return List.of();
        return List.of(new Row("Pearl | 진주: "+HudValues.cooldown(pearl),0xFFD7BDA8),
                new Row("Shield | 방패: "+HudValues.cooldown(shield),0xFFFFA45B));
    }
}

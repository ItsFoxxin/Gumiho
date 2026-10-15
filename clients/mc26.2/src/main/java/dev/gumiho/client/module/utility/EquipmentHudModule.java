package dev.gumiho.client.module.utility;
import dev.gumiho.client.module.SnapshotHudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.ArrayList;
import java.util.List;
public final class EquipmentHudModule extends SnapshotHudModule {
    public EquipmentHudModule() {
        super("equipment_hud", "Equipment Durability", "장비 내구도",
                "Remaining durability for armor and held tools; warning below 25%. Does not equip or repair items",
                "방어구와 손에 든 도구의 내구도 표시. 자동 장착이나 수리는 하지 않습니다",
                ModuleCategory.UTILITY, false, 178, 353, "Equipment | 장비", 6);
    }
    @Override protected List<Row> readRows(Minecraft client) {
        if (client.player == null) return List.of();
        var rows = new ArrayList<Row>();
        var slots = new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        String[] names = {"Head | 머리", "Chest | 가슴", "Legs | 다리", "Feet | 발", "Main | 주 손", "Off | 보조"};
        for (int i=0;i<slots.length;i++) {
            var item = client.player.getItemBySlot(slots[i]);
            if (!item.isDamageableItem()) continue;
            int percent = HudValues.durability(item.getDamageValue(), item.getMaxDamage());
            int remaining = Math.max(0,item.getMaxDamage()-item.getDamageValue());
            rows.add(new Row(names[i] + ": " + remaining + "/" + item.getMaxDamage() + " (" + percent + "%)",
                    percent <= 25 ? 0xFFFF7664 : 0xFFD7BDA8));
        }
        return rows;
    }
}

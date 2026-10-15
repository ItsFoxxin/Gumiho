package dev.gumiho.client.module.utility;
import dev.gumiho.client.module.SnapshotHudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import java.util.List;
public final class CombatResourcesHudModule extends SnapshotHudModule {
    public CombatResourcesHudModule() {
        super("combat_resources_hud", "Combat Resources", "전투 자원",
                "Inventory counts: totems, pearls, apples, crystals, arrows and wind charges. Updates once per tick",
                "토템, 진주, 사과, 수정, 화살, 바람 탄환 수를 틱마다 표시합니다",
                ModuleCategory.UTILITY, false, 178, 258, "Resources | 자원", 3);
    }
    @Override protected List<Row> readRows(Minecraft client) {
        int totem=0, pearl=0, apple=0, crystal=0, arrow=0, wind=0;
        if (client.player == null) return List.of();
        var inventory=client.player.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++) {
            var item=inventory.getItem(slot); int n=item.getCount();
            if(item.is(Items.TOTEM_OF_UNDYING)) totem+=n;
            else if(item.is(Items.ENDER_PEARL)) pearl+=n;
            else if(item.is(Items.GOLDEN_APPLE)||item.is(Items.ENCHANTED_GOLDEN_APPLE)) apple+=n;
            else if(item.is(Items.END_CRYSTAL)) crystal+=n;
            else if(item.is(Items.ARROW)||item.is(Items.SPECTRAL_ARROW)||item.is(Items.TIPPED_ARROW)) arrow+=n;
            else if(item.is(Items.WIND_CHARGE)) wind+=n;
        }
        return List.of(new Row("Totem "+totem+"  •  Pearl "+pearl,0xFFD7BDA8),
                new Row("Gap "+apple+"  •  Crystal "+crystal,0xFFD7BDA8),
                new Row("Arrows "+arrow+"  •  Wind "+wind,0xFFFFA45B));
    }
}

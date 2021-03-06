package net.wildbill22.draco.items.dragoneggs;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.wildbill22.draco.entities.dragons.EntityDracoTenebrosus;
import net.wildbill22.draco.items.ModItems;
import net.wildbill22.draco.items.weapons.ModWeapons;
import net.wildbill22.draco.lib.BALANCE;

public class ItemNightDragonEgg extends ItemDragonEgg {
	public static final String name = "nightDragonEgg";
	private final static String dragonName = EntityDracoTenebrosus.name;

	public ItemNightDragonEgg() {
		super(name, dragonName);
//		String dragonName = EntityDracoTenebrosus.name;
//		this.addDragonFood(dragonName, ModItems.villagerHeart);
		this.addDragonFood(dragonName, Items.beef);
		this.addDragonFood(dragonName, Items.cooked_beef);
		this.addNightDragonAbilities(dragonName);
		if (BALANCE.DRAGON_PLAYER_ABILITIES.NIGHT_DRAGON_INVISIBILITY)
			this.addInvisibleInTheDark(dragonName);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4) {
		list.add(StatCollector.translateToLocal("egg.wildbill22_draco_putThisEgg"));
		list.add(StatCollector.translateToLocal("egg.wildbill22_draco_youWillGetStaff")); 
		list.add(StatCollector.translateToLocal("egg.wildbill22_draco_intoNightDragon"));
	}
	
	@Override
	public String getEggName() {
		return name;
	}

	@Override
	public Item getEggItem() {
		return ModItems.nightDragonEgg;
	}

	@Override
	public ItemStack getStaffItemStack() {
		return new ItemStack(ModWeapons.nightDragonStaff);
	}
}

package com.flansmod.common;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.fluids.Fluid;

/**
 * This class handles all the configs for Flan's Mod
 * Config names should be in title case with a space between every word
 * @author Lithius
 *
 */
@Config(modid = FlansMod.MODID)
public final class FlansConfig {
	
	public static ClientCategory client = new ClientCategory();
	
	@Name("Add All Paintjobs to Creative")
	@Comment("Whether or not all paintjobs should be added in creative")
	public static boolean addAllPaintjobsToCreative = false;
	
	@Name("Gunpowder Recipe")
	@Comment("Whether or not the Flan's Mod recipe for gunpowder should be available, (3 charcoal + 1 glowstone)")
	public static boolean addGunpowderRecipe = true;
	
	@Name("Force Update JSONs")
	@Comment("Turn this on to force re-create all JSON files. Should only be used in dev environment")
	public static boolean forceUpdateJSONs = false;
	
	@Name("Enable Enchantment Module")
	@Comment("Whether or not to enable gun-related enchantments added by Flan's Mod")
	public static boolean enchantmentModuleEnabled = true;
	
	@Name("Disabled Vehicles")
	@Comment({
		"Any vehicle shortnames included in here will not be craftable in the vehicle crafting table",
		"Every entry should be separated by a new line"
	})
	public static String[] disabledVehicles = {}; //List of shortnames of disabled vehicles
	
	@Name("Global Fuel Use Multiplier")
	@Comment("Fuel use will be multiplied by this value. Higher means more fuel used per tick")
	public static float globalFuelUseMultiplier = 1F;
	
	@Name("Vehicle Fuel Transfer Rate")
	@Comment("How much fuel (mb) to transfer from a container to the vehicle in one tick")
	@RangeInt(min = 0, max = 10000)
	public static int vehicleFuelTransferRate = 50;
	
	@Name("Vehicle Energy Transfer Rate")
	@Comment("How much fuel (FE/RF) to transfer from a container to the vehicle in one tick")
	@RangeInt(min = 0, max = 1000000)
	public static int vehicleEnergyTransferRate = 2000;
	
	@Name("Generate Dungeon Loot")
	@Comment("Whether or not chests in randomly generated structures should have Flan's Mod loot")
	public static boolean generateDungeonLoot = false;
	
	@Name("Spawn Mobs with Armor")
	@Comment("If true, skeletons and zombies have a chance to spawn with random Flan's Mod armor")
	public static boolean mobArmorSpawn = false;
	
	@Name("Accepted Fuels")
	@Comment({
		"The fluids listed below will be usable as fuel by Flan's vehicles",
		"You can find these names with mods (Crafttweaker, Modtweaker, or TellMe)",
		"or you can open up the relevant mod's .jar file and look in the .lang files for fluid names"
	})
	public static String[] acceptedFuelNames = {"refined_fuel", "refined_biofuel", "diesel", "gasoline", "biodiesel"};
	
	public static class ClientCategory {
		@Name("Shoot on Right Click")
		@Comment("If true, guns will be shot on right click, if false, guns will be shot on left click")
		public boolean shootOnRightClick = false;
	}
	
	/**
	 * Checks to see if a vehicle's crafting recipe has been disabled by the config.
	 * True if the vehicle associated with the shortname given has been disabled.
	 * @param 	vehicleShortname
	 * @return true if the vehicle has been explicitly disabled by the config, false otherwise.
	 */
	public static boolean isVehicleDisabled (String vehicleShortname) {
		for (String shortname : disabledVehicles) {
			if (shortname.equals(vehicleShortname)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check to see whether or not the given fluid is a compatible fuel
	 * @param fluid	the possible fuel fluid to check
	 * @return		true if the fluid given is compatible, false otherwise
	 */
	public static boolean isFuel(Fluid fluid) {
		String fluidName = fluid.getName();
		
		for (String acceptedFluidName : acceptedFuelNames) {
			if (fluidName.equals(acceptedFluidName)) {
				return true;
			}
		}
		
		return false;
	}
	
}

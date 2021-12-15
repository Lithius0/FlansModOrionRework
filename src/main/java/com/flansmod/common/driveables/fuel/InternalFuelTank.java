package com.flansmod.common.driveables.fuel;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

/**
 * Keeps track of the fuel/energy within a driveable.
 * Fuel is measured in millibuckets(mb) and energy is measured in RF/FE.
 * One mb of fuel is equated being 1,200 RF. This is using Immersive Petroleum's gasoline as a standard
 * which creates 1,200 RF per bucket in an unmodified Compression Dynamo from Thermal Expansion. 
 */
public abstract class InternalFuelTank {
	
	/**
	 * Whether or not the fuel tank has any fuel/energy left at all
	 * @return	false if the fuel tank is completely empty, true otherwise
	 */
	public abstract boolean hasFuel();
	
	/**
	 * Whether or not the fuel tank has enough fuel/energy for one more tick.
	 * Will return false if there is a bit of fuel/energy left, but not enough for the stated consumption.
	 * Will automatically convert from fuel consumption to energy consumption. 
	 * Call this method as if the driveable is burning fuel.
	 * @param consumption	the amount of fuel/energy the driveable will consume the next tick
	 * @return				true if there is enough fuel/energy for one more tick, false otherwise
	 */
	public abstract boolean hasFuel(float consumption);
	
	
	/**
	 * Sets the driveable to a given percentage instantly, skipping the receive methods.
	 * Useful for testing or spawning a preset vehicle.
	 * Do not use this method for standard refuelling procedure, use the receive methods instead.
	 * @param percentage	what level to fill the driveable to, 0 = empty, 1 = full
	 */
	public abstract void setFillPercentage(float percentage);
	
	/**
	 * Sets the driveable's fuel/energy, skipping the receive methods and ignoring fuel transfer limits.
	 * Use this method when reading NBT.
	 * Do not use this method for standard refuelling procedure, use the receive methods instead.
	 * @param amount	how much fuel/energy to fill to, 
	 */
	public abstract void setFillAmount(float amount);
	
	/**
	 * Gets the amount of fuel/energy currently in the tank.
	 * @return	the amount of fuel/energy in the tank. (mb/RF)
	 */
	public abstract float getFillLevel();
	
	/**
	 * Gets the maximum amount of fuel/energy currently in the tank.
	 * Will return energy if driveable is electric (i.e. isElectric flag is true)
	 * @return	the maximum amount of fuel/energy that can fit in the tank. (mb/RF)
	 */
	public abstract float getMaxFillLevel();
	
	/**
	 * Removes some fuel/energy from storage. Does not check whether or not the instance has any fuel/energy left.
	 * Automatically takes care of conversion from fuel to RF. 
	 * It is recommended to check with the {@link #hasFuel(float)} method first.
	 * @param amountToConsume	how much fuel to burn this tick
	 */
	public abstract void consume(float amountToConsume);
	
	/**
	 * Attempts to drain the item in the fuel slot of fuel/energy and put it into the fuel tank/battery
	 * @param stack	the item to drain energy/fuel from
	 * @return the resulting ItemStack of the item given. 
	 * 			Will only really be different than {@code stack} in the case of buckets.
	 */
	public abstract ItemStack handleFuelItem(@Nonnull ItemStack stack);
}

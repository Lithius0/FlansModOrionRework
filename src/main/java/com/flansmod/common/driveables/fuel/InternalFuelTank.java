package com.flansmod.common.driveables.fuel;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Keeps track of the fuel/energy within a driveable.
 * Fuel is measured in millibuckets(mb) and energy is measured in RF/FE.
 * One mb of fuel is equated being 1,200 RF. This is using Immersive Petroleum's gasoline as a standard
 * which creates 1,200 RF per bucket in an unmodified Compression Dynamo. 
 * This class could eventually get upgraded to a Forge IFluidTank, but that's outside current scope.
 * @author Lithius
 */
public class InternalFuelTank implements IEnergyStorage {
	/**
	 * All the types of fuels you can put into Flan's driveables
	 */
	final private static Set<Fluid> acceptedFuels = new HashSet<Fluid>();
	
	/**
	 * When the fuel tank is told to consume, this value represents how many RF = 1 mb.
	 */
	final private static int ENERGY_CONSUMPTION_MULTIPLIER = 1200;
	
	/**
	 * How fast you can put fuel into the driveable (mb/t)
	 * TODO: Make this a config later
	 */
	private static float fuelTransferRate = 50;
	/**
	 * How fast you can put energy into the driveable (FE/t)
	 * TODO: Make this a config later
	 */
	private static int energyTransferRate = 2000;
	
	/**
	 * The amount of fuel in the driveable, in millibuckets of gasoline.
	 * Note that because most vehicles will burn less than 1mb/t, this value is a float
	 * rather than an integer, unlike standard Forge fluid tanks.
	 */
	private float fuel; 
	/**
	 * The max amount of fuel the driveable can have, in millibuckets of gasoline
	 */
	private float maxFuel;
	
	/**
	 * The current amount of energy in the driveable, in units of FE or RF.
	 * Unlike fuel, most vehicles should burn at least 10RF/t, so there's no need for float precision.
	 * Keeping it as an integer makes interacting with the FE/RF system easier
	 */
	private int energy;
	/**
	 * The max amount of energy the driveable can have, in units of FE or RF
	 */
	private int maxEnergy;
	
	/**
	 * Whether or not the driveable runs on energy rather than fuel
	 */
	private boolean isElectric;

	public InternalFuelTank(float maxFuel, boolean isElectric) {
		this.isElectric = isElectric;
		this.fuel = 0;
		this.energy = 0;
		if (isElectric) {
			this.maxFuel = 0;
			this.maxEnergy = (int) maxFuel;
		} else {
			this.maxFuel = maxFuel;
			this.maxEnergy = 0;
		}
	}
	
	/**
	 * Add a new compatible fuel fluid into class.
	 * This allows the given fluid to be used as a fuel.
	 * @param newFluid
	 */
	public static void addFluid(Fluid newFluid) {
		InternalFuelTank.acceptedFuels.add(newFluid);
	}
	
	/**
	 * Check to see whether or not the given fluid is a compatible fuel
	 * @param fluid	the possible fuel fluid to check
	 * @return		true if the fluid given is compatible, false otherwise
	 */
	public static boolean isFuel(Fluid fluid) {
		return InternalFuelTank.acceptedFuels.contains(fluid);
	}
	
	/**
	 * Returns whether or not the driveable is electric
	 * @return	true if driveable is electric, false otherwise
	 */
	public boolean checkIfElectric() {
		return isElectric;
	}
	
	/**
	 * Whether or not the fuel tank has any fuel/energy left at all
	 * @return	false if the fuel tank is completely empty, true otherwise
	 */
	public boolean hasFuel() {
		return isElectric ? (energy > 0) : (fuel > 0);
	}
	
	/**
	 * Whether or not the fuel tank has enough fuel/energy for one more tick.
	 * Will return false if there is a bit of fuel/energy left, but not enough for the stated consumption.
	 * Will automatically convert from fuel consumption to energy consumption. 
	 * Call this method as if the driveable is burning fuel.
	 * @param consumption	the amount of fuel/energy the driveable will consume the next tick
	 * @return				true if there is enough fuel/energy for one more tick, false otherwise
	 */
	public boolean hasFuel(float consumption) {
		if (isElectric) {
			consumption *= ENERGY_CONSUMPTION_MULTIPLIER;
			//The Math.ceil() is to be safe and make sure we don't over-consume since energy is stored as an int
			return (energy - Math.ceil(consumption)) >= 0;
		} else {
			return (fuel - consumption) >= 0;
		}
	}
	
	/**
	 * Puts some fuel in the fuel tank. This method uses floating-point numbers.
	 * It is recommended to use the int variant of this method when dealing with Forge fluids.
	 * A negative number will return 0.
	 * @param maxReceive	the max output of the giving container, in millibuckets.
	 * 						i.e. how many millibuckets the fuel can/bucket can give the fuel tank this tick
	 * @param simulate		if true, the fuel will not actually be put into the fuel tank but this method 
	 * 						will still return how many mb the tank would have received.
	 * @return				how many millibuckets of fuel the container has received
	 */
	public float receiveFuel(float maxReceive, boolean simulate) {

		float fuelReceived = 0;
		if (!isElectric) {
			fuelReceived = MathHelper.clamp(Math.min(fuelTransferRate, maxReceive), 0, maxFuel - fuel);
			if (!simulate)
				fuel += fuelReceived;
		}
        return fuelReceived;
	}

	/**
	 * Puts some fuel in the fuel tank. This method uses integers rather than floating point.
	 * It has some extra checks as a result.
	 * This method will refuse to put fuel in the fuel tank if there is less than 1 millibucket of space remaining
	 * to prevent over-filling.
	 * A negative number will return 0.
	 * @param fluidReceive	the max output and fluid type of the giving container, in millibuckets.
	 * 						i.e. how many millibuckets the fuel can/bucket can give the fuel tank this tick
	 * @param simulate		if true, the fuel will not actually be put into the fuel tank but this method 
	 * 						will still return how many mb the tank would have received.
	 * @return				how many millibuckets of fuel the container has received
	 */
	public int receiveFuel(int maxReceive, boolean simulate) {

		int fuelReceived = 0;
		if (!isElectric) {
			fuelReceived = MathHelper.clamp(Math.min((int)fuelTransferRate, maxReceive), 0, (int)(maxFuel - fuel));
			if (!simulate)
				fuel += fuelReceived;
		}
        return fuelReceived;
	}

	/**
	 * Puts some fuel in the fuel tank. This method uses Forge fluids.
	 * This method will refuse to put fuel in the fuel tank if there is less than 1 millibucket of space remaining
	 * to prevent over-filling.
	 * Will refuse to fill tank and will return 0 if fluid is not a recognized fuel
	 * A negative number will return 0.
	 * @param maxReceive	the max output of the giving container, in millibuckets.
	 * 						i.e. how many millibuckets the fuel can/bucket can give the fuel tank this tick
	 * @param simulate		if true, the fuel will not actually be put into the fuel tank but this method 
	 * 						will still return how many mb the tank would have received.
	 * @return				how many millibuckets of fuel the container has received
	 */
	public int receiveFuel(FluidStack fluidReceive, boolean simulate) {

		int fuelReceived = 0;
		if (!isElectric && isFuel(fluidReceive.getFluid())) {
			fuelReceived = MathHelper.clamp(Math.min((int)fuelTransferRate, fluidReceive.amount), 0, (int)(maxFuel - fuel));
			if (!simulate)
				fuel += fuelReceived;
		}
        return fuelReceived;
	}
	
	/**
	 * Sets the driveable to a given percentage instantly, skipping the receive methods.
	 * Useful for testing or spawning a preset vehicle.
	 * Do not use this method for standard refuelling procedure, use the receive methods instead.
	 * @param percentage	what level to fill the driveable to, 0 = empty, 1 = full
	 */
	public void setFillPercentage(float percentage) {
		percentage = MathHelper.clamp(percentage, 0, 1);
		if (isElectric) {
			energy = (int) (maxEnergy * percentage);
		} else {
			fuel = maxFuel * percentage;
		}
	}
	
	/**
	 * Sets the driveable's fuel/energy, skipping the receive methods.
	 * Use this method when reading NBT.
	 * Do not use this method for standard refuelling procedure, use the receive methods instead.
	 * @param amount	how much fuel/energy to fill to, 
	 */
	public void setFillAmount(float amount) {
		if (isElectric) {
			amount = MathHelper.clamp(amount, 0, maxEnergy);
			energy = (int)amount;
		} else {
			amount = MathHelper.clamp(amount, 0, maxFuel);
			fuel = amount;
		}
	}
	
	/**
	 * Gets the amount of fuel/energy currently in the tank.
	 * Will return energy if driveable is electric (i.e. isElectric flag is true)
	 * @return	the amount of fuel/energy in the tank. (mb/RF)
	 */
	public float getFillLevel() {
		if (isElectric) {
			return this.energy;
		} else {
			return this.fuel;
		}
	}
	
	/**
	 * Gets the maximum amount of fuel/energy currently in the tank.
	 * Will return energy if driveable is electric (i.e. isElectric flag is true)
	 * @return	the maximum amount of fuel/energy that can fit in the tank. (mb/RF)
	 */
	public float getMaxFillLevel() {
		if (isElectric) {
			return this.maxEnergy;
		} else {
			return this.maxFuel;
		}
	}
	
	/**
	 * Removes some fuel/energy from storage. Does not check whether or not the instance has any fuel/energy left.
	 * Automatically takes care of conversion from fuel to RF. 
	 * It is recommended to check with the {@link #hasFuel(float)} method first.
	 * @param amountToConsume	how much fuel to burn this tick
	 */
	public void consume(float amountToConsume) {
		if (isElectric) {
			amountToConsume *= ENERGY_CONSUMPTION_MULTIPLIER;
			
			//It is okay to round up and have some occasional losses for RF.
			//Energy storage is typically measured in millions anyway
			energy = Math.max(0, energy - (int)Math.ceil(amountToConsume));
		} else {
			fuel = Math.max(0, fuel - amountToConsume);
		}
	}
	
	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		int energyReceived = 0;
		if (isElectric) {
			energyReceived = MathHelper.clamp(Math.min(energyTransferRate, maxReceive), 0, maxEnergy - energy);
	        if (!simulate)
	            energy += energyReceived;
		}
        return energyReceived;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		return 0;
	}

	@Override
	public int getEnergyStored() {
		return energy;
	}

	@Override
	public int getMaxEnergyStored() {
		return maxEnergy;
	}

	@Override
	public boolean canExtract() {
		return false;
	}

	@Override
	public boolean canReceive() {
        return isElectric;
	}

	
	
}

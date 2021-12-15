package com.flansmod.common.driveables.fuel;

import javax.annotation.Nonnull;

import com.flansmod.common.FlansConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * This extension of InternalFuelTank represents an electric battery.
 * It automatically converts from mb of fuel to RF/FE.
 * 
 * TODO: Give this the Forge Energy Capability
 */
public class VehicleBattery extends InternalFuelTank {

	/**
	 * When the fuel tank is told to consume, this value represents how many RF = 1 mb.
	 */
	final private static int ENERGY_CONSUMPTION_MULTIPLIER = 1200;
	
	/**
	 * Conversion from fuel to RF for storage. 
	 * Electric vehicles will have roughly half the endurance as combustion vehicles.
	 */
	final private static int ENERGY_STORAGE_MULTIPLIER = 600;
	
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
	
	private VehicleBattery(int maxEnergy) {
		this.energy = 0;
		this.maxEnergy = maxEnergy;
	}
	
	/**
	 * Creates a vehicle battery from energy.
	 * @param maxEnergy the maximum amount of energy that can be stored in this battery (RF/FE)
	 * @return a vehicle battery with the its max energy capacity set to {@code maxEnergy}
	 */
	public static VehicleBattery createFromEnergy(int maxEnergy) {
		return new VehicleBattery(maxEnergy);
	}
	
	/**
	 * Creates a vehicle battery by converting from a max fuel value to its equivalent max energy value.
	 * 1 mb of fuel = 1200 RF.
	 * @param maxFuel the maximum equivalent amount of fuel that can be put in this batter (mb)
	 * @return a vehicle batter with its max energy capacity set to the energy equivalent of {@code maxFuel}
	 */
	public static VehicleBattery convertFromFuel(float maxFuel) {
		return new VehicleBattery((int)(maxFuel * ENERGY_STORAGE_MULTIPLIER));
	}
	
	/**
	 * Attempts to put energy into the battery. Limited by the energy transfer rate set in the configs.
	 * @param maxReceive	the amount that can be receieved (RF/FE)
	 * @param simulate		false if energy should be transferred
	 * @return the amount of the energy that was transferred, 
	 * 			or, if {@code simulate} is set to true, the amount that would have been transferred
	 */
	public int receiveEnergy(int maxReceive, boolean simulate) {
		int energyReceived = 0;
		energyReceived = MathHelper.clamp(Math.min(FlansConfig.vehicleEnergyTransferRate, maxReceive), 0, maxEnergy - energy);
        if (!simulate)
            energy += energyReceived;
        return energyReceived;
	}
	
	@Override
	public boolean hasFuel() {
		return this.energy > 0;
	} 
	
	@Override
	public boolean hasFuel(float consumption) {
		consumption *= ENERGY_CONSUMPTION_MULTIPLIER;
		//The Math.ceil() is to be safe and make sure we don't over-consume since energy is stored as an int
		return (energy - Math.ceil(consumption)) >= 0;
	}

	@Override
	public void setFillPercentage(float percentage) {
		percentage = MathHelper.clamp(percentage, 0, 1);
		energy = (int) (maxEnergy * percentage);
	}

	@Override
	public void setFillAmount(float amount) {
		amount = MathHelper.clamp(amount, 0, maxEnergy);
		energy = (int)amount;
	}

	@Override
	public float getFillLevel() {
		return this.energy;
	}

	@Override
	public float getMaxFillLevel() {
		return this.maxEnergy;
	}
	
	@Override
	public void consume(float amountToConsume) {
		amountToConsume *= ENERGY_CONSUMPTION_MULTIPLIER;
		
		//It is okay to round up and have some occasional losses for RF.
		//Energy storage is typically measured in millions anyway
		energy = Math.max(0, energy - (int)Math.ceil(amountToConsume));
	}
	

	@Override
	public ItemStack handleFuelItem(@Nonnull ItemStack stack) {
		
		//Checking for energy to drain
		if (stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
			IEnergyStorage energyItem = stack.getCapability(CapabilityEnergy.ENERGY, null);
			
			if (energyItem.canExtract()) {
				//Check to see how much we can extract
				int amountToDrain = energyItem.extractEnergy(FlansConfig.vehicleEnergyTransferRate, true);
				
				//Transfer to fuel tank
				int amountTransferred = this.receiveEnergy(amountToDrain, false);
				
				//Extract energy from the item
				energyItem.extractEnergy(amountTransferred, false);
			}
		}
		
		//If the ItemStack doesn't have energy, we do nothing with it.
		return stack;
	}

}

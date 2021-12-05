package com.flansmod.common.driveables.fuel;

import javax.annotation.Nonnull;

import com.flansmod.common.FlansConfig;
import com.flansmod.common.parts.EnumPartCategory;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * Keeps track of the fuel/energy within a driveable.
 * Fuel is measured in millibuckets(mb) and energy is measured in RF/FE.
 * One mb of fuel is equated being 1,200 RF. This is using Immersive Petroleum's gasoline as a standard
 * which creates 1,200 RF per bucket in an unmodified Compression Dynamo. 
 * This class could eventually get upgraded to use Capabilities, but that's outside current scope.
 */
public class InternalFuelTank implements IEnergyStorage {
	/**
	 * When the fuel tank is told to consume, this value represents how many RF = 1 mb.
	 */
	final private static int ENERGY_CONSUMPTION_MULTIPLIER = 1200;
	
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
			fuelReceived = MathHelper.clamp(Math.min(FlansConfig.vehicleFuelTransferRate, maxReceive), 0, maxFuel - fuel);
			if (!simulate)
				fuel += fuelReceived;
		}
        return fuelReceived;
	}

	/**
	 * Puts some fuel in the fuel tank. This method uses integers rather than floating point.
	 * This method will refuse to put fuel in the fuel tank if there is less than 1 millibucket of space remaining
	 * to prevent over-filling.
	 * A negative number will return 0.
	 * @param maxReceive	the max output and fluid type of the giving container, in millibuckets.
	 * 						i.e. how many millibuckets the fuel can/bucket can give the fuel tank this tick
	 * @param simulate		if true, the fuel will not actually be put into the fuel tank but this method 
	 * 						will still return how many mb the tank would have received.
	 * @return				how many millibuckets of fuel the container has received
	 */
	public int receiveFuel(int maxReceive, boolean simulate) {

		int fuelReceived = 0;
		if (!isElectric) {
			fuelReceived = MathHelper.clamp(Math.min((int)(FlansConfig.vehicleFuelTransferRate), maxReceive), 0, (int)(maxFuel - fuel));
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
	 * @param fluidReceive	the FluidStack to try and fill the tank with. The amount of the stack should
	 * 						be the max output of the giving container and the Fluid of the stack needs to be
	 * 						an accepted fuel
	 * @param simulate		if true, the fuel will not actually be put into the fuel tank but this method 
	 * 						will still return how many mb the tank would have received.
	 * @return				how many millibuckets of fuel the container has received
	 */
	public int receiveFuel(FluidStack fluidReceive, boolean simulate) {

		int fuelReceived = 0;
		if (!isElectric && FlansConfig.isFuel(fluidReceive.getFluid())) {
			fuelReceived = MathHelper.clamp(Math.min((int)(FlansConfig.vehicleFuelTransferRate), fluidReceive.amount), 0, (int)(maxFuel - fuel));
			if (!simulate)
				fuel += fuelReceived;
		}
        return fuelReceived;
	}
	
	/**
	 * Puts some fuel in the fuel tank. This method uses Forge's buckets.
	 * Method will drain the full 1000mb regardless of the fuel transfer rate.
	 * Will refuse to fill tank and will return 0 if fluid is not a recognized fuel.
	 * @param fluidReceive	the Fluid that is currently in the bucket, needs to be an accepted fuel
	 * @param simulate		if true, the fuel will not actually be put into the fuel tank but this method 
	 * 						will still return how many mb the tank would have received.
	 * @return				how many millibuckets of fuel the container has received
	 */
	public int receiveFuelBucket(Fluid fluidReceive, boolean simulate) {

		if (!isElectric && FlansConfig.isFuel(fluidReceive) && ((maxFuel - fuel) >= Fluid.BUCKET_VOLUME)) {
			if (!simulate)
				fuel += Fluid.BUCKET_VOLUME;
			
			return Fluid.BUCKET_VOLUME;
		} else {
			return 0;
		}
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
	 * Sets the driveable's fuel/energy, skipping the receive methods and ignoring fuel transfer limits.
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
			energyReceived = MathHelper.clamp(Math.min(FlansConfig.vehicleEnergyTransferRate, maxReceive), 0, maxEnergy - energy);
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

	/**
	 * Used to determine if this storage can receive energy.
	 * If this is false, then any calls to receiveEnergy will return 0.
	 * This is method implemented from IEnergyStorage and only tests energy.
	 * For most purposes, use {@link #hasFuel()}
	 */
	@Override
	public boolean canReceive() {
        return isElectric;
	}

	
	/**
	 * Attempts to drain
	 * @param stack
	 * @return
	 */
	public ItemStack handleFuelItem(@Nonnull ItemStack stack) {
		Item item = stack.getItem();
		
		if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null) && !this.isElectric) {
			IFluidHandlerItem fuelItem = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
			
			IFluidTankProperties[] fluidTanksToDrain = fuelItem.getTankProperties();
			
			for (IFluidTankProperties fluidTank : fluidTanksToDrain) {
				//Checking to make sure container isn't empty and the fuel inside is actually a fuel
				FluidStack fuelStack = fluidTank.getContents();
				if (fuelStack != null && FlansConfig.isFuel(fuelStack.getFluid())) {
					Fluid fluidToDrain = fuelStack.getFluid();
					// Forge fluid buckets
					// Buckets must transfer the full 1000 mb in one tick, so they have special handling
					if (item instanceof UniversalBucket) {
						//Checking to see if the fuel tank can hold 1000mb more
						if (this.receiveFuelBucket(fluidToDrain, true) == Fluid.BUCKET_VOLUME) {
							//Fill the tank
							this.receiveFuelBucket(fluidToDrain, false);
							
							//Drain the bucket stack
							stack.shrink(1);
							
							//Returning empty bucket
							if (stack.isEmpty()) {
								stack = new ItemStack(item);
							}
						}
						
					//Normal fluid containers
					} else {
						//Check to see how much we can drain first
						FluidStack amountToDrain = fuelItem.drain(fuelStack, false);
						
						//Filling the internal tank
						int amountTransferred = this.receiveFuel(amountToDrain, false);
						
						//Draining the item
						fuelItem.drain(new FluidStack(fluidToDrain, amountTransferred), true);
					}
					break; //Only drain one container at a time. Once we find one, we'll move on to next tick.
				}
			}
			
		}
		
		//Checking for energy to drain
		if (stack.hasCapability(CapabilityEnergy.ENERGY, null) && this.isElectric) {
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
		
		/**
		 * This bit looks for Flan's Mod fuel items
		 * This is legacy and it is recommended that Forge Fluid tanks are used instead.
		 */
		if(item instanceof ItemPart && !this.isElectric)
		{
			PartType part = ((ItemPart)item).type;
			// Check it is a fuel item
			if(part.category == EnumPartCategory.FUEL)
			{
				//The max amount the fuel can can transfer is its full capacity
				int maxTransferAmount = stack.getMaxDamage() - stack.getItemDamage();
				
				int amountTransferred = this.receiveFuel(maxTransferAmount, false);
				
				// Damage the fuel item to indicate being used up
				//TODO: Refactor this
				stack.setItemDamage(stack.getItemDamage() + amountTransferred);
				if (stack.getItemDamage() >= stack.getMaxDamage()) {
					stack.setCount(stack.getCount() - 1);
					
					if (stack.getCount() <= 0) {
						stack = ItemStack.EMPTY;
					}
				}
				//TODO: Test if the fuel cans get used up properly
			}
			
		//If the ItemStack doesn't have energy or fuel, we do nothing with it.
		}
		
		return stack;
	} 
}

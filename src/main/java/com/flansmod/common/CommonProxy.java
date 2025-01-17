package com.flansmod.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.flansmod.common.driveables.ContainerDriveableInventory;
import com.flansmod.common.driveables.ContainerDriveableMenu;
import com.flansmod.common.driveables.DriveablePart;
import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.driveables.mechas.ContainerMechaInventory;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.guns.ContainerGunModTable;
import com.flansmod.common.guns.boxes.ContainerGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.network.PacketBreakSound;
import com.flansmod.common.paintjob.ContainerPaintjobTable;
import com.flansmod.common.paintjob.TileEntityPaintjobTable;
import com.flansmod.common.parts.EnumPartCategory;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.teams.ArmourBoxType;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.InfoType;

public class CommonProxy
{
	protected static Pattern zipJar = Pattern.compile("(.+)\\.(zip|jar)$");
	
	public void LoadAssetsFromFlanFolder()
	{
		// No-op, client only
	}
			
	public void addMissingJSONs(HashMap<Integer, InfoType> types)
	{
		
	}
	
	/**
	 * A ton of client only methods follow
	 */
	public void preInit()
	{
	}
	
	public void init()
	{
	}
	
	public void forceReload()
	{
	}
	
	public void registerRenderers()
	{
	}
	
	public void doTutorialStuff(EntityPlayer player, EntityDriveable entityType)
	{
	}
	
	public void changeControlMode(EntityPlayer player)
	{
	}
	
	public boolean mouseControlEnabled()
	{
		return false;
	}
	
	public void openDriveableMenu(EntityPlayer player, World world, EntityDriveable driveable)
	{
	}
	
	public <T> T loadModel(String s, String shortName, Class<T> typeClass)
	{
		return null;
	}
	
	public void loadSound(String contentPack, String type, String sound)
	{
	}
	
	public boolean isThePlayer(EntityPlayer player)
	{
		return false;
	}
	
	public void buyGun(GunBoxType type, InfoType gun)
	{
	}
	
	/**
	 * Gets the client GUI element from ClientProxy
	 */
	public Object getClientGui(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		return null;
	}
	
	/**
	 * Gets the container for the specified GUI
	 */
	public Container getServerGui(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		switch(ID)
		{
			case 0: return null; //Driveable crafting. No server side
			case 1: return null; //Driveable repair. No server side
			case 2: return new ContainerGunModTable(player.inventory, world);
			case 3: return new ContainerDriveableMenu(player.inventory, world);
			case 4: return new ContainerDriveableMenu(player.inventory, world, true, ((EntitySeat)player.getRidingEntity()).driveable);
			case 5: return new ContainerGunBox(player.inventory);
			//Plane inventory screens
			case 6: return new ContainerDriveableInventory(player.inventory, world, ((EntitySeat)player.getRidingEntity()).driveable, 0);
			case 7: return new ContainerDriveableInventory(player.inventory, world, ((EntitySeat)player.getRidingEntity()).driveable, 1);
			case 8: return new ContainerDriveableMenu(player.inventory, world, true, ((EntitySeat)player.getRidingEntity()).driveable);
			case 9: return new ContainerDriveableInventory(player.inventory, world, ((EntitySeat)player.getRidingEntity()).driveable, 2);
			case 10: return new ContainerMechaInventory(player.inventory, world, (EntityMecha)((EntitySeat)player.getRidingEntity()).driveable);
			case 11: return null; //Armour box. No server side
			case 12: return new ContainerDriveableInventory(player.inventory, world, ((EntitySeat)player.getRidingEntity()).driveable, 3);
			case 13: return new ContainerPaintjobTable(player.inventory, world, (TileEntityPaintjobTable)world.getTileEntity(new BlockPos(x, y, z)));
		}
		return null;
	}
	
	/**
	 * Play a block break sound here
	 */
	public void playBlockBreakSound(int x, int y, int z, Block blockHit)
	{
		FlansMod.packetHandler.sendToAll(new PacketBreakSound(x, y, z, blockHit));
	}
	
	public void craftDriveable(EntityPlayer player, DriveableType type)
	{
		//Create a temporary copy of the player inventory for backup purposes
		InventoryPlayer temporaryInventory = new InventoryPlayer(null);
		temporaryInventory.copyInventory(player.inventory);
		
		//This becomes false if some recipe element is not found on the player
		boolean canCraft = true;
		
		//Making sure the crafting recipe is not disabled
		canCraft = !FlansMod.disabledVehicles.contains(type.shortName);
		
		//Iterate over rows then columns
		for(ItemStack recipeStack : type.driveableRecipe)
		{
			//The total amount of items found that match this recipe stack
			int totalAmountFound = 0;
			//Iterate over the player's inventory
			for(int n = 0; n < player.inventory.getSizeInventory(); n++)
			{
				//Get the stack in each slot
				ItemStack stackInSlot = player.inventory.getStackInSlot(n).copy();
				//If the stack is what we want
				if(stackInSlot != null && stackInSlot.getItem() == recipeStack.getItem() && stackInSlot.getItemDamage() == recipeStack.getItemDamage())
				{
					//Work out the amount to take from the stack
					int amountFound = Math.min(stackInSlot.getCount(), recipeStack.getCount() - totalAmountFound);
					//Take it
					stackInSlot.setCount(stackInSlot.getCount() - amountFound);
					//Check for empty stacks
					if(stackInSlot.getCount() <= 0)
						stackInSlot = ItemStack.EMPTY.copy();
					//Put the modified stack back in the inventory
					player.inventory.setInventorySlotContents(n, stackInSlot);
					//Increase the amount found counter
					totalAmountFound += amountFound;
					//If we have enough, stop looking
					if(totalAmountFound == recipeStack.getCount())
						break;
				}
			}
			//If we didn't find enough, give the stack a red outline
			if(totalAmountFound < recipeStack.getCount())
			{
				//For some reason, the player sent a craft packet, despite being unable to
				canCraft = false;
				break;
			}
		}
		
		//Some item was missing. Restore inventory and return
		if(!canCraft)
		{
			player.inventory.copyInventory(temporaryInventory);
			return;
		}
		
		//Now we no longer need the temporary inventory backup, so we will use it to find the best stack of engines		
		//Collect up all the engines into neat and tidy stacks so we can find if any of them are big enough and which of those stacks are best
		HashMap<PartType, ItemStack> engines = new HashMap<>();
		
		//Find some suitable engines
		for(int n = 0; n < temporaryInventory.getSizeInventory(); n++)
		{
			//Get the stack in each slot
			ItemStack stackInSlot = temporaryInventory.getStackInSlot(n);
			//Check to see if its a part
			if(stackInSlot != null && stackInSlot.getItem() instanceof ItemPart)
			{
				PartType partType = ((ItemPart)stackInSlot.getItem()).type;
				//Check its an engine
				if(partType.category == EnumPartCategory.ENGINE && partType.worksWith.contains(EnumType.getFromObject(type)))
				{
					//If we already have engines of this type, add these ones to the stack
					if(engines.containsKey(partType))
					{
						engines.get(partType).setCount(engines.get(partType).getCount() + stackInSlot.getCount());
					}
					//Else, make this the first stack
					else engines.put(partType, stackInSlot);
				}
			}
		}
		
		//Find the stack of engines that is fastest but which also has enough for this driveable
		float bestEngineSpeed = -1F;
		ItemStack bestEngineStack = ItemStack.EMPTY.copy();
		for(PartType part : engines.keySet())
		{
			//If this engine outperforms the currently selected best one and there are enough of them, swap
			if(part.engineSpeed > bestEngineSpeed && engines.get(part).getCount() >= type.numEngines())
			{
				bestEngineSpeed = part.engineSpeed;
				bestEngineStack = engines.get(part).copy();
			}
		}
		
		//If the player doesn't have any suitable engines, return
		if(bestEngineStack == null || bestEngineStack.isEmpty())
		{
			player.inventory.copyInventory(temporaryInventory);
			return;
		}
		
		//Remove the engines from the inventory
		int numEnginesAcquired = 0;
		for(int n = 0; n < player.inventory.getSizeInventory(); n++)
		{
			//Get the stack in each slot
			ItemStack stackInSlot = player.inventory.getStackInSlot(n);
			//Check to see if its the engine we want
			if(stackInSlot != null && !stackInSlot.isEmpty() && stackInSlot.getItem() == bestEngineStack.getItem())
			{
				//Work out the amount to take from the stack
				int amountFound = Math.min(stackInSlot.getCount(), type.numEngines() - numEnginesAcquired);
				//Take it
				stackInSlot.setCount(stackInSlot.getCount() - amountFound);
				//Check for empty stacks
				if(stackInSlot.getCount() <= 0)
					stackInSlot = ItemStack.EMPTY.copy();
				//Put the modified stack back in the inventory
				player.inventory.setInventorySlotContents(n, stackInSlot);
				//Increase the amount found counter
				numEnginesAcquired += amountFound;
				//If we have enough, stop looking
				if(numEnginesAcquired == type.numEngines())
					break;
			}
		}
		
		//Give them their brand new shiny driveable item :D
		ItemStack driveableStack = new ItemStack(type.item);
		NBTTagCompound tags = new NBTTagCompound();
		tags.setString("Engine", ((ItemPart)bestEngineStack.getItem()).type.shortName);
		tags.setString("Type", type.shortName);
		for(EnumDriveablePart part : EnumDriveablePart.values())
		{
			tags.setInteger(part.getShortName() + "_Health", type.health.get(part) == null ? 0 : type.health.get(part).health);
			tags.setBoolean(part.getShortName() + "_Fire", false);
		}
		driveableStack.setTagCompound(tags);
		if(!player.inventory.addItemStackToInventory(driveableStack))
			player.dropItem(driveableStack, false);
	}
	
	public void repairDriveable(EntityPlayer driver, EntityDriveable driving, DriveablePart part)
	{
		//If any of this parts parent parts are broken, then it cannot be repaired
		for(EnumDriveablePart parent : part.type.getParents())
		{
			if(!driving.isPartIntact(parent))
				return;
		}
		
		//Create a temporary copy of the player inventory for backup purposes
		InventoryPlayer temporaryInventory = new InventoryPlayer(null);
		temporaryInventory.copyInventory(driver.inventory);
		
		//This becomes false if some recipe element is not found on the player
		boolean canRepair = true;
		
		//Get the array of stacks needed
		ArrayList<ItemStack> stacksNeeded = driving.getDriveableType().getItemsRequired(part, driving.getDriveableData().engine);
		//Draw the stacks that should be in each slot
		for(ItemStack stackNeeded : stacksNeeded)
		{
			//The total amount of items found that match this recipe stack
			int totalAmountFound = 0;
			//Iterate over the temporary inventory
			for(int m = 0; m < temporaryInventory.getSizeInventory(); m++)
			{
				//Get the stack in each slot
				ItemStack stackInSlot = temporaryInventory.getStackInSlot(m).copy();
				//If the stack is what we want
				if(stackInSlot.getItem() == stackNeeded.getItem() && stackInSlot.getItemDamage() == stackNeeded.getItemDamage())
				{
					//Work out the amount to take from the stack
					int amountFound = Math.min(stackInSlot.getCount(), stackNeeded.getCount() - totalAmountFound);
					//Take it
					stackInSlot.setCount(stackInSlot.getCount() - amountFound);
					//Check for empty stacks
					if(stackInSlot.getCount() <= 0)
						stackInSlot = ItemStack.EMPTY.copy();
					//Put the modified stack back in the inventory
					temporaryInventory.setInventorySlotContents(m, stackInSlot);
					//Increase the amount found counter
					totalAmountFound += amountFound;
					//If we have enough, stop looking
					if(totalAmountFound == stackNeeded.getCount())
						break;
				}
			}
			if(totalAmountFound < stackNeeded.getCount())
				canRepair = false;
		}
		
		if(canRepair)
		{
			driver.inventory.copyInventory(temporaryInventory);
			part.health = Math.max(1, part.maxHealth / 10);
			part.onFire = false;
			part.dead = false;
			driving.checkParts();
		}
	}
	
	public boolean isScreenOpen()
	{
		return false;
	}
	
	public boolean isKeyDown(int key)
	{
		return false;
	}
	
	public boolean keyDown(int keycode)
	{
		return false;
	}
	
	public void buyArmour(String shortName, int piece, ArmourBoxType type)
	{
	
	}
}

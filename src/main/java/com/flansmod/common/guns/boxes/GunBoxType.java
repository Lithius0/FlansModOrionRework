package com.flansmod.common.guns.boxes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistry;

import com.flansmod.common.FlansMod;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

public class GunBoxType extends BoxType
{
	public BlockGunBox block;
	
	/**
	 * Stores pages for the gun box indexed by their title (unlocalized!)
	 */
	public HashMap<String, GunBoxPage> pagesByTitle;
	public ArrayList<GunBoxPage> pages;
	
	/**
	 * Points to the page we are currently adding to.
	 */
	private GunBoxPage currentPage;
	
	public GunBoxPage defaultPage;
	
	private static int lastIconIndex = 2;
	public static HashMap<String, GunBoxType> gunBoxMap = new HashMap<>();
	
	public GunBoxType(TypeFile file)
	{
		super(file);
		
		pagesByTitle = new HashMap<>();
		pages = new ArrayList<>();
	}
	
	@Override
	public void preRead(TypeFile file)
	{
		super.preRead(file);
		//Make sure NumGuns is read before anything else
		for(String line : file.getLines())
		{
			String[] split = line.split(" ");
			if(split.length < 2)
				continue;
			
			if(split[0].equals("NumGuns"))
			{
				pagesByTitle.put("default", currentPage = new GunBoxPage("default"));
				pages.add(currentPage);
			}
		}
	}
	
	@Override
	public void postRead(TypeFile file)
	{
		super.postRead(file);
		gunBoxMap.put(this.shortName, this);
	}

	@Override
	protected void read(String[] split, TypeFile file)
	{
		super.read(split, file);
		try
		{
			//Sets the current page of the reader.
			if(split[0].equals("SetPage"))
			{
				String pageName = split[1];
				for(int i = 2; i < split.length; i++)
					pageName += " " + split[i];
				if(pagesByTitle.get(pageName) == null)
					pagesByTitle.put(pageName, currentPage = new GunBoxPage(pageName));
				pages.add(currentPage);
				
			}
			//Add an info type at the top level.
			else if(split[0].equals("AddGun") || split[0].equals("AddType"))
			{
				currentPage.addNewEntry(InfoType.getType(split[1]), getRecipe(split));
			}
			//Add a subtype (such as ammo) to the current top level InfoType
			else if(split[0].equals("AddAmmo") || split[0].equals("AddAltType") || split[0].equals("AddAltAmmo") || split[0].equals("AddAlternateAmmo"))
			{
				currentPage.addAmmoToCurrentEntry(InfoType.getType(split[1]), getRecipe(split));
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("Reading gun box file failed : " + shortName);
			FlansMod.log.throwing(e);
		}
	}
	
	@Override
	public void registerItem(IForgeRegistry<Item> registry)
	{
		item = new ItemBlock(block).setRegistryName(shortName + "_item");
		registry.register(item);
	}
	
	@Override
	public void registerBlock(IForgeRegistry<Block> registry)
	{
		registry.register(block);
	}

	/**
	 * Turns the split line of text into a raw recipe for parsing later
	 * @param split	a line in the box TypeFile
	 * @return a Map mapping a required item name and the required item amount
	 */
	private Map<String, Integer> getRecipe(String[] split)
	{
		Map<String, Integer> recipe = new HashMap<String, Integer>();
		
		//Start at 2 to skip AddAmmo/AddGun and shortname
		for(int i = 2; i < split.length; i += 2)
		{
			//In the files, amount comes first, then item
			recipe.put(split[i + 1], Integer.parseInt(split[i]));
		}
		
		return recipe;
	}

	public static GunBoxType getBox(String s)
	{
		return gunBoxMap.get(s);
	}
	
	public static GunBoxType getBox(Block block)
	{
		for(GunBoxType type : gunBoxMap.values())
		{
			if(type.block == block)
				return type;
		}
		return null;
	}
	
	/**
	 * Goes through every entry in this box and parses their raw data into actual recipes.
	 */
	public void parseAllPages() {
		for (GunBoxPage page : pages) {
			for (GunBoxEntry entry : page.entries) {
				entry.parse();
			}
		}
	}

	/**
	 * Goes through every entry in every box and parses their raw data into actual recipes.
	 * Call this when other recipes are being registered.
	 */
	public static void parseAllBoxes() {
		for (Map.Entry<String, GunBoxType> box : gunBoxMap.entrySet()) {
			box.getValue().parseAllPages();
		}
	}
	
	/**
	 * Represents a page in the gun box
	 */
	public static class GunBoxPage
	{
		public List<GunBoxEntryTopLevel> entries;
		/**
		 * Points to the gun box entry we are currently reading from file. Allows for the old format to write in ammo on a separate line to the gun.
		 */
		private GunBoxEntryTopLevel currentlyEditing;
		public String name;
		
		public GunBoxPage(String s)
		{
			name = s;
			entries = new ArrayList<>();
		}
		
		public void addNewEntry(InfoType type, Map<String, Integer> recipe)
		{
			GunBoxEntryTopLevel entry = new GunBoxEntryTopLevel(type, recipe);
			entries.add(entry);
			currentlyEditing = entry;
		}
		
		public void addAmmoToCurrentEntry(InfoType type, Map<String, Integer> recipe)
		{
			currentlyEditing.addAmmo(type, recipe);
		}
	}
	
	/**
	 * Represents an entry on a page of the gun box.
	 */
	public static class GunBoxEntry
	{
		/** The InfoType of the item we're crafting */
		public InfoType type;
		/** The unparsed data of the crafting recipe. */
		private Map<String, Integer> rawData;
		/** A list of all the requirements to craft the item. */
		public List<ItemStack> requiredParts;
		
		public GunBoxEntry(InfoType type, Map<String, Integer> recipe)
		{
			this.type = type;
			this.rawData = recipe;
			this.requiredParts = new ArrayList<ItemStack>();
		}
		
		public boolean haveEnoughOf(InventoryPlayer inv, ItemStack stackNeeded)
		{
			//Create a temporary copy of the player inventory for backup purposes
			InventoryPlayer temporaryInventory = new InventoryPlayer(null);
			for(int i = 0; i < inv.getSizeInventory(); i++)
			{
				temporaryInventory.setInventorySlotContents(i, inv.getStackInSlot(i).copy());
			}
			
			return haveEnoughOf(temporaryInventory, stackNeeded, false);
		}
		
		private boolean haveEnoughOf(InventoryPlayer temporaryInventory, ItemStack stackNeeded, boolean takeItems)
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
			return totalAmountFound >= stackNeeded.getCount();
		}
		
		public boolean canCraft(InventoryPlayer inv, boolean takeItems)
		{
			//Create a temporary copy of the player inventory for backup purposes
			InventoryPlayer temporaryInventory = new InventoryPlayer(null);
			for(int i = 0; i < inv.getSizeInventory(); i++)
			{
				temporaryInventory.setInventorySlotContents(i, inv.getStackInSlot(i).copy());
			}

			//This becomes false if some recipe element is not found on the player
			boolean canCraft = true;
			
			//Draw the stacks that should be in each slot
			for(ItemStack stackNeeded : requiredParts)
			{
				if(!haveEnoughOf(temporaryInventory, stackNeeded, takeItems))
				{
					canCraft = false;
					break;
				}
			}
			
			if(canCraft && takeItems)
			{
				inv.copyInventory(temporaryInventory);
			}
			
			return canCraft;
		}
		
		/**
		 * Parses the raw data into an ItemStack for easy checking. 
		 * Can be called while game is ongoing to refresh recipe.
		 */
		public void parse() {
			for (Map.Entry<String, Integer> recipeItem : this.rawData.entrySet()) {
				if (recipeItem.getKey().contains(".")) {
					String[] splitName = recipeItem.getKey().split("\\.");
					requiredParts.add(getRecipeElement(splitName[0], recipeItem.getValue(), Integer.valueOf(splitName[1])));
				} else {
					requiredParts.add(getRecipeElement(recipeItem.getKey(), recipeItem.getValue(), 0));
				}
			}
		}
		
		/**
		 * Sets the raw crafting data of this entry to newRawData.
		 * The data given can be turned into the actual crafting recipe by called {@link #parse()}
		 * @param newRawData a map of crafting data. The String is the name of the ingredient, the integer is the amount
		 */
		public void setRawData(Map<String, Integer> newRawData) {
			this.rawData = newRawData;
		}
	}
	
	/**
	 * Represents a top level entry, normally a gun. This has child entries, normally ammo that are listed below in the GUI.
	 */
	public static class GunBoxEntryTopLevel extends GunBoxEntry
	{
		public List<GunBoxEntry> childEntries;
		
		public GunBoxEntryTopLevel(InfoType type, Map<String, Integer> recipe)
		{
			super(type, recipe);
			childEntries = new ArrayList<>();
		}

		public void addAmmo(InfoType type, Map<String, Integer> recipe)
		{
			childEntries.add(new GunBoxEntry(type, recipe));
		}
	}

	public GunBoxEntry canCraft(InfoType type)
	{
		for(GunBoxPage page : pagesByTitle.values())
		{
			for(GunBoxEntryTopLevel entry : page.entries)
			{
				if(entry.type == type)
					return entry;
				else
				{
					for(GunBoxEntry child : entry.childEntries)
					{
						if(child.type == type)
							return child;
					}
				}
			}
		}
		return null;
	}
}

package com.flansmod.common.teams;

import java.util.ArrayList;
import java.util.List;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

import net.minecraft.client.model.ModelBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PlayerClass extends InfoType implements IPlayerClass
{
	public static List<PlayerClass> classes = new ArrayList<>();
	
	public List<String[]> startingItemStrings = new ArrayList<>();
	public List<ItemStack> startingItems = new ArrayList<>();
	public boolean horse = false;
	
	/**
	 * Override armour. If this is set, then it will override the team armour
	 */
	public ItemStack hat, chest, legs, shoes;
	
	@Override
	public List<ItemStack> GetStartingItems()
	{
		return startingItems;
	}
	
	@Override
	public boolean GetHorse()
	{
		return horse;
	}
	
	@Override
	public ItemStack GetHat()
	{
		return hat;
	}
	
	@Override
	public ItemStack GetChest()
	{
		return chest;
	}
	
	@Override
	public ItemStack GetLegs()
	{
		return legs;
	}
	
	@Override
	public ItemStack GetShoes()
	{
		return shoes;
	}
	
	@Override
	public String GetName()
	{
		return name;
	}
	
	@Override
	public String GetShortName()
	{
		return name;
	}
	
	public PlayerClass(TypeFile file)
	{
		super(file);
		classes.add(this);
	}
	
	@Override
	protected void read(String[] split, TypeFile file)
	{
		super.read(split, file);
		if(split[0].equals("AddItem"))
		{
			startingItemStrings.add(split);
		}
		if(split[0].equals("SkinOverride"))
			texture = split[1];
		if(split[0].equals("Hat") || split[0].equals("Helmet"))
		{
			if(split[1].equals("None"))
				return;
			for(Item item : FlansMod.armourItems)
			{
				ArmourType armour = ((ItemTeamArmour)item).type;
				if(armour != null && armour.shortName.equals(split[1]))
					hat = new ItemStack(item);
			}
		}
		if(split[0].equals("Chest") || split[0].equals("Top"))
		{
			if(split[1].equals("None"))
				return;
			for(Item item : FlansMod.armourItems)
			{
				ArmourType armour = ((ItemTeamArmour)item).type;
				if(armour != null && armour.shortName.equals(split[1]))
					chest = new ItemStack(item);
			}
		}
		if(split[0].equals("Legs") || split[0].equals("Bottom"))
		{
			if(split[1].equals("None"))
				return;
			for(Item item : FlansMod.armourItems)
			{
				ArmourType armour = ((ItemTeamArmour)item).type;
				if(armour != null && armour.shortName.equals(split[1]))
					legs = new ItemStack(item);
			}
		}
		if(split[0].equals("Shoes") || split[0].equals("Boots"))
		{
			if(split[1].equals("None"))
				return;
			for(Item item : FlansMod.armourItems)
			{
				ArmourType armour = ((ItemTeamArmour)item).type;
				if(armour != null && armour.shortName.equals(split[1]))
					shoes = new ItemStack(item);
			}
		}
	}
	
	/**
	 * This loads the items once for clients connecting to remote servers, since the clients can't tell what attachments a gun has in the GUI and they need to load it at least once
	 */
	@Override
	protected void postRead(TypeFile file)
	{
		//onWorldLoad(null);
		
		try
		{
			
			startingItems.clear();
			for(String[] split : startingItemStrings)
			{
				Item matchingItem = null;
				int amount = 1;
				int damage = 0;
				String[] itemNames = split[1].split("\\+");
				for(Object object : Item.REGISTRY)
				{
					Item item = (Item)object;
					if(item != null && (item.getTranslationKey().equals(itemNames[0]) || item.getTranslationKey().split("\\.").length > 1 && item.getTranslationKey().split("\\.")[1].equals(itemNames[0])))
						matchingItem = item;
				}
				for(InfoType type : InfoType.infoTypes.values())
				{
					if(type.shortName.equals(itemNames[0]) && type.item != null)
						matchingItem = type.item;
				}
				if(matchingItem == null)
				{
					FlansMod.log.warn("Tried to add " + split[1] + " to player class " + shortName + " but the item did not exist");
					return;
				}
				if(split.length > 2)
				{
					amount = Integer.parseInt(split[2]);
				}
				if(split.length > 3)
				{
					damage = Integer.parseInt(split[3]);
				}
				ItemStack stack = new ItemStack(matchingItem, amount, damage);
				if(itemNames.length > 1 && matchingItem instanceof ItemGun)
				{
					GunType gunType = ((ItemGun)matchingItem).GetType();
					NBTTagCompound tags = new NBTTagCompound();
					NBTTagCompound attachmentTags = new NBTTagCompound();
					int genericID = 0;
					for(int i = 0; i < itemNames.length - 1; i++)
					{
						AttachmentType attachment = AttachmentType.getAttachment(itemNames[i + 1]);
						if(attachment != null)
						{
							String tagName = null;
							switch(attachment.type)
							{
								case sights: tagName = "scope";
									break;
								case barrel: tagName = "barrel";
									break;
								case stock: tagName = "stock";
									break;
								case grip: tagName = "grip";
									break;
								case generic: tagName = "generic_" + genericID++;
									break;
							}
							NBTTagCompound specificAttachmentTags = new NBTTagCompound();
							new ItemStack(attachment.item).writeToNBT(specificAttachmentTags);
							attachmentTags.setTag(tagName, specificAttachmentTags);
						}
						//Maybe it was a paintjob
						else
						{
							Paintjob paintjob = gunType.getPaintjob(itemNames[i + 1]);
							if(paintjob != null)
								tags.setString("Paint", paintjob.iconName);
						}
					}
					tags.setTag("attachments", attachmentTags);
					stack.setTagCompound(tags);
				}
				startingItems.add(stack);
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("Interpreting player class file failed.");
			FlansMod.log.throwing(e);
		}
	}
	
	/**
	 * In the loading phase item IDs are all up in the air and so too are NBT tags regarding ItemStacks
	 * So to avoid guns with attachments having their attachments replaced with incorrect ones,
	 * random guns and other silly things, we read the relevant lines here, as the world loads
	 */
	/*
	 * Edit 29/01/2021 - I think this may be a legacy concern. ItemStack now references an Item, not an item ID. Moving back to the PostRead step
	@Override
	public void onWorldLoad(World world)
	{
		if(world != null && world.isRemote)
			return;
		try
		{
			
			startingItems.clear();
			for(String[] split : startingItemStrings)
			{
				Item matchingItem = null;
				int amount = 1;
				int damage = 0;
				String[] itemNames = split[1].split("\\+");
				for(Object object : Item.REGISTRY)
				{
					Item item = (Item)object;
					if(item != null && (item.getTranslationKey().equals(itemNames[0]) || item.getTranslationKey().split("\\.").length > 1 && item.getTranslationKey().split("\\.")[1].equals(itemNames[0])))
						matchingItem = item;
				}
				for(InfoType type : InfoType.infoTypes.values())
				{
					if(type.shortName.equals(itemNames[0]) && type.item != null)
						matchingItem = type.item;
				}
				if(matchingItem == null)
				{
					FlansMod.log.warn("Tried to add " + split[1] + " to player class " + shortName + " but the item did not exist");
					return;
				}
				if(split.length > 2)
				{
					amount = Integer.parseInt(split[2]);
				}
				if(split.length > 3)
				{
					damage = Integer.parseInt(split[3]);
				}
				ItemStack stack = new ItemStack(matchingItem, amount, damage);
				if(itemNames.length > 1 && matchingItem instanceof ItemGun)
				{
					GunType gunType = ((ItemGun)matchingItem).GetType();
					NBTTagCompound tags = new NBTTagCompound();
					NBTTagCompound attachmentTags = new NBTTagCompound();
					int genericID = 0;
					for(int i = 0; i < itemNames.length - 1; i++)
					{
						AttachmentType attachment = AttachmentType.getAttachment(itemNames[i + 1]);
						if(attachment != null)
						{
							String tagName = null;
							switch(attachment.type)
							{
								case sights: tagName = "scope";
									break;
								case barrel: tagName = "barrel";
									break;
								case stock: tagName = "stock";
									break;
								case grip: tagName = "grip";
									break;
								case generic: tagName = "generic_" + genericID++;
									break;
							}
							NBTTagCompound specificAttachmentTags = new NBTTagCompound();
							new ItemStack(attachment.item).writeToNBT(specificAttachmentTags);
							attachmentTags.setTag(tagName, specificAttachmentTags);
						}
						//Maybe it was a paintjob
						else
						{
							Paintjob paintjob = gunType.getPaintjob(itemNames[i + 1]);
							if(paintjob != null)
								tags.setString("Paint", paintjob.iconName);
						}
					}
					tags.setTag("attachments", attachmentTags);
					stack.setTagCompound(tags);
				}
				startingItems.add(stack);
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("Interpreting player class file failed.");
			FlansMod.log.throwing(e);
		}
	}
	*/
	
	public static PlayerClass getClass(String s)
	{
		for(PlayerClass playerClass : classes)
		{
			if(playerClass.shortName.equals(s))
				return playerClass;
		}
		return null;
	}
	
	@Override
	protected void preRead(TypeFile file)
	{
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public ModelBase GetModel()
	{
		return null;
	}
}

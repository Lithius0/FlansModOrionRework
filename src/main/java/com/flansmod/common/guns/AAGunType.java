package com.flansmod.common.guns;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.ModelBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.flansmod.client.model.ModelAAGun;
import com.flansmod.common.FlansMod;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

public class AAGunType extends InfoType
{
	/**
	 * The ammo types used by this gun
	 */
	public List<BulletType> ammo = new ArrayList<>();
	public int reloadTime;
	public int recoil = 5;
	public int accuracy;
	public int damage;
	public int shootDelay;
	public int numBarrels;
	public boolean fireAlternately;
	public int health;
	public int gunnerX, gunnerY, gunnerZ;
	public String shootSound;
	public String reloadSound;
	public ModelAAGun model;
	public float topViewLimit = 75F;
	public float bottomViewLimit = 0F;
	public int[] barrelX, barrelY, barrelZ;
	/**
	 * Sentry mode. If target players is true then it either targets everyone on the other team, or everyone other than the owner when not playing with teams
	 */
	public boolean targetMobs = false, targetPlayers = false, targetVehicles = false, targetPlanes = false, targetMechas = false;
	/**
	 * Targeting radius
	 */
	public float targetRange = 10F;
	/**
	 * If true, then all barrels share the same ammo slot
	 */
	public boolean shareAmmo = false;
	
	public static List<AAGunType> infoTypes = new ArrayList<>();
	
	public AAGunType(TypeFile file)
	{
		super(file);
		infoTypes.add(this);
	}
	
	@Override
	protected void read(String[] split, TypeFile file)
	{
		super.read(split, file);
		try
		{
			if(FMLCommonHandler.instance().getSide().isClient() && split[0].equals("Model"))
			{
				model = FlansMod.proxy.loadModel(split[1], shortName, ModelAAGun.class);
			}
			
			damage = readForInt(split, "Damage", damage);
			reloadTime = readForInt(split, "ReloadTime", reloadTime);
			recoil = readForInt(split, "Recoil", recoil);
			accuracy = readForInt(split, "Accuracy", accuracy);
			shootDelay = readForInt(split, "ShootDelay", shootDelay);
			fireAlternately = readBoolean(split, "FireAlternately", fireAlternately);
			health = readForInt(split, "Health", health);
			topViewLimit = readForFloat(split, "TopViewLimit", topViewLimit);
			bottomViewLimit = readForFloat(split, "BottomViewLimit", bottomViewLimit);
			targetMobs = readBoolean(split, "TargetMobs", targetMobs);
			targetPlayers = readBoolean(split, "TargetPlayers", targetPlayers);
			targetVehicles = readBoolean(split, "TargetVehicles", targetVehicles);
			targetPlanes = readBoolean(split, "TargetPlanes", targetPlanes);
			targetMechas = readBoolean(split, "TargetMechas", targetMechas);
			shareAmmo = readBoolean(split, "ShareAmmo", shareAmmo);
			targetRange = readForFloat(split, "TargetRange", targetRange);
			bottomViewLimit = readForFloat(split, "BottomViewLimit", bottomViewLimit);
			
			if(split[0].equals("TargetDriveables"))
				targetMechas = targetPlanes = targetVehicles = Boolean.parseBoolean(split[1]);
			
			if(split[0].equals("ShootSound"))
			{
				shootSound = split[1];
				FlansMod.proxy.loadSound(contentPack, "aaguns", split[1]);
			}
			if(split[0].equals("ReloadSound"))
			{
				reloadSound = split[1];
				FlansMod.proxy.loadSound(contentPack, "aaguns", split[1]);
			}
			if(split[0].equals("NumBarrels"))
			{
				numBarrels = Integer.parseInt(split[1]);
				barrelX = new int[numBarrels];
				barrelY = new int[numBarrels];
				barrelZ = new int[numBarrels];
			}
			if(split[0].equals("Barrel"))
			{
				int id = Integer.parseInt(split[1]);
				barrelX[id] = Integer.parseInt(split[2]);
				barrelY[id] = Integer.parseInt(split[3]);
				barrelZ[id] = Integer.parseInt(split[4]);
			}
			if(split[0].equals("Health"))
			{
				health = Integer.parseInt(split[1]);
			}
			if(split[0].equals("Ammo"))
			{
				BulletType type = BulletType.getBullet(split[1]);
				if(type != null)
				{
					ammo.add(type);
				}
			}
			if(split[0].equals("GunnerPos"))
			{
				gunnerX = Integer.parseInt(split[1]);
				gunnerY = Integer.parseInt(split[2]);
				gunnerZ = Integer.parseInt(split[3]);
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("" + e);
		}
	}
	
	public boolean isAmmo(BulletType type)
	{
		return ammo.contains(type);
	}
	
	public boolean isAmmo(ItemStack stack)
	{
		if(stack == null || stack.isEmpty())
			return false;
		return stack.getItem() instanceof ItemBullet && isAmmo(((ItemBullet)stack.getItem()).type);
	}
	
	public static AAGunType getAAGun(String s)
	{
		for(AAGunType gun : infoTypes)
		{
			if(gun.shortName.equals(s))
				return gun;
		}
		return null;
	}
	
	/**
	 * To be overriden by subtypes for model reloading
	 */
	public void reloadModel()
	{
		model = FlansMod.proxy.loadModel(modelString, shortName, ModelAAGun.class);
	}
	
	@Override
	public void addLoot(LootTableLoadEvent event)
	{
		//Do not add AA guns to dungeon chests. That would be so op.
	}
	
	@Override
	protected void preRead(TypeFile file)
	{
	}
	
	@Override
	protected void postRead(TypeFile file)
	{
		if(numBarrels > 16)
		{
			FlansMod.log.warn("Detected AA Gun type with ludicrous number of barrels " + numBarrels);
			numBarrels = 16;
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public ModelBase GetModel()
	{
		return model;
	}
}

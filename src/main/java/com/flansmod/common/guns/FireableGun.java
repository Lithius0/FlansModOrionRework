package com.flansmod.common.guns;

import com.flansmod.common.types.InfoType;

import net.minecraft.item.ItemStack;

/**
 * Class used for storing the properties of a gun
 */
public class FireableGun
{
	//Radius of the bullet spread
	private float spread;
	private EnumSpreadPattern spreadPattern;
	// Speed a bullet fired from this gun will travel at. (0 means instant/raytraced)
	private float bulletSpeed;
	// the damage this gun will cause
	private float damage;
	// the damage against vehicles
	private float vehicledamage;
	
	// InfoType of the weapon being fired
	private InfoType infoType;

	/**
	 * Direct input constructor, primary for reading from NBT.
	 */
	public FireableGun(float damage, float spread, float bulletSpeed, float vehicleDamage, EnumSpreadPattern spreadPattern, InfoType infoType)
	{
		this.damage = damage;
		this.spread = spread;
		this.bulletSpeed = bulletSpeed;
		this.vehicledamage = vehicleDamage;
		this.spreadPattern = spreadPattern;
		this.infoType = infoType;
	}
	
	/**
	 * Takes the bulletType and the item and figures out all the stats by itself
	 * Used for 
	 * @param type          InfoType of the gun
	 * @param baseDamage    Damage of the gun
	 * @param gunItem       The item of the gun being fired, used to find attachments
	 */
	public FireableGun(GunType type, ShootableType bullet, ItemStack gunItem)
	{
		this.damage = type.getDamage(gunItem) * bullet.damageVsLiving;
		this.spread = type.getSpread(gunItem) * bullet.bulletSpread;
		this.bulletSpeed = type.getBulletSpeed(gunItem) * bullet.throwSpeed;
		this.vehicledamage = type.getDamage(gunItem) * bullet.damageVsDriveable;
		this.spreadPattern = type.getSpreadPattern(gunItem);
		this.infoType = type;
	}

	/**
	 * This method is for instances where there isn't a item associated with the gun and therefore no attachments
	 * Primarily used for vehicles
	 * @param type          InfoType of the gun
	 * @param baseDamage    Damage of the gun
	 * @param gunItem       The item of the gun being fired, used to find attachments
	 */
	public FireableGun(GunType type, ShootableType bullet)
	{
		this.damage = type.damage * bullet.damageVsLiving;
		this.spread = type.bulletSpread * bullet.bulletSpread;
		this.bulletSpeed = type.bulletSpeed * bullet.throwSpeed;
		this.vehicledamage = type.damage * bullet.damageVsDriveable;
		this.spreadPattern = type.spreadPattern;
		this.infoType = type;
	}
	
	/**
	 * This constructor is a hacky solution to allow vehicles to fire guns.
	 * A better way is to have a GunType class which only has gun stats set those as fields for a vehicle's weapons
	 * Then this constructor and FireableGun(GunType, BulletType) could be merged
	 * baseDamage, bulletSpeed, and bulletSpread, should be the stats of the vehicle, not the BulletType
	 * @param bullet        Bullet being fired
	 * @param baseDamage    Base damage of the gun, not the bullet
	 * @param bulletSpeed   Speed of the bullet
	 * @param bulletSpread  Radius of the bullet spread
	 */
	public FireableGun(ShootableType bullet, float baseDamage, float bulletSpeed, float bulletSpread) 
	{
		this.damage = baseDamage * bullet.damageVsLiving;
		this.spread = bulletSpread * bullet.bulletSpread;
		this.bulletSpeed = bulletSpeed * bullet.throwSpeed;
		this.vehicledamage = baseDamage * bullet.damageVsDriveable;
		this.spreadPattern = EnumSpreadPattern.circle; //This seems to be only spread pattern possible for vehicles
		this.infoType = bullet;
	}
	
	/**
	 * This is a temporary solution to the EntityAAGun.
	 * @param aaGun
	 */
	public FireableGun(AAGunType aaGun) 
	{
		this.damage = aaGun.damage;
		this.spread = aaGun.accuracy;
		this.bulletSpeed = 10F;
		this.vehicledamage = aaGun.damage;
		this.spreadPattern = EnumSpreadPattern.circle;
		this.infoType = aaGun;
	}
	
	/**
	 * @return the bullet spread of this gun
	 */
	public float getGunSpread()
	{
		return spread;
	}
	
	public EnumSpreadPattern getSpreadPattern()
	{
		return spreadPattern;
	}
	
	public InfoType getInfoType() {
		return this.infoType;
	}
	
	/**
	 * @return the shortName of the InfoType of this gun
	 */
	public String getShortName()
	{
		return this.infoType.shortName;
	}
	
	/**
	 * @return the damage this gun will cause to anything that isn't a vehicle
	 */
	public float getDamage()
	{
		return damage;
	}
	
	/**
	 * @return the speed a bullet fired from this gun will travel at. (0 means instant/raytraced)
	 */
	public float getBulletSpeed()
	{
		return this.bulletSpeed;
	}
	
	/**
	 * @return the damage this gun will cause against vehicles
	 */
	public float getDamageAgainstVehicles()
	{
		return this.vehicledamage;
	}
	
	public void MultiplySpread(float multiplier) { spread *= multiplier; }
	public void MultiplyDamage(float multiplier) { damage *= multiplier; }
}

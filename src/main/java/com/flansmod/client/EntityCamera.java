package com.flansmod.client;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.world.World;

import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.vector.Vector3f;

/**
 * Third-person camera for driveables. 
 * 
 * Applies a rotated offset from the driveable position.
 * Camera has a linear interpolation step to smooth motion.
 */
public class EntityCamera extends EntityLivingBase
{
	public EntityDriveable driveable;
	public Vector3f offset;
	public EntityCamera(World world)
	{
		super(world);
		setSize(0F, 0F);
	}
	
	public EntityCamera(World world, EntityDriveable d)
	{
		this(world, d, new Vector3f(-10f, 2f, 0f));
	}
	
	public EntityCamera(World world, EntityDriveable d, Vector3f offset)
	{
		this(world);
		driveable = d;
		setPosition(d.posX, d.posY, d.posZ);
		this.offset = offset; //TODO: Make this a vehicle config later
	}
	
	@Override
	public void onUpdate()
	{
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		
		//Finding the position the camera is moving towards
		//This is just the driveable's position with the rotated applied offset.
		Vector3f desiredPosition = driveable.axes.findLocalVectorGlobally(offset);
		
		//Scaling the offset so that driveables going faster will be zoomed out.
		//Scaling to the sqrt of the speed so the effect never gets too insane.
		//Driveables moving at 50 m/s (Elytra + rocket is ~30 m/s) will be zoomed out 70% compared to normal
		desiredPosition.scale((float) (Math.sqrt(driveable.getSpeed()) * 0.1 + 1));
		
		Vector3f.add(desiredPosition, new Vector3f(driveable.posX, driveable.posY, driveable.posZ), desiredPosition);

		float lerpAlpha = 0.25F;
		
		//Lerping between desired position and old position
		double newX = (1 - lerpAlpha) * posX + lerpAlpha * desiredPosition.x;
		double newY = (1 - lerpAlpha) * posY + lerpAlpha * desiredPosition.y;
		double newZ = (1 - lerpAlpha) * posZ + lerpAlpha * desiredPosition.z;
		
		
		setPosition(newX, newY, newZ);
		
		rotationYaw = driveable.axes.getYaw() - 90;
		rotationPitch = driveable.axes.getPitch();
		
		//Angle wrapping
		while(rotationYaw - prevRotationYaw >= 180F)
		{
			rotationYaw -= 360F;
		}
		while(rotationYaw - prevRotationYaw < -180F)
		{
			rotationYaw += 360F;
		}
	}
	
	@Override
	public Iterable<ItemStack> getArmorInventoryList()
	{
		return null;
	}
	
	@Override
	public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn)
	{
		return ItemStack.EMPTY.copy();
	}
	
	@Override
	public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack)
	{
	
	}
	
	@Override
	public EnumHandSide getPrimaryHand()
	{
		return EnumHandSide.RIGHT;
	}
	
}

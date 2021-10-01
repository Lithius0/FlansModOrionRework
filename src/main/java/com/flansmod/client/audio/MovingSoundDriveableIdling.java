package com.flansmod.client.audio;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.driveables.EntityDriveable;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MovingSoundDriveableIdling extends MovingSound {
	private final EntityDriveable driveable;
	
	public MovingSoundDriveableIdling(EntityDriveable driveable)
	{
		super(FlansModResourceHandler.getSoundEvent(driveable.getDriveableType().startSound), SoundCategory.PLAYERS);
		this.driveable = driveable;
	    repeat = true;
	    repeatDelay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.attenuationType = ISound.AttenuationType.LINEAR;
	}
	
	public void update()
	{
		if(driveable.isDead)
		{
			donePlaying = true;
		}
		else
		{
			//Updating position
			xPosF = (float)driveable.posX;
			yPosF = (float)driveable.posY;
			zPosF = (float)driveable.posZ;

			if (driveable.throttle < EntityDriveable.ENGINE_SOUND_THRESHOLD && driveable.throttle > 0.001F && driveable.canProducePower(driveable.getCurrentFuelConsumption()))
			{
				volume = 1;
			}
			else
			{
				volume = 0;
			}
		}
	}
}

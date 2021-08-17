package com.flansmod.client.audio;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.EntityDriveable;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MovingSoundDriveable extends MovingSound 
{
	private final EntityDriveable driveable;
	/**
	 * Whether or not the driveable can idle. If true, this sound's volume goes to 0 when the driveable's throttle
	 * dips below the ENGINE_SOUND_THRESHOLD as defined in EntityDriveable. 
	 * 
	 * This class isn't responsible for the idle sound, a MovingSoundDriveableIdling sound will need to be played.
	 */
	private final boolean canIdle;
	
	public MovingSoundDriveable(EntityDriveable driveable)
	{
		super(FlansModResourceHandler.getSoundEvent(driveable.getDriveableType().engineSound), SoundCategory.PLAYERS);
	    this.driveable = driveable;
		this.canIdle = !driveable.getDriveableType().startSound.isEmpty();
	    repeat = true;
	    repeatDelay = 0;
        this.volume = FlansMod.soundRange / 16F;
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
			
			if ((!canIdle || driveable.throttle >= EntityDriveable.ENGINE_SOUND_THRESHOLD) && driveable.throttle > 0.001F && driveable.hasFuel())
			{
				float throttleModifier = driveable.throttle * 0.5F + 0.5F; //Throttle can change the volume by a max of 50%
		        this.volume = (FlansMod.soundRange / 16F) * throttleModifier;
			}
			else
			{
				volume = 0;
			}
		}
	}
}

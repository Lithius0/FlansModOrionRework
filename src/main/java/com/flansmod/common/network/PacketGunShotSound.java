package com.flansmod.common.network;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class PacketGunShotSound extends PacketBase {
	String sound = "";
	boolean silenced = false;
	double posX, posY, posZ;
	
	//Empty constructor to create empty packet. Required for the handler to decode
	public PacketGunShotSound() {
		
	}
	
	public PacketGunShotSound(String sound, boolean silenced, double posX, double posY, double posZ) {
		this.sound = sound;
		this.silenced = silenced;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
	}

	public static void sendSoundPacket(EntityPlayer player, String sound, boolean silenced)
	{
		if (sound != null && !sound.isEmpty()) {
			double x = player.posX;
			double y = player.posY;
			double z = player.posZ;
			FlansMod.getPacketHandler().sendToAllAround(new PacketGunShotSound(sound, silenced, x, y, z), x, y, z, FlansMod.soundRange, player.dimension);
		}
	}
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
		writeUTF(data, sound);
		data.writeBoolean(silenced);
		data.writeDouble(posX);
		data.writeDouble(posY);
		data.writeDouble(posZ);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
		sound = readUTF(data);
		silenced = data.readBoolean();
		posX = data.readDouble();
		posY = data.readDouble();
		posZ = data.readDouble();
	}

	@Override
	public void handleServerSide(EntityPlayerMP playerEntity) {
		FlansMod.log.warn("Received play sound packet on server. Skipping.");
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer) {
		if (sound != null && !sound.isEmpty())
		{
			SoundEvent shotSound = FlansModResourceHandler.getSoundEvent(sound);
			clientPlayer.getEntityWorld().playSound(posX, posY, posZ, shotSound, SoundCategory.PLAYERS, silenced ? 0.4F : 8F, silenced ? 8F : 1F, false);
		}
	}

}

package com.flansmod.apocalypse.client.model;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import com.flansmod.apocalypse.common.entity.EntitySkullBoss;

public class RenderSkullBoss extends Render<EntitySkullBoss>
{
	private static final ResourceLocation texture = new ResourceLocation("flansmodapocalypse", "textures/entity/skullboss.png");
	private ModelSkullBoss model;
	
	public RenderSkullBoss(RenderManager rm)
	{
		super(rm);
		model = new ModelSkullBoss();
	}
	
	public void doRender(EntitySkullBoss entity, double x, double y, double z, float p_76986_8_, float partialTicks)
	{
		bindEntityTexture(entity);
		
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		
		GlStateManager.rotate(-entity.rotationYaw + entity.GetSpawnSpin(partialTicks), 0, 1, 0);
		GlStateManager.rotate(entity.rotationPitch, 0, 0, 1);
		GlStateManager.scale(32f, 32f, 32f);
		
		float laughFactor = entity.GetLaughFactor(partialTicks);
		
		GlStateManager.pushMatrix();
		{
			GlStateManager.rotate(laughFactor * 15.0f, 0, 0, 1);
			model.renderHead(1F / 16F);
		}
		GlStateManager.popMatrix();
		
	
		GlStateManager.pushMatrix();
		{
			GlStateManager.rotate(-laughFactor * 15.0f, 0, 0, 1);
			model.renderJaw(1F / 16F);		
		}
		GlStateManager.popMatrix();
		
		
		GlStateManager.popMatrix();
	}
	
	@Override
	protected ResourceLocation getEntityTexture(EntitySkullBoss entity)
	{
		return texture;
	}
		
	public static class Factory implements IRenderFactory<EntitySkullBoss>
	{
		@Override
		public Render<EntitySkullBoss> createRenderFor(RenderManager manager)
		{
			return new RenderSkullBoss(manager);
		}
	}
}

package net.wildbill22.draco.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.wildbill22.draco.lib.REFERENCE;

public class RenderCatapult extends RenderLiving {
	private static ResourceLocation texture;

	public RenderCatapult(ModelBase par1ModelBase, float par2) {
		super(par1ModelBase, par2);
		texture = new ResourceLocation(REFERENCE.MODID + ":textures/models/catapult.png");
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return texture;
	}
}

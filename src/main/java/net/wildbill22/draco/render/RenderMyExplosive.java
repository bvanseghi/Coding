package net.wildbill22.draco.render;

import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class RenderMyExplosive extends RenderSnowball {
	private static ResourceLocation texture;

	public RenderMyExplosive(Item item) {
		super(item);
//		texture = new ResourceLocation(REFERENCE.MODID + ":textures/items/fireball.png");
		texture = new ResourceLocation("minecraft:fireball"); // Until we make a texture
	}
	
	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return texture;
	}
}

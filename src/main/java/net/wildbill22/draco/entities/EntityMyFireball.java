package net.wildbill22.draco.entities;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.wildbill22.draco.items.ItemMyFireball;
import net.wildbill22.draco.lib.LogHelper;

public class EntityMyFireball extends EntityThrowable {

	public EntityMyFireball(World world, double p_i1778_2_, double p_i1778_4_, double p_i1778_6_) {
		super(world, p_i1778_2_, p_i1778_4_, p_i1778_6_);
	}
	
	public EntityMyFireball(World par1World, EntityLivingBase par2EntityLivingBase) {
		super(par1World, par2EntityLivingBase);
	}
	
	public EntityMyFireball(World par1World) {
		super(par1World);
	}	

	@Override
	protected void onImpact(MovingObjectPosition movObjPos) {
		if (!this.worldObj.isRemote)
			LogHelper.info("EntityMyFireball landed!");

		doThrowAndFireDamage(movObjPos);

		// Get rid of the used fireball
		if (!this.worldObj.isRemote)
			this.setDead();
	}
	
	private boolean doThrowAndFireDamage(MovingObjectPosition movObjPos) {
		if (movObjPos.entityHit != null) {
			if (!movObjPos.entityHit.isImmuneToFire() && 
					movObjPos.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), ItemMyFireball.fireballDamage))
				movObjPos.entityHit.setFire(5);
			for (int i = 0; i < 4; ++i)	{
				this.worldObj.spawnParticle("crit", this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
			}
			return true;
		}
		return false;
	}
}

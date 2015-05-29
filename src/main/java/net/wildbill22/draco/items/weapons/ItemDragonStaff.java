package net.wildbill22.draco.items.weapons;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.wildbill22.draco.Core;
import net.wildbill22.draco.Creative_Tab;
import net.wildbill22.draco.entities.EntityMyExplosive;
import net.wildbill22.draco.entities.EntityMyFireball;
import net.wildbill22.draco.entities.dragons.DragonRegistry.IDragonStaffHandler;
import net.wildbill22.draco.entities.player.DragonPlayer;
import net.wildbill22.draco.items.ItemDragonEgg;
import net.wildbill22.draco.items.ModItems;
import net.wildbill22.draco.lib.LogHelper;
import net.wildbill22.draco.lib.NBTCoordinates;
import net.wildbill22.draco.lib.REFERENCE;
import net.wildbill22.draco.network.StaffUpdateDamageTarget;
import net.wildbill22.draco.network.StaffUpdateSetTargetOnFire;

public abstract class ItemDragonStaff extends ItemSword implements IDragonStaffHandler {
    protected float damageAmplifier = 0;  	// FIXME: This is not multi-player safe! (short window to collide)
    protected float field_150934_a;
    protected Abilities abilities;
    MovingObjectPosition mop;
    
	public ItemDragonStaff(ToolMaterial material, String name) {
		super(material);
        this.field_150934_a = 4.0F + material.getDamageVsEntity();

        this.setCreativeTab(Creative_Tab.TabDraco_Animus);
		this.setUnlocalizedName(REFERENCE.Unlocalized_Path + name);
		this.setTextureName(REFERENCE.Texture_Path + name);

		setMaxStackSize(1);
		abilities = new Abilities();
		abilities.addFireballs();
	}
	
	/**
	 * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
	 */
	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
		// Only spawn new entities on the server!
		if (!world.isRemote) {
			// Staff doesn't do anything if no egg for it in hoard
			if (!DragonPlayer.get(player).isEggInHoard(this.getEggName())) {
				setNoEggInHoard(itemStack);
				player.addChatMessage(new ChatComponentText("No egg in hoard for this staff!"));
				setActive(itemStack, false);
				return itemStack;
			}
			// egg was put back in, so activate the staff
			else {
				setEggInHoard(itemStack);
				setActive(itemStack, true);
			}
			
			// Change mode if shift key pressed
			if (player.isSneaking()) {
				abilities.nextMode(itemStack);
				player.addChatMessage(new ChatComponentText("Staff in mode: " + abilities.getModeName(itemStack) + "!"));
				return itemStack;
			}

			// Change between dragon and human
			if (abilities.getModeNumber(itemStack) == abilities.CHANGE_FORM) {
				if (DragonPlayer.get(player).isDragon()) {
					DragonPlayer.get(player).setDragon(false, true);
					ItemDragonEgg.setHumanAbilities(player);  // Set some things that got changed!
					player.addChatMessage(new ChatComponentText("Changed to human!"));					
				} else {
					DragonPlayer.get(player).setDragonName(abilities.dragonTextureName);
					DragonPlayer.get(player).setDragon(true, true);
					player.addChatMessage(new ChatComponentText("Changed to dragon!"));
				}
				return itemStack;
			}
			else if (getCooldown(itemStack) > 0) {
				return itemStack;
			}
			// No powers for humans!
			else if (!DragonPlayer.get(player).isDragon()) {
				player.addChatMessage(new ChatComponentText("Silly human, only dragons have powers!"));
				setActive(itemStack, false);
				return itemStack;
			}
			else if (DragonPlayer.get(player).getDragonName().compareTo(abilities.dragonTextureName) != 0) {
				player.addChatMessage(new ChatComponentText("This staff is only for " + abilities.dragonDisplayName + "!"));
				setActive(itemStack, false);
				return itemStack;				
			}
		}
		
		// Only spawn new entities on the server!
		if (!world.isRemote) {
			// Fireballs
			if (abilities.getModeNumber(itemStack) == abilities.FIREBALLS) {
				world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
				/* This method will spawn an entity in the World, you can use with anything that extends
				* the Entity class, in this case it's the EntityMyFireball class
				*/
				world.spawnEntityInWorld(new EntityMyFireball(world, player));
				setCooldown(itemStack, 100);
			}
			// Explosive Fireballs
			else if (abilities.getModeNumber(itemStack) == abilities.EXPLOSIVE_FIREBALLS) {
				world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
				/* This method will spawn an entity in the World, you can use with anything that extends
				* the Entity class, in this case it's the EntityMyExplosive class
				*/
				world.spawnEntityInWorld(new EntityMyExplosive(world, player));
				setCooldown(itemStack, 100);
			}
			else if (abilities.getModeNumber(itemStack) == abilities.INVISIBLE) {
				int amplifier = Math.max(1, DragonPlayer.get(player).getLevel() / 2);
				LogHelper.info("ItemDragonStaff: Amplifier = " + amplifier);
	            player.addPotionEffect(new PotionEffect(Potion.invisibility.getId(), (60 * 20 * amplifier), amplifier));
			}
			else if (abilities.getModeNumber(itemStack) == abilities.NIGHTVISION) {
				int amplifier = Math.max(1, DragonPlayer.get(player).getLevel() / 2);
				LogHelper.info("ItemDragonStaff: Amplifier = " + amplifier);
	            player.addPotionEffect(new PotionEffect(Potion.nightVision.getId(), (60 * 20 * amplifier), amplifier));
			}
			else if (abilities.getModeNumber(itemStack) == abilities.WATERBREATHING) {
				int amplifier = Math.max(1, DragonPlayer.get(player).getLevel() / 2);
				LogHelper.info("ItemDragonStaff: Amplifier = " + amplifier);
	            player.addPotionEffect(new PotionEffect(Potion.waterBreathing.getId(), (60 * 20 * amplifier), amplifier));
			}
			else if (abilities.getModeNumber(itemStack) == abilities.TELEPORTING) {
//				int amplifier = Math.max(1, DragonPlayer.get(player).getLevel() / 2);
				world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
				world.spawnEntityInWorld(new EntityEnderPearl(world, player));
				setCooldown(itemStack, 100);
			}
			else if (abilities.getModeNumber(itemStack) == abilities.SWIFTNESS) {
				int amplifier = Math.max(1, DragonPlayer.get(player).getLevel() / 3);
				LogHelper.info("ItemDragonStaff: Amplifier = " + amplifier);
	            player.addPotionEffect(new PotionEffect(Potion.moveSpeed.getId(), (60 * 20 * amplifier), amplifier));
			}
			else if (abilities.getModeNumber(itemStack) == abilities.HASTE) {
				int amplifier = Math.max(1, DragonPlayer.get(player).getLevel() / 4);
				LogHelper.info("ItemDragonStaff: Amplifier = " + amplifier);
	            player.addPotionEffect(new PotionEffect(Potion.digSpeed.getId(), (60 * 20 * amplifier), amplifier));
			}
			else if (abilities.getModeNumber(itemStack) == abilities.REGENERATION) {
				int amplifier = Math.max(1, DragonPlayer.get(player).getLevel() / 3);
				LogHelper.info("ItemDragonStaff: Amplifier = " + amplifier);
	            player.addPotionEffect(new PotionEffect(Potion.regeneration.getId(), (60 * 20 * amplifier), amplifier));
			}
			else if (abilities.getModeNumber(itemStack) == abilities.GOLDENEYE) {
				int amplifier = Math.max(1, DragonPlayer.get(player).getLevel() / 3);
				LogHelper.info("ItemDragonStaff: Amplifier = " + amplifier);
				goldenEye(player);
			}
		// On client
		} else if (isActive(itemStack)) { 
			// Fire Breathing
			if (abilities.getModeNumber(itemStack) == abilities.FIREBREATHING) {
				int amplifier = DragonPlayer.get(player).getLevel();
		    	spawnFlames(world, player, 2.0F, amplifier);
		    	Entity entity = getMouseOver(world, amplifier * 2.0F + 4.5F);  // normal reach is 4.5F in survival
				if (entity instanceof EntityLivingBase) {
					LogHelper.info("Hit entity at: " + entity.posX + "," + entity.posY + "," + entity.posZ);
		        	Core.modChannel.sendToServer(new StaffUpdateSetTargetOnFire(entity.posX, entity.posY, entity.posZ));
		    	}
			}
			else if (abilities.getModeNumber(itemStack) == abilities.SOUNDWAVE) {
				int amplifier = DragonPlayer.get(player).getLevel();
		    	spawnSoundWave(world, player, 2.0F, amplifier);
		    	Entity entity = getMouseOver(world, amplifier * 2.0F + 4.5F);  // normal reach is 4.5F in survival
				world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
				if (entity instanceof EntityLivingBase) {
					LogHelper.info("Hit entity at: " + entity.posX + "," + entity.posY + "," + entity.posZ);
		        	Core.modChannel.sendToServer(new StaffUpdateDamageTarget(entity.posX, entity.posY, entity.posZ));
		    	}				
			}
		}
		return itemStack;
	}
	
    // spawn particle name, position x,y,z velocity x,y,z
	private void spawnFlames(World world, EntityPlayer player, float minD, int amplifier) {
		Vec3 look = player.getLook(1.0F);
        double dxMin = (double)((float)player.posX + look.xCoord * minD);
        double dyMin = (double)((float)player.posY + player.eyeHeight + look.yCoord * minD);
        double dzMin = (double)((float)player.posZ + look.zCoord * minD);
        world.spawnParticle("smoke", dxMin, dyMin, dzMin, look.xCoord * 0.05 * amplifier, look.yCoord * 0.05 * amplifier, 
        		look.zCoord * 0.05 * amplifier);
        world.spawnParticle("flame", dxMin, dyMin, dzMin, look.xCoord * 0.05 * amplifier, look.yCoord * 0.05 * amplifier, 
        		look.zCoord * 0.05 * amplifier);        
    }
	
    // spawn particle name, position x,y,z velocity x,y,z
	private void spawnSoundWave(World world, EntityPlayer player, float minD, int amplifier) {
		Vec3 look = player.getLook(1.0F);
    	for (int i = 0; i < 4; i++) {
    		float d0 = world.rand.nextFloat() - 0.2F;
            double dxMin = (double)((float)player.posX + d0 + look.xCoord * minD);
//            double dyMin = (double)((float)player.posY + d0 + player.eyeHeight + look.yCoord * minD);
            double dyMin = (double)((float)player.posY + d0 + look.yCoord * minD);
            double dzMin = (double)((float)player.posZ + d0 + look.zCoord * minD);
            world.spawnParticle("crit", dxMin, dyMin, dzMin, look.xCoord * 0.05 * amplifier, look.yCoord * 0.05 * amplifier, 
            		look.zCoord * 0.05 * amplifier);
    	}
//        world.spawnParticle("crit", entity.posX + d0, entity.posY + d0, entity.posZ + d0, 0, 0, 0);		    		
    }
	
    /**
     * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
     */
    public Entity getMouseOver(World world, float reach)  {
    	Minecraft mc = Minecraft.getMinecraft();
        if (mc.renderViewEntity != null) {
            if (mc.theWorld != null) {
                mc.pointedEntity = null;
                double d0 = reach;
                mc.objectMouseOver = mc.renderViewEntity.rayTrace(d0, reach);
                
                double d1 = d0;
                Vec3 vec3 = mc.renderViewEntity.getPosition(reach);
                if (mc.objectMouseOver != null) {
                    d1 = mc.objectMouseOver.hitVec.distanceTo(vec3);
                }

                Vec3 vec31 = mc.renderViewEntity.getLook(reach);
                Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
                mc.pointedEntity = null;
                Vec3 vec33 = null;
                float f1 = 1.0F;
                @SuppressWarnings("rawtypes")
				List list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.renderViewEntity, 
                		mc.renderViewEntity.boundingBox.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0)
                		.expand((double)f1, (double)f1, (double)f1));
                double d2 = d1;

                for (int i = 0; i < list.size(); ++i) {
                    Entity entity = (Entity)list.get(i);
                    if (entity.canBeCollidedWith()) {
                        float f2 = entity.getCollisionBorderSize();
                        AxisAlignedBB axisalignedbb = entity.boundingBox.expand((double)f2, (double)f2, (double)f2);
                        MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
                        if (axisalignedbb.isVecInside(vec3)) {
                            if (0.0D < d2 || d2 == 0.0D) {
                            	mc.pointedEntity = entity;
                                vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                                d2 = 0.0D;
                            }
                        }
                        else if (movingobjectposition != null) {
                            double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                            if (d3 < d2 || d2 == 0.0D) {
                                if (entity == mc.renderViewEntity.ridingEntity && !entity.canRiderInteract()) {
                                    if (d2 == 0.0D) {
                                    	mc.pointedEntity = entity;
                                        vec33 = movingobjectposition.hitVec;
                                    }
                                }
                                else {
                                	mc.pointedEntity = entity;
                                    vec33 = movingobjectposition.hitVec;
                                    d2 = d3;
                                }
                            }
                        }
                    }
                }
                if (mc.pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                    mc.objectMouseOver = new MovingObjectPosition(mc.pointedEntity, vec33);
                    if (mc.pointedEntity instanceof EntityLivingBase || mc.pointedEntity instanceof EntityItemFrame) {
                        mc.pointedEntity = mc.pointedEntity;
                    }
                }
            }
        }
        return mc.pointedEntity;
    }

    private void goldenEye(EntityPlayer player) {
		final NBTCoordinates playerLocation;
		TileEntityChest closestChest = null;
		float distance;
		float closestDistance = 1000000.0F; // Show direction to chests closer than this distance (up to 1000 blocks away)
		boolean foundCoins = false;
		
		if (player != null){
			playerLocation = new NBTCoordinates((int)player.posX, (int)player.posY, (int)player.posZ);	
			
//			for (Object tileEntity : Minecraft.getMinecraft().theWorld.loadedTileEntityList){
			for (Object tileEntity : player.worldObj.loadedTileEntityList){
				foundCoins = false;
				if (tileEntity instanceof TileEntityChest){
					TileEntityChest chestEntity = (TileEntityChest) tileEntity;
					Object chestBlock = player.worldObj.getBlock(chestEntity.xCoord, chestEntity.yCoord, chestEntity.zCoord);
					if (chestBlock instanceof BlockChest){
						for (int j = 0; j < chestEntity.getSizeInventory(); j++) {
							ItemStack itemStack = chestEntity.getStackInSlot(j);
							if (itemStack != null) {
								if (itemStack.getItem() == ModItems.goldCoin) {
									foundCoins = true;
									break;
								}
							}
						}
						if (foundCoins) {
							NBTCoordinates chestLocation = new NBTCoordinates(chestEntity.xCoord, chestEntity.yCoord, chestEntity.zCoord);
							distance = chestLocation.getDistanceSquaredToChunkCoordinates(playerLocation);
							if (distance < closestDistance){
								closestDistance = distance;
								closestChest = chestEntity; 
							}
						}
					}
				}
			}
			if (closestChest != null){
				// TODO: Animate with a beeping sound (or flashing) when pointing to chest!
				LogHelper.info("Nearest chest at: " + closestChest.xCoord + "," + closestChest.yCoord + "," + closestChest.zCoord + "!");
				player.addChatMessage(new ChatComponentText("Found gold coins at: " + closestChest.xCoord + "," + closestChest.yCoord + "," + closestChest.zCoord + "!"));				
			}
			else { 
				LogHelper.info("No gold coins found nearby");
				player.addChatMessage(new ChatComponentText("No gold coins found nearby"));
			}
		}		
    }

    /**
     * Current implementations of this method in child classes do not use the entry argument beside ev. They just raise
     * the damage on the stack.
     */
	@Override
    public boolean hitEntity(ItemStack itemStack, EntityLivingBase hitEntity, EntityLivingBase playerEntity)  {
		if (playerEntity instanceof EntityPlayer) {
			damageAmplifier = DragonPlayer.get((EntityPlayer) playerEntity).getLevel() / 2;
		}
        itemStack.damageItem(1, playerEntity);
        return true;
    }

    /**
     * Gets a map of item attribute modifiers, used by ItemSword to increase hit damage.
     */
	@Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Multimap getItemAttributeModifiers()
    {
        Multimap multimap = super.getItemAttributeModifiers();
        multimap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(field_111210_e, 
        		"Weapon modifier", (double)this.field_150934_a + damageAmplifier, 0));
        return multimap;
    }
    
    private void setNoEggInHoard(ItemStack itemStack) {
    	NBTTagCompound tagCompound = itemStack.getTagCompound();
		tagCompound.setBoolean("eggInHoard", false);    	
    }
    
    private void setEggInHoard(ItemStack itemStack) {
    	NBTTagCompound tagCompound = itemStack.getTagCompound();
		tagCompound.setBoolean("eggInHoard", true);    	
    }

    private boolean isEggInHoard(ItemStack itemStack) {    	
    	NBTTagCompound tagCompound = getTagCompound(itemStack);
		return tagCompound.getBoolean("eggInHoard");
    }
    
    private int getCooldown(ItemStack itemStack) {
    	NBTTagCompound tagCompound = getTagCompound(itemStack);
    	return tagCompound.getInteger("cooldown");
    }

    private void setCooldown(ItemStack itemStack, int cooldown) {
    	NBTTagCompound tagCompound = getTagCompound(itemStack);
    	tagCompound.setInteger("cooldown", cooldown);
    }

    private void setActive(ItemStack itemStack, boolean active) {
    	NBTTagCompound tagCompound = itemStack.getTagCompound();
		tagCompound.setBoolean("active", active);    	
    }

    private boolean isActive(ItemStack itemStack) {    	
    	NBTTagCompound tagCompound = getTagCompound(itemStack);
		return tagCompound.getBoolean("active");
    }
        
    /**
     * Called each tick as long the item is on a player inventory. Uses by maps to check if is on a player hand and
     * update it's contents.
     */
    public void onUpdate(ItemStack itemStack, World p_77663_2_, Entity p_77663_3_, int p_77663_4_, boolean p_77663_5_) {
    	super.onUpdate(itemStack, p_77663_2_, p_77663_3_, p_77663_4_, p_77663_5_);
    	
    	NBTTagCompound tagCompound = getTagCompound(itemStack);
		int cooldown = tagCompound.getInteger("cooldown");
			
		if (cooldown > 0) {
			cooldown--;
			boolean effect = (cooldown % 6) < 3;  // Flash the staff, to show cool down is in effect
			tagCompound.setBoolean("effect", effect);
			tagCompound.setInteger("cooldown", cooldown);
		}		
    }
    
    private NBTTagCompound getTagCompound(ItemStack itemStack) {    	
		if (!itemStack.hasTagCompound()) { // Ensure the ItemStack has an NBTTagCompound
			NBTTagCompound tagCompound = new NBTTagCompound();
			tagCompound.setInteger("mode", 0);
			tagCompound.setBoolean("eggInHoard", true);
			tagCompound.setBoolean("effect", true);
			tagCompound.setInteger("cooldown", 0);
			tagCompound.setBoolean("active", true);
			itemStack.setTagCompound(tagCompound);
		}
		return itemStack.getTagCompound();
    }

    // Add this if you want the "enchanted" look in the inventory
	@Override
	public boolean hasEffect(ItemStack itemStack, int pass) {
    	NBTTagCompound tagCompound = getTagCompound(itemStack);
		return isEggInHoard(itemStack) && tagCompound.getBoolean("effect");
	}
	
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
		return super.getItemStackDisplayName(stack) + " mode: " + abilities.getModeName(stack) + "!";
    }
    
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4) {
		list.add("Staff that allows you to change");
		list.add("from human to " + abilities.dragonDisplayName + ".");
		list.add("Use shift + right click to change modes.");
		list.add(stack.getMaxDamage() - stack.getItemDamage() + " Hits Remaining");
		list.add(((ItemDragonStaff)stack.getItem()).field_150934_a + " Attack Damage");
		list.add("Current ability: " + abilities.getModeName(stack) + "!");
	}

    // Stuff below for modes
    
    public static class Abilities {
	    private Map<Integer, String> staffModes = Maps.newHashMap();
	    private String dragonTextureName;
	    private String dragonDisplayName;
		private int FIREBALLS = 0;  // Keep this first
		private int CHANGE_FORM = 1;
		private int EXPLOSIVE_FIREBALLS = 2;
		private int INVISIBLE = 3;
		private int NIGHTVISION = 4;
		private int WATERBREATHING = 5;
		private int TELEPORTING = 6;
		private int SWIFTNESS = 7;
		private int HASTE = 8;
		private int REGENERATION = 9;
		private int FIREBREATHING = 10;
		private int SOUNDWAVE = 11;
		private int GOLDENEYE = 12;
		private int NUM_MODES = 13;

		/**
	     * Adds ability to staff to change between dragon and human.
	     * 
	     * @param dragonTextureName unlocalizedName or any unique string 
		 * @param dragonDisplayName - Like "Water Dragon"
	     */
		public void addChangeForm(String dragonTextureName, String dragonDisplayName) {
			this.dragonTextureName = dragonTextureName;
			this.dragonDisplayName = dragonDisplayName;
			addMode(CHANGE_FORM, "Change form");
		}
		/**
	     * Adds ability to staff to throw fireballs.
	     */
		public void addFireballs() {
			addMode(FIREBALLS, "Spawns Fireballs");
		}
		/**
	     * Adds ability to staff to throw explosive fireballs.
	     */
		public void addExplosiveFireballs() {
			addMode(EXPLOSIVE_FIREBALLS, "Spawn Explosive Fireballs");
		}
		/**
	     * Adds ability to staff to teleport using ender pearls.
	     */
		public void addTeleporting() {
			addMode(TELEPORTING, "Teleport");
		}
		/**
	     * Adds ability to staff to become invisible.
	     */
		public void addInvisible() {
			addMode(INVISIBLE, "Invisible");
		}
		/**
	     * Adds ability to staff to move faster.
	     */
		public void addSwiftness() {
			addMode(SWIFTNESS, "Swiftness");
		}
		/**
	     * Adds ability to staff to dig faster.
	     */
		public void addHaste() {
			addMode(HASTE, "Breaks blocks faster");
		}
		/**
	     * Adds ability to regenerate (heal yourself).
	     */
		public void addRegeneration() {
			addMode(REGENERATION, "Regeneration - heal yourself");
		}
		/**
	     * Adds ability to breath fire.
	     */
		public void addFireBreathing() {
			addMode(this.FIREBREATHING, "Fire breathing");
		}
		/**
	     * Adds ability to harm with sound.
	     */
		public void addSoundWave() {
			addMode(this.SOUNDWAVE, "Sound wave");
		}
		/**
	     * Adds ability to find gold coins.
	     */
		public void addGoldenEye() {
			addMode(this.GOLDENEYE, "Golden Eye");
		}

		private void addMode(int mode, String text) {
			staffModes.put(mode, text);
		}
		
	    public String getModeName(ItemStack stack) {
	    	return staffModes.get(getModeNumber(stack));
	    }
    
	    private int getModeNumber(ItemStack stack) {
			if (!stack.hasTagCompound()) { // Ensure the ItemStack has an NBTTagCompound with a "mode" tag
				NBTTagCompound tagCompound = new NBTTagCompound();
				tagCompound.setInteger("mode", 0);
				tagCompound.setBoolean("eggInHoard", true);
				stack.setTagCompound(tagCompound);
			}
			return stack.getTagCompound().getInteger("mode") % NUM_MODES; // Ensure mode is < NUM_MODES
	    }
	    
	    // TODO: Use an iterator
	    private void nextMode(ItemStack stack) {
	    	int mode = getModeNumber(stack) + 1;
	    	for (; mode < NUM_MODES; mode++) {
	    		if (staffModes.containsKey(mode)) {
	    			break;
	    		}
	    	}
	    	if (mode >= NUM_MODES)
	    		mode = FIREBALLS; // set to 0
			stack.getTagCompound().setInteger("mode", mode);
	    }
    }    
}

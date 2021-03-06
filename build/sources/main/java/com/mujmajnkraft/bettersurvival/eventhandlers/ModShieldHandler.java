package com.mujmajnkraft.bettersurvival.eventhandlers;

import com.mujmajnkraft.bettersurvival.init.ModEnchantments;
import com.mujmajnkraft.bettersurvival.items.ItemCustomShield;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.command.AdvancementCommand;
import net.minecraft.command.CommandException;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModShieldHandler {
	
	//Responsible for damage reduction while blocking
	@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled = true)
	public void onEvent(LivingHurtEvent event)
	{
		float amount = event.getAmount();
		DamageSource source = event.getSource();
		if (event.getAmount() > 0.0F && canBlockDamageSource(source, event.getEntityLiving()))
	       {
			ItemCustomShield shield = (ItemCustomShield) event.getEntityLiving().getActiveItemStack().getItem();
			if (!source.isUnblockable())
			{
				int l = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.blockpower, event.getEntityLiving().getActiveItemStack());
				event.setAmount(amount*(1.0f-shield.blockpower)/(1+l));
				if (event.getEntityLiving() instanceof EntityPlayer)
				{
					damageShield(amount, (EntityPlayer)event.getEntityLiving());
				}
			}
			else
			{
				int l = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.spellshield, event.getEntityLiving().getActiveItemStack());
				if (l > 0)
				{
					event.setAmount(amount*(1.0f-(shield.blockpower*(l/3.0F))));
					if (event.getEntityLiving() instanceof EntityPlayer)
					{
						damageShield(amount, (EntityPlayer)event.getEntityLiving());
					}
				}
			}

	        if (!source.isProjectile())
	        {
	            Entity entity = source.getImmediateSource();

	            if (entity instanceof EntityLivingBase)
	            {
	                ((EntityLivingBase) entity).knockBack(event.getEntityLiving(), 0.5F, event.getEntityLiving().posX - entity.posX, event.getEntityLiving().posZ - entity.posZ);
	            }
	        }
	    }
	}
		
	//Copy of EntityPlayer.damageShield with a few changes to work with custom shields
	protected void damageShield(float damage, EntityPlayer player)
	{
	    if (damage >= 3.0F && player.getActiveItemStack().getItem() instanceof ItemCustomShield)
	    {
	        ItemStack copyBeforeUse = player.getActiveItemStack().copy();
	        int i = 1 + MathHelper.floor(damage);
	        player.getActiveItemStack().damageItem(i, player);

	        if (player.getActiveItemStack().isEmpty())
	        {
	            EnumHand enumhand = player.getActiveHand();
	            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, copyBeforeUse, enumhand);

	            if (enumhand == EnumHand.MAIN_HAND)
	            {
	            	player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
	            }
	            else
	            {
	                player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
	            }

	            player.resetActiveHand();
	            player.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + player.world.rand.nextFloat() * 0.4F);
	        }
	    }
	}

	//Copy of EntityPlayer.canBlockDamageSource with a few changes to work with custom shields
	private boolean canBlockDamageSource(DamageSource damageSourceIn, EntityLivingBase entity)
	{
		if (entity.getActiveItemStack().getItem() instanceof ItemCustomShield)
	    {
	        Vec3d vec3d = damageSourceIn.getDamageLocation();

	        if (vec3d != null)
	        {
	            Vec3d vec3d1 = entity.getLook(1.0F);
	            Vec3d vec3d2 = vec3d.subtractReverse(new Vec3d(entity.posX, entity.posY, entity.posZ)).normalize();
	            vec3d2 = new Vec3d(vec3d2.x, 0.0D, vec3d2.z);

	            if (vec3d2.dotProduct(vec3d1) < 0.0D)
	            {
	                return true;
	            }
	        }
	    }
			
		return false;
	}
	
	@SubscribeEvent
	public void onEvent(LivingUpdateEvent event)
	{
		EntityLivingBase entity = event.getEntityLiving();
		if (entity instanceof EntityPlayer)
		{
			EntityPlayer player =(EntityPlayer) entity;
			if (player.getActiveItemStack().getItem() instanceof ItemCustomShield)
			{
				AttributeModifier KRmodifier = new AttributeModifier(ItemCustomShield.knockbackModifierUUID, "shield_knockback_adjustment", 1, 0);
				if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.heavy, player.getActiveItemStack()) > 0)
				{
					if (!player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).hasModifier(KRmodifier))
					{
						player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).applyModifier(KRmodifier);
					}
				}
				else
				{
					player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).removeModifier(KRmodifier);
				}
				int l = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.weightless, player.getActiveItemStack());
				int w = ((ItemCustomShield)player.getActiveItemStack().getItem()).getWeight();
				float i = w - 2*l >= 0 ? w - 2*l : 0;
				double multiplyer = Math.pow(2, 1 - i) - 1;
				AttributeModifier speedmodifier = new AttributeModifier(ItemCustomShield.weightModifierUUID, "shield_speed_adjustment", multiplyer, 2);
				if (!player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).hasModifier(speedmodifier))
				{
					player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).applyModifier(speedmodifier);
				}
				else if (player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getModifier(ItemCustomShield.weightModifierUUID).getAmount() != multiplyer)
				{
					player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).removeModifier(ItemCustomShield.weightModifierUUID);
					player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).applyModifier(speedmodifier);
				}
			}
			else
			{
				player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).removeModifier(ItemCustomShield.knockbackModifierUUID);
				player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).removeModifier(ItemCustomShield.weightModifierUUID);
			}
		}
	}
		
	//Prevents arrows from hitting the blocking player
	@SubscribeEvent(priority=EventPriority.HIGHEST, receiveCanceled=true)
	public void onEvent(LivingAttackEvent event)
	{
		if (!event.getEntityLiving().getActiveItemStack().isEmpty() && event.getSource().getImmediateSource() instanceof EntityArrow)
		{
			if (canBlockDamageSource(event.getSource(), event.getEntityLiving()))
			{
				if (event.getEntityLiving() instanceof EntityPlayer)
				{
					EntityPlayer player = (EntityPlayer) event.getEntityLiving();
					player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8F, 0.8F + player.world.rand.nextFloat() * 0.4F);
					ItemStack copyBeforeUse = player.getActiveItemStack().copy();
					player.getActiveItemStack().damageItem(1, player);
					
					if (player instanceof EntityPlayerMP)
					{
						EntityPlayerMP playerMP = (EntityPlayerMP) player;
						try {
							grantAdvancement(playerMP);
						} catch (CommandException e)
						{
							e.printStackTrace();
						}
					}

			        if (player.getActiveItemStack().isEmpty())
			        {
			            EnumHand enumhand = player.getActiveHand();
			            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, copyBeforeUse, enumhand);

			            if (enumhand == EnumHand.MAIN_HAND)
			            {
			                player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
			            }
			            else
			            {
			                player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
			            }

			            player.resetActiveHand();
			            player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.8F, 0.8F + player.world.rand.nextFloat() * 0.4F);
			        }
				}
				event.setCanceled(true);
			}
		}
	}
	
	public void grantAdvancement(EntityPlayerMP playerMP) throws CommandException
	{
		AdvancementProgress progress = playerMP.getAdvancements().getProgress(AdvancementCommand.findAdvancement(playerMP.getServer(), "minecraft:story/deflect_arrow"));
		for (String s : progress.getRemaningCriteria())
        {
			playerMP.getAdvancements().grantCriterion(AdvancementCommand.findAdvancement(playerMP.getServer(), "minecraft:story/deflect_arrow"), s);
        }
	}
}

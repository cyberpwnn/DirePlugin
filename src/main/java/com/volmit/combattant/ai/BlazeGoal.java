package com.volmit.combattant.ai;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import com.volmit.combattant.Gate;
import com.volmit.combattant.fx.TracerFireball;
import com.volmit.volume.lang.collections.GList;
import com.volmit.volume.math.M;

public class BlazeGoal extends GOAL
{
	@Override
	public void onHurt(Location from, LivingEntity src, LivingEntity c, double damage)
	{
		moveAwayFromXZ(c, from, Gate.AI_GOAL_BLAZE_FLEE_ON_DAMAGE_SPEED);
		move(c, Vector.getRandom().subtract(Vector.getRandom()).normalize().setY(0));
	}

	@Override
	public void onSoundDiscovered(Location near, Location far, GList<Location> sounds, LivingEntity c, GList<LivingEntity> nearbyEntities, GList<LivingEntity> nearbyEntitiesLOS)
	{
		if(M.r(Gate.AI_GOAL_BLAZE_INITIATE_CHANCE) && !nearbyEntitiesLOS.isEmpty())
		{
			attack(c, nearbyEntitiesLOS);
		}

		hover(c);
	}

	private void hover(LivingEntity c)
	{
		if(heightOffGround(c) < Gate.AI_GOAL_BLAZE_HOVER_HEIGHT)
		{
			ascend(c, Gate.AI_GOAL_BLAZE_HOVER_ASCEND_SPEED);
		}
	}

	private void firingAt(LivingEntity c, LivingEntity t)
	{
		target(c, t);
		double d = c.getEyeLocation().distance(t.getEyeLocation());
		lookAt(c, t);

		if(d > Gate.AI_GOAL_BLAZE_PRIME_RANGE)
		{
			moveCloserToXZ(c, t.getEyeLocation(), Gate.AI_GOAL_BLAZE_APPROACH_SPEED);
			speak(c, Sound.ENTITY_BLAZE_AMBIENT, 0.5f);
		}

		else
		{
			moveAwayFromXZ(c, t.getEyeLocation(), Gate.AI_GOAL_BLAZE_SEPERATION_SPEED);
		}
	}

	private void attack(LivingEntity c, GList<LivingEntity> nearbyEntitiesLOS)
	{
		LivingEntity le = closest(c, nearbyEntitiesLOS);

		if(M.r(Gate.AI_GOAL_BLAZE_ATTACK_TARGET_RANDOM_CHANCE))
		{
			le = nearbyEntitiesLOS.pickRandom();
		}

		if(M.r(Gate.AI_GOAL_BLAZE_STRAFE_TARGET_CHANCE) && le != null)
		{
			move(c, strafe(c, le.getEyeLocation()));
		}

		firingAt(c, le);
		new TracerFireball(c, le.getEyeLocation()).fireTicks(Gate.AI_GOAL_BLAZE_TRACER_FIRE_TICKS).speed(Gate.AI_GOAL_BLAZE_TRACER_SPEED).gforce(Gate.AI_GOAL_BLAZE_TRACER_GFORCE).fire();
	}

	@Override
	public double getListeningPower(LivingEntity c)
	{
		return Gate.AI_GOAL_BLAZE_LISTENING_POWER;
	}

	@Override
	public void onPityTick(LivingEntity c)
	{

	}
}

package com.volmit.combattant.control;

import static com.volmit.combattant.Gate.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.volmit.combattant.object.ValueControl;
import com.volmit.combattant.services.HydrationSVC;
import com.volmit.combattant.services.StaminaSVC;
import com.volmit.volume.bukkit.U;
import com.volmit.volume.bukkit.pawn.IPawn;
import com.volmit.volume.bukkit.pawn.Tick;
import com.volmit.volume.bukkit.task.TICK;
import com.volmit.volume.math.M;

public class StaminaController implements IPawn
{
	@Tick()
	public void regenStamina()
	{
		if(TICK.tick % STAMINA_GEN_TICK_INTERVAL == 0)
		{
			for(Player i : Bukkit.getServer().getOnlinePlayers())
			{
				ValueControl s = U.getService(StaminaSVC.class).get(i);

				if(i.hasPotionEffect(PotionEffectType.ABSORPTION) && s.getMax() < MAX_STAMINA_ABSORBTION)
				{
					s.setMax(s.getMax() + 250);
				}

				if(i.hasPotionEffect(PotionEffectType.POISON) || i.hasPotionEffect(PotionEffectType.WITHER))
				{
					if(M.r(0.25))
					{
						s.setMax(s.getMax() - 550);
					}
				}

				if(s.getMax() < MIN_STAMINA)
				{
					s.setMax(MIN_STAMINA);
				}

				if(s.getMax() > MAX_STAMINA)
				{
					s.setMax(s.getMax() - 100);
				}

				if(s.getMax() < MAX_STAMINA)
				{
					s.setMax(s.getMax() + 100);
				}

				if(Math.abs(s.getMax() - MAX_STAMINA) < 120)
				{
					s.setMax(MAX_STAMINA);
				}

				// i.sendMessage(F.repeat("\n ", 20));
				//
				// for(double j : s.getRates().k())
				// {
				// if(s.getRates().get(j) < 0)
				// {
				// i.sendMessage((j > 0 ? C.DARK_GREEN + "+" : C.DARK_RED) + "" + F.f(j, 2) +
				// C.GRAY + " -| ");
				// }
				// }
				// for(double j : s.getRates().k())
				// {
				// if(s.getRates().get(j) >= 0)
				// {
				// i.sendMessage((j > 0 ? C.GREEN + "+" : C.RED) + "" + F.f(j, 2) + C.GRAY + "
				// -> " + C.WHITE + F.f(s.rates.get(j)));
				// }
				// }
				//
				// i.sendMessage(C.GOLD + "Stamina: " + C.WHITE + F.f((int) s.current) + C.GRAY
				// + " / " + C.WHITE + F.f((int) s.getMax()) + " " + (s.rate > 0 ? C.GREEN + "+"
				// : C.RED) + "" + F.f(s.rate, 2));

				if(s.getCurrent() / s.getMax() < 0.48 && Math.abs(s.rate) < 1)
				{
					double b = 0;
					boolean sat = false;
					boolean food = false;

					if(i.getSaturation() >= STAMINA_SATURATION_CONSUME)
					{
						b += STAMINA_SATURATION_GEN;
						sat = true;
					}

					else if(i.getFoodLevel() > STAMINA_MIN_FOOD_LEVEL)
					{
						food = true;
						b += STAMINA_FOOD_GEN;
					}

					s.rate(((STAMINA_GEN_BASE + b) * i.getFoodLevel()), STAMINA_GEN_OVER_TICKS);

					if(sat)
					{
						i.setSaturation((float) (i.getSaturation() - STAMINA_SATURATION_CONSUME));
					}

					if(food)
					{
						i.setFoodLevel(i.getFoodLevel() - 1);
						i.setSaturation(10);
					}
				}
			}
		}

		for(Player i : Bukkit.getServer().getOnlinePlayers())
		{
			ValueControl s = U.getService(StaminaSVC.class).get(i);

			if(i.isHandRaised())
			{
				ItemStack is = i.getInventory().getItemInMainHand();
				ItemStack io = i.getInventory().getItemInOffHand();

				if(is != null && is.getType().equals(Material.BOW))
				{
					s.rate(-STAMINA_DRAIN_BOW_DRAW, 4);
				}

				if(io != null && io.getType().equals(Material.SHIELD))
				{
					s.rate(-STAMINA_DRAIN_SHIELD_ARMED, 4);
				}
			}
			if(s.getCurrent() < 250)
			{
				i.setSprinting(false);
				i.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 55, 0, false, false));
			}
		}
	}

	@EventHandler
	public void on(BlockBreakEvent e)
	{
		ValueControl s = U.getService(StaminaSVC.class).get(e.getPlayer());
		s.rate(-STAMINA_DRAIN_BREAK_PLACE, STAMINA_DRAIN_BREAK_PLACE_TICKS);

		if(e.getPlayer().hasPotionEffect(PotionEffectType.FAST_DIGGING))
		{
			s.rate(-STAMINA_DRAIN_BREAK_PLACE, STAMINA_DRAIN_BREAK_PLACE_TICKS);
		}
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent e)
	{
		if(e.getItem().getType().equals(Material.GOLDEN_APPLE))
		{
			ValueControl v = U.getService(HydrationSVC.class).get(e.getPlayer());
			v.rate(STAMINA_GAIN_GAPPLE, STAMINA_GAIN_GAPPLE_TICKS);

			if(e.getPlayer().getFireTicks() > 0)
			{
				e.getPlayer().setFireTicks(0);
			}
		}
	}

	@EventHandler
	public void on(BlockPlaceEvent e)
	{
		ValueControl s = U.getService(StaminaSVC.class).get(e.getPlayer());
		s.rate(-STAMINA_DRAIN_BREAK_PLACE, STAMINA_DRAIN_BREAK_PLACE_TICKS);
	}

	@EventHandler
	public void on(ProjectileLaunchEvent e)
	{
		if(e.getEntity() instanceof Arrow)
		{
			((Arrow) e.getEntity()).setKnockbackStrength(0);
		}

		if(e.getEntity() instanceof Arrow && ((Projectile) e.getEntity()).getShooter() instanceof Player)
		{
			ValueControl s = U.getService(StaminaSVC.class).get((Player) ((Projectile) e.getEntity()).getShooter());

			if(s.getCurrent() < 75)
			{
				((Arrow) e.getEntity()).setCritical(false);
				((Arrow) e.getEntity()).setVelocity(((Arrow) e.getEntity()).getVelocity().clone().multiply(0.15).clone().add(new Vector(0, -0.15, 0)));
			}
		}
	}

	@EventHandler
	public void on(EntityDamageByEntityEvent e)
	{
		if(e.getEntity().getType().equals(EntityType.PLAYER))
		{
			if(e.getDamage() > 0)
			{
				ValueControl s = U.getService(StaminaSVC.class).get((Player) e.getEntity());
				s.rate(-STAMINA_DRAIN_ON_DAMAGED * e.getDamage(), STAMINA_DRAIN_ON_DAMAGED_TICKS);
				s.rate(-STAMINA_DRAIN_ON_DAMAGED_PERSIST * e.getDamage(), STAMINA_DRAIN_ON_DAMAGED_PERSIST_TICKS);

				if(s.getCurrent() > STAMINA_DRAIN_ON_DAMAGED * e.getDamage() * STAMINA_DRAIN_ON_DAMAGED_TICKS)
				{
					e.setDamage(e.getDamage() / STAMINA_ON_DAMAGED_REDUCTION);
				}
			}
		}

		if(e.getDamager().getType().equals(EntityType.PLAYER))
		{
			if(e.getDamage() > 0)
			{
				ValueControl s = U.getService(StaminaSVC.class).get((Player) e.getDamager());
				s.rate(-STAMINA_DRAIN_ON_DAMAGE * e.getDamage(), STAMINA_DRAIN_ON_DAMAGE_TICKS);
				s.rate(-STAMINA_DRAIN_ON_DAMAGE_PERSIST * e.getDamage(), STAMINA_DRAIN_ON_DAMAGE_PERSIST_TICKS);

				if(s.getCurrent() > STAMINA_DRAIN_ON_DAMAGE * e.getDamage() * STAMINA_DRAIN_ON_DAMAGE_TICKS)
				{
					e.setDamage(e.getDamage() * STAMINA_ON_DAMAGE_MULTIPLIER);
				}
			}
		}
	}

	@EventHandler
	public void on(FoodLevelChangeEvent e)
	{
		if(e.getEntity() instanceof Player)
		{
			ValueControl s = U.getService(StaminaSVC.class).get((Player) e.getEntity());
			int from = ((Player) e.getEntity()).getFoodLevel();
			int to = e.getFoodLevel();
			int change = to - from;
			s.rate(STAMINA_FOLLOW_FOOD_MULTIPLIER * change, STAMINA_FOLLOW_FOOD_TICKS);
			s.rate(STAMINA_FOLLOW_FOOD_MULTIPLIER_BURST * change, STAMINA_FOLLOW_FOOD_BURST_TICKS);

			if(change > 0)
			{
				ValueControl ss = U.getService(HydrationSVC.class).get((Player) e.getEntity());
				ss.rate(STAMINA_GAIN_FOOD_TRICKLE, STAMINA_GAIN_FOOD_TRICKLE_TICKS_MULTIPLIER * change);
			}
		}
	}

	@EventHandler
	public void on(EntityDamageEvent e)
	{
		if(e.getEntity() instanceof Player)
		{
			Player p = (Player) e.getEntity();
			ValueControl s = U.getService(StaminaSVC.class).get(p);
			s.rate(-STAMINA_DRAIN_ON_DAMAGED * e.getDamage(), STAMINA_DRAIN_ON_DAMAGED_TICKS);
			s.rate(-STAMINA_DRAIN_ON_DAMAGED_PERSIST * e.getDamage(), STAMINA_DRAIN_ON_DAMAGED_PERSIST_TICKS);

			if(s.getCurrent() > STAMINA_DRAIN_ON_DAMAGED * e.getDamage() * STAMINA_DRAIN_ON_DAMAGED_TICKS)
			{
				e.setDamage(e.getDamage() / STAMINA_ON_DAMAGED_REDUCTION);
			}

			if(e.getCause().equals(DamageCause.FIRE) || e.getCause().equals(DamageCause.FIRE_TICK))
			{
				ValueControl h = U.getService(HydrationSVC.class).get(p);
				h.rate(-100, 2);
			}
		}
	}

	@EventHandler
	public void on(PlayerMoveEvent e)
	{
		if(e.getFrom().getX() == e.getTo().getX() && e.getFrom().getZ() == e.getTo().getZ())
		{
			return;
		}

		if(e.getPlayer().isSprinting())
		{
			ValueControl s = U.getService(StaminaSVC.class).get(e.getPlayer());
			s.rate(-STAMINA_SPRINT_DRAIN, STAMINA_SPRINT_DRAIN_TICKS);
		}
	}
}

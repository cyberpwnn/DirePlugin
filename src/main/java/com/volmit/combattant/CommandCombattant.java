package com.volmit.combattant;

import org.bukkit.inventory.ItemStack;

import com.volmit.volume.bukkit.command.Command;
import com.volmit.volume.bukkit.command.PawnCommand;
import com.volmit.volume.bukkit.command.VolumeSender;
import com.volmit.volume.bukkit.util.text.C;
import com.volmit.volume.lang.format.F;

public class CommandCombattant extends PawnCommand
{
	@Command
	private CommandReload reload;

	@Command
	private CommandWeight weight;

	public CommandCombattant()
	{
		super("combattant", "combat", "comb", "cbt", "cb");
	}

	@Override
	public boolean handle(VolumeSender sender, String[] args)
	{
		@SuppressWarnings("deprecation")
		ItemStack hand = sender.player().getItemInHand();

		String s = "";

		if(hand != null)
		{
			s += "Hand: " + C.WHITE + F.f(Combattant.inst.main.weightController.getWeight(hand)) + " " + C.GRAY;
		}

		s += "Total: " + C.WHITE + F.f(Combattant.inst.main.weightController.getWeight(sender.player().getInventory()));

		sender.sendMessage(s);

		return true;
	}
}

package com.earth2me.essentials;

import static com.earth2me.essentials.I18n._;
import com.earth2me.essentials.commands.NotEnoughArgumentsException;
import java.io.File;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class Worth implements IConf
{
	private static final Logger logger = Logger.getLogger("Minecraft");
	private final EssentialsConf config;

	public Worth(File dataFolder)
	{
		config = new EssentialsConf(new File(dataFolder, "worth.yml"));
		config.setTemplateName("/worth.yml");
		config.load();
	}

	public BigDecimal getPrice(ItemStack itemStack)
	{
		String itemname = itemStack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");
		BigDecimal result;
		result = config.getBigDecimal("worth." + itemname + "." + itemStack.getDurability(), BigDecimal.ONE.negate());
		if (result.signum() < 0)
		{
			result = config.getBigDecimal("worth." + itemname + ".0", BigDecimal.ONE.negate());
		}
		if (result.signum() < 0)
		{
			result = config.getBigDecimal("worth." + itemname, BigDecimal.ONE.negate());
		}
		if (result.signum() < 0)
		{
			result = config.getBigDecimal("worth-" + itemStack.getTypeId(), BigDecimal.ONE.negate());
		}
		if (result.signum() < 0)
		{
			return null;
		}
		return result;
	}

	public int getAmount(IEssentials ess, User user, ItemStack is, String[] args, boolean isBulkSell) throws Exception
	{
		if (is == null || is.getType() == Material.AIR)
		{
			throw new Exception(_("itemSellAir"));
		}
		int id = is.getTypeId();
		int amount = 0;

		if (args.length > 1)
		{
			try {
				amount = Integer.parseInt(args[1].replaceAll("[^0-9]", ""));
			}
			catch (NumberFormatException ex) {
				throw new NotEnoughArgumentsException(ex);
			}
			if (args[1].startsWith("-"))
			{
				amount = -amount;
			}
		}

		boolean stack = args.length > 1 && args[1].endsWith("s");
		boolean requireStack = ess.getSettings().isTradeInStacks(id);

		if (requireStack && !stack)
		{
			throw new Exception(_("itemMustBeStacked"));
		}

		int max = 0;
		for (ItemStack s : user.getInventory().getContents())
		{
			if (s == null || !s.isSimilar(is))
			{
				continue;
			}
			max += s.getAmount();
		}

		if (stack)
		{
			amount *= is.getType().getMaxStackSize();
		}
		if (amount < 1)
		{
			amount += max;
		}

		if (requireStack)
		{
			amount -= amount % is.getType().getMaxStackSize();
		}
		if (amount > max || amount < 1)
		{
			if (!isBulkSell)
			{
				user.sendMessage(_("itemNotEnough2"));
				user.sendMessage(_("itemNotEnough3"));
				throw new Exception(_("itemNotEnough1"));
			}
			else
			{
				return amount;
			}
		}

		return amount;
	}

	public void setPrice(ItemStack itemStack, double price)
	{
		if (itemStack.getType().getData() == null)
		{
			config.setProperty("worth." + itemStack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", ""), price);
		}
		else
		{
			// Bukkit-bug: getDurability still contains the correct value, while getData().getData() is 0.
			config.setProperty("worth." + itemStack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "") + "." + itemStack.getDurability(), price);
		}
		config.removeProperty("worth-" + itemStack.getTypeId());
		config.save();
	}

	@Override
	public void reloadConfig()
	{
		config.load();
	}
}

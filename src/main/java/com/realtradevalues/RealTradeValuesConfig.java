package com.realtradevalues;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup(RealTradeValuesConfig.CONFIG_GROUP)
public interface RealTradeValuesConfig extends Config
{
	String CONFIG_GROUP = "realtradevalues";

	@ConfigItem(
		position = 1,
		keyName = "thirdAgePickaxe",
		name = "3rd Age pickaxe",
		description = "Value in billions of GP (e.g. 10.0 for 10B GP)"
	)
	@Units("B")
	default double thirdAgePickaxe()
	{
		return 10.0;
	}

	@ConfigItem(
		position = 2,
		keyName = "thirdAgeAxe",
		name = "3rd Age axe",
		description = "Value in billions of GP (e.g. 5.0 for 5B GP)"
	)
	@Units("B")
	default double thirdAgeAxe()
	{
		return 5.0;
	}

	@ConfigItem(
		position = 3,
		keyName = "thirdAgeFellingAxe",
		name = "3rd Age felling axe",
		description = "Value in billions of GP (e.g. 5.0 for 5B GP)"
	)
	@Units("B")
	default double thirdAgeFellingAxe()
	{
		return 5.0;
	}

	@ConfigItem(
		position = 4,
		keyName = "thirdAgeDruidicRobeTop",
		name = "3rd Age druidic robe top",
		description = "Value in billions of GP (e.g. 3.5 for 3.5B GP)"
	)
	@Units("B")
	default double thirdAgeDruidicRobeTop()
	{
		return 3.5;
	}

	@ConfigItem(
		position = 5,
		keyName = "thirdAgeDruidicRobeBottoms",
		name = "3rd Age druidic robe bottoms",
		description = "Value in billions of GP (e.g. 3.5 for 3.5B GP)"
	)
	@Units("B")
	default double thirdAgeDruidicRobeBottoms()
	{
		return 3.5;
	}
}

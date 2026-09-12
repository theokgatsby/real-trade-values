package com.realtradevalues;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RealTradeValuesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RealTradeValuesPlugin.class);
		RuneLite.main(args);
	}
}

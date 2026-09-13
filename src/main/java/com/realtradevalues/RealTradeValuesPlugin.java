package com.realtradevalues;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.QuantityFormatter;

@Slf4j
@PluginDescriptor(
	name = "Real Trade Values",
	description = "Shows actual trade values when exceeding the 2.1B integer limit and allows custom 3rd age item values",
	tags = {"trade", "value", "prices", "lots"}
)
public class RealTradeValuesPlugin extends Plugin
{
	private static final double BILLION = 1_000_000_000.0;
	private static final int TRADE_CONTAINER_ID = 90;
	private static final int TRADE_OTHER_CONTAINER_ID = 91;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private RealTradeValuesConfig config;

	@Provides
	RealTradeValuesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RealTradeValuesConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Real Trade Values started!");
		clientThread.invokeLater(this::updateTradeWidgets);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Real Trade Values stopped!");
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (containerId == TRADE_CONTAINER_ID || containerId == TRADE_OTHER_CONTAINER_ID)
		{
			updateTradeWidgets();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		int groupId = event.getGroupId();
		if (groupId == InterfaceID.TRADEMAIN || groupId == InterfaceID.TRADECONFIRM)
		{
			updateTradeWidgets();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		updateTradeWidgets();
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		updateTradeWidgets();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (RealTradeValuesConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			updateTradeWidgets();
		}
	}

	public void updateTradeWidgets()
	{
		Widget yourOfferHeader = client.getWidget(InterfaceID.Trademain.YOUR_OFFER_HEADER);
		Widget otherOfferHeader = client.getWidget(InterfaceID.Trademain.OTHER_OFFER_HEADER);
		Widget youWillGive = client.getWidget(InterfaceID.Tradeconfirm.YOU_WILL_GIVE);
		Widget youWillReceive = client.getWidget(InterfaceID.Tradeconfirm.YOU_WILL_RECEIVE);

		if (yourOfferHeader == null && youWillGive == null)
		{
			return;
		}

		if (yourOfferHeader != null && !yourOfferHeader.isHidden())
		{
			ItemContainer yourContainer = client.getItemContainer(TRADE_CONTAINER_ID);
			long yourValue = calculateTradeValue(yourContainer);
			updateWidgetText(yourOfferHeader, "Your offer:", hasItems(yourContainer), yourValue);
		}

		if (otherOfferHeader != null && !otherOfferHeader.isHidden())
		{
			ItemContainer otherContainer = client.getItemContainer(TRADE_OTHER_CONTAINER_ID);
			long otherValue = calculateTradeValue(otherContainer);
			String base = getHeaderBase(otherOfferHeader.getText());
			updateWidgetText(otherOfferHeader, base, hasItems(otherContainer), otherValue);
		}

		if (youWillGive != null && !youWillGive.isHidden())
		{
			ItemContainer yourContainer = client.getItemContainer(TRADE_CONTAINER_ID);
			long yourValue = calculateTradeValue(yourContainer);
			updateWidgetText(youWillGive, "You are about to give:", hasItems(yourContainer), yourValue);
		}

		if (youWillReceive != null && !youWillReceive.isHidden())
		{
			ItemContainer otherContainer = client.getItemContainer(TRADE_OTHER_CONTAINER_ID);
			long otherValue = calculateTradeValue(otherContainer);
			updateWidgetText(youWillReceive, "In return you will receive:", hasItems(otherContainer), otherValue);
		}
	}

	private String getHeaderBase(String currentText)
	{
		if (currentText == null || currentText.isEmpty())
		{
			return "";
		}

		int brIndex = currentText.indexOf("<br>");
		if (brIndex != -1)
		{
			return currentText.substring(0, brIndex);
		}

		int parenIndex = currentText.indexOf("(");
		if (parenIndex != -1)
		{
			return currentText.substring(0, parenIndex).trim();
		}

		return currentText;
	}

	private void updateWidgetText(Widget widget, String base, boolean hasItems, long totalValue)
	{
		if (widget == null || base == null || base.isEmpty())
		{
			return;
		}

		String newText;
		if (hasItems)
		{
			newText = base + "<br>(<col=ffffff>" + QuantityFormatter.formatNumber(totalValue) + "</col>)";
		}
		else
		{
			newText = base;
		}

		if (!newText.equals(widget.getText()))
		{
			widget.setText(newText);
		}
	}

	public boolean hasItems(ItemContainer container)
	{
		if (container == null)
		{
			return false;
		}

		for (Item item : container.getItems())
		{
			if (item != null && item.getId() > 0 && item.getQuantity() > 0)
			{
				return true;
			}
		}

		return false;
	}

	public long calculateTradeValue(ItemContainer container)
	{
		if (container == null)
		{
			return 0L;
		}

		long totalValue = 0L;
		for (Item item : container.getItems())
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}

			long price = getItemPrice(item.getId());
			totalValue += (long) item.getQuantity() * price;
		}

		return totalValue;
	}

	public long getItemPrice(int rawItemId)
	{
		int itemId = itemManager.canonicalize(rawItemId);

		switch (itemId)
		{
			case ItemID._3A_PICKAXE:
				if (config.thirdAgePickaxe() > 0)
				{
					return (long) Math.round(config.thirdAgePickaxe() * BILLION);
				}
				break;

			case ItemID._3A_AXE:
				if (config.thirdAgeAxe() > 0)
				{
					return (long) Math.round(config.thirdAgeAxe() * BILLION);
				}
				break;

			case ItemID._3A_AXE_2H:
				if (config.thirdAgeFellingAxe() > 0)
				{
					return (long) Math.round(config.thirdAgeFellingAxe() * BILLION);
				}
				break;

			case ItemID._3A_DRUIDIC_TOP:
				if (config.thirdAgeDruidicRobeTop() > 0)
				{
					return (long) Math.round(config.thirdAgeDruidicRobeTop() * BILLION);
				}
				break;

			case ItemID._3A_DRUIDIC_BOTTOMS:
				if (config.thirdAgeDruidicRobeBottoms() > 0)
				{
					return (long) Math.round(config.thirdAgeDruidicRobeBottoms() * BILLION);
				}
				break;

			case ItemID.COINS:
				return 1L;

			case ItemID.PLATINUM:
				return 1000L;
		}

		int gePrice = itemManager.getItemPrice(itemId);
		return Math.max(0L, (long) gePrice);
	}
}

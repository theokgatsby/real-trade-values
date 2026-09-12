package com.realtradevalues;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.QuantityFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RealTradeValuesTest
{
	@Mock
	@Bind
	private Client client;

	@Mock
	@Bind
	private ClientThread clientThread;

	@Mock
	@Bind
	private ItemManager itemManager;

	@Mock
	@Bind
	private RealTradeValuesConfig config;

	@Inject
	private RealTradeValuesPlugin plugin;

	@Before
	public void before()
	{
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);

		when(config.thirdAgePickaxe()).thenReturn(10.0);
		when(config.thirdAgeAxe()).thenReturn(5.0);
		when(config.thirdAgeFellingAxe()).thenReturn(5.0);
		when(config.thirdAgeDruidicRobeTop()).thenReturn(3.5);
		when(config.thirdAgeDruidicRobeBottoms()).thenReturn(3.5);

		when(itemManager.canonicalize(ItemID._3A_PICKAXE)).thenReturn(ItemID._3A_PICKAXE);
		when(itemManager.canonicalize(ItemID._3A_AXE)).thenReturn(ItemID._3A_AXE);
		when(itemManager.canonicalize(ItemID._3A_AXE_2H)).thenReturn(ItemID._3A_AXE_2H);
		when(itemManager.canonicalize(ItemID._3A_DRUIDIC_TOP)).thenReturn(ItemID._3A_DRUIDIC_TOP);
		when(itemManager.canonicalize(ItemID._3A_DRUIDIC_BOTTOMS)).thenReturn(ItemID._3A_DRUIDIC_BOTTOMS);
		when(itemManager.canonicalize(ItemID.COINS)).thenReturn(ItemID.COINS);
		when(itemManager.canonicalize(ItemID.PLATINUM)).thenReturn(ItemID.PLATINUM);
		when(itemManager.canonicalize(4151)).thenReturn(4151);
		when(itemManager.getItemPrice(4151)).thenReturn(1_500_000);
	}

	@Test
	public void testConfigDefaults()
	{
		RealTradeValuesConfig defaultConfig = new RealTradeValuesConfig() {};
		assertEquals(10.0, defaultConfig.thirdAgePickaxe(), 0.001);
		assertEquals(5.0, defaultConfig.thirdAgeAxe(), 0.001);
		assertEquals(5.0, defaultConfig.thirdAgeFellingAxe(), 0.001);
		assertEquals(3.5, defaultConfig.thirdAgeDruidicRobeTop(), 0.001);
		assertEquals(3.5, defaultConfig.thirdAgeDruidicRobeBottoms(), 0.001);
	}

	@Test
	public void testGetItemPriceConfiguredItems()
	{
		assertEquals(10_000_000_000L, plugin.getItemPrice(ItemID._3A_PICKAXE));
		assertEquals(5_000_000_000L, plugin.getItemPrice(ItemID._3A_AXE));
		assertEquals(5_000_000_000L, plugin.getItemPrice(ItemID._3A_AXE_2H));
		assertEquals(3_500_000_000L, plugin.getItemPrice(ItemID._3A_DRUIDIC_TOP));
		assertEquals(3_500_000_000L, plugin.getItemPrice(ItemID._3A_DRUIDIC_BOTTOMS));
	}

	@Test
	public void testGetItemPriceCustomDecimalValues()
	{
		when(config.thirdAgePickaxe()).thenReturn(10.1);
		when(config.thirdAgeDruidicRobeTop()).thenReturn(3.75);

		assertEquals(10_100_000_000L, plugin.getItemPrice(ItemID._3A_PICKAXE));
		assertEquals(3_750_000_000L, plugin.getItemPrice(ItemID._3A_DRUIDIC_TOP));
	}

	@Test
	public void testGetItemPriceCurrencies()
	{
		assertEquals(1L, plugin.getItemPrice(ItemID.COINS));
		assertEquals(1000L, plugin.getItemPrice(ItemID.PLATINUM));
	}

	@Test
	public void testGetItemPriceRegularGeItem()
	{
		assertEquals(1_500_000L, plugin.getItemPrice(4151));
	}

	@Test
	public void testCalculateTradeValueEmpty()
	{
		assertEquals(0L, plugin.calculateTradeValue(null));

		ItemContainer container = mock(ItemContainer.class);
		when(container.getItems()).thenReturn(new Item[0]);
		assertEquals(0L, plugin.calculateTradeValue(container));
	}

	@Test
	public void testCalculateTradeValueOverMaxCash()
	{
		// 1x 3rd Age pickaxe (10B) + 1x 3rd Age axe (5B) + 5,000,000 platinum tokens (5B) + 100M coins (100M)
		// Total: 20,100,000,000 GP
		Item pickaxe = new Item(ItemID._3A_PICKAXE, 1);
		Item axe = new Item(ItemID._3A_AXE, 1);
		Item platTokens = new Item(ItemID.PLATINUM, 5_000_000);
		Item coins = new Item(ItemID.COINS, 100_000_000);

		ItemContainer container = mock(ItemContainer.class);
		when(container.getItems()).thenReturn(new Item[]{pickaxe, axe, platTokens, coins});

		long total = plugin.calculateTradeValue(container);
		assertEquals(20_100_000_000L, total);
		assertEquals("20,100,000,000", QuantityFormatter.formatNumber(total));
	}

	@Test
	public void testHasItems()
	{
		assertFalse(plugin.hasItems(null));

		ItemContainer emptyContainer = mock(ItemContainer.class);
		when(emptyContainer.getItems()).thenReturn(new Item[0]);
		assertFalse(plugin.hasItems(emptyContainer));

		ItemContainer invalidContainer = mock(ItemContainer.class);
		when(invalidContainer.getItems()).thenReturn(new Item[]{new Item(-1, 0)});
		assertFalse(plugin.hasItems(invalidContainer));

		ItemContainer validContainer = mock(ItemContainer.class);
		when(validContainer.getItems()).thenReturn(new Item[]{new Item(ItemID.COINS, 1)});
		assertTrue(plugin.hasItems(validContainer));
	}

	@Test
	public void testUpdateTradeWidgetsMainScreen()
	{
		Widget yourOffer = mock(Widget.class);
		Widget otherOffer = mock(Widget.class);

		when(client.getWidget(InterfaceID.Trademain.YOUR_OFFER_HEADER)).thenReturn(yourOffer);
		when(client.getWidget(InterfaceID.Trademain.OTHER_OFFER_HEADER)).thenReturn(otherOffer);

		when(yourOffer.isHidden()).thenReturn(false);
		when(yourOffer.getText()).thenReturn("Your offer:<br>(<col=ffffff>Lots! coins</col>)");

		when(otherOffer.isHidden()).thenReturn(false);
		when(otherOffer.getText()).thenReturn("Zezima's offer:<br>(<col=ffffff>Lots! coins</col>)");

		ItemContainer yourContainer = mock(ItemContainer.class);
		when(yourContainer.getItems()).thenReturn(new Item[]{new Item(ItemID._3A_PICKAXE, 1)});
		when(client.getItemContainer(InventoryID.TRADE)).thenReturn(yourContainer);

		ItemContainer otherContainer = mock(ItemContainer.class);
		when(otherContainer.getItems()).thenReturn(new Item[]{new Item(ItemID.PLATINUM, 3_000_000)});
		when(client.getItemContainer(InventoryID.TRADEOTHER)).thenReturn(otherContainer);

		plugin.updateTradeWidgets();

		verify(yourOffer).setText("Your offer:<br>(<col=ffffff>10,000,000,000</col>)");
		verify(otherOffer).setText("Zezima's offer:<br>(<col=ffffff>3,000,000,000</col>)");
	}

	@Test
	public void testUpdateTradeWidgetsConfirmScreen()
	{
		Widget youWillGive = mock(Widget.class);
		Widget youWillReceive = mock(Widget.class);

		when(client.getWidget(InterfaceID.Tradeconfirm.YOU_WILL_GIVE)).thenReturn(youWillGive);
		when(client.getWidget(InterfaceID.Tradeconfirm.YOU_WILL_RECEIVE)).thenReturn(youWillReceive);

		when(youWillGive.isHidden()).thenReturn(false);
		when(youWillGive.getText()).thenReturn("You are about to give:<br>(<col=ffffff>Lots! coins</col>)");

		when(youWillReceive.isHidden()).thenReturn(false);
		when(youWillReceive.getText()).thenReturn("In return you will receive:<br>(<col=ffffff>Lots! coins</col>)");

		ItemContainer yourContainer = mock(ItemContainer.class);
		when(yourContainer.getItems()).thenReturn(new Item[]{new Item(ItemID._3A_PICKAXE, 1)});
		when(client.getItemContainer(InventoryID.TRADE)).thenReturn(yourContainer);

		ItemContainer otherContainer = mock(ItemContainer.class);
		when(otherContainer.getItems()).thenReturn(new Item[]{new Item(ItemID._3A_AXE, 2)});
		when(client.getItemContainer(InventoryID.TRADEOTHER)).thenReturn(otherContainer);

		plugin.updateTradeWidgets();

		verify(youWillGive).setText("You are about to give:<br>(<col=ffffff>10,000,000,000</col>)");
		verify(youWillReceive).setText("In return you will receive:<br>(<col=ffffff>10,000,000,000</col>)");
	}

	@Test
	public void testUpdateTradeWidgetsNoUpdateIfTextAlreadyMatches()
	{
		Widget yourOffer = mock(Widget.class);
		when(client.getWidget(InterfaceID.Trademain.YOUR_OFFER_HEADER)).thenReturn(yourOffer);
		when(yourOffer.isHidden()).thenReturn(false);
		when(yourOffer.getText()).thenReturn("Your offer:<br>(<col=ffffff>10,000,000,000</col>)");

		ItemContainer yourContainer = mock(ItemContainer.class);
		when(yourContainer.getItems()).thenReturn(new Item[]{new Item(ItemID._3A_PICKAXE, 1)});
		when(client.getItemContainer(InventoryID.TRADE)).thenReturn(yourContainer);

		plugin.updateTradeWidgets();

		verify(yourOffer, never()).setText("Your offer:<br>(<col=ffffff>10,000,000,000</col>)");
	}
}

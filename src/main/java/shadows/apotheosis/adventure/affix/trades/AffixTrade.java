package shadows.apotheosis.adventure.affix.trades;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import shadows.apotheosis.adventure.affix.AffixHelper;
import shadows.apotheosis.adventure.loot.LootController;
import shadows.apotheosis.adventure.loot.LootRarity;
import shadows.apotheosis.village.wanderer.JsonTrade;
import shadows.placebo.json.PlaceboJsonReloadListener.TypeKeyedBase;

public class AffixTrade extends TypeKeyedBase<JsonTrade> implements JsonTrade {

	protected final boolean rare;

	public AffixTrade(int rarityOffset, boolean rare) {
		this.rare = rare;
	}

	@Override
	@Nullable
	public MerchantOffer getOffer(Entity pTrader, Random pRand) {
		if (!(pTrader.level instanceof ServerLevel)) return null;
		Player nearest = pTrader.level.getNearestPlayer(pTrader, 32);
		float luck = nearest != null ? nearest.getLuck() : 0;
		ItemStack affixItem = LootController.createRandomLootItem(pRand, null, luck, (ServerLevel) pTrader.level);
		affixItem.getTag().putBoolean("apoth_merchant", true);
		ItemStack stdItem = affixItem.copy();
		stdItem.setTag(null);
		LootRarity rarity = AffixHelper.getRarity(affixItem);
		ItemStack emeralds = new ItemStack(Items.EMERALD, 8 + rarity.ordinal() * 8);
		if (rarity.isAtLeast(LootRarity.MYTHIC)) {
			emeralds = new ItemStack(Items.EMERALD_BLOCK, 20 + (rarity == LootRarity.ANCIENT ? 30 : 0));
		}
		return new MerchantOffer(stdItem, emeralds, affixItem, 1, 100, 1);
	}

	@Override
	public boolean isRare() {
		return this.rare;
	}

}

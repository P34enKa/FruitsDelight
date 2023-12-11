package dev.xkmc.fruitsdelight.init.food;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.fruitsdelight.content.block.PassableLeavesBlock;
import dev.xkmc.fruitsdelight.init.FruitsDelight;
import dev.xkmc.fruitsdelight.init.data.PlantDataEntry;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

public enum FDTrees implements PlantDataEntry<FDTrees> {
	PEAR(() -> Blocks.BIRCH_LOG, FDTreeType.NORMAL, 3, 0.3f, false),
	HAWBERRY(() -> Blocks.SPRUCE_LOG, FDTreeType.NORMAL, 2, 0.3f, true),
	LYCHEE(() -> Blocks.JUNGLE_LOG, FDTreeType.NORMAL, 2, 0.3f, true),
	MANGO(() -> Blocks.JUNGLE_LOG, FDTreeType.NORMAL, 3, 0.3f, false),
	PERSIMMON(() -> Blocks.SPRUCE_LOG, FDTreeType.NORMAL, 3, 0.3f, false),
	PEACH(() -> Blocks.JUNGLE_LOG, FDTreeType.NORMAL, 3, 0.3f, false),
	ORANGE(() -> Blocks.OAK_LOG, FDTreeType.NORMAL, 3, 0.3f, false),
	APPLE(() -> Blocks.OAK_LOG, FDTreeType.NORMAL, str -> () -> Items.APPLE),
	MANGOSTEEN(() -> Blocks.OAK_LOG, FDTreeType.TALL, 3, 0.3f, false),
	;

	private final BlockEntry<PassableLeavesBlock> leaves;
	private final BlockEntry<SaplingBlock> sapling;
	private final Supplier<Item> fruit;
	private final Lazy<TreeConfiguration> treeConfig, treeConfigWild;

	public final ResourceKey<ConfiguredFeature<?, ?>> configKey, configKeyWild;
	public final ResourceKey<PlacedFeature> placementKey;

	public final Supplier<Block> log;
	public boolean genTree = false;

	FDTrees(Supplier<Block> log, FDTreeType height, Function<String, Supplier<Item>> items) {
		String name = name().toLowerCase(Locale.ROOT);
		this.log = log;
		this.treeConfig = Lazy.of(() -> buildTreeConfig(log, height, false));
		this.treeConfigWild = Lazy.of(() -> buildTreeConfig(log, height, true));
		this.configKey = ResourceKey.create(Registries.CONFIGURED_FEATURE,
				new ResourceLocation(FruitsDelight.MODID, "tree/" + name + "_tree"));
		this.configKeyWild = ResourceKey.create(Registries.CONFIGURED_FEATURE,
				new ResourceLocation(FruitsDelight.MODID, "tree/" + name + "_tree_wild"));
		this.placementKey = ResourceKey.create(Registries.PLACED_FEATURE,
				new ResourceLocation(FruitsDelight.MODID, "tree/" + name + "_tree"));

		leaves = FruitsDelight.REGISTRATE
				.block(name + "_leaves", p -> new PassableLeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)))
				.blockstate(this::buildLeavesModel)
				.loot((pvd, block) -> buildFruit(pvd, block, getSapling(), getFruit()))
				.tag(BlockTags.LEAVES, BlockTags.MINEABLE_WITH_HOE)
				.item().tag(ItemTags.LEAVES).build()
				.register();

		sapling = FruitsDelight.REGISTRATE.block(
						name + "_sapling", p -> new SaplingBlock(new TreeGrower(),
								BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
						))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models()
						.cross(ctx.getName(), pvd.modLoc("block/" + ctx.getName()))
						.renderType("cutout")))
				.tag(BlockTags.SAPLINGS)
				.item().model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("block/" + ctx.getName())))
				.tag(ItemTags.SAPLINGS).build()
				.register();

		fruit = items.apply(name);
	}

	FDTrees(Supplier<Block> log, FDTreeType height, int food, float sat, boolean fast) {
		this(log, height, name -> FruitsDelight.REGISTRATE
				.item(name, p -> new Item(p.food(food(food, sat, fast))))
				.transform(b -> PlantDataEntry.addFruitTags(name, b))
				.register());
		genTree = true;
	}


	public PassableLeavesBlock getLeaves() {
		return leaves.get();
	}

	public Item getFruit() {
		return fruit.get();
	}

	public SaplingBlock getSapling() {
		return sapling.get();
	}

	public void registerComposter() {
		ComposterBlock.COMPOSTABLES.put(getFruit(), 0.65f);
		ComposterBlock.COMPOSTABLES.put(getLeaves().asItem(), 0.3f);
		ComposterBlock.COMPOSTABLES.put(getSapling().asItem(), 0.3f);
	}

	public void registerConfigs(BootstapContext<ConfiguredFeature<?, ?>> ctx) {
		ctx.register(configKey, new ConfiguredFeature<>(Feature.TREE, treeConfig.get()));
		ctx.register(configKeyWild, new ConfiguredFeature<>(Feature.TREE, treeConfigWild.get()));
	}

	public void registerPlacements(BootstapContext<PlacedFeature> ctx) {
		ctx.register(placementKey, new PlacedFeature(
				ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(configKeyWild),
				VegetationPlacements.treePlacement(
						PlacementUtils.countExtra(0, 0.2F, 2), getSapling())));
	}

	@Override
	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}

	@Override
	public ResourceKey<PlacedFeature> getPlacementKey() {
		return placementKey;
	}

	private TreeConfiguration buildTreeConfig(Supplier<Block> log, FDTreeType height, boolean wild) {
		return height.build(log.get(), getLeaves(), wild);
	}

	private void buildLeavesModel(DataGenContext<Block, PassableLeavesBlock> ctx, RegistrateBlockstateProvider pvd) {
		pvd.getVariantBuilder(ctx.get())
				.forAllStatesExcept(state -> {
							String name = name().toLowerCase(Locale.ROOT) + "_" +
									state.getValue(PassableLeavesBlock.STATE).getSerializedName();
							return ConfiguredModel.builder()
									.modelFile(pvd.models().withExistingParent(name, "block/leaves")
											.texture("all", "block/" + name)).build();
						},
						LeavesBlock.DISTANCE, LeavesBlock.PERSISTENT, LeavesBlock.WATERLOGGED);
	}

	private static void buildFruit(RegistrateBlockLootTables pvd, Block block, Block sapling, Item fruit) {
		pvd.add(block, LootTable.lootTable().withPool(LootPool.lootPool().add(
				AlternativesEntry.alternatives(
						LootItem.lootTableItem(block)
								.when(MatchTool.toolMatches(ItemPredicate.Builder.item()
										.hasEnchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH,
												MinMaxBounds.Ints.atLeast(1))))),
						LootItem.lootTableItem(fruit)
								.when(LootItemBlockStatePropertyCondition
										.hasBlockStateProperties(block)
										.setProperties(StatePropertiesPredicate.Builder.properties()
												.hasProperty(PassableLeavesBlock.STATE, PassableLeavesBlock.State.FRUITS)))
								.apply(ApplyBonusCount.addUniformBonusCount(Enchantments.BLOCK_FORTUNE, 1)),
						LootItem.lootTableItem(sapling)
								.when(BonusLevelTableCondition.bonusLevelFlatChance(Enchantments.BLOCK_FORTUNE,
										1 / 20f, 1 / 16f, 1 / 12f, 1 / 10f))
				)
		).when(ExplosionCondition.survivesExplosion())));
	}

	private static FoodProperties food(int food, float sat, boolean fast) {
		var ans = new FoodProperties.Builder()
				.nutrition(food).saturationMod(sat);
		if (fast) ans.fast();
		return ans.build();
	}

	public static void register() {
	}

	private class TreeGrower extends AbstractTreeGrower {

		@Nullable
		@Override
		protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource rand, boolean large) {
			return configKey;
		}

	}

}

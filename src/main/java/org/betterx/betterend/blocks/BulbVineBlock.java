package org.betterx.betterend.blocks;



import org.betterx.betterend.registry.block.EndVineBlocks;
import org.betterx.betterend.registry.item.EndResourceItems;
import org.betterx.bclib.blocks.BaseVineBlock;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndItems;
import de.ambertation.wover.block.api.BlockProperties;
import de.ambertation.wover.block.api.BlockProperties.TripleShape;
import de.ambertation.wover.loot.api.LootLookupProvider;

// 26.3 split net.minecraft.advancements.criterion into .predicates (StatePropertiesPredicate,
// ...) and .triggers (every *Trigger).
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class BulbVineBlock extends BaseVineBlock {
    public BulbVineBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public boolean isValidBonemealTarget(
            LevelReader world,
            BlockPos pos,
            BlockState state,
            BonemealSource bonemealSource
    ) {
        return false;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        boolean canPlace = super.canSurvive(state, world, pos);
        return (state.is(this) && state.getValue(SHAPE) == TripleShape.BOTTOM)
                ? canPlace
                : canPlace && world.getBlockState(
                        pos.below()).is(this);
    }

    public static LootTable.Builder buildLoot(Block block, LootLookupProvider provider) {
        // 26.3 removed LootItemBlockStatePropertyCondition and folded block matching into MatchBlock,
        // which wraps an advancements BlockPredicate and therefore resolves the block into a HolderSet
        // eagerly - hence the block lookup taken from the provider. Semantically identical, but it
        // serializes as {"type": "minecraft:match_block", "blocks": ..., "state": {...}} instead of
        // {"condition": "minecraft:block_state_property", "block": ..., "properties": {...}}.
        final LootItemCondition.Builder bottom = MatchBlock.blockMatches(
                provider.blockLookup(),
                block,
                StatePropertiesPredicate.Builder
                        .properties()
                        .hasProperty(SHAPE, BlockProperties.TripleShape.BOTTOM)
        );

        // Every other vine in the mod takes its loot from VineBlockTrait, which is
        // dropWithSilktouchOrHoeOrShears - vanilla's rule that a vine only yields to shears or silk touch.
        // This block carries VineBlockTrait too, but the explicit LOOT_TABLE trait on its registration
        // overrides the trait's table, so before this gate bulb_vine was the one vine in either mod that
        // could be harvested bare-handed. The gate is the provider's own combined condition, so it stays
        // identical to what the trait would have produced.
        final LootItemCondition.Builder shearsOrHoeOrSilk = provider.shearsOrHoeSilkTouchCondition();

        return LootTable
                .lootTable()
                .withPool(
                        LootPool
                                .lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(LootItem.lootTableItem(EndResourceItems.GLOWING_BULB.asItem())
                                             .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                                )
                                .when(shearsOrHoeOrSilk)
                                .when(bottom)
                )
                .withPool(
                        LootPool
                                .lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(LootItem.lootTableItem(EndVineBlocks.BULB_VINE_SEED.asItem())
                                             .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                                             .when(ExplosionCondition.survivesExplosion())
                                             .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                                     provider.fortune(),
                                                     LootLookupProvider.VANILLA_LEAVES_SAPLING_CHANCES
                                             ))
                                )
                                .when(shearsOrHoeOrSilk)
                                .when(bottom.invert())
                );
    }
}

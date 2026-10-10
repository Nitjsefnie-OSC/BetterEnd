package org.betterx.betterend.blocks;



import org.betterx.betterend.registry.block.EndSaplingBlocks;
import org.betterx.betterend.registry.block.EndStoneBlocks;
import org.betterx.betterend.registry.item.EndResourceItems;
import de.ambertation.wover.sets.api.blocks.SlotType;

import org.betterx.bclib.blocks.UnderwaterPlantBlock;
import org.betterx.betterend.blocks.EndBlockProperties.HydraluxShape;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndItems;
import de.ambertation.wover.loot.api.LootLookupProvider;

// 26.3 split net.minecraft.advancements.criterion into .predicates (StatePropertiesPredicate,
// ...) and .triggers (every *Trigger).
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class HydraluxBlock extends UnderwaterPlantBlock {

    public static final EnumProperty<HydraluxShape> SHAPE = EndBlockProperties.HYDRALUX_SHAPE;

    public HydraluxBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateManager) {
        stateManager.add(SHAPE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockState down = world.getBlockState(pos.below());
        HydraluxShape shape = state.getValue(SHAPE);
        if (shape == HydraluxShape.FLOWER_BIG_TOP || shape == HydraluxShape.FLOWER_SMALL_TOP) {
            return down.is(this);
        } else if (shape == HydraluxShape.ROOTS) {
            return down.is(EndStoneBlocks.SULPHURIC_ROCK.getBlock(SlotType.SOURCE)) && world.getBlockState(pos.above())
                                                                                       .is(this);
        } else {
            return down.is(this) && world.getBlockState(pos.above()).is(this);
        }
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
    public boolean isBonemealSuccess(
            Level world,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            BonemealSource bonemealSource
    ) {
        return false;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public ItemStack getCloneItemStack(LevelReader levelReader, BlockPos blockPos, BlockState blockState, boolean includeData) {
        return new ItemStack(EndSaplingBlocks.HYDRALUX_SAPLING);
    }

    /**
     * The flower bottom drops 1-4 petals, the roots drop 1-2 saplings, everything else nothing; there is no
     * block item - replacing the block class's former {@code getDrops} override with a data-driven table.
     */
    public static LootTable.Builder buildLoot(Block block, LootLookupProvider provider) {
        return LootTable
                .lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ContextIntProviders.exactly(1))
                        .when(AnyOfCondition.anyOf(
                                shapeCond(block, provider, HydraluxShape.FLOWER_BIG_BOTTOM),
                                shapeCond(block, provider, HydraluxShape.FLOWER_SMALL_BOTTOM)))
                        .add(LootItem.lootTableItem(EndResourceItems.HYDRALUX_PETAL)
                                     .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 4)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ContextIntProviders.exactly(1))
                        .when(shapeCond(block, provider, HydraluxShape.ROOTS))
                        .add(LootItem.lootTableItem(EndSaplingBlocks.HYDRALUX_SAPLING)
                                     .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2)))));
    }

    /**
     * 26.3 removed {@code LootItemBlockStatePropertyCondition} and folded block matching into
     * {@link MatchBlock}, which wraps an advancements {@code BlockPredicate} and therefore resolves the
     * block into a {@code HolderSet} eagerly - hence the block lookup taken from the provider.
     * Semantically identical, but it serializes as
     * {@code {"type": "minecraft:match_block", "blocks": ..., "state": {...}}} instead of
     * {@code {"condition": "minecraft:block_state_property", "block": ..., "properties": {...}}}.
     */
    private static LootItemCondition.Builder shapeCond(
            Block block,
            LootLookupProvider provider,
            HydraluxShape shape
    ) {
        return MatchBlock.blockMatches(
                provider.blockLookup(),
                block,
                StatePropertiesPredicate.Builder.properties().hasProperty(SHAPE, shape)
        );
    }
}

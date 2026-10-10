package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.block.EndVineBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Sweeps every BetterEnd block that implements {@link BonemealableBlock} through the same
 * {@link BoneMealItem#growCrop} path a player's bone meal takes.
 * <p>
 * Each plant is placed on the first support (end stone, then every full-cube BetterEnd block, then a few
 * vanilla soils; below, above, or behind it for attached plants; dry, then submerged) where it
 * {@code canSurvive}. A plant that accepts bone meal there ({@code isValidBonemealTarget}) must then
 * change <i>something</i> - its own state, the blocks around it, or a dropped item - within
 * {@link #ATTEMPTS} applications, because otherwise the player's bone meal is consumed for nothing. It
 * must also never be destroyed outright: umbrella moss survives on any soil but used to turn into tall
 * umbrella moss, which only survives on jungle moss or mycelium, so bone meal on end stone deleted it.
 * <p>
 * Plants that decline bone meal ({@code isValidBonemealTarget == false}) are only logged; several do so
 * deliberately (bulb vine, tenanea flowers, the underwater decorations, the large amaranita).
 * <p>
 * #416 was reported as "bone meal does not work on filalux wings". The wings never implemented
 * {@link BonemealableBlock} in any version (they are a {@code BaseAttachedBlock} decoration, the same as on
 * 1.19 and 1.20); the filalux vine they belong to does, which {@link #filaluxVineGrowsWithBonemeal} pins.
 */
public class BonemealSweepGameTest {
    private static final BlockPos POS = new BlockPos(3, 3, 3);
    // The slowest roll is SmallJellyshroomBlock's flat 1/16 success chance; 200
    // applications leave a ~1 in 400,000 chance of a false "nothing happened".
    private static final int ATTEMPTS = 200;
    // Covers the whole arena interior around POS (the arena is 8 blocks tall, with its floor at y=0).
    private static final int MIN_XZ = 0;
    private static final int MAX_XZ = 6;
    private static final int MIN_Y = 1;
    private static final int MAX_Y = 7;
    private static final int SPAN_Y = MAX_Y - MIN_Y + 1;
    private static final int SPAN_Z = MAX_XZ - MIN_XZ + 1;
    /** Index of {@link #POS} in a {@link #snapshot}. */
    private static final int POS_INDEX = ((POS.getX() - MIN_XZ) * SPAN_Y + (POS.getY() - MIN_Y)) * SPAN_Z + (POS.getZ() - MIN_XZ);

    @GameTest(maxTicks = 400)
    public void everyBonemealTargetDoesSomethingAndSurvives(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos abs = helper.absolutePos(POS);
        final List<Block> supports = supports();

        final List<String> report = new ArrayList<>();
        final List<String> failures = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("betterend") || !(block instanceof BonemealableBlock bonemealable)) {
                continue;
            }
            final List<Site> sites = findSites(helper, block, supports);
            if (sites.isEmpty()) {
                report.add(id.getPath() + ": no survivable test site");
                continue;
            }
            int grows = 0;
            int declines = 0;
            for (Site site : sites) {
                site.place(helper);
                final BlockState placed = level.getBlockState(abs);
                if (!bonemealable.isValidBonemealTarget(level, abs, placed)) {
                    declines++;
                    continue;
                }
                final String problem = applyBonemeal(helper, placed);
                if (problem == null) {
                    grows++;
                } else {
                    failures.add(id.getPath() + " " + problem + " (" + site + ")");
                }
            }
            report.add(id.getPath() + ": " + sites.size() + " sites, grows on " + grows + ", declines on " + declines);
        }
        clear(helper);
        System.out.println("[BonemealSweep]\n" + String.join("\n", report));

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Bone meal regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    /** Bone-meals the plant at {@link #POS} until something happens; returns null on success, else what went wrong. */
    private static String applyBonemeal(GameTestHelper helper, BlockState placed) {
        final ServerLevel level = helper.getLevel();
        final BlockPos abs = helper.absolutePos(POS);
        final List<BlockState> before = snapshot(helper);
        for (int i = 0; i < ATTEMPTS; i++) {
            if (!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL, 64), level, abs)) {
                return "stopped accepting bone meal after " + i + " applications without changing anything";
            }
            final List<BlockState> after = snapshot(helper);
            final boolean drops = !level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(4.0)).isEmpty();
            // Only the bare fluid (air, or water for a submerged plant) is left where the plant was, and
            // nothing grew anywhere else: the plant is simply gone. A plant that popped off as an item counts
            // too - that is not what bone meal is for either.
            if (level.getBlockState(abs).getBlock() == placed.getFluidState().createLegacyBlock().getBlock()) {
                after.set(POS_INDEX, before.get(POS_INDEX));
                if (drops || after.equals(before)) {
                    return "DESTROYED by bone meal application " + (i + 1);
                }
                return null;
            }
            if (drops || !after.equals(before)) {
                return null;
            }
        }
        return "accepts bone meal but nothing happened in " + ATTEMPTS + " applications";
    }

    @GameTest
    public void filaluxVineGrowsWithBonemeal(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(POS.above(), Blocks.END_STONE);
        helper.setBlock(POS, EndVineBlocks.FILALUX);
        final BlockPos abs = helper.absolutePos(POS);

        if (!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, abs)) {
            throw helper.assertionException(Component.literal("filalux is not a valid bone meal target"));
        }
        if (!helper.getBlockState(POS.below()).is(EndVineBlocks.FILALUX)) {
            throw helper.assertionException(Component.literal(
                    "bone meal on filalux did not lengthen the vine, below it is " + helper.getBlockState(POS.below())));
        }
        helper.succeed();
    }

    private static List<Block> supports() {
        final List<Block> supports = new ArrayList<>();
        supports.add(Blocks.END_STONE);
        for (Block b : BuiltInRegistries.BLOCK) {
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (!id.getNamespace().equals("betterend") || b instanceof EntityBlock) continue;
            if (b.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                supports.add(b);
            }
        }
        supports.add(Blocks.OBSIDIAN);
        supports.add(Blocks.DIRT);
        supports.add(Blocks.FARMLAND);
        return supports;
    }

    private record Site(BlockState state, Direction side, Block support, boolean water) {
        void place(GameTestHelper helper) {
            clear(helper);
            if (water) {
                for (int y = MIN_Y; y <= MAX_Y; y++) helper.setBlock(POS.atY(y), Blocks.WATER);
            }
            helper.setBlock(POS.relative(side), support);
            helper.setBlock(POS, state);
        }

        boolean survives(GameTestHelper helper) {
            final BlockPos abs = helper.absolutePos(POS);
            return helper.getLevel().getBlockState(abs).is(state.getBlock()) && state.canSurvive(helper.getLevel(), abs);
        }

        @Override
        public String toString() {
            return (water ? "submerged, " : "") + side.getSerializedName() + "="
                    + BuiltInRegistries.BLOCK.getKey(support).getPath() + ", " + state;
        }
    }

    /** Every support/fluid combination {@code block} survives on, with the first orientation that works for each. */
    private static List<Site> findSites(GameTestHelper helper, Block block, List<Block> supports) {
        final List<BlockState> states = new ArrayList<>();
        states.add(block.defaultBlockState());
        EnumProperty<Direction> facing = null;
        final Property<?> property = block.getStateDefinition().getProperty("facing");
        if (property instanceof EnumProperty<?> enumProperty && enumProperty.getValueClass() == Direction.class) {
            @SuppressWarnings("unchecked") final EnumProperty<Direction> directions = (EnumProperty<Direction>) enumProperty;
            facing = directions;
            for (Direction d : directions.getPossibleValues()) {
                final BlockState s = block.defaultBlockState().setValue(directions, d);
                if (!states.contains(s)) states.add(s);
            }
        }

        final List<Site> sites = new ArrayList<>();
        for (boolean water : new boolean[]{false, true}) {
            for (Block support : supports) {
                search:
                for (BlockState candidate : states) {
                    final BlockState state = candidate.hasProperty(BlockStateProperties.WATERLOGGED)
                            ? candidate.setValue(BlockStateProperties.WATERLOGGED, water)
                            : candidate;
                    final Direction[] supportSides = facing != null
                            ? new Direction[]{state.getValue(facing).getOpposite()}
                            : new Direction[]{Direction.DOWN, Direction.UP};
                    for (Direction side : supportSides) {
                        final Site site = new Site(state, side, support, water);
                        site.place(helper);
                        if (site.survives(helper)) {
                            sites.add(site);
                            break search;
                        }
                    }
                }
            }
        }
        return sites;
    }

    private static void clear(GameTestHelper helper) {
        for (int x = MIN_XZ; x <= MAX_XZ; x++)
            for (int y = MIN_Y; y <= MAX_Y; y++)
                for (int z = MIN_XZ; z <= MAX_XZ; z++)
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        helper.getLevel()
              .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(POS)).inflate(8.0))
              .forEach(ItemEntity::discard);
    }

    private static List<BlockState> snapshot(GameTestHelper helper) {
        final List<BlockState> out = new ArrayList<>();
        for (int x = MIN_XZ; x <= MAX_XZ; x++)
            for (int y = MIN_Y; y <= MAX_Y; y++)
                for (int z = MIN_XZ; z <= MAX_XZ; z++)
                    out.add(helper.getBlockState(new BlockPos(x, y, z)));
        return out;
    }
}

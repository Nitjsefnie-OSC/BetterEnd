package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.block.EndCropBlocks;
import org.betterx.betterend.registry.block.EndCrystalBlocks;
import org.betterx.betterend.registry.block.EndDecorBlocks;
import org.betterx.betterend.registry.block.EndFunctionalBlocks;
import org.betterx.betterend.registry.block.EndMetalBlocks;
import org.betterx.betterend.registry.block.EndStoneBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Pins the block properties that commit d9bac273f lost when it moved them out of the block constructors
 * (the constructors used to build them from a vanilla block or a BCLib behaviour preset; the new
 * definitions did not carry them over). Each check compares against the vanilla block the old
 * constructor copied, so a later vanilla tweak cannot make this test lie.
 */
public class BlockPropertyRegressionGameTest {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    @GameTest
    public void propertiesLostInTheConstructorMoveAreBack(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();

        // broke instantly
        sameStrength(failures, EndCropBlocks.CAVE_PUMPKIN, Blocks.PUMPKIN);
        sameSound(failures, EndCropBlocks.CAVE_PUMPKIN, Blocks.PUMPKIN);
        sameMapColor(failures, EndCropBlocks.CAVE_PUMPKIN, Blocks.PUMPKIN);
        sameStrength(failures, EndDecorBlocks.NEON_CACTUS_BLOCK, Blocks.CACTUS);
        sameSound(failures, EndDecorBlocks.NEON_CACTUS_BLOCK, Blocks.CACTUS);
        sameMapColor(failures, EndDecorBlocks.NEON_CACTUS_BLOCK, Blocks.CACTUS);
        expect(failures, EndCrystalBlocks.SMARAGDANT_CRYSTAL_SHARD, "destroyTime 1.5",
                EndCrystalBlocks.SMARAGDANT_CRYSTAL_SHARD.defaultDestroyTime() == 1.5F);

        // eternal portal surface: walk-through, unbreakable, immovable, like a nether portal
        final BlockState portal = EndFunctionalBlocks.END_PORTAL_BLOCK.defaultBlockState();
        expect(failures, EndFunctionalBlocks.END_PORTAL_BLOCK, "no collision",
                portal.getCollisionShape(helper.getLevel(), helper.absolutePos(POS), CollisionContext.empty())
                      .isEmpty());
        expect(failures, EndFunctionalBlocks.END_PORTAL_BLOCK, "not breakable by hand",
                portal.getDestroySpeed(helper.getLevel(), helper.absolutePos(POS)) >= Blocks.BEDROCK.getExplosionResistance()
                        || portal.getDestroySpeed(helper.getLevel(), helper.absolutePos(POS)) < 0);
        expect(failures, EndFunctionalBlocks.END_PORTAL_BLOCK, "pushReaction BLOCK",
                portal.getPistonPushReaction() == PushReaction.IMMOVEABLE);
        sameSound(failures, EndFunctionalBlocks.END_PORTAL_BLOCK, Blocks.NETHER_PORTAL);

        // stone-like blocks lost their vanilla base strength and map colour to STONE_BLOCK's 2/6 default
        sameStrength(failures, EndMetalBlocks.CHARCOAL_BLOCK, Blocks.COAL_BLOCK);
        sameMapColor(failures, EndMetalBlocks.CHARCOAL_BLOCK, Blocks.COAL_BLOCK);
        sameStrength(failures, EndMetalBlocks.AMBER_BLOCK, Blocks.DIAMOND_BLOCK);
        sameSound(failures, EndMetalBlocks.AMBER_BLOCK, Blocks.DIAMOND_BLOCK);
        expect(failures, EndMetalBlocks.AMBER_BLOCK, "mapColor COLOR_YELLOW",
                EndMetalBlocks.AMBER_BLOCK.defaultMapColor() == MapColor.COLOR_YELLOW);
        sameStrength(failures, EndStoneBlocks.BRIMSTONE, Blocks.END_STONE);
        sameStrength(failures, EndStoneBlocks.MISSING_TILE, Blocks.END_STONE);
        sameStrength(failures, EndFunctionalBlocks.RESPAWN_OBELISK, Blocks.END_STONE);
        sameMapColor(failures, EndFunctionalBlocks.RESPAWN_OBELISK, Blocks.END_STONE);

        // glass-like crystals
        sameSound(failures, EndCrystalBlocks.AURORA_CRYSTAL, Blocks.GLASS);
        for (Block crystal : List.of(EndCrystalBlocks.AURORA_CRYSTAL, EndCrystalBlocks.SMARAGDANT_CRYSTAL)) {
            final BlockState state = crystal.defaultBlockState();
            final BlockPos pos = helper.absolutePos(POS);
            expect(failures, crystal, "no mob spawns on it",
                    !state.isValidSpawn(helper.getLevel(), pos, EntityTypes.ZOMBIE));
            expect(failures, crystal, "not suffocating", !state.isSuffocating(helper.getLevel(), pos));
        }

        // map colour of the white wool it was copied from
        expect(failures, EndFunctionalBlocks.SILK_MOTH_NEST, "mapColor SNOW",
                EndFunctionalBlocks.SILK_MOTH_NEST.defaultMapColor() == MapColor.SNOW);

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Block property regressions (" + failures.size() + "):\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    private static void expect(List<String> failures, Block block, String what, boolean ok) {
        if (!ok) failures.add(BuiltInRegistries.BLOCK.getKey(block) + ": expected " + what);
    }

    private static void sameStrength(List<String> failures, Block block, Block vanilla) {
        if (block.defaultDestroyTime() != vanilla.defaultDestroyTime()
                || block.getExplosionResistance() != vanilla.getExplosionResistance()) {
            failures.add(BuiltInRegistries.BLOCK.getKey(block) + ": strength " + block.defaultDestroyTime() + "/"
                    + block.getExplosionResistance() + " instead of " + BuiltInRegistries.BLOCK.getKey(vanilla) + "'s "
                    + vanilla.defaultDestroyTime() + "/" + vanilla.getExplosionResistance());
        }
    }

    private static void sameSound(List<String> failures, Block block, Block vanilla) {
        final SoundType actual = block.defaultBlockState().getSoundType();
        final SoundType expected = vanilla.defaultBlockState().getSoundType();
        if (actual != expected) {
            failures.add(BuiltInRegistries.BLOCK.getKey(block) + ": sound " + actual.getBreakSound()
                    + " instead of " + BuiltInRegistries.BLOCK.getKey(vanilla) + "'s " + expected.getBreakSound());
        }
    }

    private static void sameMapColor(List<String> failures, Block block, Block vanilla) {
        if (block.defaultMapColor() != vanilla.defaultMapColor()) {
            failures.add(BuiltInRegistries.BLOCK.getKey(block) + ": map colour " + block.defaultMapColor().id
                    + " instead of " + BuiltInRegistries.BLOCK.getKey(vanilla) + "'s " + vanilla.defaultMapColor().id);
        }
    }
}

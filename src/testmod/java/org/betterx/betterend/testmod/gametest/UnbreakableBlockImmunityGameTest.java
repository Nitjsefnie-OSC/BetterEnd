package org.betterx.betterend.testmod.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Every betterend: block that is unbreakable like bedrock (negative destroy speed) must also resist the
 * wither and the ender dragon, which ignore hardness and only consult {@code wither_immune} /
 * {@code dragon_immune} - see https://github.com/quiqueck/BetterEnd/issues/406.
 */
public class UnbreakableBlockImmunityGameTest {
    @GameTest
    public void unbreakableBlocksAreWitherAndDragonImmune(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        int unbreakable = 0;

        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            final String id = entry.getKey().location().toString();
            if (!id.startsWith("betterend:")) continue;

            for (BlockState state : entry.getValue().getStateDefinition().getPossibleStates()) {
                if (state.getDestroySpeed(helper.getLevel(), BlockPos.ZERO) >= 0) continue;
                unbreakable++;
                if (WitherBoss.canDestroy(state) || !state.is(BlockTags.WITHER_IMMUNE)) {
                    failures.add(id + " " + state + ": unbreakable but the wither can destroy it");
                }
                if (!state.is(BlockTags.DRAGON_IMMUNE)) {
                    failures.add(id + " " + state + ": unbreakable but not dragon_immune");
                }
                break;
            }
        }

        if (unbreakable == 0) {
            throw helper.assertionException(Component.literal("found no unbreakable betterend: block to check"));
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "unbreakable blocks the wither/dragon can still break:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }
}

package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.integration.PatchouliGuideBook;
import org.betterx.betterend.item.GuideBookItem;
import de.ambertation.wover.test.api.gametest.MockPlayers;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

/**
 * Covers #498: the guidebook must be safe to use without Patchouli, and with Patchouli installed the
 * book has to actually load (Patchouli >= 1.20 rejects books without {@code use_resource_pack}) and open.
 * Patchouli is only on the dev runtime with {@code ./gradlew :runGametest -PwithPatchouli}; without it
 * this test only covers the "not installed" path.
 */
public class GuideBookGameTest {
    @GameTest
    public void guideBookUseMatchesPatchouliAvailability(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GuideBookItem.GUIDE_BOOK));

        if (!BetterEnd.PATCHOULI.isLoaded()) {
            final InteractionResult result = GuideBookItem.GUIDE_BOOK.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            if (result != InteractionResult.CONSUME) {
                throw helper.assertionException(Component.literal(
                        "without Patchouli the guidebook should do nothing, got " + result
                ));
            }
            helper.succeed();
            return;
        }

        final ItemStack bookStack = WithPatchouli.bookStack();
        if (!bookStack.is(GuideBookItem.GUIDE_BOOK)) {
            throw helper.assertionException(Component.literal(
                    "Patchouli did not load " + PatchouliGuideBook.BOOK_ID + " (book stack is " + bookStack
                            + ") - check the log for 'Failed to load book'"
            ));
        }
        final InteractionResult result = GuideBookItem.GUIDE_BOOK.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        if (result != InteractionResult.SUCCESS) {
            throw helper.assertionException(Component.literal(
                    "with Patchouli the guidebook should open the book, got " + result
            ));
        }
        helper.succeed();
    }

    // Kept separate so Patchouli classes are only loaded when Patchouli is installed.
    private static final class WithPatchouli {
        // PatchouliAPI#getBookStack answers patchouli:guide_book for any id, so ask the registry itself
        // (internal API, fine for a test) whether the book loaded and is bound to our item.
        static ItemStack bookStack() {
            final Book book = BookRegistry.INSTANCE.books.get(PatchouliGuideBook.BOOK_ID);
            return book == null ? ItemStack.EMPTY : book.getBookItem();
        }
    }
}

package org.betterx.betterend.integration;

import org.betterx.betterend.BetterEnd;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import vazkii.patchouli.api.PatchouliAPI;

/**
 * Only references Patchouli classes. Callers must check {@code BetterEnd.PATCHOULI.isLoaded()} before
 * touching this class, so BetterEnd still runs when Patchouli is not installed.
 */
public final class PatchouliGuideBook {
    public static final Identifier BOOK_ID = BetterEnd.C.mk("guidebook");

    private PatchouliGuideBook() {
    }

    public static void open(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, BOOK_ID);
    }
}

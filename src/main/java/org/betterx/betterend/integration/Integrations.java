package org.betterx.betterend.integration;

import org.betterx.bclib.api.v2.ModIntegrationAPI;
import org.betterx.bclib.integration.ModIntegration;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.config.Configs;
import org.betterx.betterend.events.PlayerAdvancementsCallback;
import org.betterx.betterend.item.GuideBookItem;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class Integrations {
    public static final ModIntegration FLAMBOYANT_REFABRICATED = ModIntegrationAPI.register(new FlamboyantRefabricatedIntegration());

    public static void init() {
        if (BetterEnd.PATCHOULI.isLoaded()) {
            GuideBookItem.register();
            Identifier advId = Identifier.withDefaultNamespace("end/enter_end_gateway");

            PlayerAdvancementsCallback.PLAYER_ADVANCEMENT_COMPLETE.register((player, advancement, criterionName) -> {
                if (advId.equals(advancement.id()) && Configs.GENERATOR_CONFIG.giveGuideBook.get()) {
                    player.addItem(new ItemStack(GuideBookItem.GUIDE_BOOK));
                }
            });
        }
    }
}
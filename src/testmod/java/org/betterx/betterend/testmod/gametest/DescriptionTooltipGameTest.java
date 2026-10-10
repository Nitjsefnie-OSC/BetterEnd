package org.betterx.betterend.testmod.gametest;

import org.betterx.bclib.trait.block.DescriptionBlockTrait;
import org.betterx.betterend.registry.block.EndSaplingBlocks;
import de.ambertation.wover.block.api.trait.BlockTrait;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written tooltip lines ({@link DescriptionBlockTrait}) on BetterEnd blocks are translated in English and
 * German, and the hydralux sapling carries its water-depth line.
 */
public class DescriptionTooltipGameTest {
    private static final String[] LANGUAGES = {"en_us", "de_de"};

    @GameTest
    public void descriptionLinesAreTranslated(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();

        if (!descriptionKeys(EndSaplingBlocks.HYDRALUX_SAPLING).contains("tooltip.betterend.hydralux_sapling.depth")) {
            failures.add("the hydralux sapling does not carry tooltip.betterend.hydralux_sapling.depth");
        }

        for (String language : LANGUAGES) {
            final JsonObject json = readLanguage(language, failures);
            if (json == null) continue;
            for (Block block : BuiltInRegistries.BLOCK) {
                if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("betterend")) continue;
                for (String key : descriptionKeys(block)) {
                    if (!json.has(key)) {
                        failures.add(BuiltInRegistries.BLOCK.getKey(block) + ": " + key + " is missing from " + language);
                    }
                }
            }
        }

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Tooltip translation regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    private static List<String> descriptionKeys(Block block) {
        return BlockTrait.runtimeTraits(block)
                         .filter(t -> t instanceof DescriptionBlockTrait)
                         .flatMap(t -> ((DescriptionBlockTrait) t).translationKeys.stream())
                         .toList();
    }

    private static JsonObject readLanguage(String language, List<String> failures) {
        final String file = "assets/betterend/lang/" + language + ".json";
        final Path path = FabricLoader.getInstance()
                                      .getModContainer("betterend")
                                      .flatMap(mod -> mod.findPath(file))
                                      .orElse(null);
        if (path == null) {
            failures.add(file + " not found in the betterend mod");
            return null;
        }
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            failures.add(file + " can not be read: " + e);
            return null;
        }
    }
}

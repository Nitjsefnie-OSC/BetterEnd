package org.betterx.datagen.betterend.worldgen.features;


import org.betterx.betterend.registry.block.EndCropBlocks;
import org.betterx.betterend.registry.block.EndMushroomBlocks;
import org.betterx.betterend.registry.block.EndPlantBlocks;
import org.betterx.betterend.registry.block.EndTerrainBlocks;
import org.betterx.betterend.registry.block.EndVineBlocks;
import org.betterx.betterend.registry.block.EndWallPlantBlocks;
import org.betterx.betterend.registry.block.EndWaterPlantBlocks;
import org.betterx.betterend.registry.block.EndWoodBlocks;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.registry.features.EndConfiguredVegetation;
import org.betterx.betterend.registry.features.EndVegetationFeatures;
import org.betterx.betterend.world.features.*;
import org.betterx.betterend.world.features.bushes.BushFeature;
import org.betterx.betterend.world.features.bushes.BushFeatureConfig;
import org.betterx.betterend.world.features.bushes.BushWithOuterFeature;
import org.betterx.betterend.world.features.bushes.BushWithOuterFeatureConfig;
import de.ambertation.wover.core.api.ModCore;
import de.ambertation.wover.datagen.api.provider.multi.WoverFeatureProvider;
import de.ambertation.wover.feature.api.placed.BoundPlacedFeatureKey;
import de.ambertation.wover.feature.api.placed.PlacedFeatureKey;

import net.minecraft.core.HolderSet;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.MultifaceGrowthFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import org.jetbrains.annotations.NotNull;

public class VegetationFeaturesProvider extends WoverFeatureProvider {
    public VegetationFeaturesProvider(@NotNull ModCore modCore) {
        super(modCore, modCore.id("vegetation"));
    }

    @Override
    protected void bootstrapFeatures(BootstrapContext<Feature> context) {
        EndConfiguredVegetation.DRAGON_TREE.bootstrap(context).register();
        EndConfiguredVegetation.GIGANTIC_AMARANITA.bootstrap(context).register();
        EndConfiguredVegetation.HELIX_TREE.bootstrap(context).register();
        EndConfiguredVegetation.DRAGON_HELIX_TREE.bootstrap(context).register();
        EndConfiguredVegetation.JELLYSHROOM.bootstrap(context).register();
        EndConfiguredVegetation.LACUGROVE.bootstrap(context).register();
        EndConfiguredVegetation.LUCERNIA.bootstrap(context).register();
        EndConfiguredVegetation.MOSSY_GLOWSHROOM.bootstrap(context).register();
        EndConfiguredVegetation.PYTHADENDRON_TREE.bootstrap(context).register();
        EndConfiguredVegetation.TENANEA.bootstrap(context).register();
        EndConfiguredVegetation.UMBRELLA_TREE.bootstrap(context).register();

        EndConfiguredVegetation.LARGE_AMARANITA.bootstrap(context).register();
        EndConfiguredVegetation.LUMECORN.bootstrap(context).register();
        EndConfiguredVegetation.TENANEA_BUSH.bootstrap(context).register();
    }

    @Override
    protected void bootstrapPlaced(BootstrapContext<PlacedFeature> context) {
        //Trees
        registerVegetation(context, EndVegetationFeatures.MOSSY_GLOWSHROOM, 2);
        registerVegetation(context, EndVegetationFeatures.PYTHADENDRON_TREE, 1);
        registerVegetation(context, EndVegetationFeatures.LACUGROVE, 4);
        registerVegetation(context, EndVegetationFeatures.DRAGON_TREE, 2);
        registerVegetation(context, EndVegetationFeatures.TENANEA, 3);
        registerVegetation(context, EndVegetationFeatures.HELIX_TREE, 1);
        registerVegetation(context, EndVegetationFeatures.DRAGON_HELIX_TREE, 1);
        registerVegetation(context, EndVegetationFeatures.UMBRELLA_TREE, 2);
        registerVegetation(context, EndVegetationFeatures.JELLYSHROOM, 2);
        registerVegetation(context, EndVegetationFeatures.GIGANTIC_AMARANITA, 1);
        registerVegetation(context, EndVegetationFeatures.LUCERNIA, 3);

        //Bushes
        registerVegetation(context, EndVegetationFeatures.TENANEA_BUSH, 12);
        registerVegetation(context, EndVegetationFeatures.LUMECORN, 5);
        registerVegetation(context, EndVegetationFeatures.LARGE_AMARANITA, 5);
        registerVegetation(context, EndVegetationFeatures.AMARANITA_PATCH, EndFeatures.AMARANITA_PATCH_FEATURE, 1);
        registerVegetation(context, EndVegetationFeatures.NEON_CACTUS, EndFeatures.NEON_CACTUS_FEATURE, 2);
        registerVegetation(context, EndVegetationFeatures.PYTHADENDRON_BUSH, new BushFeature(new BushFeatureConfig(EndWoodBlocks.PYTHADENDRON_LEAVES, EndWoodBlocks.PYTHADENDRON.getBark())), 3);
        registerVegetation(context, EndVegetationFeatures.DRAGON_TREE_BUSH, new BushFeature(new BushFeatureConfig(EndWoodBlocks.DRAGON_TREE_LEAVES, EndWoodBlocks.DRAGON_TREE.getBark())), 5);
        registerVegetation(context, EndVegetationFeatures.LUCERNIA_BUSH, new BushWithOuterFeature(new BushWithOuterFeatureConfig(EndWoodBlocks.LUCERNIA_LEAVES, EndWoodBlocks.LUCERNIA_OUTER_LEAVES, EndWoodBlocks.LUCERNIA.getBark())), 10);
        registerVegetation(context, EndVegetationFeatures.LUCERNIA_BUSH_RARE, new BushWithOuterFeature(new BushWithOuterFeatureConfig(EndWoodBlocks.LUCERNIA_LEAVES, EndWoodBlocks.LUCERNIA_OUTER_LEAVES, EndWoodBlocks.LUCERNIA.getBark())), 1);

        //Vines
        registerVegetation(context, EndVegetationFeatures.DENSE_VINE, new VineFeature(new VineFeatureConfig(EndVineBlocks.DENSE_VINE, 24)), 3);
        registerVegetation(context, EndVegetationFeatures.TWISTED_VINE, new VineFeature(new VineFeatureConfig(EndVineBlocks.TWISTED_VINE, 24)), 1);
        registerVegetation(context, EndVegetationFeatures.BULB_VINE, new VineFeature(new VineFeatureConfig(EndVineBlocks.BULB_VINE, 24)), 5);
        registerVegetation(context, EndVegetationFeatures.JUNGLE_VINE, new VineFeature(new VineFeatureConfig(EndVineBlocks.JUNGLE_VINE, 24)), 5);
        registerVegetation(context, EndVegetationFeatures.BLUE_VINE, new BlueVineFeature(ScatterFeatureConfig.blueVine()), 1);

        // Ceil plants
        registerVegetation(context, EndVegetationFeatures.SMALL_JELLYSHROOM_CEIL, new SingleInvertedScatterFeature(new SinglePlantFeatureConfig(EndMushroomBlocks.SMALL_JELLYSHROOM, 8)), 8);

        // Wall Plants
        registerVegetation(context, EndVegetationFeatures.PURPLE_POLYPORE, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.PURPLE_POLYPORE, 3)), 5);
        registerVegetation(context, EndVegetationFeatures.AURANT_POLYPORE, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.AURANT_POLYPORE, 3)), 5);
        registerVegetation(context, EndVegetationFeatures.TAIL_MOSS, new WallPlantFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.TAIL_MOSS, 3)), 15);
        registerVegetation(context, EndVegetationFeatures.CYAN_MOSS, new WallPlantFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.CYAN_MOSS, 3)), 15);
        registerVegetation(context, EndVegetationFeatures.TAIL_MOSS_WOOD, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.TAIL_MOSS, 4)), 25);
        registerVegetation(context, EndVegetationFeatures.CYAN_MOSS_WOOD, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.CYAN_MOSS, 4)), 25);
        registerVegetation(context, EndVegetationFeatures.TWISTED_MOSS, new WallPlantFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.TWISTED_MOSS, 6)), 15);
        registerVegetation(context, EndVegetationFeatures.TWISTED_MOSS_WOOD, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.TWISTED_MOSS, 6)), 25);
        registerVegetation(context, EndVegetationFeatures.BULB_MOSS, new WallPlantFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.BULB_MOSS, 6)), 1);
        registerVegetation(context, EndVegetationFeatures.BULB_MOSS_WOOD, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.BULB_MOSS, 6)), 15);
        registerVegetation(context, EndVegetationFeatures.SMALL_JELLYSHROOM_WALL, new WallPlantFeature(new WallPlantFeatureConfig(EndMushroomBlocks.SMALL_JELLYSHROOM, 4)), 4);
        registerVegetation(context, EndVegetationFeatures.SMALL_JELLYSHROOM_WOOD, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndMushroomBlocks.SMALL_JELLYSHROOM, 4)), 8);
        registerVegetation(context, EndVegetationFeatures.JUNGLE_FERN_WOOD, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.JUNGLE_FERN, 3)), 12);
        registerVegetation(context, EndVegetationFeatures.RUSCUS, new WallPlantFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.RUSCUS, 6)), 10);
        registerVegetation(context, EndVegetationFeatures.RUSCUS_WOOD, new WallPlantOnLogFeature(new WallPlantFeatureConfig(EndWallPlantBlocks.RUSCUS, 6)), 10);

        //Sky plants
        registerVegetation(context, EndVegetationFeatures.FILALUX, new FilaluxFeature(ScatterFeatureConfig.filalux()), 1);

        // Water
        registerVegetation(context, EndVegetationFeatures.BUBBLE_CORAL, new UnderwaterPlantFeature(new SinglePlantFeatureConfig(EndWaterPlantBlocks.BUBBLE_CORAL, 6)), 10);
        registerVegetation(context, EndVegetationFeatures.BUBBLE_CORAL_RARE, new UnderwaterPlantFeature(new SinglePlantFeatureConfig(EndWaterPlantBlocks.BUBBLE_CORAL, 3)), 4);
        registerVegetation(context, EndVegetationFeatures.END_LILY, new EndLilyFeature(new ScatterFeatureConfig(6)), 10);
        registerVegetation(context, EndVegetationFeatures.END_LILY_RARE, new EndLilyFeature(new ScatterFeatureConfig(3)), 4);
        registerVegetation(context, EndVegetationFeatures.END_LOTUS, new EndLotusFeature(new ScatterFeatureConfig(7)), 6);
        registerVegetation(context, EndVegetationFeatures.END_LOTUS_LEAF, new EndLotusLeafFeature(new ScatterFeatureConfig(20)), 4);
        registerVegetation(context, EndVegetationFeatures.HYDRALUX, new HydraluxFeature(new ScatterFeatureConfig(5)), 5);
        registerVegetation(context, EndVegetationFeatures.POND_ANEMONE, new UnderwaterPlantFeature(new SinglePlantFeatureConfig(EndWaterPlantBlocks.POND_ANEMONE, 6)), 10);
        registerVegetation(context, EndVegetationFeatures.MENGER_SPONGE, new MengerSpongeFeature(new ScatterFeatureConfig(5)), 1);
        registerVegetation(context, EndVegetationFeatures.FLAMAEA, new SinglePlantFeature(new SinglePlantFeatureConfig(EndWaterPlantBlocks.FLAMAEA, 12, false, 5)), 20);
        registerVegetation(context, EndVegetationFeatures.CHARNIA_RED, new CharniaFeature(SinglePlantFeatureConfig.charnia(EndWaterPlantBlocks.CHARNIA_RED)), 10);
        registerVegetation(context, EndVegetationFeatures.CHARNIA_PURPLE, new CharniaFeature(SinglePlantFeatureConfig.charnia(EndWaterPlantBlocks.CHARNIA_PURPLE)), 10);
        registerVegetation(context, EndVegetationFeatures.CHARNIA_CYAN, new CharniaFeature(SinglePlantFeatureConfig.charnia(EndWaterPlantBlocks.CHARNIA_CYAN)), 10);
        registerVegetation(context, EndVegetationFeatures.CHARNIA_LIGHT_BLUE, new CharniaFeature(SinglePlantFeatureConfig.charnia(EndWaterPlantBlocks.CHARNIA_LIGHT_BLUE)), 10);
        registerVegetation(context, EndVegetationFeatures.CHARNIA_ORANGE, new CharniaFeature(SinglePlantFeatureConfig.charnia(EndWaterPlantBlocks.CHARNIA_ORANGE)), 10);
        registerVegetation(context, EndVegetationFeatures.CHARNIA_GREEN, new CharniaFeature(SinglePlantFeatureConfig.charnia(EndWaterPlantBlocks.CHARNIA_GREEN)), 10);
        registerVegetation(context, EndVegetationFeatures.CHARNIA_RED_RARE, new CharniaFeature(SinglePlantFeatureConfig.charnia(EndWaterPlantBlocks.CHARNIA_RED)), 2);

        // Plants
        registerVegetation(context, EndVegetationFeatures.UMBRELLA_MOSS, new DoublePlantFeature(new DoublePlantFeatureConfig(EndPlantBlocks.UMBRELLA_MOSS, EndPlantBlocks.UMBRELLA_MOSS_TALL, 5)), 3);
        registerVegetation(context, EndVegetationFeatures.TWISTED_UMBRELLA_MOSS, new DoublePlantFeature(new DoublePlantFeatureConfig(EndPlantBlocks.TWISTED_UMBRELLA_MOSS, EndPlantBlocks.TWISTED_UMBRELLA_MOSS_TALL, 6)), 3);

        registerVegetation(context, EndVegetationFeatures.CREEPING_MOSS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.CREEPING_MOSS, 5)), 3);
        registerVegetation(context, EndVegetationFeatures.CHORUS_GRASS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.CHORUS_GRASS, 4)), 3);
        registerVegetation(context, EndVegetationFeatures.CRYSTAL_GRASS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.CRYSTAL_GRASS, 8, false)), 20);
        registerVegetation(context, EndVegetationFeatures.SHADOW_PLANT, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.SHADOW_PLANT, 6)), 5);
        registerVegetation(context, EndVegetationFeatures.MURKWEED, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.MURKWEED, 3)), 2);
        registerVegetation(context, EndVegetationFeatures.NEEDLEGRASS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.NEEDLEGRASS, 3)), 1);
        registerVegetation(context, EndVegetationFeatures.SHADOW_BERRY, new SinglePlantFeature(new SinglePlantFeatureConfig(EndCropBlocks.SHADOW_BERRY, 2)), 1);
        registerVegetation(context, EndVegetationFeatures.BUSHY_GRASS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.BUSHY_GRASS, 8, false)), 10);
        registerVegetation(context, EndVegetationFeatures.BUSHY_GRASS_WG, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.BUSHY_GRASS, 5)), 8);
        registerVegetation(context, EndVegetationFeatures.AMBER_GRASS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.AMBER_GRASS, 6)), 7);
        registerVegetation(context, EndVegetationFeatures.JUNGLE_GRASS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.JUNGLE_GRASS, 7, 3)), 6);
        registerVegetation(context, EndVegetationFeatures.SMALL_JELLYSHROOM_FLOOR, new SinglePlantFeature(new SinglePlantFeatureConfig(EndMushroomBlocks.SMALL_JELLYSHROOM, 5, 5)), 2);
        registerVegetation(context, EndVegetationFeatures.BLOSSOM_BERRY, new SinglePlantFeature(new SinglePlantFeatureConfig(EndCropBlocks.BLOSSOM_BERRY, 4, 4)), 3);
        registerVegetation(context, EndVegetationFeatures.BLOOMING_COOKSONIA, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.BLOOMING_COOKSONIA, 5)), 5);
        registerVegetation(context, EndVegetationFeatures.SALTEAGO, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.SALTEAGO, 5)), 5);
        registerVegetation(context, EndVegetationFeatures.VAIOLUSH_FERN, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.VAIOLUSH_FERN, 5)), 5);
        registerVegetation(context, EndVegetationFeatures.FRACTURN, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.FRACTURN, 5)), 5);
        registerVegetation(context, EndVegetationFeatures.UMBRELLA_MOSS_RARE, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.UMBRELLA_MOSS, 3)), 2);
        registerVegetation(context, EndVegetationFeatures.CREEPING_MOSS_RARE, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.CREEPING_MOSS, 3)), 2);
        registerVegetation(context, EndVegetationFeatures.TWISTED_UMBRELLA_MOSS_RARE, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.TWISTED_UMBRELLA_MOSS, 3)), 2);
        registerVegetation(context, EndVegetationFeatures.ORANGO, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.ORANGO, 5)), 6);
        registerVegetation(context, EndVegetationFeatures.AERIDIUM, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.AERIDIUM, 5, 4)), 5);
        registerVegetation(context, EndVegetationFeatures.LUTEBUS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.LUTEBUS, 5, 2)), 5);
        registerVegetation(context, EndVegetationFeatures.LAMELLARIUM, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.LAMELLARIUM, 5)), 6);
        registerVegetation(context, EndVegetationFeatures.SMALL_AMARANITA, new SinglePlantFeature(new SinglePlantFeatureConfig(EndMushroomBlocks.SMALL_AMARANITA_MUSHROOM, 5, 5)), 4);
        registerVegetation(context, EndVegetationFeatures.GLOBULAGUS, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.GLOBULAGUS, 5, 3)), 6);
        registerVegetation(context, EndVegetationFeatures.CLAWFERN, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.CLAWFERN, 5, 4)), 5);
        registerVegetation(context, EndVegetationFeatures.BOLUX_MUSHROOM, new SinglePlantFeature(new SinglePlantFeatureConfig(EndMushroomBlocks.BOLUX_MUSHROOM, 5, 5)), 2);
        registerVegetation(context, EndVegetationFeatures.CHORUS_MUSHROOM, new SinglePlantFeature(new SinglePlantFeatureConfig(EndCropBlocks.CHORUS_MUSHROOM, 3, 5)), 1);
        registerVegetation(context, EndVegetationFeatures.AMBER_ROOT, new SinglePlantFeature(new SinglePlantFeatureConfig(EndCropBlocks.AMBER_ROOT, 5, 5)), 1);
        registerVegetation(context, EndVegetationFeatures.INFLEXIA, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.INFLEXIA, 7, false, 3)), 16);
        registerVegetation(context, EndVegetationFeatures.FLAMMALIX, new SinglePlantFeature(new SinglePlantFeatureConfig(EndPlantBlocks.FLAMMALIX, 3, false, 7)), 5);
        registerVegetation(context, EndVegetationFeatures.LANCELEAF, new LanceleafFeature(ScatterFeatureConfig.lanceleaf()), 2);
        registerVegetation(context, EndVegetationFeatures.GLOW_PILLAR, new GlowPillarFeature(ScatterFeatureConfig.glowPillar()), 1);

        // Cover patches: the original countRange(16,256) x onEveryLayer(2) with spread chance 1.0
        // and search range 20 was up to ~512 aggressive placements per chunk - it plastered every
        // crystal-moss surface and occupied the block spaces the crystal grass (which runs BEFORE
        // this, order was never the problem) needed. That got scaled all the way down to
        // countRange(2,6), which then read as too sparse. Settling on a middle ground.
        EndVegetationFeatures.CRYSTAL_MOSS_COVER.inlineConfiguration(context)
                                                .withFeature(new MultifaceGrowthFeature(EndPlantBlocks.CRYSTAL_MOSS_COVER, 8, true, true, true, 0.75f, HolderSet.direct(Block::builtInRegistryHolder, EndTerrainBlocks.CRYSTAL_MOSS, Blocks.END_STONE)))
                                                .inlinePlace()
                                                .countRange(16, 32)
                                                .onEveryLayer(1)
                                                .onlyInBiome()
                                                .register();
    }

    // 26.3 merged the former (feature, config) overload into this one: a Feature carries its own
    // configuration, so callers build the instance they want and there is nothing left to configure here.
    private static <F extends Feature> void registerVegetation(
            BootstrapContext<PlacedFeature> context,
            PlacedFeatureKey key,
            F feature,
            int density
    ) {
        key
                .inlineConfiguration(context)
                .withFeature(feature)
                .inlinePlace()
                .onEveryLayerMax(density)
                .onlyInBiome()
                .register();
    }

    private static void registerVegetation(
            BootstrapContext<PlacedFeature> context,
            BoundPlacedFeatureKey key,
            int density
    ) {
        key
                .place(context)
                .onEveryLayerMax(density)
                .onlyInBiome()
                .register();
    }
}

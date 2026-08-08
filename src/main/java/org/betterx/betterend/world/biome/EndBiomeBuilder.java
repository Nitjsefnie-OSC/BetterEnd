package org.betterx.betterend.world.biome;

import org.betterx.bclib.interfaces.SurfaceMaterialProvider;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.registry.EndSounds;
import de.ambertation.wover.biome.api.BiomeKey;
import de.ambertation.wover.biome.api.builder.BiomeBootstrapContext;
import de.ambertation.wover.biome.api.builder.BiomeBuilder;
import de.ambertation.wover.biome.api.data.BiomeData;
import de.ambertation.wover.biome.api.data.BiomeGenerationDataContainer;
import de.ambertation.wover.generator.api.biomesource.WoverBiomeBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.EndPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EndBiomeBuilder extends WoverBiomeBuilder.AbstractWoverBiomeBuilder<EndBiomeBuilder> {
    /**
     * Replacement for {@code SurfaceRules.ON_FLOOR}, which Minecraft 26.3 deleted.
     * <p>
     * 26.3 moved these four constants out of Java and into the new
     * {@code minecraft:worldgen/material_condition} <b>datapack</b> registry (see
     * {@code net.minecraft.data.worldgen.material.VanillaMaterialConditions}). We deliberately do
     * <b>not</b> resolve them through {@link SurfaceRules#getCondition(net.minecraft.core.HolderGetter,
     * ResourceKey)}, for the same two reasons WoVer's {@code SurfaceRuleBuilderImpl} inlines them:
     * <ul>
     * <li>resolving the key needs a {@code HolderGetter<ConditionSource>}, and the biome configs build
     * their rules eagerly - in static initialisers and in {@code surface()} - outside any registry
     * bootstrap;</li>
     * <li>a holder reference would make every rule BetterEnd emits depend on a vanilla data file that a
     * datapack could override or remove, silently changing our surfaces.</li>
     * </ul>
     * The inline forms are value-identical to the deleted constants; verified against the 26.3 jar's
     * {@code VanillaMaterialConditions.bootstrap}, which registers them as
     * {@code stoneDepthCheck(0, false, FLOOR)}, {@code stoneDepthCheck(0, true, FLOOR)},
     * {@code stoneDepthCheck(0, false, CEILING)} and {@code stoneDepthCheck(0, true, CEILING)}.
     */
    public static final SurfaceRules.ConditionSource ON_FLOOR
            = SurfaceRules.stoneDepthCheck(0, false, CaveSurface.FLOOR);

    /**
     * Replacement for the deleted {@code SurfaceRules.UNDER_FLOOR}. See {@link #ON_FLOOR}.
     */
    public static final SurfaceRules.ConditionSource UNDER_FLOOR
            = SurfaceRules.stoneDepthCheck(0, true, CaveSurface.FLOOR);

    /**
     * Replacement for the deleted {@code SurfaceRules.ON_CEILING}. See {@link #ON_FLOOR}.
     */
    public static final SurfaceRules.ConditionSource ON_CEILING
            = SurfaceRules.stoneDepthCheck(0, false, CaveSurface.CEILING);

    /**
     * Replacement for the deleted {@code SurfaceRules.UNDER_CEILING}. See {@link #ON_FLOOR}.
     */
    public static final SurfaceRules.ConditionSource UNDER_CEILING
            = SurfaceRules.stoneDepthCheck(0, true, CaveSurface.CEILING);

    protected boolean hasCave = false;
    protected SurfaceMaterialProvider surface;
    protected BiomeFactory biomeFactory;

    protected EndBiomeBuilder(
            BiomeBootstrapContext context,
            BiomeKey<EndBiomeBuilder> key,
            TagKey<Biome>... biomeTag
    ) {
        super(context, key);
        this.surface = EndBiome.Config.DEFAULT_MATERIAL;
        this
                .music(SoundEvents.MUSIC_END)
                .waterColor(BiomeBuilder.DEFAULT_END_WATER_COLOR)
                .waterFogColor(BiomeBuilder.DEFAULT_END_WATER_FOG_COLOR)
                .fogColor(BiomeBuilder.DEFAULT_END_FOG_COLOR)
                .skyColor(BiomeBuilder.DEFAULT_END_SKY_COLOR)
                .mood(EndSounds.AMBIENT_DUST_WASTELANDS)
                .temperature(BiomeBuilder.DEFAULT_END_TEMPERATURE)
                .downfall(BiomeBuilder.DEFAULT_END_WETNESS)
                .hasPrecipitation(false)
                .intendedPlacement(biomeTag.length > 0 ? biomeTag[0] : null)
                .tag(biomeTag);
    }

    EndBiomeBuilder configure(EndBiome.Config biomeConfig) {
        // 26.3: SurfaceRules.isBiome resolves its keys into a HolderSet eagerly, so
        // SurfaceRuleBuilder.build() gained a HolderGetter<Biome> parameter. The lookup is only
        // consulted when a biome filter was set on the builder - which none of BetterEnd's
        // SurfaceMaterialProviders do - but the bootstrap context we were constructed with provides a
        // real one anyway, so no null is handed to WoVer.
        this.startSurface()
            .rule(biomeConfig.surfaceMaterial().surface().build(bootstrapContext.lookup(Registries.BIOME)))
            .finishSurface();

        this.surface = biomeConfig.surfaceMaterial();

        this.hasCave = biomeConfig.hasCaves();
        if (biomeConfig.hasReturnGateway()) {
            this.feature(GenerationStep.Decoration.SURFACE_STRUCTURES, EndPlacements.END_GATEWAY_RETURN);
        }
        biomeConfig.addCustomBuildData(this);

        EndFeatures.addDefaultFeatures(this, biomeConfig.hasCaves());
        this.biomeFactory = biomeConfig;

        return this;
    }

    public static <C extends EndBiome.Config> EndBiomeKey<C, ?> createKey(
            @NotNull String name
    ) {
        return new EndBiomeKey<>(
                BetterEnd.C.id(name.replace(' ', '_').toLowerCase()),
                null
        );
    }


    public static <C extends EndBiome.Config, PC extends EndBiome.Config> EndBiomeKey<C, PC> createKey(
            @NotNull String name,
            @Nullable EndBiomeKey<PC, ?> parentOrNull
    ) {
        return new EndBiomeKey<>(
                BetterEnd.C.id(name.replace(' ', '_').toLowerCase()),
                parentOrNull
        );
    }

    @Override
    public void registerBiomeData(BootstrapContext<BiomeData> dataContext) {
        final EndBiome biome = biomeFactory.instantiateBiome(fogDensity, key, new BiomeGenerationDataContainer(parameters, intendedPlacement), terrainHeight, genChance, edgeSize, vertical, edge, parent, hasCave, surface);
        biome.datagenSetup(dataContext);
        dataContext.register(key.dataKey, biome);
    }

    @FunctionalInterface
    public interface BiomeFactory {
        @NotNull
        EndBiome instantiateBiome(
                float fogDensity,
                BiomeKey<?> key,
                @NotNull BiomeGenerationDataContainer generatorData,
                float terrainHeight,
                float genChance,
                int edgeSize,
                boolean vertical,
                @Nullable ResourceKey<Biome> edge,
                @Nullable ResourceKey<Biome> parent,
                boolean hasCave,
                SurfaceMaterialProvider surface
        );
    }
}



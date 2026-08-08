package org.betterx.betterend.world.carvers;

import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.generator.TerrainGenerator;
import de.ambertation.wover.generator.api.biomesource.end.WoverEndConfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

/**
 * {@link WorldCarver} port of the legacy {@code RoundCaveFeature}: carves a single noise-distorted
 * ellipsoidal cavern per start chunk. Shape math is a 1:1 port of {@code RoundCaveFeature#generate} &ndash;
 * same {@link OpenSimplexNoise} seed ({@code MHelper.getSeed(534, centerX, centerZ)}), same
 * {@code hr = radius * 0.75}, {@code nr = radius * 0.25} noise radius blend, and same vertical squash (the
 * {@code (y-cy)} distance term is multiplied by {@code vertical_squash} before it enters the sphere test).
 *
 * <h2>26.3 carver rewrite</h2>
 * The carver API was rebuilt from the ground up in 26.3 and this class had to follow:
 * <ul>
 *     <li>{@code WorldCarver} is an interface now, no longer a generic abstract class, and carvers carry
 *     their own configuration directly - exactly the collapse {@code ConfiguredFeature -> Feature} went
 *     through. {@code CarverConfiguration}, {@code ConfiguredWorldCarver} and {@code CarverDebugSettings}
 *     are all deleted, so {@code EndCaveCarverConfiguration} is gone and its two extra fields
 *     ({@code radius}, {@code vertical_squash}) are record components here. The dispatch codec lives in
 *     {@code BuiltInRegistries.CARVER_TYPE}; the configured instances live in the {@code Registries.CARVER}
 *     datapack registry (was {@code CONFIGURED_CARVER}).</li>
 *     <li>{@code carve} lost <em>all</em> world access. Its parameters are now
 *     {@code (WorldGenerationContext, RandomSource, ChunkPos current, ChunkPos start, CarverOutput)}: no
 *     {@code ChunkAccess}, no biome getter, no {@code Aquifer}, no {@code CarvingMask}. A carver may only
 *     record chunk-local {@code (x, y, z)} bits through {@link CarverOutput#carve}; the chunk generator
 *     applies the finished mask afterwards, resolving each masked position against the aquifer and
 *     re-running the surface rules where needed.</li>
 * </ul>
 *
 * <h2>What that removed here</h2>
 * <ul>
 *     <li><b>Replaceability.</b> The {@code replaceable} {@code HolderSet<Block>} left the config together
 *     with {@code CarverConfiguration}; nothing in 26.3 consults a block tag while carving (vanilla deleted
 *     {@code minecraft:*_carver_replaceables} for the same reason). Whatever the mask marks is carved.</li>
 *     <li><b>The {@code SURFACE_ROOF} guard.</b> It kept the top five blocks below {@code WORLD_SURFACE_WG}
 *     uncarved so a tall cavern could not breach an island top and open a pit to the sky. There is no
 *     heightmap - and no chunk - to read any more. The whole cavern is still capped at
 *     {@link #caveBandCeiling()}, which is the guard that matters for islands sitting <em>above</em> the
 *     band, but a column whose island surface lies <em>below</em> the ceiling no longer gets a guaranteed
 *     roof. Worth a look during the runtime smoke test.</li>
 *     <li><b>{@code isWaterNear}.</b> Defensive only (the End disables aquifers) and impossible without a
 *     chunk; 26.3 resolves fluid per masked position through {@code Aquifer#computeSubstance} anyway.</li>
 *     <li><b>The cave-surface coat.</b> {@code CaveSurfaceCoater} wrote the cave biome's floor/ceiling/wall
 *     materials into the blocks an invocation had just carved. Writing blocks from a carver is no longer
 *     possible at all, so it is deleted; {@code EndCaveBiome#getCeil}/{@code #getWall} are left in place for
 *     whatever re-implements it (a decoration feature over the carved band is the obvious shape).</li>
 * </ul>
 */
public record EndCaveCarver(
        float probability,
        HeightProvider y,
        IntProvider radius,
        float verticalSquash
) implements WorldCarver {
    public static final MapCodec<EndCaveCarver> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.floatRange(0.0F, 1.0F)
                         .fieldOf("probability")
                         .forGetter(EndCaveCarver::probability),
                    HeightProvider.CODEC.fieldOf("y").forGetter(EndCaveCarver::y),
                    IntProviders.CODEC.fieldOf("radius").forGetter(EndCaveCarver::radius),
                    Codec.FLOAT.fieldOf("vertical_squash").forGetter(EndCaveCarver::verticalSquash)
            ).apply(instance, EndCaveCarver::new)
    );

    /** Fallback cave-band ceiling used only before the End generator config is initialised. */
    private static final int DEFAULT_BAND_CEILING = WoverEndConfig.DEFAULT_CAVE_BIOMES_TOP_Y;

    @Override
    public MapCodec<EndCaveCarver> codec() {
        return MAP_CODEC;
    }

    @Override
    public boolean isStartChunk(RandomSource random) {
        return random.nextFloat() <= probability;
    }

    /**
     * Kept at the legacy value. Note that {@code NoiseBasedChunkGenerator#applyCarvers} in 26.3 walks a fixed
     * 17x17 chunk neighbourhood and never consults {@code getRange()}; it is declared only because the
     * interface offers it and other chunk generators may.
     */
    @Override
    public int getRange() {
        return 3;
    }

    /**
     * The highest world Y a cave may carve: the top of the vertical cave-biome band
     * ({@link WoverEndConfig#caveBiomesTopY}). Caves ARE that band, so carving must never remove terrain
     * above it - otherwise a tall cavern reaches up across the void gap and hollows the underside of a
     * floating island that sits well above the band, leaving a thin flat-bottomed slab. Read from the
     * live End generator config (via {@link TerrainGenerator#config}); falls back to the default only
     * before {@code initNoise} has run.
     */
    static int caveBandCeiling() {
        final WoverEndConfig cfg = TerrainGenerator.config;
        return cfg != null ? cfg.caveBiomesTopY : DEFAULT_BAND_CEILING;
    }

    @Override
    public boolean carve(
            WorldGenerationContext context,
            RandomSource random,
            ChunkPos chunkPos,
            ChunkPos startChunkPos,
            CarverOutput output
    ) {
        // Center is chosen inside the *start* chunk; the cavern may reach into the chunk we are
        // currently masking, so below we only touch columns of `chunkPos`.
        //
        // (a) Draw center x/z/y and radius EXACTLY as before. Vanilla applyCarvers visits this start
        //     chunk once per target chunk in its 17x17 neighborhood loop, and every visit MUST
        //     consume the same four `random` draws in the same order so the cavern geometry is
        //     identical no matter which target chunk triggered the invocation. These draws are
        //     therefore unconditional and precede any early-out.
        final int centerX = startChunkPos.getBlockX(random.nextInt(16));
        final int centerZ = startChunkPos.getBlockZ(random.nextInt(16));
        final int centerY = y.sample(random, context);
        final int rad = radius.sample(random);
        final float squash = verticalSquash;

        final int reach = rad + 5;

        final int minBX = chunkPos.getMinBlockX();
        final int minBZ = chunkPos.getMinBlockZ();

        // (b) O(1) early rejection. The cavern's horizontal footprint is the axis-aligned box
        //     [centerX +- reach] x [centerZ +- reach], where reach = radius + noise margin 5 is the
        //     widest the noise-distorted sphere can extend. If that box does not overlap this chunk's
        //     16-wide block-column range, no column here can satisfy dist < r*r, so nothing would be
        //     written. The four random draws above are already consumed, so this early-out cannot
        //     change the world: a rejected invocation could not have carved a single block anyway.
        if (centerX + reach < minBX || centerX - reach > minBX + 15
                || centerZ + reach < minBZ || centerZ - reach > minBZ + 15) {
            return false;
        }

        // Constructed only after the reject: the noise depends solely on centerX/centerZ (identical
        // for every target chunk of this start chunk), so building it here instead of before the
        // early-out changes no output.
        final OpenSimplexNoise noise = new OpenSimplexNoise(MHelper.getSeed(534, centerX, centerZ));

        final double hr = rad * 0.75;
        final double nr = rad * 0.25;

        // The mask's own bounds are the hard limit now - writing outside them would index past its BitSet.
        final int minGenY = Math.max(context.getMinGenY(), output.minY());
        final int maxGenY = Math.min(context.getMinGenY() + context.getGenDepth() - 1, output.maxY());

        // Cap the whole cavern at the cave-band ceiling so it can never carve terrain above the band
        // (e.g. the underside of a floating island sitting above it - the flat-slab bug).
        final int bandCeiling = caveBandCeiling();
        final int y1 = Math.max(MHelper.floor(centerY - reach / squash), minGenY);
        final int y2 = Math.min(Math.min(MHelper.floor(centerY + reach / squash), maxGenY), bandCeiling);
        if (y1 > y2) {
            return false;
        }

        boolean carved = false;
        for (int lx = 0; lx < 16; lx++) {
            final int x = minBX + lx;
            if (Math.abs(x - centerX) > reach) {
                continue;
            }
            final int xsq = MHelper.sqr(x - centerX);
            for (int lz = 0; lz < 16; lz++) {
                final int z = minBZ + lz;
                if (Math.abs(z - centerZ) > reach) {
                    continue;
                }
                final int zsq = MHelper.sqr(z - centerZ);
                final int dxz = xsq + zsq;
                for (int cy = y1; cy <= y2; cy++) {
                    final int ysq = (int) MHelper.sqr((cy - centerY) * squash);
                    final double r = noise.eval(x * 0.1, cy * 0.1, z * 0.1) * nr + hr;
                    final double dist = dxz + ysq;
                    if (dist < r * r) {
                        output.carve(lx, cy, lz);
                        carved = true;
                    }
                }
            }
        }

        return carved;
    }
}

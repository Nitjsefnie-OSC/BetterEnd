package org.betterx.betterend.world.carvers;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.generator.TerrainGenerator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

/**
 * {@link WorldCarver} port of the legacy {@code TunelCaveFeature}: carves a continuous, noise-field driven
 * tunnel network. {@link #getRange()} is {@code 0} because the underlying noise field is spatially continuous
 * &ndash; each chunk carves only its own columns and the tunnels line up across chunk borders automatically.
 * <p>
 * The three {@link OpenSimplexNoise} instances ({@code noiseH}/{@code noiseV}/{@code noiseD}) are built in the
 * exact same order as the legacy feature: {@code new LegacyRandomSource(seed)} then three {@code nextInt()}
 * draws.
 *
 * <h2>26.3 carver rewrite</h2>
 * See {@link EndCaveCarver} for the shape of the new API. Three things this carver used are simply no longer
 * reachable from {@code carve}, and each was resolved as follows:
 * <ul>
 *     <li><b>The noise seed.</b> It used to come from {@code CarvingContext#randomState()} (a positional
 *     random factory keyed on a fixed identifier). {@code WorldGenerationContext} carries no
 *     {@code RandomState} and the {@code RandomSource} handed to {@code carve} is re-seeded per chunk, which
 *     a continuous field cannot use. The seed now comes from {@link TerrainGenerator#noiseSeed}, which the
 *     End generator sets from the world seed in {@code initNoise} - so the tunnel layout still differs per
 *     world, which was the explicit requirement of the old comment here.</li>
 *     <li><b>The per-corner cave-biome blend.</b> The four corner factors came from a biome getter
 *     ({@code 1.0} when the corner biome {@code is(IS_END_CAVE)}) and produced a bilinear {@code density}
 *     that both gated the column ({@code density < 0.5}) and biased the noise. {@code carve} has no biome
 *     access at all now. It does not need the gate: carvers are per-biome to begin with (the generator looks
 *     the carver list up on the start chunk's biome), and this carver is only attached to
 *     {@code EndCaveBiome}, so every invocation is already "inside a cave biome" - the case where all four
 *     corners were {@code 1.0} and {@code density} was exactly {@code 0}. That value is now a constant. What
 *     is lost is the smooth taper at cave-biome <em>borders</em>: tunnels now run at full strength right up
 *     to the edge chunk instead of thinning out across it.</li>
 *     <li><b>The surface height.</b> {@code chunk.getHeight(WORLD_SURFACE_WG, lx, lz)} drove both the y-loop
 *     bound and the {@code gradient} roof guard that stopped tunnels five blocks below the island top. There
 *     is no chunk and no heightmap in the new API, and BetterEnd's own
 *     {@code TerrainGenerator#getSurfaceHeight} is a locked full-column density solve - far too expensive for
 *     256 columns per chunk. Both now key off {@link EndCaveCarver#caveBandCeiling()} instead, i.e. the top
 *     of the vertical cave-biome band: the loop stops there and the gradient tapers over the last ten blocks
 *     below it. That keeps tunnels inside the band (which is what the band means) but no longer guarantees a
 *     roof over an island whose surface happens to sit <em>below</em> the ceiling. Flagged for the runtime
 *     smoke test.</li>
 * </ul>
 * The block-level {@code is(END_STONES)} test and the {@code isWaterNear} check are gone with the same API
 * change; masking a position that is already void air is a no-op for the generator, which resolves every
 * masked position through the aquifer before writing.
 */
public record EndTunnelCarver(float probability, float threshold) implements WorldCarver {
    public static final MapCodec<EndTunnelCarver> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.floatRange(0.0F, 1.0F)
                         .fieldOf("probability")
                         .forGetter(EndTunnelCarver::probability),
                    Codec.FLOAT.fieldOf("threshold").forGetter(EndTunnelCarver::threshold)
            ).apply(instance, EndTunnelCarver::new)
    );

    /**
     * Inside a cave biome all four legacy corner factors were {@code 1.0}, so the bilinear blend collapsed to
     * {@code 1 - 1 == 0}. See the class doc for why the blend itself could not be kept.
     */
    private static final float DENSITY = 0F;

    private record Noises3(long seed, OpenSimplexNoise h, OpenSimplexNoise v, OpenSimplexNoise d) {}

    // Carving runs per chunk; cache the three noises so we don't reallocate them for every chunk.
    // Keyed on the derived seed so a world reload with a different seed rebuilds them.
    private static volatile Noises3 cached;

    @Override
    public MapCodec<EndTunnelCarver> codec() {
        return MAP_CODEC;
    }

    @Override
    public boolean isStartChunk(RandomSource random) {
        return random.nextFloat() <= probability;
    }

    @Override
    public int getRange() {
        return 0;
    }

    @Override
    public boolean carve(
            WorldGenerationContext context,
            RandomSource random,
            ChunkPos chunkPos,
            ChunkPos startChunkPos,
            CarverOutput output
    ) {
        // NoiseBasedChunkGenerator.applyCarvers iterates a FIXED 17x17 chunk neighborhood around every
        // generating chunk and, for each neighbor that isStartChunk, calls this carve() with chunkPos = the
        // CURRENT chunk being generated and startChunkPos = the NEIGHBOR chunk, so up to 17*17 = 289
        // invocations hit the same current chunk. This tunnel carver's noise field is chunk-local and fully
        // deterministic: it reads only the current chunk's own columns/noise and ignores startChunkPos
        // entirely, so all 289 invocations produce byte-for-byte identical output. Carving once - on the
        // chunk's own start invocation - therefore yields the identical world at 1/289th the cost.
        // isStartChunk stays probability-based; probability is 1.0 in data, so every chunk still self-carves.
        if (!startChunkPos.equals(chunkPos)) {
            return false;
        }

        final Noises3 noises = getNoises();
        if (noises == null) {
            return false; // generator not initialised yet - nothing sane to carve against
        }

        final int x1 = chunkPos.getMinBlockX();
        final int z1 = chunkPos.getMinBlockZ();

        // The mask's own bounds are the hard limit now - writing outside them would index past its BitSet.
        // Its minY is exactly context.getMinGenY() + 1, i.e. the `minGenY + 1` the loop used to start at.
        final int minGenY = Math.max(context.getMinGenY(), output.minY());
        final int maxGenY = Math.min(context.getMinGenY() + context.getGenDepth() - 1, output.maxY());
        // Caves are the vertical cave-biome band; never carve terrain above its ceiling, or a tunnel eats
        // into floating islands that sit above the band (see EndCaveCarver#caveBandCeiling). This also
        // stands in for the removed WORLD_SURFACE_WG heightmap in the gradient guard below.
        final int ceiling = Math.min(EndCaveCarver.caveBandCeiling(), maxGenY);

        boolean carved = false;
        for (int lx = 0; lx < 16; lx++) {
            final int wx = lx + x1;
            for (int lz = 0; lz < 16; lz++) {
                final int wz = lz + z1;
                for (int y = minGenY; y <= ceiling; y++) {
                    final float gradient = 1 - Mth.clamp((ceiling - y) * 0.1F, 0F, 1F);
                    if (gradient > 0.5F) {
                        break;
                    }
                    float val = Mth.abs((float) noises.h.eval(wx * 0.02, y * 0.01, wz * 0.02));
                    final float vert = Mth.sin((y + (float) noises.v.eval(wx * 0.01, wz * 0.01) * 20) * 0.1F) * 0.9F;
                    final float dist = (float) noises.d.eval(wx * 0.1, y * 0.1, wz * 0.1) * 0.12F;
                    val = (val + vert * vert + dist) + DENSITY + gradient;
                    if (val < threshold) {
                        output.carve(lx, y, lz);
                        carved = true;
                    }
                }
            }
        }

        return carved;
    }

    /**
     * Builds (or returns the cached) three noises, seeded off the End generator's world seed. Returns
     * {@code null} before {@code TerrainGenerator#initNoise} has run, which can only happen if this carver is
     * attached to a biome outside the BetterEnd generator.
     */
    private static Noises3 getNoises() {
        final long seed = TerrainGenerator.noiseSeed;
        if (seed == 0L) {
            return null;
        }
        final Noises3 local = cached;
        if (local != null && local.seed == seed) {
            return local;
        }
        final RandomSource rand = new LegacyRandomSource(seed);
        final Noises3 built = new Noises3(
                seed,
                new OpenSimplexNoise(rand.nextInt()),
                new OpenSimplexNoise(rand.nextInt()),
                new OpenSimplexNoise(rand.nextInt())
        );
        cached = built;
        return built;
    }
}

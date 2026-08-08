package org.betterx.datagen.betterend.worldgen;

import org.betterx.betterend.registry.EndCarvers;
import org.betterx.betterend.world.carvers.EndCaveCarver;
import org.betterx.betterend.world.carvers.EndTunnelCarver;
import de.ambertation.wover.core.api.ModCore;
import de.ambertation.wover.datagen.api.WoverRegistryContentProvider;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

/**
 * Datapack registry provider for the two BetterEnd carvers ({@code betterend:round_cave} and
 * {@code betterend:tunnel_cave}).
 * <p>
 * 26.3 collapsed {@code ConfiguredWorldCarver<?>} into {@link WorldCarver} - a carver carries its own
 * configuration now - and renamed the registry from {@code Registries.CONFIGURED_CARVER} to
 * {@link Registries#CARVER}. So this provider registers carver instances directly instead of calling
 * {@code carver.configured(config)}, and the fields that lived on the deleted {@code CarverConfiguration}
 * base ({@code yScale}, {@code lava_level}, {@code replaceable}, {@code debug_settings}) are gone with it:
 * nothing in the new carve path consults them, so they are not carried over as placeholders either.
 */
public class CarverProvider extends WoverRegistryContentProvider<WorldCarver> {
    public CarverProvider(ModCore modCore) {
        super(modCore, "BetterEnd - Carvers", Registries.CARVER);
    }

    @Override
    protected void bootstrap(BootstrapContext<WorldCarver> context) {
        // Round cavern: legacy RoundCaveFeature -> uniform radius 10..30, vertical squash 1.6.
        // Top tracks WoverEndConfig#DEFAULT_CAVE_BIOMES_TOP_Y (32 -> 56 -> 48): carving is hard-capped at
        // the cave-biome band ceiling anyway (see EndCaveCarver#caveBandCeiling), so a cavern-center range
        // that stopped short of the ceiling left the band's upper reaches biome territory but rarely
        // actually carved. 56 (matching the band's first raise) fixed missing floors but let caves breach
        // island surfaces with only a block or two of roof in many places, since small islands can center
        // as low as Y40; 56 pulled back to match the band's own correction to 48. Bottom kept at 8 - this
        // widens the band upward rather than shifting it, so existing low-elevation caves are unaffected.
        context.register(
                EndCarvers.ROUND_CAVE,
                new EndCaveCarver(
                        0.4F,
                        UniformHeight.of(VerticalAnchor.absolute(8), VerticalAnchor.absolute(56)),
                        UniformInt.of(10, 30),
                        1.6F
                )
        );

        // Continuous tunnel network: legacy TunelCaveFeature -> threshold 0.15, probability 1.0.
        context.register(
                EndCarvers.TUNNEL_CAVE,
                new EndTunnelCarver(1.0F, 0.15F)
        );
    }
}

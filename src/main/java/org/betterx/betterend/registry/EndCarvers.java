package org.betterx.betterend.registry;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.world.carvers.EndCaveCarver;
import org.betterx.betterend.world.carvers.EndTunnelCarver;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

/**
 * Registers the two BetterEnd {@link WorldCarver} types and exposes the {@link ResourceKey}s of their
 * configured instances.
 * <p>
 * 26.3 collapsed the carver registries the same way it collapsed features: a {@link WorldCarver} now carries
 * its own configuration and its own {@link MapCodec}, so the built-in registry holds the <em>codecs</em>
 * ({@link BuiltInRegistries#CARVER_TYPE}, was {@code BuiltInRegistries.CARVER}) while the configured
 * instances live in the {@link Registries#CARVER} datapack registry (was {@code CONFIGURED_CARVER}), where
 * {@code CarverProvider} writes them.
 */
public class EndCarvers {
    public static final MapCodec<EndCaveCarver> END_ROUND_CAVE =
            registerType("end_round_cave", EndCaveCarver.MAP_CODEC);
    public static final MapCodec<EndTunnelCarver> END_TUNNEL_CAVE =
            registerType("end_tunnel_cave", EndTunnelCarver.MAP_CODEC);

    public static final ResourceKey<WorldCarver> ROUND_CAVE = carverKey("round_cave");
    public static final ResourceKey<WorldCarver> TUNNEL_CAVE = carverKey("tunnel_cave");

    private static <C extends WorldCarver> MapCodec<C> registerType(String name, MapCodec<C> codec) {
        return Registry.register(BuiltInRegistries.CARVER_TYPE, BetterEnd.C.mk(name), codec);
    }

    private static ResourceKey<WorldCarver> carverKey(String name) {
        return ResourceKey.create(Registries.CARVER, BetterEnd.C.mk(name));
    }

    /**
     * Referencing this class triggers static initialization, which performs the
     * {@link BuiltInRegistries#CARVER_TYPE} registrations above. Called from
     * {@code BetterEnd.onInitialize}.
     */
    public static void ensureStaticallyLoaded() {
    }
}

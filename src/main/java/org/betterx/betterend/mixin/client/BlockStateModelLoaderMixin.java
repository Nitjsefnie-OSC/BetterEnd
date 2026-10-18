package org.betterx.betterend.mixin.client;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.world.generator.GeneratorOptions;

import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link BlockStateModelLoader#loadBlockStates} first lists every blockstate json file (as a
 * file-id to resource-stack map) and then turns each entry into the models of the block it defines.
 * To make betterend's {@code custom_chorus_plant}/{@code custom_chorus_flower} files (shipped in the
 * betterend namespace) act as the definition for {@code minecraft:chorus_plant}/{@code chorus_flower},
 * we rewrite that listing: the resource stack of the custom file replaces the one of the vanilla file,
 * and the custom file itself is dropped (no block with that id exists).
 * <p>
 * Swapping the listing, rather than the per-entry file-to-id resolution, keeps every entry consistent
 * (file name and block id match), which matters for mods like ModernFix that load the listed
 * blockstates lazily by block id (#599).
 * <p>
 * The listing call lives inside the lambda passed to {@code CompletableFuture.supplyAsync} in
 * {@code loadBlockStates}, not in {@code loadBlockStates} itself, so the mixin must target that
 * synthetic lambda method ({@code lambda$loadBlockStates$0} as of 26.1.2 - this is a compiler-generated
 * name, not covered by official Mojang mappings, so it can and does shift between builds; verify via
 * javap if this mixin ever fails to apply again) rather than {@code loadBlockStates} directly.
 */
@Mixin(BlockStateModelLoader.class)
public abstract class BlockStateModelLoaderMixin {
    @Unique
    private static final FileToIdConverter BE_BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");

    @Inject(method = "lambda$loadBlockStates$0", at = @At("RETURN"), cancellable = true)
    private static void be_switchModelOnLoad(
            ResourceManager resourceManager,
            CallbackInfoReturnable<Map<Identifier, List<Resource>>> cir
    ) {
        if (!GeneratorOptions.changeChorusPlant()) return;

        Map<Identifier, List<Resource>> resources = new HashMap<>(cir.getReturnValue());
        be_replaceDefinition(resources, "chorus_plant");
        be_replaceDefinition(resources, "chorus_flower");
        cir.setReturnValue(resources);
    }

    @Unique
    private static void be_replaceDefinition(Map<Identifier, List<Resource>> resources, String path) {
        List<Resource> custom = resources.remove(BE_BLOCKSTATE_LISTER.idToFile(BetterEnd.C.mk("custom_" + path)));
        if (custom != null) {
            resources.put(BE_BLOCKSTATE_LISTER.idToFile(Identifier.withDefaultNamespace(path)), custom);
        }
    }
}

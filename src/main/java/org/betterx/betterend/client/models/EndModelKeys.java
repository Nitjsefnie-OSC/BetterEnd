package org.betterx.betterend.client.models;

import org.betterx.betterend.BetterEnd;
import de.ambertation.wover.block.api.model.ModelKey;
import de.ambertation.wover.block.api.trait.BlockTraitKey;

/**
 * Creates the {@link ModelKey}s for BetterEnd's model shapes. Each shape declares its key next to the common factory
 * that binds it; the client generator behind the key is registered by
 * {@link org.betterx.betterend.client.models.EndClientModels} through the {@code wover.client.traits} entrypoint.
 */
public final class EndModelKeys {
    private EndModelKeys() {
    }

    public static <P> ModelKey<P> key(String path) {
        return new ModelKey<>(BlockTraitKey.ofUnique(BetterEnd.C, "model/" + path));
    }
}

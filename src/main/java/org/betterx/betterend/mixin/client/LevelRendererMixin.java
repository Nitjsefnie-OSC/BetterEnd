package org.betterx.betterend.mixin.client;

import org.betterx.betterend.client.render.BetterEndSkyRenderer;

import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher.PreparedFrame;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code LevelRenderEvents#START_MAIN} (used by {@link BetterEndSkyRenderer}) fires from inside
 * {@code LevelRenderer#executeSolid}, i.e. while that method's own {@code RenderPass} (opened by
 * the caller, {@code LevelRenderer#addMainPass}) is already active. The frontend command encoder
 * only ever allows one open render pass at a time, so opening a second one there — which is what
 * {@link BetterEndSkyRenderer} used to do — crashes with "Close the existing render pass before
 * creating a new one!". Fabric's {@code LevelTerrainRenderContext} doesn't expose that pass, so it
 * is captured here instead and handed to {@link BetterEndSkyRenderer} to draw into directly.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "executeSolid", at = @At("HEAD"))
    private void be_captureSolidRenderPass(
            ChunkSectionsToRender chunkSectionsToRender,
            PreparedFrame featureFrame,
            RenderPass renderPass,
            CallbackInfo ci
    ) {
        BetterEndSkyRenderer.setActiveSolidRenderPass(renderPass);
    }

    @Inject(method = "executeSolid", at = @At("RETURN"))
    private void be_releaseSolidRenderPass(CallbackInfo ci) {
        BetterEndSkyRenderer.setActiveSolidRenderPass(null);
    }
}

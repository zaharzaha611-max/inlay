package com.example.inlay.client;

import com.example.inlay.InlayMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class InlayClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRendererFactories.register(InlayMod.INLAID_BLOCK_ENTITY, InlaidBlockEntityRenderer::new);
	}
}

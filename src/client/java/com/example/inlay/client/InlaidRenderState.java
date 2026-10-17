package com.example.inlay.client;

import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.texture.Sprite;

/** Всё, что нужно рендереру для отрисовки, собирается заранее в updateRenderState. */
public class InlaidRenderState extends BlockEntityRenderState {
	public Sprite hostSprite;
	public final Sprite[] inlaySprites = new Sprite[6]; // по Direction.getIndex()
	public final int[] faceLight = new int[6];
}

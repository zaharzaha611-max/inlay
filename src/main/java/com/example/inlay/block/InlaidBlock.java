package com.example.inlay.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Блок-носитель. Сам ничего не рисует (BlockWithEntity по умолчанию невидим) -
 * всю графику делает InlaidBlockEntityRenderer на клиенте.
 */
public class InlaidBlock extends BlockWithEntity {
	public static final MapCodec<InlaidBlock> CODEC = createCodec(InlaidBlock::new);

	public InlaidBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new InlaidBlockEntity(pos, state);
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		// В выживании выпадает исходный блок и все вставленные блоки.
		if (!world.isClient() && !player.isCreative() && world.getBlockEntity(pos) instanceof InlaidBlockEntity be) {
			be.dropContents(world, pos);
		}
		return super.onBreak(world, pos, state, player);
	}
}

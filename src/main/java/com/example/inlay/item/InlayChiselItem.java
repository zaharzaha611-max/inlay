package com.example.inlay.item;

import com.example.inlay.InlayMod;
import com.example.inlay.block.InlaidBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Резец.
 *  ПКМ по грани, в ОСНОВНОЙ руке резец, в ЛЕВОЙ руке (off-hand) любой блок -> блок «утапливается» в эту грань.
 *  Shift + ПКМ -> достаёт вставленный блок из этой грани; когда вставок не осталось, исходный блок возвращается.
 */
public class InlayChiselItem extends Item {
	public InlayChiselItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		World world = ctx.getWorld();
		PlayerEntity player = ctx.getPlayer();
		if (player == null) return ActionResult.PASS;

		BlockPos pos = ctx.getBlockPos();
		Direction face = ctx.getSide();
		BlockState target = world.getBlockState(pos);
		boolean alreadyInlaid = target.isOf(InlayMod.INLAID_BLOCK);

		// ---- извлечение ----
		if (player.isSneaking()) {
			if (!alreadyInlaid) return ActionResult.PASS;
			if (world.isClient()) return ActionResult.SUCCESS;
			if (!(world.getBlockEntity(pos) instanceof InlaidBlockEntity be)) return ActionResult.PASS;

			Block old = be.getInlay(face);
			if (old == null) return ActionResult.PASS;

			be.setInlay(face, null);
			giveBack(player, old);
			if (!be.hasAnyInlay()) {
				world.setBlockState(pos, be.getHost().getDefaultState());
			}
			world.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.8f, 1.2f);
			return ActionResult.SUCCESS;
		}

		// ---- вставка ----
		ItemStack offHand = player.getOffHandStack();
		if (!(offHand.getItem() instanceof BlockItem blockItem)) return ActionResult.PASS;
		Block inlay = blockItem.getBlock();
		if (inlay == InlayMod.INLAID_BLOCK) return ActionResult.PASS;

		if (!alreadyInlaid && !canBeHost(world, pos, target)) return ActionResult.PASS;
		if (world.isClient()) return ActionResult.SUCCESS;

		if (!alreadyInlaid) {
			Block hostBlock = target.getBlock();
			world.setBlockState(pos, InlayMod.INLAID_BLOCK.getDefaultState());
			if (world.getBlockEntity(pos) instanceof InlaidBlockEntity be) {
				be.setHost(hostBlock);
			}
		}

		if (world.getBlockEntity(pos) instanceof InlaidBlockEntity be) {
			Block previous = be.getInlay(face);
			if (previous == inlay) return ActionResult.PASS;
			if (previous != null) giveBack(player, previous);

			be.setInlay(face, inlay);
			if (!player.isCreative()) offHand.decrement(1);
			world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.8f, 1.0f);
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	/** Хостом может быть только обычный цельный куб без блок-сущности и не неразрушимый. */
	private static boolean canBeHost(World world, BlockPos pos, BlockState state) {
		if (state.isAir() || state.hasBlockEntity()) return false;
		if (state.getHardness(world, pos) < 0) return false; // бедрок и т.п.
		if (!state.isFullCube(world, pos)) return false;
		return state.getFluidState().isEmpty();
	}

	private static void giveBack(PlayerEntity player, Block block) {
		if (block.asItem() == net.minecraft.item.Items.AIR) return;
		if (!player.isCreative()) {
			player.getInventory().offerOrDrop(new ItemStack(block));
		}
	}
}

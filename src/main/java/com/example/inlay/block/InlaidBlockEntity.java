package com.example.inlay.block;

import com.example.inlay.InlayMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Хранит: какой блок «под» нами (host) и какой блок вставлен в каждую из 6 граней. */
public class InlaidBlockEntity extends BlockEntity {
	private Block host = Blocks.STONE;
	private final Block[] inlays = new Block[6]; // индекс = Direction.getIndex(), null = пусто

	public InlaidBlockEntity(BlockPos pos, BlockState state) {
		super(InlayMod.INLAID_BLOCK_ENTITY, pos, state);
	}

	public Block getHost() {
		return host;
	}

	@Nullable
	public Block getInlay(Direction face) {
		return inlays[face.getIndex()];
	}

	public boolean hasAnyInlay() {
		for (Block b : inlays) {
			if (b != null) return true;
		}
		return false;
	}

	public void setHost(Block host) {
		this.host = host;
		sync();
	}

	public void setInlay(Direction face, @Nullable Block block) {
		inlays[face.getIndex()] = block;
		sync();
	}

	private void sync() {
		markDirty();
		if (world != null) {
			BlockState s = getCachedState();
			world.updateListeners(pos, s, s, Block.NOTIFY_ALL);
		}
	}

	public void dropContents(World world, BlockPos pos) {
		if (host.asItem() != net.minecraft.item.Items.AIR) {
			Block.dropStack(world, pos, new ItemStack(host));
		}
		for (Block b : inlays) {
			if (b != null && b.asItem() != net.minecraft.item.Items.AIR) {
				Block.dropStack(world, pos, new ItemStack(b));
			}
		}
	}

	// ---------- сохранение ----------

	private static String idOf(@Nullable Block b) {
		return b == null ? "" : Registries.BLOCK.getId(b).toString();
	}

	@Nullable
	private static Block blockOf(String id) {
		Identifier parsed = id.isEmpty() ? null : Identifier.tryParse(id);
		if (parsed == null) return null;
		Block b = Registries.BLOCK.get(parsed);
		return b == Blocks.AIR ? null : b;
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putString("host", idOf(host));
		for (Direction d : Direction.values()) {
			view.putString("inlay_" + d.asString(), idOf(inlays[d.getIndex()]));
		}
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		Block h = blockOf(view.getString("host", ""));
		host = h == null ? Blocks.STONE : h;
		for (Direction d : Direction.values()) {
			inlays[d.getIndex()] = blockOf(view.getString("inlay_" + d.asString(), ""));
		}
	}

	// ---------- синхронизация с клиентом ----------

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}
}

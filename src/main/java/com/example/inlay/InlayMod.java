package com.example.inlay;

import com.example.inlay.block.InlaidBlock;
import com.example.inlay.block.InlaidBlockEntity;
import com.example.inlay.item.InlayChiselItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class InlayMod implements ModInitializer {
	public static final String MOD_ID = "inlay";

	public static InlaidBlock INLAID_BLOCK;
	public static BlockEntityType<InlaidBlockEntity> INLAID_BLOCK_ENTITY;
	public static Item INLAY_CHISEL;

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Служебный блок: в него превращается любой «хост», когда в него что-то вставили.
		Identifier blockId = id("inlaid_block");
		INLAID_BLOCK = Registry.register(Registries.BLOCK, blockId, new InlaidBlock(
				AbstractBlock.Settings.create()
						.registryKey(RegistryKey.of(RegistryKeys.BLOCK, blockId))
						.mapColor(MapColor.STONE_GRAY)
						.strength(1.5f, 6.0f)
						.sounds(BlockSoundGroup.STONE)));

		INLAID_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("inlaid_block"),
				FabricBlockEntityTypeBuilder.create(InlaidBlockEntity::new, INLAID_BLOCK).build());

		// Инструмент - резец.
		Identifier itemId = id("inlay_chisel");
		INLAY_CHISEL = Registry.register(Registries.ITEM, itemId, new InlayChiselItem(
				new Item.Settings()
						.registryKey(RegistryKey.of(RegistryKeys.ITEM, itemId))
						.maxCount(1)));

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(INLAY_CHISEL));
	}
}

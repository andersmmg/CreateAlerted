package com.andersmmg.create_alerted.datagen;

import com.andersmmg.create_alerted.Create_alerted;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Set.of(), List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)), lookupProvider);
    }

    public static class ModBlockLoot extends BlockLootSubProvider {
        public ModBlockLoot(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        }

        @Override
        protected void generate() {
            dropSelf(Create_alerted.BASIC_ALARM_BLOCK.get());
            dropSelf(Create_alerted.ANNOYING_ALARM_BLOCK.get());
            dropSelf(Create_alerted.BUZZ_ALARM_BLOCK.get());
        }

        @Override
        protected @NotNull Iterable<Block> getKnownBlocks() {
            return List.of(
                    Create_alerted.BASIC_ALARM_BLOCK.get(),
                    Create_alerted.ANNOYING_ALARM_BLOCK.get(),
                    Create_alerted.BUZZ_ALARM_BLOCK.get()
            );
        }
    }
}

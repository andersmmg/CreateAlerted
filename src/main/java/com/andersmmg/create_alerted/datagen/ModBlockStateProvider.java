package com.andersmmg.create_alerted.datagen;

import com.andersmmg.create_alerted.Create_alerted;
import com.andersmmg.create_alerted.block.AlarmBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Create_alerted.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        alarmBlock(Create_alerted.BASIC_ALARM_BLOCK.get());
        alarmBlock(Create_alerted.ANNOYING_ALARM_BLOCK.get());
        alarmBlock(Create_alerted.BUZZ_ALARM_BLOCK.get());
    }

    private void alarmBlock(Block block) {
        ModelFile offModel = models().getExistingFile(modLoc("block/alarm_off"));
        ModelFile onModel = models().getExistingFile(modLoc("block/alarm_on"));

        getVariantBuilder(block)
                .forAllStates(state -> {
                    Direction facing = state.getValue(AlarmBlock.FACING);
                    boolean lit = state.getValue(AlarmBlock.LIT);
                    int x = switch (facing) {
                        case DOWN -> 180;
                        case NORTH, SOUTH, WEST, EAST -> 90;
                        default -> 0;
                    };
                    int y = switch (facing) {
                        case NORTH -> 0;
                        case SOUTH -> 180;
                        case WEST -> 270;
                        case EAST -> 90;
                        default -> 0;
                    };
                    return ConfiguredModel.builder()
                            .modelFile(lit ? onModel : offModel)
                            .rotationX(x)
                            .rotationY(y)
                            .build();
                });
    }
}

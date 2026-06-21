package com.andersmmg.create_alerted.datagen;

import com.andersmmg.create_alerted.CreateAlerted;
import com.andersmmg.create_alerted.block.AlarmBlock;
import com.andersmmg.create_alerted.block.SmokeDetectorBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, CreateAlerted.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        alarmBlock(CreateAlerted.ALARM_BLOCK.get());
        smokeDetectorBlock(CreateAlerted.SMOKE_DETECTOR_BLOCK.get());
    }

    private void alarmBlock(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();

        ModelFile offPlain = models().withExistingParent("block/" + name + "_off_plain", modLoc("block/templates/alarm_plain"))
                .renderType("minecraft:cutout");
        ModelFile offCaged = models().withExistingParent("block/" + name + "_off_caged", modLoc("block/templates/alarm_cage"))
                .renderType("minecraft:cutout");
        ModelFile onPlain = models().withExistingParent("block/" + name + "_on_plain", modLoc("block/templates/alarm_plain"))
                .renderType("minecraft:cutout")
                .texture("lamp", modLoc("block/light_redstone"))
                .texture("particle", modLoc("block/light_redstone"));
        ModelFile onCaged = models().withExistingParent("block/" + name + "_on_caged", modLoc("block/templates/alarm_cage"))
                .renderType("minecraft:cutout")
                .texture("lamp", modLoc("block/light_redstone"))
                .texture("particle", modLoc("block/light_redstone"));

        getVariantBuilder(block)
                .forAllStates(state -> {
                    Direction facing = state.getValue(AlarmBlock.FACING);
                    boolean powered = state.getValue(AlarmBlock.POWERED);
                    boolean caged = state.getValue(AlarmBlock.CAGE);

                    ModelFile model;
                    if (caged) {
                        model = powered ? onCaged : offCaged;
                    } else {
                        model = powered ? onPlain : offPlain;
                    }

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
                            .modelFile(model)
                            .rotationX(x)
                            .rotationY(y)
                            .build();
                });
    }

    private void smokeDetectorBlock(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();

        ModelFile off = new ModelFile.UncheckedModelFile(modLoc("block/" + name));
        ModelFile on = new ModelFile.UncheckedModelFile(modLoc("block/" + name + "_on"));

        getVariantBuilder(block)
                .forAllStates(state -> {
                    Direction facing = state.getValue(SmokeDetectorBlock.FACING);
                    boolean powered = state.getValue(SmokeDetectorBlock.POWERED);

                    ModelFile model = powered ? on : off;

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
                            .modelFile(model)
                            .rotationX(x)
                            .rotationY(y)
                            .build();
                });
    }
}

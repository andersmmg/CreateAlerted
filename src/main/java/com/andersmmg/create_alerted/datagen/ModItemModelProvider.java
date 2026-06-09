package com.andersmmg.create_alerted.datagen;

import com.andersmmg.create_alerted.Create_alerted;
import com.andersmmg.create_alerted.block.AnnoyingAlarmBlock;
import com.andersmmg.create_alerted.block.BasicAlarmBlock;
import com.andersmmg.create_alerted.block.BuzzAlarmBlock;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Create_alerted.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent("alarm_basic", modLoc("block/alarm_off"));
        withExistingParent("alarm_annoying", modLoc("block/alarm_off"));
        withExistingParent("alarm_buzz", modLoc("block/alarm_off"));
    }
}

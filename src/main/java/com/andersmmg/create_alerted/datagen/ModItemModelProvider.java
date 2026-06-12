package com.andersmmg.create_alerted.datagen;

import com.andersmmg.create_alerted.CreateAlerted;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CreateAlerted.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent("alarm", modLoc("block/alarm_off_plain"));
    }
}

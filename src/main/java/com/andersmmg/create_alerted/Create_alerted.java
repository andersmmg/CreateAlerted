package com.andersmmg.create_alerted;

import com.andersmmg.create_alerted.block.AlarmBlock;
import com.andersmmg.create_alerted.block.AnnoyingAlarmBlock;
import com.andersmmg.create_alerted.block.BasicAlarmBlock;
import com.andersmmg.create_alerted.block.BuzzAlarmBlock;
import com.andersmmg.create_alerted.integration.SableCompat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Create_alerted.MODID)
public class Create_alerted {
    public static final String MODID = "create_alerted";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    private static final BlockBehaviour.Properties ALARM_PROPERTIES = BlockBehaviour.Properties.of()
            .strength(3.0f).requiresCorrectToolForDrops().noOcclusion()
            .lightLevel(state -> state.getValue(AlarmBlock.LIT) ? 7 : 0);

    public static final DeferredBlock<BasicAlarmBlock> BASIC_ALARM_BLOCK = BLOCKS.registerBlock("alarm_basic",
            BasicAlarmBlock::new,
            ALARM_PROPERTIES);
    public static final DeferredItem<BlockItem> BASIC_ALARM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("alarm_basic", BASIC_ALARM_BLOCK);

    public static final DeferredBlock<AnnoyingAlarmBlock> ANNOYING_ALARM_BLOCK = BLOCKS.registerBlock("alarm_annoying",
            AnnoyingAlarmBlock::new,
            ALARM_PROPERTIES);
    public static final DeferredItem<BlockItem> ANNOYING_ALARM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("alarm_annoying", ANNOYING_ALARM_BLOCK);

    public static final DeferredBlock<BuzzAlarmBlock> BUZZ_ALARM_BLOCK = BLOCKS.registerBlock("alarm_buzz",
            BuzzAlarmBlock::new,
            ALARM_PROPERTIES);
    public static final DeferredItem<BlockItem> BUZZ_ALARM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("alarm_buzz", BUZZ_ALARM_BLOCK);

    public Create_alerted(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        AllSoundEvents.SOUND_EVENTS.register(modEventBus);

        SableCompat.init();

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(BASIC_ALARM_BLOCK_ITEM);
            event.accept(ANNOYING_ALARM_BLOCK_ITEM);
            event.accept(BUZZ_ALARM_BLOCK_ITEM);
        }
    }
}

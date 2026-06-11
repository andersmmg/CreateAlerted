package com.andersmmg.create_alerted;

import com.andersmmg.create_alerted.block.*;
import com.andersmmg.create_alerted.integration.SableCompat;
import com.andersmmg.create_alerted.menu.AlarmMenu;
import com.andersmmg.create_alerted.network.AlarmFrequencyPayload;
import com.andersmmg.create_alerted.screen.AlarmScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.LoggerFactory;

@Mod(CreateAlerted.MODID)
public class CreateAlerted {
    public static final String MODID = "create_alerted";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    private static final BlockBehaviour.Properties ALARM_PROPERTIES = BlockBehaviour.Properties.of()
            .strength(3.0f).requiresCorrectToolForDrops().noOcclusion()
            .lightLevel(state -> state.getValue(AlarmBlock.POWERED) ? 7 : 0);
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
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<AlarmMenu>> ALARM_MENU =
            MENUS.register("alarm", () -> IMenuTypeExtension.create(AlarmMenu::fromNetwork));    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlarmBlockEntity>> ALARM_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("alarm",
                    () -> BlockEntityType.Builder.of(
                            AlarmBlockEntity::new,
                            BASIC_ALARM_BLOCK.get(),
                            ANNOYING_ALARM_BLOCK.get(),
                            BUZZ_ALARM_BLOCK.get()
                    ).build(null));

    public CreateAlerted(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        AllSoundEvents.SOUND_EVENTS.register(modEventBus);

        SableCompat.init();

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPayload);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerRenderers);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void clientSetup(FMLClientSetupEvent event) {
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        LoggerFactory.getLogger("CreateAlerted").info("Registering alarm BER for block entity type: {}", ALARM_BLOCK_ENTITY.get());
        event.registerBlockEntityRenderer(ALARM_BLOCK_ENTITY.get(), AlarmBlockEntityRenderer::new);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ALARM_MENU.get(), AlarmScreen::new);
    }

    private void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToServer(
                AlarmFrequencyPayload.TYPE,
                AlarmFrequencyPayload.CODEC,
                AlarmFrequencyPayload::handle
        );
    }



    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(BASIC_ALARM_BLOCK_ITEM);
            event.accept(ANNOYING_ALARM_BLOCK_ITEM);
            event.accept(BUZZ_ALARM_BLOCK_ITEM);
        }
    }
}

package com.andersmmg.create_alerted;

import com.andersmmg.create_alerted.block.*;
import com.andersmmg.create_alerted.integration.SableCompat;
import com.andersmmg.create_alerted.item.AlarmBlockItem;
import com.andersmmg.create_alerted.menu.AlarmMenu;
import com.andersmmg.create_alerted.network.AlarmFrequencyPayload;
import com.andersmmg.create_alerted.network.AlarmTypePayload;
import com.andersmmg.create_alerted.screen.AlarmScreen;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CreateAlerted.MODID)
public class CreateAlerted {
    public static final String MODID = "create_alerted";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<AlarmMenu>> ALARM_MENU = MENUS.register("alarm", () -> IMenuTypeExtension.create(AlarmMenu::fromNetwork));
    private static final BlockBehaviour.Properties ALARM_PROPERTIES = BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> state.getValue(AlarmBlock.POWERED) ? 7 : 0);
    public static final DeferredBlock<AlarmBlock> ALARM_BLOCK = BLOCKS.registerBlock("alarm", AlarmBlock::new, ALARM_PROPERTIES);
    public static final DeferredItem<AlarmBlockItem> ALARM_BLOCK_ITEM = ITEMS.registerItem("alarm", props -> new AlarmBlockItem(ALARM_BLOCK.get(), props));
    private static final BlockBehaviour.Properties SMOKE_DETECTOR_PROPERTIES = BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> state.getValue(SmokeDetectorBlock.POWERED) ? 7 : 0);

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAlerted.class);
    public static final DeferredBlock<SmokeDetectorBlock> SMOKE_DETECTOR_BLOCK = BLOCKS.registerBlock("smoke_detector", SmokeDetectorBlock::new, SMOKE_DETECTOR_PROPERTIES);
    public static final DeferredItem<BlockItem> SMOKE_DETECTOR_BLOCK_ITEM = ITEMS.registerItem("smoke_detector", props -> new BlockItem(SMOKE_DETECTOR_BLOCK.get(), props));

    static {
        LOGGER.info("Registering Create Alerted tooltip modifier provider");
        TooltipModifier.REGISTRY.registerProvider(key -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(key);
            if (CreateAlerted.MODID.equals(id.getNamespace())) {
                return new ItemDescription.Modifier(key, FontHelper.Palette.STANDARD_CREATE);
            }
            return null;
        });
    }

    public CreateAlerted(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        AllSoundEvents.SOUND_EVENTS.register(modEventBus);

        SableCompat.init();

        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event -> event.addListener(AlarmTypeManager.INSTANCE));

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPayload);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerBlockColors);
        modEventBus.addListener(this::registerItemColors);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToServer(AlarmFrequencyPayload.TYPE, AlarmFrequencyPayload.CODEC, AlarmFrequencyPayload::handle);
        registrar.playToServer(AlarmTypePayload.TYPE, AlarmTypePayload.CODEC, AlarmTypePayload::handle);
    }    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlarmBlockEntity>> ALARM_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("alarm", () -> BlockEntityType.Builder.of(AlarmBlockEntity::new, ALARM_BLOCK.get()).build(null));

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ALARM_BLOCK_ITEM);
            event.accept(SMOKE_DETECTOR_BLOCK_ITEM);
        }
    }    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeDetectorBlockEntity>> SMOKE_DETECTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("smoke_detector", () -> BlockEntityType.Builder.of(SmokeDetectorBlockEntity::new, SMOKE_DETECTOR_BLOCK.get()).build(null));

    private void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor blockColor = (state, level, pos, tintIndex) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof AlarmBlockEntity be) {
                return be.getColor();
            }
            return AlarmBlock.DEFAULT_COLOR;
        };
        event.register(blockColor, ALARM_BLOCK.get());

    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        LoggerFactory.getLogger("CreateAlerted").info("Registering alarm BER for block entity type: {}", ALARM_BLOCK_ENTITY.get());
        event.registerBlockEntityRenderer(ALARM_BLOCK_ENTITY.get(), AlarmBlockEntityRenderer::new);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ALARM_MENU.get(), AlarmScreen::new);
    }





    private void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor itemColor = (stack, tintIndex) -> {
            var dyedItemColor = stack.get(DataComponents.DYED_COLOR);
            if (dyedItemColor != null) {
                return dyedItemColor.rgb() & 0xFFFFFF;
            }
            var blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData != null && !blockEntityData.isEmpty()) {
                DyeColor dc = DyeColor.byName(blockEntityData.copyTag().getString("DyeColor"), null);
                if (dc != null) {
                    return dc.getTextureDiffuseColor() & 0xFFFFFF;
                }
            }
            return AlarmBlock.DEFAULT_COLOR;
        };
        event.register(itemColor, ALARM_BLOCK_ITEM.get());
    }


}

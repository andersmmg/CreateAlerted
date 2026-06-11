package com.andersmmg.create_alerted.network;

import com.andersmmg.create_alerted.CreateAlerted;
import com.andersmmg.create_alerted.block.AlarmBlockEntity;
import com.andersmmg.create_alerted.menu.AlarmMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AlarmFrequencyPayload(BlockPos pos, boolean first, ItemStack stack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AlarmFrequencyPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "alarm_frequency"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlarmFrequencyPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AlarmFrequencyPayload::pos,
                    StreamCodec.of(
                            FriendlyByteBuf::writeBoolean,
                            FriendlyByteBuf::readBoolean
                    ), AlarmFrequencyPayload::first,
                    StreamCodec.of(
                            (buf, stack) -> {
                                boolean present = !stack.isEmpty();
                                buf.writeBoolean(present);
                                if (present) {
                                    ItemStack.STREAM_CODEC.encode(buf, stack);
                                }
                            },
                            buf -> buf.readBoolean() ? ItemStack.STREAM_CODEC.decode(buf) : ItemStack.EMPTY
                    ), AlarmFrequencyPayload::stack,
                    AlarmFrequencyPayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            var level = player.level();
            if (level.getBlockEntity(pos) instanceof AlarmBlockEntity be) {
                be.setFrequency(first, stack);
                if (player.containerMenu instanceof AlarmMenu menu) {
                    menu.broadcastChanges();
                }
            }
        });
    }
}

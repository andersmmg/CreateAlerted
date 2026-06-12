package com.andersmmg.create_alerted.network;

import com.andersmmg.create_alerted.CreateAlerted;
import com.andersmmg.create_alerted.block.AlarmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AlarmTypePayload(BlockPos pos, ResourceLocation typeId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AlarmTypePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "alarm_type"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, AlarmTypePayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AlarmTypePayload::pos,
                    ResourceLocation.STREAM_CODEC, AlarmTypePayload::typeId,
                    AlarmTypePayload::new
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
                be.setTypeId(typeId);
            }
        });
    }
}

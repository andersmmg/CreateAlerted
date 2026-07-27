package com.andersmmg.create_alerted.network;

import com.andersmmg.create_alerted.CreateAlerted;
import com.andersmmg.create_alerted.block.AlarmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AlarmVisualPayload(BlockPos pos, ResourceLocation visualId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AlarmVisualPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "alarm_visual"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlarmVisualPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AlarmVisualPayload::pos,
                    ResourceLocation.STREAM_CODEC, AlarmVisualPayload::visualId,
                    AlarmVisualPayload::new
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
                be.setVisualId(visualId);
            }
        });
    }
}

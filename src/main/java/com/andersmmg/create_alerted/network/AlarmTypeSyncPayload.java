package com.andersmmg.create_alerted.network;

import com.andersmmg.create_alerted.CreateAlerted;
import com.andersmmg.create_alerted.block.AlarmTypeManager;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record AlarmTypeSyncPayload(List<ResourceLocation> order) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AlarmTypeSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "alarm_type_sync"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, AlarmTypeSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC), AlarmTypeSyncPayload::order,
                    AlarmTypeSyncPayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> AlarmTypeManager.INSTANCE.setClientOrder(order));
    }
}

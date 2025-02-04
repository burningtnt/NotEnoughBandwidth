package cn.ussshenzhou.notenoughbandwidth.network.compressed;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import cn.ussshenzhou.notenoughbandwidth.managers.PacketTypeIndexManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record CompressedCustomPayloadPacket(PacketType<CompressedCustomPayloadPacket> type, CustomPacketPayload payload) implements CustomPayload, Packet<PacketListener> {
    public static final PacketType<CompressedCustomPayloadPacket> S_TYPE = new PacketType<>(PacketFlow.SERVERBOUND, NotEnoughBandwidth.id("c2s/compressed"));
    public static final PacketType<CompressedCustomPayloadPacket> C_TYPE = new PacketType<>(PacketFlow.CLIENTBOUND, NotEnoughBandwidth.id("s2c/compressed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompressedCustomPayloadPacket> S_CODEC = ofCodec(PacketFlow.SERVERBOUND, payload -> new CompressedCustomPayloadPacket(S_TYPE, payload));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompressedCustomPayloadPacket> C_CODEC = CompressedCustomPayloadPacket.ofCodec(PacketFlow.CLIENTBOUND, payload -> new CompressedCustomPayloadPacket(S_TYPE, payload));

    public static <T extends CustomPayload> StreamCodec<RegistryFriendlyByteBuf, T> ofCodec(PacketFlow flow, Function<CustomPacketPayload, T> constructor) {
        return new StreamCodec<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, T packet) {
                ResourceLocation type = packet.payload().type().id();

                encodeHeader(buf, type);

                StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> codec = getCodec(type);
                if (codec != null) {
                    codec.encode(buf, packet.payload());
                }
            }

            private void encodeHeader(RegistryFriendlyByteBuf buf, ResourceLocation type) {
                int index = PacketTypeIndexManager.getIndex(type);
                if (index >>> 31 == 0) {
                    buf.writeByte(index >>> 24);
                    buf.writeResourceLocation(type);
                }
                if (index >>> 31 == 1) {
                    if ((index >>> 30 & 1) == 1) {
                        buf.writeMedium(index >>> 8);
                    } else {
                        buf.writeInt(index);
                    }
                }
            }

            @Override
            public T decode(RegistryFriendlyByteBuf buf) {
                ResourceLocation type = decodeHeader(buf);

                StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> codec = getCodec(type);
                if (codec != null) {
                    return constructor.apply(codec.decode(buf));
                } else {
                    int i = buf.readableBytes();
                    if (i >= 0 && i <= 1048576) {
                        buf.skipBytes(i);
                    } else {
                        throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
                    }

                    return constructor.apply(new DiscardedPayload(type));
                }
            }

            private ResourceLocation decodeHeader(RegistryFriendlyByteBuf buf) {
                ResourceLocation type;
                int fixed = buf.readUnsignedByte() & 0xff;
                if (fixed >>> 7 == 0) {
                    type = buf.readResourceLocation();
                } else {
                    if (fixed >>> 6 == 0) {
                        type = PacketTypeIndexManager.getResourceLocation(buf.readUnsignedMedium(), false);
                    } else {
                        type = PacketTypeIndexManager.getResourceLocation(buf.readUnsignedShort(), true);
                    }
                }

                if (type == null) {
                    throw new IllegalStateException("Unknown packet index: " + fixed);
                }
                return type;
            }

            @SuppressWarnings({"unchecked", "UnstableApiUsage"})
            private @Nullable StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> getCodec(ResourceLocation type) {
                return (StreamCodec<? super FriendlyByteBuf, CustomPacketPayload>) NetworkRegistry.getCodec(type, ConnectionProtocol.PLAY, flow);
            }
        };
    }

    @Override
    public void handle(PacketListener listener) {
        switch (listener.flow()) {
            case CLIENTBOUND -> ((ClientCommonPacketListener) listener).handleCustomPayload(payload.toVanillaClientbound());
            case SERVERBOUND -> ((ServerCommonPacketListener) listener).handleCustomPayload(payload.toVanillaServerbound());
        }
    }
}

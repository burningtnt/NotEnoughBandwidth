package org.teacon.neb.network.indexed;

import org.teacon.neb.NotEnoughBandwidth;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
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
import java.io.IOException;
import java.util.List;

/**
 * Instead of vanilla {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload#codec(CustomPacketPayload.FallbackProvider, List, ConnectionProtocol, PacketFlow)},
 * we here use such protocol to avoid putting a huge ResourceLocation into ByteBuf.
 * <p>
 * <h4>Fixed 8 bits header</h4>
 * <pre>
 * ┌------------- 1 byte (8 bits) ---------------┐
 * │               function flags                │
 * ├---┬-----------------------------------------┤
 * │ t │            reserved (7 bits)            │
 * └---┴-----------------------------------------┘
 *
 * t = tight_indexed (1 bit)
 * reserved = for future use (7 bits)
 * </pre>
 *
 * <h4>Index Header (t)</h4>
 * <pre>
 *
 * - If t=0 (NOT tight):
 *
 *   ┌-------- 1 byte ---------┬-------- 1 byte --------┬-------- 1 byte --------┐
 *   ┌------------- 12 bits ---------------┬-------------- 12 bits --------------┐
 *   │    namespace-id (capacity 4096)     │       path-id (capacity 4096)       │
 *   └-------------------------------------┴-------------------------------------┘
 *
 * - If t=1 (tight):
 *
 *   ┌--------- 1 byte ----------┬--------- 1 byte ---------┐
 *   ┌--------- 8 bits ----------┬--------- 8 bits ---------┐
 *   │namespace-id (capacity 256)│  path-id (capacity 256)  │
 *   └---------------------------┴--------------------------┘
 *
 * </pre>
 *
 * <h4>Then packet data.</h4>
 *
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record IndexPacket(PacketType<IndexPacket> type,CustomPacketPayload payload) implements Packet<PacketListener> {
    public static final PacketType<IndexPacket> S_TYPE = new PacketType<>(PacketFlow.SERVERBOUND, NotEnoughBandwidth.id("c2s/indexed"));
    public static final PacketType<IndexPacket> C_TYPE = new PacketType<>(PacketFlow.CLIENTBOUND, NotEnoughBandwidth.id("s2c/indexed"));

    public static final StreamCodec<FriendlyByteBuf, IndexPacket> S_CODEC = ofCodec(S_TYPE), C_CODEC = ofCodec(C_TYPE);

    public static StreamCodec<FriendlyByteBuf, IndexPacket> ofCodec(PacketType<IndexPacket> packetType) {
        return new StreamCodec<>() {
            @Override
            public void encode(FriendlyByteBuf buf, IndexPacket packet) {
                ResourceLocation type = packet.payload().type().id();

                IndexLookup.Result index = IndexLookup.getInstance().getIndex(type);
                if (index == IndexLookup.EMPTY) {
                    // Should NOT be here: Packets with unknown index should be sent with vanilla CustomPayloadPacket.
                    throw new AssertionError("ResourceLocation " + type + " is unknown.");
                }

                if (index.namespace() < 256 && index.path() < 256) {
                    buf.writeByte(0x80).writeByte(index.namespace()).writeByte(index.path());
                } else {
                    buf.writeByte(0x00).writeMedium(((index.namespace() & 0xFFF) << 12) | (index.path() & 0xFFF));
                }

                StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> codec = getCodec(type);
                if (codec != null) {
                    codec.encode(buf, packet.payload());
                } else {
                    // Should NOT be here: Packets with known index should be known by vanilla CustomPayloadPacket.
                    throw new AssertionError("ResourceLocation " + type + " is unknown.");
                }
            }

            @Override
            public IndexPacket decode(FriendlyByteBuf buf) {
                int namespace, path;
                if ((buf.readByte() & 0x80) != 0) {
                    namespace = buf.readUnsignedByte();
                    path = buf.readUnsignedByte();
                } else {
                    int i = buf.readUnsignedMedium();
                    namespace = (i >> 12) & 0xFFF;
                    path = i & 0xFFF;
                }

                ResourceLocation type = IndexLookup.getInstance().getType(namespace, path);

                StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> codec = getCodec(type);
                if (codec != null) {
                    CustomPacketPayload payload;
                    try {
                        payload = codec.decode(buf);
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Failed encoding custom payload: " + type, e);
                    }

                    return new IndexPacket(packetType, payload);
                } else {
                    int i = buf.readableBytes();
                    if (i >= 0 && i <= 1048576) {
                        buf.skipBytes(i);
                    } else {
                        throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
                    }

                    return new IndexPacket(packetType, new DiscardedPayload(type));
                }
            }

            @SuppressWarnings({"unchecked", "UnstableApiUsage"})
            private @Nullable StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> getCodec(ResourceLocation type) {
                return (StreamCodec<? super FriendlyByteBuf, CustomPacketPayload>) NetworkRegistry.getCodec(type, ConnectionProtocol.PLAY, packetType.flow());
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

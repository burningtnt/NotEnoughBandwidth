package cn.ussshenzhou.notenoughbandwidth.network;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import cn.ussshenzhou.notenoughbandwidth.network.compress.CompressedEncoder;
import cn.ussshenzhou.notenoughbandwidth.network.compress.CompressedPacket;
import cn.ussshenzhou.notenoughbandwidth.network.indexed.IndexLookup;
import cn.ussshenzhou.notenoughbandwidth.network.indexed.IndexPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.codec.IdDispatchCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = NotEnoughBandwidth.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkManager {
    private NetworkManager() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        IndexLookup.initialize();
    }

    private static final AttributeKey<Boolean> CONFIGURATION_FINISHED = AttributeKey.valueOf(NotEnoughBandwidth.id("aggressive_flag").toString());

    public static void enable(Connection connection) {
        connection.channel().attr(CONFIGURATION_FINISHED).set(Boolean.TRUE);
    }

    public static CompressedEncoder.CompressedTransfer transferPackage(Connection connection, Packet<?> packet) {
        if (connection.channel().attr(CONFIGURATION_FINISHED).get() == null) {
            return null;
        }

        // #1: Wrap CustomPayload as IndexPacket.
        if (packet instanceof VanillaCustomPayload pp) {
            CustomPacketPayload payload = pp.payload();
            ResourceLocation type = payload.type().id();

            switch (type.getNamespace()) {
                case "c", "neoforge", "minecraft", "velocity" -> {
                }
                default -> {
                    if (IndexLookup.getInstance().getIndex(type) != IndexLookup.EMPTY) {
                        packet = new IndexPacket(switch (connection.getSending()) {
                            case CLIENTBOUND -> IndexPacket.C_TYPE;
                            case SERVERBOUND -> IndexPacket.S_TYPE;
                        }, payload);
                    }
                }
            }
        }

        List<Packet<?>> packets;
        if (packet instanceof BundlePacket<?> bundle) {
            packets = new ArrayList<>();
            for (Packet<?> sub : bundle.subPackets()) {
                packets.add(sub);
            }
        } else {
            packets = List.of(packet);
        }

        // #2: Compress the packet.
        return new CompressedEncoder.CompressedTransfer(switch (connection.getSending()) {
            case CLIENTBOUND -> CompressedPacket.C_TYPE;
            case SERVERBOUND -> CompressedPacket.S_TYPE;
        }, packets);
    }
}

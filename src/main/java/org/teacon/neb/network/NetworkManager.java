package org.teacon.neb.network;

import org.teacon.neb.NotEnoughBandwidth;
import org.teacon.neb.network.aggressive.AggressiveBuffer;
import org.teacon.neb.network.aggressive.CompressedPacket;
import org.teacon.neb.network.indexed.IndexLookup;
import org.teacon.neb.network.indexed.IndexPacket;
import com.google.common.collect.ImmutableSet;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@EventBusSubscriber(modid = NotEnoughBandwidth.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkManager {
    private NetworkManager() {
    }

    private static final Set<PacketType<?>> BLACK_LIST = ImmutableSet.of(
            GamePacketTypes.CLIENTBOUND_LOGIN,

            CommonPacketTypes.SERVERBOUND_KEEP_ALIVE,
            CommonPacketTypes.CLIENTBOUND_KEEP_ALIVE,

            GamePacketTypes.CLIENTBOUND_COMMAND_SUGGESTIONS,
            GamePacketTypes.CLIENTBOUND_COMMANDS,
            GamePacketTypes.SERVERBOUND_CHAT_COMMAND,
            GamePacketTypes.SERVERBOUND_CLIENT_COMMAND,
            GamePacketTypes.SERVERBOUND_COMMAND_SUGGESTION,

            GamePacketTypes.CLIENTBOUND_PLAYER_INFO_UPDATE,
            GamePacketTypes.CLIENTBOUND_PLAYER_INFO_REMOVE
    );

    @SubscribeEvent
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Register an unused fence to avoid mod mismatch exception.
        // This packet should NEVER be sent.
        event.registrar(ModList.get().getModContainerById(NotEnoughBandwidth.MODID).orElseThrow(
                () -> new AssertionError("Huh? Why is NotEnoughBandwidth absent?")
        ).getModInfo().getVersion().toString()).configurationBidirectional(
                new CustomPacketPayload.Type<>(NotEnoughBandwidth.id("fence")),
                new StreamCodec<>() {
                    @Override
                    public @NotNull CustomPacketPayload decode(@NotNull FriendlyByteBuf buffer) {
                        throw new AssertionError();
                    }

                    @Override
                    public void encode(@NotNull FriendlyByteBuf buffer, @NotNull CustomPacketPayload value) {
                        throw new AssertionError();
                    }
                }, (payload, context) -> {
                    throw new AssertionError();
                }
        );
    }

    public static ResourceLocation getPacketType(Packet<?> packet) {
        return switch (packet) {
            case VanillaCustomPayload payload -> payload.payload().type().id();
            case IndexPacket(PacketType<IndexPacket> ignored, CustomPacketPayload payload) -> payload.type().id();
            default -> packet.type().id();
        };
    }

    public static void enable(Connection connection) {
        AggressiveBuffer.initialize(connection);
    }

    public static void tick(Connection connection) {
        AggressiveBuffer buffer = AggressiveBuffer.get(connection);
        if (buffer != null) {
            buffer.tick();
        }
    }

    public static void release(Connection connection) {
        AggressiveBuffer.release(connection);
    }

    public static boolean onSendPacket(Connection connection, Packet<?> packet) {
        AggressiveBuffer buffer = AggressiveBuffer.get(connection);
        if (buffer == null || BLACK_LIST.contains(packet.type())) {
            return false;
        }

        translatePacket(connection, packet, buffer);
        return true;
    }

    private static void translatePacket(Connection connection, Packet<?> packet, AggressiveBuffer buffer) {
        switch (packet) {
            case CompressedPacket ignored -> throw new AssertionError("CompressedPacket should NOT be pushed into the packet flow.");
            case VanillaCustomPayload pp -> {
                CustomPacketPayload payload = pp.payload();
                ResourceLocation type = payload.type().id();

                if (IndexLookup.getInstance().getIndex(type) != IndexLookup.EMPTY) {
                    packet = new IndexPacket(switch (connection.getSending()) {
                        case CLIENTBOUND -> IndexPacket.C_TYPE;
                        case SERVERBOUND -> IndexPacket.S_TYPE;
                    }, payload);
                }

                buffer.push(packet);
            }
            case BundlePacket<?> bundle -> {
                for (Packet<?> sub : bundle.subPackets()) {
                    translatePacket(connection, sub, buffer);
                }
            }
            default -> buffer.push(packet);
        }
    }
}

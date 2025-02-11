package cn.ussshenzhou.notenoughbandwidth.network;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.AggressiveBuffer;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.CompressedPacket;
import cn.ussshenzhou.notenoughbandwidth.network.indexed.IndexLookup;
import cn.ussshenzhou.notenoughbandwidth.network.indexed.IndexPacket;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = NotEnoughBandwidth.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkManager {
    private NetworkManager() {
    }

    private static final Set<PacketType<? extends Packet<? extends ICommonPacketListener>>> BLACK_LIST = new HashSet<>();

    static {
        BLACK_LIST.add(GamePacketTypes.CLIENTBOUND_LOGIN);
        BLACK_LIST.add(CommonPacketTypes.SERVERBOUND_KEEP_ALIVE);
        BLACK_LIST.add(CommonPacketTypes.CLIENTBOUND_KEEP_ALIVE);

        if (ModList.get().isLoaded("neoforwarding")) {
            BLACK_LIST.add(GamePacketTypes.CLIENTBOUND_COMMANDS);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Register a unused fence to avoid mod mismatch exception.
        // This packet should NEVER be sent.
        event.registrar("1").configurationBidirectional(
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

        IndexLookup.initialize();
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

    public static boolean onSendPacket(Connection connection, Packet<?> packet) {
        AggressiveBuffer buffer = AggressiveBuffer.get(connection);
        if (buffer == null || BLACK_LIST.contains(packet.type())) {
            return false;
        }

        switch (packet) {
            case CompressedPacket ignored -> throw new AssertionError();
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
                    buffer.push(sub);
                }
            }
            default -> buffer.push(packet);
        }
        return true;
    }
}

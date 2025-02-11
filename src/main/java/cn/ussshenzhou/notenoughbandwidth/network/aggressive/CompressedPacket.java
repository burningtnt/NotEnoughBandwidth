package cn.ussshenzhou.notenoughbandwidth.network.aggressive;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import io.netty.buffer.ByteBuf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.neoforged.fml.loading.FMLEnvironment;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record CompressedPacket(
        PacketType<CompressedPacket> type,
        ByteBuf buf
) implements Packet<PacketListener> {
    public static final PacketType<CompressedPacket> S_TYPE = new PacketType<>(PacketFlow.SERVERBOUND, NotEnoughBandwidth.id("c2s/compressed"));
    public static final PacketType<CompressedPacket> C_TYPE = new PacketType<>(PacketFlow.CLIENTBOUND, NotEnoughBandwidth.id("s2c/compressed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompressedPacket> S_CODEC = ofCodec(S_TYPE), C_CODEC = ofCodec(C_TYPE);

    public static StreamCodec<RegistryFriendlyByteBuf, CompressedPacket> ofCodec(PacketType<CompressedPacket> type) {
        return new StreamCodec<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf target, CompressedPacket packet) {
                ByteBuf buf = packet.buf().retainedDuplicate();
                target.writeBytes(buf);
                try {
                    target.writeBytes(buf);
                } finally {
                    buf.release();
                }
            }

            @Override
            public CompressedPacket decode(RegistryFriendlyByteBuf compressed) {
                return new CompressedPacket(type, compressed.readBytes(compressed.readableBytes()));
            }
        };
    }

    @Override
    public void handle(PacketListener listener) {
        if (!FMLEnvironment.production) { // For maximum compatibility, ignore this exception in Production Environment.
             throw new AssertionError("CompressedPacket should be handled by CompressedDecoder.");
        }
    }
}

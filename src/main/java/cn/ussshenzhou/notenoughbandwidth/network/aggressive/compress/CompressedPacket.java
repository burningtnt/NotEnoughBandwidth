package cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.neoforged.fml.loading.FMLEnvironment;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.Reference;

/**
 * <p>A packet that is considered to be compressed will be process as the graph below.</p>
 *
 * <p>1. Convert: Packets is converted to 'CompressedEncoder.CompressedTransfer'.</p>
 * <p>2. Delegate Encode: Packets is read by 'CompressedEncoder', which delegate the encoding process to 'PacketEncoder'.</p>
 * <p>3. Networking: Packets are compressed with ZSTD and sent by netty.</p>
 * <p>4. Handle: Packets are handled.</p>
 */
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
                try {
                    CompressHelper.compress(buf, target);
                } finally {
                    buf.release();
                }
            }

            @Override
            public CompressedPacket decode(RegistryFriendlyByteBuf compressed) {
                return new CompressedPacket(type, CompressHelper.decompress(compressed));
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

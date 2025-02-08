package cn.ussshenzhou.notenoughbandwidth.network.compress;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

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

    public static final StreamCodec<RegistryFriendlyByteBuf, CompressedPacket> S_CODEC = ofCodec(S_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompressedPacket> C_CODEC = ofCodec(C_TYPE);

    public static StreamCodec<RegistryFriendlyByteBuf, CompressedPacket> ofCodec(PacketType<CompressedPacket> packetType) {
        return new StreamCodec<>() {
            @Override
            public CompressedPacket decode(RegistryFriendlyByteBuf compressed) {
                int size = VarInt.read(compressed);
                if (size == 0) {
                    return new CompressedPacket(packetType, compressed.readBytes(compressed.readableBytes()));
                }

                int s2;
                ByteBuf original = Unpooled.directBuffer(size, size);
                if (compressed.isDirect()) {
                    s2 = decompress(compressed, original);
                    compressed.skipBytes(compressed.readableBytes());
                } else {
                    int remain = compressed.readableBytes();
                    ByteBuf direct = Unpooled.directBuffer(remain, remain);
                    direct.readBytes(compressed, remain);

                    s2 = decompress(direct, original);
                    direct.release();
                }

                if (size != s2) {
                    throw new IllegalStateException("Size mismatched!");
                }

                original.writerIndex(size);

                return new CompressedPacket(packetType, original);
            }

            private int decompress(ByteBuf from, ByteBuf to) {
                try (ZstdDecompressCtx ctx = new ZstdDecompressCtx()) {
                    return ctx.setMagicless(true).decompress(
                            to.nioBuffer(to.writerIndex(), to.writableBytes()),
                            from.nioBuffer()
                    );
                }
            }

            @Override
            public void encode(RegistryFriendlyByteBuf target, CompressedPacket packet) {
                ByteBuf original = packet.buf();

                int size = original.readableBytes();
                if (size == 0) {
                    return;
                }

                if (size <= 64) {
                    VarInt.write(target, 0);
                    target.writeBytes(original);
                } else {
                    VarInt.write(target, size);

                    int compressedSize = (int) Zstd.compressBound(size);
                    target.ensureWritable(compressedSize);

                    ByteBuf o2 = null, t2 = null;
                    if (!original.isDirect()) {
                        o2 = Unpooled.directBuffer(size, size);
                        o2.writeBytes(original, size);
                    }
                    if (!target.isDirect()) {
                        t2 = Unpooled.directBuffer(compressedSize, compressedSize);
                    }

                    int realSize = compress(Objects.requireNonNullElse(o2, original), Objects.requireNonNullElse(t2, target));
                    if (o2 != null) {
                        original.skipBytes(size);
                        o2.release();
                    }
                    if (t2 != null) {
                        target.writeBytes(t2, realSize);
                        t2.release();
                    } else {
                        target.writerIndex(target.writerIndex() + realSize);
                    }
                }
            }

            private int compress(ByteBuf from, ByteBuf to) {
                try (ZstdCompressCtx ctx = new ZstdCompressCtx()) {
                    return ctx.setLevel(Zstd.defaultCompressionLevel()).setChecksum(false).setMagicless(true).compress(
                            to.nioBuffer(to.writerIndex(), to.writableBytes()),
                            from.nioBuffer()
                    );
                }
            }
        };
    }

    @Override
    public void handle(PacketListener listener) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        StreamCodec<ByteBuf, Packet<PacketListener>> codec = (StreamCodec) ((ICommonPacketListener) listener).getConnection().getInboundProtocol().codec();
        while (buf.readableBytes() != 0) {
            int length = VarInt.read(buf);

            ByteBuf pkt = buf.slice(buf.readerIndex(), length).asReadOnly();
            codec.decode(pkt).handle(listener);
            if (pkt.readerIndex() != pkt.capacity()) {
                throw new IllegalStateException();
            }

            buf.skipBytes(length);
        }
    }
}

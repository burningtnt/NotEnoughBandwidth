package org.teacon.neb.network.aggressive.compress;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import org.teacon.neb.NotEnoughBandwidth;
import org.teacon.neb.network.NetworkManager;
import org.teacon.neb.network.aggressive.CompressedPacket;
import org.teacon.neb.profiler.IProfiler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.VarInt;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.List;

@ChannelHandler.Sharable
public final class CompressDecoder extends MessageToMessageDecoder<CompressedPacket> {
    public static final String ID = NotEnoughBandwidth.id("compressed_decoder").toString();

    public static final CompressDecoder INSTANCE = new CompressDecoder();

    private static final MethodHandle DECODE;
    private static final VarHandle PROTOCOL_INFO;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(PacketDecoder.class, MethodHandles.lookup());

            DECODE = lookup.findVirtual(
                    PacketDecoder.class, "decode", MethodType.methodType(void.class, ChannelHandlerContext.class, ByteBuf.class, List.class)
            );
            PROTOCOL_INFO = lookup.findVarHandle(PacketDecoder.class, "protocolInfo", ProtocolInfo.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final ThreadLocal<Object2IntMap<ResourceLocation>> SIZES = ThreadLocal.withInitial(Object2IntOpenHashMap::new);

    private CompressDecoder() {
    }

    @Override
    protected void decode(ChannelHandlerContext context, CompressedPacket msg, List<Object> out) {
        PacketDecoder<?> decoder = (PacketDecoder<?>) context.pipeline().get("decoder");
        if (((ProtocolInfo<?>) PROTOCOL_INFO.get(decoder)).id() != ConnectionProtocol.PLAY) {
            throw new AssertionError("CompressDecoder should only be enabled in PLAY connection state.");
        }

        Object2IntMap<ResourceLocation> sizes = SIZES.get();
        ByteBuf buf = CompressContext.get().decompress(msg.buf());
        while (buf.readableBytes() != 0) {
            int length = VarInt.read(buf);
            ByteBuf packet = buf.slice(buf.readerIndex(), length).readerIndex(0).writerIndex(length);

            int size = out.size();
            try {
                DECODE.invokeExact(decoder, context, packet, out);
            } catch (Throwable t2) {
                throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
            }

            if (packet.readerIndex() != packet.capacity()) {
                throw new AssertionError("PacketDecoder should consume all bytes, or throw an exception.");
            }
            buf.skipBytes(length);

            switch (out.size() - size) {
                case 0 -> {
                }
                case 1 -> {
                    ResourceLocation type = NetworkManager.getPacketType((Packet<?>) out.getLast());
                    sizes.put(type, sizes.getOrDefault(type, 0) + packet.writerIndex());
                }
                default -> throw new AssertionError("PacketDecoder should only push one packet.");
            }
        }

        IProfiler.PROFILER.onReceivePacket(Object2IntMaps.unmodifiable(sizes), buf.writerIndex(), msg.buf().writerIndex());
        sizes.clear();
        buf.release();
        msg.buf().release();
    }
}

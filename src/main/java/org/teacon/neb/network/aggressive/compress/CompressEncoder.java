package org.teacon.neb.network.aggressive.compress;

import net.minecraft.network.ConnectionProtocol;
import org.teacon.neb.NotEnoughBandwidth;
import org.teacon.neb.network.NetworkManager;
import org.teacon.neb.network.aggressive.CompressedPacket;
import org.teacon.neb.profiler.IProfiler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.VarInt;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.List;

@ChannelHandler.Sharable
public final class CompressEncoder extends MessageToMessageEncoder<CompressEncoder.CompressedTransfer> {
    public static final String ID = NotEnoughBandwidth.id("compressed_encoder").toString();

    public static final CompressEncoder INSTANCE = new CompressEncoder();

    private static final MethodHandle ENCODE;

    static {
        try {
            ENCODE = MethodHandles.privateLookupIn(PacketEncoder.class, MethodHandles.lookup()).findVirtual(
                    PacketEncoder.class, "encode", MethodType.methodType(void.class, ChannelHandlerContext.class, Packet.class, ByteBuf.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final ThreadLocal<Object2IntMap<ResourceLocation>> SIZES = ThreadLocal.withInitial(Object2IntOpenHashMap::new);

    private CompressEncoder() {
    }

    public record CompressedTransfer(PacketType<CompressedPacket> type, Collection<Packet<?>> packets) {
    }

    @Override
    protected void encode(ChannelHandlerContext context, CompressedTransfer transfer, List<Object> out) {
        PacketEncoder<?> encoder = (PacketEncoder<?>) context.pipeline().get("encoder");
        if (encoder.getProtocolInfo().id() != ConnectionProtocol.PLAY) {
            throw new AssertionError("CompressEncoder should only be enabled in PLAY connection state.");
        }

        Object2IntMap<ResourceLocation> sizes = SIZES.get();
        ByteBuf buf = context.alloc().directBuffer(), temp = context.alloc().directBuffer();

        for (Packet<?> packet : transfer.packets()) {
            ByteBuf t = temp.duplicate();
            try {
                ENCODE.invokeExact(encoder, context, packet, t);
            } catch (Throwable t2) {
                throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
            }

            int size = t.writerIndex();
            ResourceLocation type = NetworkManager.getPacketType(packet);

            sizes.put(type, sizes.getOrDefault(type, 0) + size);
            VarInt.write(buf, size);
            buf.writeBytes(t);
        }

        CompressContext.get().compress(buf, temp);
        if (buf.writerIndex() != 0) {
            IProfiler.PROFILER.onTransmitPacket(Object2IntMaps.unmodifiable(sizes), buf.writerIndex(), temp.writerIndex());
        }
        sizes.clear();

        try {
            ENCODE.invokeExact(encoder, context, (Packet<?>) new CompressedPacket(transfer.type(), temp), buf);
        } catch (Throwable t2) {
            throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
        }

        temp.release();
        out.add(buf);
    }
}

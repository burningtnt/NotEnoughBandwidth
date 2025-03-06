package cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import cn.ussshenzhou.notenoughbandwidth.network.NetworkManager;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.CompressedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.VarInt;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

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

    private CompressEncoder() {
    }

    public record CompressedTransfer(PacketType<CompressedPacket> type, Collection<Packet<?>> packets) {
    }

    @Override
    protected void encode(ChannelHandlerContext context, CompressedTransfer transfer, List<Object> out) {
        PacketEncoder<?> encoder = (PacketEncoder<?>) context.pipeline().get("encoder");

        ByteBuf buf = context.alloc().directBuffer(), temp = context.alloc().directBuffer();
        for (Packet<?> packet : transfer.packets()) {
            ByteBuf t = temp.duplicate();
            try {
                ENCODE.invokeExact(encoder, context, packet, t);
            } catch (Throwable t2) {
                throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
            }

            int size = t.writerIndex();
            NotEnoughBandwidth.PROFILER.onSendPacket(NetworkManager.getPacketType(packet), size);

            VarInt.write(buf, size);
            buf.writeBytes(t);
        }

        CompressContext.get().compress(buf, temp);
        try {
            ENCODE.invokeExact(encoder, context, (Packet<?>) new CompressedPacket(transfer.type(), temp), buf);
        } catch (Throwable t2) {
            throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
        }

        temp.release();
        out.add(buf);
    }
}

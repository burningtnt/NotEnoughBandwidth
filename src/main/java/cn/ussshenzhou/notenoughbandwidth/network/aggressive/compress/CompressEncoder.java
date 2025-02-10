package cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
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
public class CompressEncoder extends MessageToMessageEncoder<CompressEncoder.CompressedTransfer> {
    public static final String ID = NotEnoughBandwidth.id("compressed_encoder").toString();

    public static final CompressEncoder INSTANCE = new CompressEncoder();

    private CompressEncoder() {
    }

    public record CompressedTransfer(PacketType<CompressedPacket> type, Collection<Packet<?>> packets) {
    }

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

    @Override
    protected void encode(ChannelHandlerContext context, CompressedTransfer transfer, List<Object> out) {
        ChannelHandler encoder = context.pipeline().get("encoder");

        ByteBuf buf = context.alloc().directBuffer(), temp = context.alloc().buffer();
        for (Packet<?> packet : transfer.packets()) {
            ByteBuf t = temp.duplicate();
            try {
                ENCODE.invokeExact((PacketEncoder<?>) encoder, context, packet, t);
            } catch (Throwable t2) {
                throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
            }

            VarInt.write(buf, t.writerIndex());
            buf.writeBytes(t);
        }

        try {
            ENCODE.invokeExact((PacketEncoder<?>) encoder, context, (Packet<?>) new CompressedPacket(transfer.type(), buf), temp);
        } catch (Throwable t2) {
            throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
        }

        buf.release();
        out.add(temp);
    }
}

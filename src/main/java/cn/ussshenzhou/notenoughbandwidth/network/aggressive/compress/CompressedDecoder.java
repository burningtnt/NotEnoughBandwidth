package cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.VarInt;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

@ChannelHandler.Sharable
public class CompressedDecoder extends MessageToMessageDecoder<CompressedPacket> {
    public static final String ID = NotEnoughBandwidth.id("compressed_decoder").toString();

    public static final CompressedDecoder INSTANCE = new CompressedDecoder();

    private CompressedDecoder() {
    }

    private static final MethodHandle DECODE;

    static {
        try {
            DECODE = MethodHandles.privateLookupIn(PacketDecoder.class, MethodHandles.lookup()).findVirtual(
                    PacketDecoder.class, "decode", MethodType.methodType(void.class, ChannelHandlerContext.class, ByteBuf.class, List.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext context, CompressedPacket msg, List<Object> out) {
        ChannelHandler decoder = context.pipeline().get("decoder");
        if (decoder == null) {
            return;
        }

        ByteBuf buf = msg.buf();
        while (buf.readableBytes() != 0) {
            int length = VarInt.read(buf);
            ByteBuf pkt = buf.slice(buf.readerIndex(), length).asReadOnly();

            try {
                DECODE.invokeExact((PacketDecoder<?>) decoder, context, pkt, out);
            } catch (Throwable t2) {
                throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
            }

            if (pkt.readerIndex() != pkt.capacity()) {
                throw new IllegalStateException();
            }

            buf.skipBytes(length);
        }
    }
}

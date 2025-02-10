package cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.CompressedPacket;
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
public class CompressDecoder extends MessageToMessageDecoder<CompressedPacket> {
    public static final String ID = NotEnoughBandwidth.id("compressed_decoder").toString();

    public static final CompressDecoder INSTANCE = new CompressDecoder();

    private CompressDecoder() {
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

        ByteBuf buf = CompressContext.get(context).decompress(msg.buf());
        while (buf.readableBytes() != 0) {
            int length = VarInt.read(buf);
            ByteBuf packet = buf.slice(buf.readerIndex(), length);

            try {
                DECODE.invokeExact((PacketDecoder<?>) decoder, context, packet, out);
            } catch (Throwable t2) {
                throw t2 instanceof RuntimeException re ? re : new RuntimeException(t2);
            }

            if (packet.readerIndex() != packet.capacity()) {
                throw new AssertionError("Should NOT reach here: PacketDecoder should consume all bytes, or throw an exception.");
            }

            buf.skipBytes(length);
        }
        buf.release();
    }
}

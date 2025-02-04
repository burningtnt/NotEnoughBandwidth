package cn.ussshenzhou.notenoughbandwidth.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPipeline;
import net.minecraft.network.PacketEncoder;

import javax.annotation.Nullable;
import java.lang.invoke.*;

/**
 * @author USS_Shenzhou
 */
public class ChannelPipeline {
    private static final MethodHandle MH_HEAD, MH_TAIL, MH_NEXT;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(DefaultChannelPipeline.class, MethodHandles.lookup());

            MH_HEAD = lookup.findVarHandle(DefaultChannelPipeline.class, "head", Class.forName("io.netty.channel.DefaultChannelPipeline.HeadContext"))
                    .toMethodHandle(VarHandle.AccessMode.GET)
                    .asType(MethodType.methodType(Object.class, ChannelHandlerContext.class));
            MH_TAIL = lookup.findVarHandle(DefaultChannelPipeline.class, "tail", Class.forName("io.netty.channel.DefaultChannelPipeline.TailContext"))
                    .toMethodHandle(VarHandle.AccessMode.GET)
                    .asType(MethodType.methodType(Object.class, ChannelHandlerContext.class));

            Class<?> target = Class.forName("io.netty.channel.AbstractChannelHandlerContext");
            lookup = MethodHandles.privateLookupIn(target, MethodHandles.lookup());

            MH_NEXT = lookup.findVarHandle(target, "next", target)
                    .toMethodHandle(VarHandle.AccessMode.GET_VOLATILE)
                    .asType(MethodType.methodType(ChannelHandlerContext.class, ChannelHandlerContext.class));;

        } catch (ReflectiveOperationException | WrongMethodTypeException e) {
            throw new AssertionError(e);
        }
    }

    @Nullable
    public static PacketEncoder<?> getPacketEncoder(DefaultChannelPipeline pipeline) {
        try {
            ChannelHandlerContext current = (ChannelHandlerContext) MH_HEAD.invokeExact(pipeline),
                    tail = (ChannelHandlerContext) MH_TAIL.invokeExact(pipeline);

            while (true) {
                if (current.handler() instanceof PacketEncoder<?> encoder) {
                    return encoder;
                }

                current = (ChannelHandlerContext) MH_NEXT.invokeExact(current);

                if (current == tail) {
                    // Not sure whether this is a bug or not: The 'last' handler won't be considered even if it is a PacketEncoder.
                    return null;
                }
            }
        } catch (Throwable t) {
            throw t instanceof RuntimeException re ? re : new RuntimeException(t);
        }
    }
}

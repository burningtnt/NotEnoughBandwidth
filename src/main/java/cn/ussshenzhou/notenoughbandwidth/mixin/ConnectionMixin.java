package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.network.NetworkManager;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Objects;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = Connection.class)
public abstract class ConnectionMixin {
    // TODO: Use coremod instead?

    @ModifyArg(method = "doSendPacket", at = @At(
            value = "INVOKE", target = "Lio/netty/channel/Channel;writeAndFlush(Ljava/lang/Object;)Lio/netty/channel/ChannelFuture;"
    ), index = 0)
    private Object transferPacket1(Object packet) {
        return Objects.requireNonNullElse(NetworkManager.transferPackage((Connection) (Object) this, (Packet<?>)packet), packet);
    }

    @ModifyArg(method = "doSendPacket", at = @At(
            value = "INVOKE", target = "Lio/netty/channel/Channel;write(Ljava/lang/Object;)Lio/netty/channel/ChannelFuture;"
    ), index = 0)
    private Object transferPacket2(Object packet) {
        return Objects.requireNonNullElse(NetworkManager.transferPackage((Connection) (Object) this, (Packet<?>)packet), packet);
    }

    @ModifyArg(method = "lambda$doSendPacket$13", at = @At(
            value = "INVOKE", target = "Lio/netty/channel/Channel;writeAndFlush(Ljava/lang/Object;)Lio/netty/channel/ChannelFuture;"
    ), index = 0)
    private Object transferPacket3(Object packet) {
        return Objects.requireNonNullElse(NetworkManager.transferPackage((Connection) (Object) this, (Packet<?>)packet), packet);
    }

    @ModifyConstant(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            constant = @Constant(intValue = 1)
    )
    private int disableDefaultFlush(int constant) {
        return 0;
    }
}

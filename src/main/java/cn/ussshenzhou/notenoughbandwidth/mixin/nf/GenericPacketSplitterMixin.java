package cn.ussshenzhou.notenoughbandwidth.mixin.nf;

import cn.ussshenzhou.notenoughbandwidth.network.aggressive.CompressedPacket;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;
import net.neoforged.neoforge.network.filters.GenericPacketSplitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(GenericPacketSplitter.class)
public class GenericPacketSplitterMixin {
    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    private void onEncodePacket(ChannelHandlerContext ctx, Packet<?> packet, List<Object> out, CallbackInfo ci) {
        if (packet instanceof CompressedPacket) {
            out.add(packet);
            ci.cancel();
        }
    }
}

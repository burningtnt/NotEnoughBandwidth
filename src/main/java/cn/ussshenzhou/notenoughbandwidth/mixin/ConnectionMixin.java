package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.managers.PacketAggregationManager;
import cn.ussshenzhou.notenoughbandwidth.modnetwork.PacketAggregationPacket;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author USS_Shenzhou
 */
@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void nebwPacketAggregate(Packet<?> packet, PacketSendListener listener, boolean flush, CallbackInfo ci) {
        Connection thiz = (Connection) (Object) this;
        if (thiz.getPacketListener() == null || thiz.getPacketListener().protocol() != ConnectionProtocol.PLAY) {
            return;
        }
        if (packet instanceof BundlePacket<?>){
            return;
        }
        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)
                && payload instanceof PacketAggregationPacket) {
            return;
        }
        if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)
                && payload instanceof PacketAggregationPacket) {
            return;
        }
        if (!PacketAggregationManager.aboutToSend(packet, thiz)) {
            ci.cancel();
        }
    }
}

package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.managers.PacketAggregationManager;
import cn.ussshenzhou.notenoughbandwidth.network.PacketAggregationPacket;
import cn.ussshenzhou.notenoughbandwidth.network.compressed.CompressedCustomPayloadPacket;
import cn.ussshenzhou.notenoughbandwidth.network.compressed.CustomPayload;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = Connection.class, priority = 1)
public abstract class ConnectionMixin {

    @Shadow
    @Nullable
    private volatile PacketListener packetListener;

    @Shadow
    public abstract void send(Packet<?> packet, @org.jetbrains.annotations.Nullable PacketSendListener listener, boolean flush);

    @Shadow
    private Channel channel;

    @Shadow
    @Final
    private PacketFlow receiving;

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void aggregatePackets(Packet<?> packet, PacketSendListener sendListener, boolean flush, CallbackInfo ci) {
        PacketListener listener = this.packetListener;
        if (listener == null || listener.protocol() == ConnectionProtocol.PLAY) {
            return;
        }

        if (packet instanceof BundlePacket<?> bundlePacket) {
            bundlePacket.subPackets().forEach(p -> this.send(p, sendListener, flush));
            ci.cancel();
            return;
        }

        if (packet instanceof CustomPayload pp && pp.payload() instanceof PacketAggregationPacket) {
            return;
        }

        if (true) {
            return;
        }

        if (!PacketAggregationManager.aboutToSend(packet, (Connection) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "doSendPacket", at = @At("HEAD"))
    private void wrapPackets(Packet<?> packet, PacketSendListener sendListener, boolean flush, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Packet<?>> packetRef) {
        if (!(packet instanceof CustomPayload pp)) {
            return;
        }

        CustomPacketPayload payload = pp.payload();
        switch (payload.type().id().getNamespace()) {
            case "c", "neoforge", "minecraft", "velocity" -> {
                return;
            }
        }

        packetRef.set(new CompressedCustomPayloadPacket(switch (receiving) {
            case CLIENTBOUND -> CompressedCustomPayloadPacket.C_TYPE;
            case SERVERBOUND -> CompressedCustomPayloadPacket.S_TYPE;
        }, payload));
    }
}

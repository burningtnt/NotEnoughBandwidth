package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.network.NetworkManager;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress.CompressDecoder;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress.CompressEncoder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.Nullable;
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
    @Shadow
    private int sentPackets;

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, @Nullable PacketSendListener sendListener, boolean flush, CallbackInfo ci) {
        if (packet.isTerminal()) {
            NetworkManager.release((Connection) (Object) this);
        } else if (sendListener == null && NetworkManager.onSendPacket((Connection) (Object) this, packet)) {
            this.sentPackets++;
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        NetworkManager.tick((Connection) (Object) this);
    }

    @Inject(method = "configureSerialization", at = @At("TAIL"))
    private static void onConfigureSerialization(ChannelPipeline pipeline, PacketFlow flow, boolean memoryOnly, BandwidthDebugMonitor bandwithDebugMonitor, CallbackInfo ci) {
        if (pipeline.get("encoder") instanceof PacketEncoder<?>) {
            pipeline.addAfter("encoder", CompressEncoder.ID, CompressEncoder.INSTANCE);
        }

        if (pipeline.get("decoder") instanceof PacketDecoder<?>) {
            pipeline.addAfter("decoder", CompressDecoder.ID, CompressDecoder.INSTANCE);
        }
    }

    @ModifyExpressionValue(method = "setupOutboundProtocol", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/UnconfiguredPipelineHandler;setupOutboundProtocol(Lnet/minecraft/network/ProtocolInfo;)Lnet/minecraft/network/UnconfiguredPipelineHandler$OutboundConfigurationTask;"
    ))
    private UnconfiguredPipelineHandler.OutboundConfigurationTask onSetupOutboundProtocol(
            UnconfiguredPipelineHandler.OutboundConfigurationTask original,
            @Local(index = 1, argsOnly = true) ProtocolInfo<?> protocolInfo
    ) {
        return original.andThen(context -> {
            context.pipeline().addAfter("encoder", CompressEncoder.ID, CompressEncoder.INSTANCE);

            if (protocolInfo.id() == ConnectionProtocol.PLAY) {
                NetworkManager.enable((Connection) (Object) this);
            }
        });
    }

    @ModifyExpressionValue(method = "setupInboundProtocol", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/UnconfiguredPipelineHandler;setupInboundProtocol(Lnet/minecraft/network/ProtocolInfo;)Lnet/minecraft/network/UnconfiguredPipelineHandler$InboundConfigurationTask;"
    ))
    private UnconfiguredPipelineHandler.InboundConfigurationTask onSetupInboundProtocol(
            UnconfiguredPipelineHandler.InboundConfigurationTask original,
            @Local(index = 1, argsOnly = true) ProtocolInfo<?> protocolInfo
    ) {
        return original.andThen(context -> {
            context.pipeline().addAfter("decoder", CompressDecoder.ID, CompressDecoder.INSTANCE);
        });
    }
}

package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.network.NetworkManager;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress.CompressedDecoder;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress.CompressedEncoder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.channel.Channel;
import net.minecraft.network.*;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    private int sentPackets;

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, @Nullable PacketSendListener sendListener, boolean flush, CallbackInfo ci) {
        if (sendListener != null) {
            return;
        }

        if (NetworkManager.onSendPacket((Connection) (Object) this, packet)) {
            this.sentPackets++;
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        NetworkManager.tick((Connection) (Object) this);
    }

    @ModifyExpressionValue(method = "setupOutboundProtocol", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/UnconfiguredPipelineHandler;setupOutboundProtocol(Lnet/minecraft/network/ProtocolInfo;)Lnet/minecraft/network/UnconfiguredPipelineHandler$OutboundConfigurationTask;"
    ))
    private UnconfiguredPipelineHandler.OutboundConfigurationTask onSetupOutboundProtocol(
            UnconfiguredPipelineHandler.OutboundConfigurationTask original,
            @Local(index = 1, argsOnly = true) ProtocolInfo<?> protocolInfo
    ) {
        if (protocolInfo.id() == ConnectionProtocol.PLAY) {
            return original.andThen(context -> {
                context.pipeline().addAfter("encoder", CompressedEncoder.ID, CompressedEncoder.INSTANCE);
                NetworkManager.enable((Connection) (Object) this);
            });
        }
        return original;
    }

    @ModifyExpressionValue(method = "setupInboundProtocol", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/UnconfiguredPipelineHandler;setupInboundProtocol(Lnet/minecraft/network/ProtocolInfo;)Lnet/minecraft/network/UnconfiguredPipelineHandler$InboundConfigurationTask;"
    ))
    private UnconfiguredPipelineHandler.InboundConfigurationTask onSetupInboundProtocol(
            UnconfiguredPipelineHandler.InboundConfigurationTask original,
            @Local(index = 1, argsOnly = true) ProtocolInfo<?> protocolInfo
    ) {
        if (protocolInfo.id() == ConnectionProtocol.PLAY) {
            return original.andThen(context -> {
                context.pipeline().addAfter("decoder", CompressedDecoder.ID, CompressedDecoder.INSTANCE);
            });
        }
        return original;
    }
}

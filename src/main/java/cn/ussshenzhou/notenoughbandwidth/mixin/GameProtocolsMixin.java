package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.network.compressed.CompressedCustomPayloadPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.ProtocolInfoBuilder;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameProtocols.class)
public class GameProtocolsMixin {
    @Inject(
            method = "lambda$static$1",
            at = @At("TAIL")
    )
    private static void onRegisterClientBoundPackets(ProtocolInfoBuilder<ClientGamePacketListener, RegistryFriendlyByteBuf> builder, CallbackInfo ci) {
        builder.addPacket(CompressedCustomPayloadPacket.C_TYPE, CompressedCustomPayloadPacket.C_CODEC);
    }

    @Inject(
            method = "lambda$static$0",
            at = @At("TAIL")
    )
    private static void onRegisterServerBoundPackets(ProtocolInfoBuilder<ServerGamePacketListener, RegistryFriendlyByteBuf> builder, CallbackInfo ci) {
        builder.addPacket(CompressedCustomPayloadPacket.S_TYPE, CompressedCustomPayloadPacket.S_CODEC);
    }
}

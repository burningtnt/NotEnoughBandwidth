package org.teacon.neb.mixin;

import org.teacon.neb.network.aggressive.CompressedPacket;
import org.teacon.neb.network.indexed.IndexPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.ProtocolInfoBuilder;
import net.minecraft.network.protocol.game.GameProtocols;
import net.neoforged.neoforge.common.extensions.IClientCommonPacketListenerExtension;
import net.neoforged.neoforge.common.extensions.IServerCommonPacketListenerExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameProtocols.class)
public class GameProtocolsMixin {
    @Inject(
            method = "lambda$static$0",
            at = @At("TAIL")
    )
    private static void onRegisterServerBoundPackets(ProtocolInfoBuilder<IServerCommonPacketListenerExtension, FriendlyByteBuf> builder, CallbackInfo ci) {
        builder.addPacket(IndexPacket.S_TYPE, IndexPacket.S_CODEC)
                .addPacket(CompressedPacket.S_TYPE, CompressedPacket.S_CODEC);
    }

    @Inject(
            method = "lambda$static$1",
            at = @At("TAIL")
    )
    private static void onRegisterClientBoundPackets(ProtocolInfoBuilder<IClientCommonPacketListenerExtension, FriendlyByteBuf> builder, CallbackInfo ci) {
        builder.addPacket(IndexPacket.C_TYPE, IndexPacket.C_CODEC)
                .addPacket(CompressedPacket.C_TYPE, CompressedPacket.C_CODEC);
    }
}

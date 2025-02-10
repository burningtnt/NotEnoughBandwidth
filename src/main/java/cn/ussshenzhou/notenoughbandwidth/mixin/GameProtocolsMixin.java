package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.network.aggressive.CompressedPacket;
import cn.ussshenzhou.notenoughbandwidth.network.indexed.IndexPacket;
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
    // Velocity will pass through packets with unknown ID.

    @Inject(
            method = "lambda$static$0",
            at = @At("TAIL")
    )
    private static void onRegisterServerBoundPackets(ProtocolInfoBuilder<ServerGamePacketListener, RegistryFriendlyByteBuf> builder, CallbackInfo ci) {
        builder.addPacket(IndexPacket.S_TYPE, IndexPacket.S_CODEC)
                .addPacket(CompressedPacket.S_TYPE, CompressedPacket.S_CODEC);
    }

    @Inject(
            method = "lambda$static$1",
            at = @At("TAIL")
    )
    private static void onRegisterClientBoundPackets(ProtocolInfoBuilder<ClientGamePacketListener, RegistryFriendlyByteBuf> builder, CallbackInfo ci) {
        builder.addPacket(IndexPacket.C_TYPE, IndexPacket.C_CODEC)
                .addPacket(CompressedPacket.C_TYPE, CompressedPacket.C_CODEC);
    }
}

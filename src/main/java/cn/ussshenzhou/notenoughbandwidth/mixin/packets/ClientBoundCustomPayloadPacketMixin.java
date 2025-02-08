package cn.ussshenzhou.notenoughbandwidth.mixin.packets;

import cn.ussshenzhou.notenoughbandwidth.network.VanillaCustomPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class ClientBoundCustomPayloadPacketMixin implements VanillaCustomPayload {
    // DO NOT remove this shadow method! It's the implementation of interface CustomPayload
    @Shadow
    public abstract @NotNull CustomPacketPayload payload();
}

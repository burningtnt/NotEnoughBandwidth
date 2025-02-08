package cn.ussshenzhou.notenoughbandwidth.mixin.packets;

import cn.ussshenzhou.notenoughbandwidth.network.VanillaCustomPayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerboundCustomPayloadPacket.class)
public abstract class ServerBoundCustomPayloadPacketMixin implements VanillaCustomPayload {
    // DO NOT remove this shadow method! It's the implementation of interface CustomPayload
    @Shadow
    public abstract @NotNull CustomPacketPayload payload();
}

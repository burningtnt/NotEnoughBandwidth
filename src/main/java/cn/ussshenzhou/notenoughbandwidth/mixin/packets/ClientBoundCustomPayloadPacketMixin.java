package cn.ussshenzhou.notenoughbandwidth.mixin.packets;

import cn.ussshenzhou.notenoughbandwidth.network.compressed.CustomPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class ClientBoundCustomPayloadPacketMixin implements CustomPayload {
    @Shadow
    public abstract CustomPacketPayload payload();
}

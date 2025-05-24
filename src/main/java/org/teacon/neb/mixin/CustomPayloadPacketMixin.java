package org.teacon.neb.mixin;

import org.teacon.neb.network.VanillaCustomPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ClientboundCustomPayloadPacket.class, ServerboundCustomPayloadPacket.class})
@Implements(@Interface(iface = VanillaCustomPayload.class, prefix = "payload$"))
public abstract class CustomPayloadPacketMixin {
}

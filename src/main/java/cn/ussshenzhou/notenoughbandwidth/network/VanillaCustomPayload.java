package cn.ussshenzhou.notenoughbandwidth.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface VanillaCustomPayload {
    CustomPacketPayload payload();
}

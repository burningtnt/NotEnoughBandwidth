package cn.ussshenzhou.notenoughbandwidth.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public interface VanillaCustomPayload {
    @NotNull
    CustomPacketPayload payload();
}

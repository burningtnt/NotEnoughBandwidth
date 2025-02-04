package cn.ussshenzhou.notenoughbandwidth.network.compressed;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Instead of vanilla {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload#codec(CustomPacketPayload.FallbackProvider, List, ConnectionProtocol, PacketFlow)},
 * we here use such protocol to avoid putting a huge ResourceLocation into bytebuf.
 * <p>
 * <h4>Fixed 8 bits header</h4>
 * <pre>
 * ┌------------- 1 byte (8 bits) ---------------┐
 * │               function flags                │
 * ├---┬---┬-------------------------------------┤
 * │ i │ t │      reserved (6 bits)              │
 * └---┴---┴-------------------------------------┘
 *
 * i = indexed (1 bit)
 * t = tight_indexed (1 bit, only valid if i=1)
 * reserved = 6 bits (for future use)
 *
 * </pre>
 *
 * <h4>Indexed packet type</h4>
 * <pre>
 * - If i=0 (not indexed):
 *
 *   ┌---------------- N bytes ----------------┐
 *   │ ResourceLocation (packet type) in UTF-8 │
 *   └-----------------------------------------┘
 *
 * - If i=1 and t=0 (indexed, NOT tight):
 *
 *   ┌-------- 1 byte ---------┬-------- 1 byte --------┬-------- 1 byte --------┐
 *   ┌------------- 12 bits ---------------┬-------------- 12 bits --------------┐
 *   │    namespace-id (capacity 4096)     │       path-id (capacity 4096)       │
 *   └-------------------------------------┴-------------------------------------┘
 *
 * - If i=1 and t=1 (indexed, tight):
 *
 *   ┌--------- 1 byte ----------┬--------- 1 byte ---------┐
 *   ┌--------- 8 bits ----------┬--------- 8 bits ---------┐
 *   │namespace-id (capacity 256)│  path-id (capacity 256)  │
 *   └---------------------------┴--------------------------┘
 *
 * </pre>
 *
 * <h4>Then packet data.</h4>
 *
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface CustomPayload {
    CustomPacketPayload payload();
}

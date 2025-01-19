package cn.ussshenzhou.notenoughbandwidth.managers;

import cn.ussshenzhou.notenoughbandwidth.helpers.DefaultChannelPipelineHelper;
import cn.ussshenzhou.notenoughbandwidth.modnetwork.PacketAggregationPacket;
import com.mojang.logging.LogUtils;
import io.netty.channel.DefaultChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author USS_Shenzhou
 */
public class PacketAggregationManager {
    private static final Object LOCK = new Object();
    private static final ConcurrentHashMap<ResourceLocation, ArrayDeque<AtomicInteger>> PACKET_FREQUENCY_COUNTER = new ConcurrentHashMap<>();
    private static final HashSet<ResourceLocation> TAKE_OVER_LIST = new HashSet<>();
    private static final WeakHashMap<Connection, HashMap<ResourceLocation, ArrayList<Packet<?>>>> PACKET_BUFFER = new WeakHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
    private static final ArrayList<ScheduledFuture<?>> TASKS = new ArrayList<>();
    private static final int PACKETS_THRESHOLD_IN_1SEC = 20 * 10;
    private static final int FLUSH_PERIOD_IN_MS = 20;
    private static final int QUEUE_CAPACITY = Math.max(1000 / FLUSH_PERIOD_IN_MS, 2);

    public static void init() {
        synchronized (LOCK) {
            PACKET_FREQUENCY_COUNTER.clear();
            TAKE_OVER_LIST.clear();
            PACKET_BUFFER.clear();
            TASKS.forEach(task -> task.cancel(false));
            TASKS.clear();
            TASKS.add(TIMER.scheduleAtFixedRate(() -> {
                synchronized (LOCK) {
                    flush();
                    for (var queue : PACKET_FREQUENCY_COUNTER.values()) {
                        if (queue.size() >= QUEUE_CAPACITY) {
                            queue.pollFirst();
                        }
                        queue.addLast(new AtomicInteger());
                    }
                }
            }, 0, 50, TimeUnit.MILLISECONDS));
        }
    }

    private static boolean isAggregating(ResourceLocation type) {
        synchronized (LOCK) {
            if (TAKE_OVER_LIST.contains(type)) {
                return true;
            }
            if (!PACKET_FREQUENCY_COUNTER.containsKey(type)) {
                ArrayDeque<AtomicInteger> queue = new ArrayDeque<>();
                queue.addLast(new AtomicInteger(1));
                PACKET_FREQUENCY_COUNTER.put(type, queue);
                return false;
            }
            int sum = PACKET_FREQUENCY_COUNTER.get(type).stream().mapToInt(AtomicInteger::get).sum();
            if (sum >= PACKETS_THRESHOLD_IN_1SEC) {
                PACKET_FREQUENCY_COUNTER.remove(type);
                TAKE_OVER_LIST.add(type);
                LogUtils.getLogger().debug("Aggregating packets of {}", type);
                return true;
            }
            var i = PACKET_FREQUENCY_COUNTER.get(type).peekLast();
            if (i == null) {
                PACKET_FREQUENCY_COUNTER.get(type).addLast(new AtomicInteger(1));
            } else {
                i.getAndIncrement();
            }
            return false;
        }
    }

    public static boolean aboutToSend(Packet<?> packet, Connection connection) {
        var type = packet.type().id();
        if (isAggregating(type)) {
            synchronized (LOCK) {
                PACKET_BUFFER.computeIfAbsent(connection, c -> new HashMap<>())
                        .computeIfAbsent(type, t -> new ArrayList<>())
                        .add(packet);
            }
            return false;
        }
        return true;
    }

    public static void flush() {
        synchronized (LOCK) {
            PACKET_BUFFER.forEach((connection, packetsMap) -> {
                var encoder = DefaultChannelPipelineHelper.getPacketEncoder((DefaultChannelPipeline) connection.channel().pipeline());
                if (encoder == null) {
                    LogUtils.getLogger().error("Failed to get PacketEncoder of connection {} {}.", connection.getDirection(), connection.getRemoteAddress());
                    return;
                }
                for (Map.Entry<ResourceLocation, ArrayList<Packet<?>>> entry : packetsMap.entrySet()) {
                    ResourceLocation type = entry.getKey();
                    ArrayList<Packet<?>> packets = entry.getValue();
                    connection.send(connection.getSending() == PacketFlow.CLIENTBOUND
                            ? new ClientboundCustomPayloadPacket(new PacketAggregationPacket(type, packets, encoder.getProtocolInfo()))
                            : new ServerboundCustomPayloadPacket(new PacketAggregationPacket(type, packets, encoder.getProtocolInfo()))
                    );
                }
                packetsMap.clear();
            });
        }
    }
}

package org.teacon.neb.profiler;

import com.mojang.logging.LogUtils;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PrometheusProfiler implements IProfiler {
    private final Counter TRANSMIT = Counter.build("neb_sent_total", "Total size of sent packets.").labelNames("id").register();
    private final Counter TRANSMIT_COMPRESSED = Counter.build("neb_sent_compressed_bytes_total", "Total size (compressed) of sent packets.").register();
    private final Counter RECEIVE = Counter.build("neb_received_total", "Total size of received packets.").labelNames("id").register();
    private final Gauge COMPRESSIBILITY = Gauge.build("neb_compressibility", "The compressibility of all transmit/received packets.").labelNames("id").register();

    private final ConcurrentMap<ResourceLocation, PacketCompressibility> TRACKING = new ConcurrentHashMap<>();

    public PrometheusProfiler(int port) {
        HTTPServer.Builder builder = new HTTPServer.Builder()
                .withPort(port)
                .withDaemonThreads(true);

        try {
            builder.build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        LogUtils.getLogger().info("Prometheus dashboard has started on port: {}", port);
    }

    @Override
    public void onTransmitPacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressedSize){
        double compressibility = Math.clamp(compressedSize / totalSize, 0, 1);

        TRANSMIT_COMPRESSED.inc(compressedSize);
        for (Object2IntMap.Entry<ResourceLocation> entry : Object2IntMaps.fastIterable(originalSizes)) {
            String packetType = entry.getKey().toString();
            COMPRESSIBILITY.labels(packetType).set(
                    TRACKING.computeIfAbsent(entry.getKey(), rl -> new PacketCompressibility())
                            .putSample(compressibility, entry.getIntValue() / totalSize)
            );
            TRANSMIT.labels(packetType).inc(entry.getIntValue());
        }
    }

    @Override
    public void onReceivePacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressedSize) {
        double compressibility = Math.clamp(compressedSize / totalSize, 0, 1);

        for (Object2IntMap.Entry<ResourceLocation> entry : Object2IntMaps.fastIterable(originalSizes)) {
            String packetType = entry.getKey().toString();
            COMPRESSIBILITY.labels(packetType).set(
                    TRACKING.computeIfAbsent(entry.getKey(), rl -> new PacketCompressibility())
                            .putSample(compressibility, entry.getIntValue() / totalSize)
            );
            RECEIVE.labels(packetType).inc(entry.getIntValue());
        }
    }
}

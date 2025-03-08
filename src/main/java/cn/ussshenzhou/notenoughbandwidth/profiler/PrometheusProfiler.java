package cn.ussshenzhou.notenoughbandwidth.profiler;

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
    public void onTransmitPacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressibility) {
        record(TRANSMIT, originalSizes, totalSize, compressibility);
    }

    @Override
    public void onReceivePacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressibility) {
        record(RECEIVE, originalSizes, totalSize, compressibility);
    }

    private void record(Counter counter, Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressibility) {
        if (Double.isNaN(compressibility) || compressibility <= 0D || compressibility >= 1D) {
            throw new IllegalArgumentException("Illegal argument compressibility: " + compressibility);
        }

        for (Object2IntMap.Entry<ResourceLocation> entry : Object2IntMaps.fastIterable(originalSizes)) {
            String packetType = entry.getKey().toString();
            COMPRESSIBILITY.labels(packetType).set(
                    TRACKING.computeIfAbsent(entry.getKey(), rl -> new PacketCompressibility())
                            .putSample(compressibility, entry.getIntValue() / totalSize)
            );
            counter.labels(packetType).inc(entry.getIntValue());
        }
    }
}

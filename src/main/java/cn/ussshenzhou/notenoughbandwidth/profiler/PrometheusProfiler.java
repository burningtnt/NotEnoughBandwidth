package cn.ussshenzhou.notenoughbandwidth.profiler;

import com.mojang.logging.LogUtils;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public final class PrometheusProfiler implements IProfiler {
    private final Counter SENT = Counter.build("neb_sent_total", "Total size of sent packets.").labelNames("id").register();
    private final Counter RECEIVED = Counter.build("neb_received_total", "Total size of received packets.").labelNames("id").register();

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
    public void onSendPacket(ResourceLocation type, int size) {
        SENT.labels(type.toString()).inc(size);
    }

    @Override
    public void onReceivePacket(ResourceLocation type, int size) {
        RECEIVED.labels(type.toString()).inc(size);
    }
}

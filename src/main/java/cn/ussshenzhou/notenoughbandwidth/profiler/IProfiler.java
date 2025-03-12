package cn.ussshenzhou.notenoughbandwidth.profiler;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceLocation;

public interface IProfiler {
    void onTransmitPacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressedSize);

    void onReceivePacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressedSize);

    static IProfiler getInstance() {
        String port = System.getenv("NEB_PROM_PORT");
        if (port == null) {
            return new IProfiler() {
                @Override
                public void onTransmitPacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressedSize) {
                }

                @Override
                public void onReceivePacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressedSize) {
                }
            };
        }

        return new PrometheusProfiler(Integer.parseInt(port));
    }
}

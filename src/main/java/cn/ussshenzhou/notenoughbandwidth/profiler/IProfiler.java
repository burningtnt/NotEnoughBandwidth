package cn.ussshenzhou.notenoughbandwidth.profiler;

import net.minecraft.resources.ResourceLocation;

public interface IProfiler {
    void onSendPacket(ResourceLocation type, int size);

    void onReceivePacket(ResourceLocation type, int size);

    static IProfiler getInstance() {
        String port = System.getenv("NEB_PROM_PORT");
        if (port == null) {
            return new IProfiler() {
                @Override
                public void onSendPacket(ResourceLocation type, int size) {
                }

                @Override
                public void onReceivePacket(ResourceLocation type, int size) {
                }
            };
        }

        return new PrometheusProfiler(Integer.parseInt(port));
    }
}

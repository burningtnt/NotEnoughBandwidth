package cn.ussshenzhou.notenoughbandwidth.network.aggressive;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress.CompressEncoder;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AggressiveBuffer {
    private static final long FLUSH_INTERVAL = 20; // (ms)

    private final Queue<Object> buffer = new ConcurrentLinkedQueue<>();
    private long lastTickTime = 0;

    private final Connection connection;

    public AggressiveBuffer(Connection connection) {
        this.connection = connection;
    }

    private static final AttributeKey<AggressiveBuffer> BUFFER = AttributeKey.valueOf(NotEnoughBandwidth.id("buffer").toString());

    public static void initialize(Connection connection) {
        AggressiveBuffer current = connection.channel().attr(BUFFER).setIfAbsent(new AggressiveBuffer(connection));
        if (current != null && !current.buffer.isEmpty()) {
            throw new IllegalStateException("Packets in the buffer hasn't been sent!");
        }
    }

    public static void release(Connection connection) {
        AggressiveBuffer current = connection.channel().attr(BUFFER).getAndSet(null);
        if (current != null) {
            current.flush();
        }
    }

    public static AggressiveBuffer get(Connection connection) {
        return connection.channel().attr(BUFFER).get();
    }

    public void push(Packet<?> packet) {
        this.buffer.add(packet);
    }

    public void tick() {
        long time = System.currentTimeMillis();
        if (time - lastTickTime > FLUSH_INTERVAL) {
            lastTickTime = time;

            flush();
        }
    }

    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        buffer.add(Boolean.TRUE);

        while (true) {
            List<Packet<?>> packets = new ArrayList<>(200);

            Object packet;
            while ((packet = buffer.poll()) != null) {
                if (packet == Boolean.TRUE) {
                    break;
                } else {
                    packets.add((Packet<?>) packet);

                    if (packets.size() > 200) {
                        break;
                    }
                }
            }

            if (!packets.isEmpty()) {
                this.connection.channel().writeAndFlush(new CompressEncoder.CompressedTransfer(switch (this.connection.getSending()) {
                    case CLIENTBOUND -> CompressedPacket.C_TYPE;
                    case SERVERBOUND -> CompressedPacket.S_TYPE;
                }, packets));
            }

            if (packet == Boolean.TRUE) {
                return;
            }
        }
    }
}

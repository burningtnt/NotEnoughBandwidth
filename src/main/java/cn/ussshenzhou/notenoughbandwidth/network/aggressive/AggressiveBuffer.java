package cn.ussshenzhou.notenoughbandwidth.network.aggressive;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress.CompressedEncoder;
import cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress.CompressedPacket;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class AggressiveBuffer {
    private static final long FLUSH_INTERVAL = 20; // (ms)

    private final Queue<Packet<?>> buffer = new ArrayDeque<>();
    private long lastTickTime = 0;

    private final Connection connection;

    public AggressiveBuffer(Connection connection) {
        this.connection = connection;
    }

    private static final AttributeKey<AggressiveBuffer> BUFFER = AttributeKey.valueOf(NotEnoughBandwidth.id("buffer").toString());

    public static void initialize(Connection connection) {
        connection.channel().attr(BUFFER).set(new AggressiveBuffer(connection));
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
        List<Packet<?>> packets = new ArrayList<>(buffer.size());
        Packet<?> packet;
        while ((packet = buffer.poll()) != null) {
            packets.add(packet);
        }

        if (packets.isEmpty()) {
            return;
        }

        this.connection.channel().writeAndFlush(new CompressedEncoder.CompressedTransfer(switch (this.connection.getSending()) {
            case CLIENTBOUND -> CompressedPacket.C_TYPE;
            case SERVERBOUND -> CompressedPacket.S_TYPE;
        }, packets));
    }
}

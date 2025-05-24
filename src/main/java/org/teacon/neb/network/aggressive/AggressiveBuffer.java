package org.teacon.neb.network.aggressive;

import io.netty.util.Attribute;
import org.teacon.neb.NotEnoughBandwidth;
import org.teacon.neb.network.aggressive.compress.CompressEncoder;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AggressiveBuffer {
    private static final long FLUSH_INTERVAL = 20; // (ms)

    private final Queue<Packet<?>> buffer = new ConcurrentLinkedQueue<>();
    private long lastTickTime = 0;

    private final Connection connection;

    public AggressiveBuffer(Connection connection) {
        this.connection = connection;
    }

    private static final AttributeKey<AggressiveBuffer> BUFFER = AttributeKey.valueOf(NotEnoughBandwidth.id("buffer").toString());

    private static Attribute<AggressiveBuffer> accessAB(Connection connection) {
        return connection.channel().attr(BUFFER);
    }

    public static void initialize(Connection connection) {
        AggressiveBuffer current = accessAB(connection).setIfAbsent(new AggressiveBuffer(connection));
        if (current != null && !current.buffer.isEmpty()) {
            throw new IllegalStateException("Packets in the buffer has been sent!");
        }
    }

    public static void release(Connection connection) {
        AggressiveBuffer current = accessAB(connection).getAndSet(null);
        if (current != null) {
            current.flush();
        }
    }

    public static AggressiveBuffer get(Connection connection) {
        return accessAB(connection).get();
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

        while (true) {
            List<Packet<?>> packets = new ArrayList<>(200);

            Packet<?> packet = null;
            while (packets.size() < 200 && (packet = buffer.poll()) != null) {
                packets.add(packet);
            }

            if (!packets.isEmpty()) {
                this.connection.channel().writeAndFlush(new CompressEncoder.CompressedTransfer(switch (this.connection.getSending()) {
                    case CLIENTBOUND -> CompressedPacket.C_TYPE;
                    case SERVERBOUND -> CompressedPacket.S_TYPE;
                }, packets));
            }

            if (packet == null) {
                break;
            }
        }
    }
}

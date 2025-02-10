package cn.ussshenzhou.notenoughbandwidth.network.aggressive.compress;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;

import java.util.Objects;

public final class CompressHelper {
    private static final int THRESHOLD = 160;

    private CompressHelper() {
    }

    public static void compress(ByteBuf original, ByteBuf target) {
        int size = original.readableBytes();

        if (size <= THRESHOLD) {
            VarInt.write(target, 0);
            target.writeBytes(original);
        } else {
            VarInt.write(target, size);

            int compressedSize = (int) Zstd.compressBound(size);
            target.ensureWritable(compressedSize);

            ByteBuf o2 = null, t2 = null;
            if (!original.isDirect()) {
                o2 = original.alloc().directBuffer(size, size);
                o2.writeBytes(original, size);
            }
            if (!target.isDirect()) {
                t2 = target.alloc().directBuffer(compressedSize, compressedSize);
            }

            int realSize = compress0(Objects.requireNonNullElse(o2, original), Objects.requireNonNullElse(t2, target));
            if (o2 != null) {
                o2.release();
            } else {
                original.skipBytes(size);
            }
            if (t2 != null) {
                t2.writerIndex(realSize);
                target.writeBytes(t2, realSize);
                t2.release();
            } else {
                target.writerIndex(target.writerIndex() + realSize);
            }
        }
    }

    public static ByteBuf decompress(ByteBuf compressed) {
        int size = VarInt.read(compressed);
        if (size == 0) {
            return compressed.readBytes(compressed.readableBytes());
        }

        int s2;
        ByteBuf original = compressed.alloc().directBuffer(size, size);
        if (compressed.isDirect()) {
            s2 = decompress0(compressed, original);
            compressed.skipBytes(compressed.readableBytes());
        } else {
            int remain = compressed.readableBytes();
            ByteBuf direct = compressed.alloc().directBuffer(remain, remain);
            direct.readBytes(compressed, remain);

            s2 = decompress0(direct, original);
            direct.release();
        }

        if (size != s2) {
            throw new IllegalStateException("Size mismatched!");
        }

        original.writerIndex(size);
        return original;
    }

    private static int compress0(ByteBuf from, ByteBuf to) {
        try (ZstdCompressCtx ctx = new ZstdCompressCtx()) {
            return ctx.setLevel(Zstd.defaultCompressionLevel()).setChecksum(false).setMagicless(true).compress(
                    to.nioBuffer(to.writerIndex(), to.writableBytes()),
                    from.nioBuffer()
            );
        }
    }

    private static int decompress0(ByteBuf from, ByteBuf to) {
        try (ZstdDecompressCtx ctx = new ZstdDecompressCtx()) {
            return ctx.setMagicless(true).decompress(
                    to.nioBuffer(to.writerIndex(), to.writableBytes()),
                    from.nioBuffer()
            );
        }
    }
}

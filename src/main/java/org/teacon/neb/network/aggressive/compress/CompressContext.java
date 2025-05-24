package org.teacon.neb.network.aggressive.compress;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;

import java.lang.ref.Cleaner;
import java.util.Objects;

public final class CompressContext {
    private static final int THRESHOLD = 160;

    private static final Cleaner CLEANER = Cleaner.create();
    private static final ThreadLocal<CompressContext> CONTEXT = ThreadLocal.withInitial(CompressContext::new);

    public static CompressContext get() {
        return CONTEXT.get();
    }

    private final ZstdCompressCtx compress;
    private final ZstdDecompressCtx decompress;

    @SuppressWarnings({"unused", "FieldCanBeLocal"}) // Keep a reference only.
    private final Cleaner.Cleanable cleanable;

    private CompressContext() {
        this.compress = new ZstdCompressCtx()
                .setLevel(Zstd.defaultCompressionLevel())
                .setChecksum(false)
                .setMagicless(true)
                .setContentSize(false);
        this.decompress = new ZstdDecompressCtx()
                .setMagicless(true);

        // TODO: We use Cleaner to close unused context for now. Maybe use a better implementation instead?
        this.cleanable = CLEANER.register(this, new CleanableImpl(compress::close, decompress::close));
    }

    private record CleanableImpl(Runnable... targets) implements Runnable {
        @Override
        public void run() {
            for (Runnable target : targets) {
                target.run();
            }
        }
    }

    public void compress(ByteBuf original, ByteBuf target) {
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

    public ByteBuf decompress(ByteBuf compressed) {
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
            compressed.readBytes(direct, remain);

            s2 = decompress0(direct, original);
            direct.release();
        }

        if (size != s2) {
            throw new IllegalStateException("Size mismatched!");
        }

        original.writerIndex(size);
        return original;
    }

    private int compress0(ByteBuf from, ByteBuf to) {
        return compress.compress(
                to.nioBuffer(to.writerIndex(), to.writableBytes()),
                from.nioBuffer()
        );
    }

    private int decompress0(ByteBuf from, ByteBuf to) {
        return decompress.decompress(
                to.nioBuffer(to.writerIndex(), to.writableBytes()),
                from.nioBuffer()
        );
    }
}

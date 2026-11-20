package gg.itzkatze.katze.replay;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

final class ReplayCompression {
    private ReplayCompression() {}

    static InputStream read(InputStream source) throws IOException {
        var input = new BufferedInputStream(source);
        input.mark(2);
        int first = input.read(), second = input.read();
        input.reset();
        return first == 0x1f && second == 0x8b ? new GZIPInputStream(input) : new ZstdInputStream(input).setLongMax(25);
    }

    static OutputStream write(OutputStream target) throws IOException { return new ZstdOutputStream(target, 6); }
}

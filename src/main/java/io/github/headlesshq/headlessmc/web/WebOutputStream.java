package io.github.headlesshq.headlessmc.web;

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * An {@link OutputStream} which buffers until it gets flushed and then passes the text on to the page.
 * Wrapped in an auto-flushing {@link java.io.PrintStream} this flushes on every println.
 */
@RequiredArgsConstructor
final class WebOutputStream extends OutputStream {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final String stream;

    @Override
    public void write(int b) {
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        buffer.write(b, off, len);
    }

    @Override
    public void flush() {
        if (buffer.size() > 0) {
            String text = buffer.toString(StandardCharsets.UTF_8);
            buffer.reset();
            WebBridge.write(stream, text);
        }
    }

}

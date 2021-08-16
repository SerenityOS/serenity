/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

public class NonBlocking {

    public static NonBlockingPumpReader nonBlockingPumpReader() {
        return new NonBlockingPumpReader();
    }

    public static NonBlockingPumpReader nonBlockingPumpReader(int size) {
        return new NonBlockingPumpReader(size);
    }

    public static NonBlockingPumpInputStream nonBlockingPumpInputStream() {
        return new NonBlockingPumpInputStream();
    }

    public static NonBlockingPumpInputStream nonBlockingPumpInputStream(int size) {
        return new NonBlockingPumpInputStream(size);
    }

    public static NonBlockingInputStream nonBlockingStream(NonBlockingReader reader, Charset encoding) {
        return new NonBlockingReaderInputStream(reader, encoding);
    }

    public static NonBlockingInputStream nonBlocking(String name, InputStream inputStream) {
        if (inputStream instanceof NonBlockingInputStream) {
            return (NonBlockingInputStream) inputStream;
        }
        return new NonBlockingInputStreamImpl(name, inputStream);
    }

    public static NonBlockingReader nonBlocking(String name, Reader reader) {
        if (reader instanceof NonBlockingReader) {
            return (NonBlockingReader) reader;
        }
        return new NonBlockingReaderImpl(name, reader);
    }

    public static NonBlockingReader nonBlocking(String name, InputStream inputStream, Charset encoding) {
        return new NonBlockingInputStreamReader(nonBlocking(name, inputStream), encoding);
    }

    private static class NonBlockingReaderInputStream extends NonBlockingInputStream {

        private final NonBlockingReader reader;
        private final CharsetEncoder encoder;

        // To encode a character with multiple bytes (e.g. certain Unicode characters)
        // we need enough space to encode them. Reading would fail if the read() method
        // is used to read a single byte in these cases.
        // Use this buffer to ensure we always have enough space to encode a character.
        private final ByteBuffer bytes;
        private final CharBuffer chars;

        private NonBlockingReaderInputStream(NonBlockingReader reader, Charset charset) {
            this.reader = reader;
            this.encoder = charset.newEncoder()
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .onMalformedInput(CodingErrorAction.REPLACE);
            this.bytes = ByteBuffer.allocate(4);
            this.chars = CharBuffer.allocate(2);
            // No input available after initialization
            this.bytes.limit(0);
            this.chars.limit(0);
        }

        @Override
        public int available() {
            return (int) (reader.available() * this.encoder.averageBytesPerChar())
                    + bytes.remaining();
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        @Override
        public int read(long timeout, boolean isPeek) throws IOException {
            boolean isInfinite = (timeout <= 0L);
            while (!bytes.hasRemaining() && (isInfinite || timeout > 0L)) {
                long start = 0;
                if (!isInfinite) {
                    start = System.currentTimeMillis();
                }
                int c = reader.read(timeout);
                if (c == EOF) {
                    return EOF;
                }
                if (c >= 0) {
                    if (!chars.hasRemaining()) {
                        chars.position(0);
                        chars.limit(0);
                    }
                    int l = chars.limit();
                    chars.array()[chars.arrayOffset() + l] = (char) c;
                    chars.limit(l + 1);
                    bytes.clear();
                    encoder.encode(chars, bytes, false);
                    bytes.flip();
                }
                if (!isInfinite) {
                    timeout -= System.currentTimeMillis() - start;
                }
            }
            if (bytes.hasRemaining()) {
                if (isPeek) {
                    return Byte.toUnsignedInt(bytes.get(bytes.position()));
                } else {
                    return Byte.toUnsignedInt(bytes.get());
                }
            } else {
                return READ_EXPIRED;
            }
        }

    }

    private static class NonBlockingInputStreamReader extends NonBlockingReader {

        private final NonBlockingInputStream input;
        private final CharsetDecoder decoder;
        private final ByteBuffer bytes;
        private final CharBuffer chars;

        public NonBlockingInputStreamReader(NonBlockingInputStream inputStream, Charset encoding) {
            this(inputStream,
                (encoding != null ? encoding : Charset.defaultCharset()).newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE));
        }

        public NonBlockingInputStreamReader(NonBlockingInputStream input, CharsetDecoder decoder) {
            this.input = input;
            this.decoder = decoder;
            this.bytes = ByteBuffer.allocate(4);
            this.chars = CharBuffer.allocate(2);
            this.bytes.limit(0);
            this.chars.limit(0);
        }

        @Override
        protected int read(long timeout, boolean isPeek) throws IOException {
            boolean isInfinite = (timeout <= 0L);
            while (!chars.hasRemaining() && (isInfinite || timeout > 0L)) {
                long start = 0;
                if (!isInfinite) {
                    start = System.currentTimeMillis();
                }
                int b = input.read(timeout);
                if (b == EOF) {
                    return EOF;
                }
                if (b >= 0) {
                    if (!bytes.hasRemaining()) {
                        bytes.position(0);
                        bytes.limit(0);
                    }
                    int l = bytes.limit();
                    bytes.array()[bytes.arrayOffset() + l] = (byte) b;
                    bytes.limit(l + 1);
                    chars.clear();
                    decoder.decode(bytes, chars, false);
                    chars.flip();
                }

                if (!isInfinite) {
                    timeout -= System.currentTimeMillis() - start;
                }
            }
            if (chars.hasRemaining()) {
                if (isPeek) {
                    return chars.get(chars.position());
                } else {
                    return chars.get();
                }
            } else {
                return READ_EXPIRED;
            }
        }

        @Override
        public int readBuffered(char[] b) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (b.length == 0) {
                return 0;
            } else {
                if (chars.hasRemaining()) {
                    int r = Math.min(b.length, chars.remaining());
                    chars.get(b);
                    return r;
                } else {
                    byte[] buf = new byte[b.length];
                    int l = input.readBuffered(buf);
                    if (l < 0) {
                        return l;
                    } else {
                        ByteBuffer currentBytes;
                        if (bytes.hasRemaining()) {
                            int transfer = bytes.remaining();
                            byte[] newBuf = new byte[l + transfer];
                            bytes.get(newBuf, 0, transfer);
                            System.arraycopy(buf, 0, newBuf, transfer, l);
                            currentBytes = ByteBuffer.wrap(newBuf);
                            bytes.position(0);
                            bytes.limit(0);
                        } else {
                            currentBytes = ByteBuffer.wrap(buf, 0, l);
                        }
                        CharBuffer chars = CharBuffer.wrap(b);
                        decoder.decode(currentBytes, chars, false);
                        chars.flip();
                        if (currentBytes.hasRemaining()) {
                            int pos = bytes.position();
                            bytes.limit(bytes.limit() + currentBytes.remaining());
                            bytes.put(currentBytes);
                            bytes.position(pos);
                        }
                        return chars.remaining();
                    }
                }
            }
        }

        @Override
        public void shutdown() {
            input.shutdown();
        }

        @Override
        public void close() throws IOException {
            input.close();
        }
    }

}

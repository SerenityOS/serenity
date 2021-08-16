/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.media.sound;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Resource Interchange File Format (RIFF) stream decoder.
 *
 * @author Karl Helgason
 */
public final class RIFFReader extends InputStream {

    private final RIFFReader root;
    private long filepointer = 0;
    private final String fourcc;
    private String riff_type = null;
    private final long ckSize;
    private final InputStream stream;
    private long avail = 0xffffffffL; // MAX_UNSIGNED_INT
    private RIFFReader lastiterator = null;

    public RIFFReader(final InputStream stream) throws IOException {

        if (stream instanceof RIFFReader) {
            root = ((RIFFReader) stream).root;
        } else {
            root = this;
        }

        this.stream = stream;

        // Check for RIFF null paddings,
        int b;
        while (true) {
            b = read();
            if (b == -1) {
                fourcc = ""; // don't put null value into fourcc,
                // because it is expected to
                // always contain a string value
                riff_type = null;
                ckSize = 0;
                avail = 0;
                return;
            }
            if (b != 0) {
                break;
            }
        }

        byte[] fourcc = new byte[4];
        fourcc[0] = (byte) b;
        readFully(fourcc, 1, 3);
        this.fourcc = new String(fourcc, US_ASCII);
        ckSize = readUnsignedInt();
        avail = ckSize;

        if (getFormat().equals("RIFF") || getFormat().equals("LIST")) {
            byte[] format = new byte[4];
            readFully(format);
            this.riff_type = new String(format, US_ASCII);
        }
    }

    public long getFilePointer() throws IOException {
        return root.filepointer;
    }

    public boolean hasNextChunk() throws IOException {
        if (lastiterator != null)
            lastiterator.finish();
        return avail != 0;
    }

    public RIFFReader nextChunk() throws IOException {
        if (lastiterator != null)
            lastiterator.finish();
        if (avail == 0)
            return null;
        lastiterator = new RIFFReader(this);
        return lastiterator;
    }

    public String getFormat() {
        return fourcc;
    }

    public String getType() {
        return riff_type;
    }

    public long getSize() {
        return ckSize;
    }

    @Override
    public int read() throws IOException {
        if (avail == 0) {
            return -1;
        }
        int b = stream.read();
        if (b == -1) {
            avail = 0;
            return -1;
        }
        avail--;
        filepointer++;
        return b;
    }

    @Override
    public int read(byte[] b, int offset, int len) throws IOException {
        if (avail == 0) {
            return -1;
        }
        if (len > avail) {
            int rlen = stream.read(b, offset, (int)avail);
            if (rlen != -1)
                filepointer += rlen;
            avail = 0;
            return rlen;
        } else {
            int ret = stream.read(b, offset, len);
            if (ret == -1) {
                avail = 0;
                return -1;
            }
            avail -= ret;
            filepointer += ret;
            return ret;
        }
    }

    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        while (len > 0) {
            int s = read(b, off, len);
            if (s < 0)
                throw new EOFException();
            if (s == 0)
                Thread.yield();
            off += s;
            len -= s;
        }
    }

    @Override
    public long skip(final long n) throws IOException {
        if (n <= 0 || avail == 0) {
            return 0;
        }
        // will not skip more than
        long remaining = Math.min(n, avail);
        while (remaining > 0) {
            // Some input streams like FileInputStream can return more bytes,
            // when EOF is reached.
            long ret = Math.min(stream.skip(remaining), remaining);
            if (ret == 0) {
                // EOF or not? we need to check.
                Thread.yield();
                if (stream.read() == -1) {
                    avail = 0;
                    break;
                }
                ret = 1;
            } else if (ret < 0) {
                // the skip should not return negative value, but check it also
                avail = 0;
                break;
            }
            remaining -= ret;
            avail -= ret;
            filepointer += ret;
        }
        return n - remaining;
    }

    @Override
    public int available() {
        return avail > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) avail;
    }

    public void finish() throws IOException {
        if (avail != 0) {
            skip(avail);
        }
    }

    // Read ASCII chars from stream
    public String readString(final int len) throws IOException {
        final byte[] buff;
        try {
            buff = new byte[len];
        } catch (final OutOfMemoryError oom) {
            throw new IOException("Length too big", oom);
        }
        readFully(buff);
        for (int i = 0; i < buff.length; i++) {
            if (buff[i] == 0) {
                return new String(buff, 0, i, US_ASCII);
            }
        }
        return new String(buff, US_ASCII);
    }

    // Read 8 bit signed integer from stream
    public byte readByte() throws IOException {
        int ch = read();
        if (ch < 0)
            throw new EOFException();
        return (byte) ch;
    }

    // Read 16 bit signed integer from stream
    public short readShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if (ch1 < 0)
            throw new EOFException();
        if (ch2 < 0)
            throw new EOFException();
        return (short)(ch1 | (ch2 << 8));
    }

    // Read 32 bit signed integer from stream
    public int readInt() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if (ch1 < 0)
            throw new EOFException();
        if (ch2 < 0)
            throw new EOFException();
        if (ch3 < 0)
            throw new EOFException();
        if (ch4 < 0)
            throw new EOFException();
        return ch1 + (ch2 << 8) | (ch3 << 16) | (ch4 << 24);
    }

    // Read 64 bit signed integer from stream
    public long readLong() throws IOException {
        long ch1 = read();
        long ch2 = read();
        long ch3 = read();
        long ch4 = read();
        long ch5 = read();
        long ch6 = read();
        long ch7 = read();
        long ch8 = read();
        if (ch1 < 0)
            throw new EOFException();
        if (ch2 < 0)
            throw new EOFException();
        if (ch3 < 0)
            throw new EOFException();
        if (ch4 < 0)
            throw new EOFException();
        if (ch5 < 0)
            throw new EOFException();
        if (ch6 < 0)
            throw new EOFException();
        if (ch7 < 0)
            throw new EOFException();
        if (ch8 < 0)
            throw new EOFException();
        return ch1 | (ch2 << 8) | (ch3 << 16) | (ch4 << 24)
                | (ch5 << 32) | (ch6 << 40) | (ch7 << 48) | (ch8 << 56);
    }

    // Read 8 bit unsigned integer from stream
    public int readUnsignedByte() throws IOException {
        int ch = read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    // Read 16 bit unsigned integer from stream
    public int readUnsignedShort() throws IOException {
        int ch1 = read();
        int ch2 = read();
        if (ch1 < 0)
            throw new EOFException();
        if (ch2 < 0)
            throw new EOFException();
        return ch1 | (ch2 << 8);
    }

    // Read 32 bit unsigned integer from stream
    public long readUnsignedInt() throws IOException {
        long ch1 = read();
        long ch2 = read();
        long ch3 = read();
        long ch4 = read();
        if (ch1 < 0)
            throw new EOFException();
        if (ch2 < 0)
            throw new EOFException();
        if (ch3 < 0)
            throw new EOFException();
        if (ch4 < 0)
            throw new EOFException();
        return ch1 + (ch2 << 8) | (ch3 << 16) | (ch4 << 24);
    }

    @Override
    public void close() throws IOException {
        finish();
        stream.close();
    }
}

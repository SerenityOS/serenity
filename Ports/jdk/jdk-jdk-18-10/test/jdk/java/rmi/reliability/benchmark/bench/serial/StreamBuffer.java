/*
 * Copyright (c) 1999, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 *
 */

package bench.serial;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * The StreamBuffer class provides a space that can be written to with an
 * OutputStream and read from with an InputStream.  It is similar to
 * PipedInput/OutputStream except that it is unsynchronized and more
 * lightweight.  StreamBuffers are used inside of the serialization benchmarks
 * in order to minimize the overhead incurred by reading and writing to/from the
 * underlying stream (using ByteArrayInput/OutputStreams results in allocation
 * of a new byte array with each cycle, while using PipedInput/OutputStreams
 * involves threading and synchronization).
 * <p>
 * Writes/reads to and from a StreamBuffer must occur in distinct phases; reads
 * from a StreamBuffer effectively close the StreamBuffer output stream.  These
 * semantics are necessary to avoid using wait/notify in
 * StreamBufferInputStream.read().
 */
public class StreamBuffer {

    /**
     * Output stream for writing to stream buffer.
     */
    private class StreamBufferOutputStream extends OutputStream {

        private int pos;

        public void write(int b) throws IOException {
            if (mode != WRITE_MODE)
                throw new IOException();
            while (pos >= buf.length)
                grow();
            buf[pos++] = (byte) b;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (mode != WRITE_MODE)
                throw new IOException();
            while (pos + len > buf.length)
                grow();
            System.arraycopy(b, off, buf, pos, len);
            pos += len;
        }

        public void close() throws IOException {
            if (mode != WRITE_MODE)
                throw new IOException();
            mode = READ_MODE;
        }
    }

    /**
     * Input stream for reading from stream buffer.
     */
    private class StreamBufferInputStream extends InputStream {

        private int pos;

        public int read() throws IOException {
            if (mode == CLOSED_MODE)
                throw new IOException();
            mode = READ_MODE;
            return (pos < out.pos) ? (buf[pos++] & 0xFF) : -1;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (mode == CLOSED_MODE)
                throw new IOException();
            mode = READ_MODE;
            int avail = out.pos - pos;
            int rlen = (avail < len) ? avail : len;
            System.arraycopy(buf, pos, b, off, rlen);
            pos += rlen;
            return rlen;
        }

        public long skip(long len) throws IOException {
            if (mode == CLOSED_MODE)
                throw new IOException();
            mode = READ_MODE;
            int avail = out.pos - pos;
            long slen = (avail < len) ? avail : len;
            pos += slen;
            return slen;
        }

        public int available() throws IOException {
            if (mode == CLOSED_MODE)
                throw new IOException();
            mode = READ_MODE;
            return out.pos - pos;
        }

        public void close() throws IOException {
            if (mode == CLOSED_MODE)
                throw new IOException();
            mode = CLOSED_MODE;
        }
    }

    private static final int START_BUFSIZE = 256;
    private static final int GROW_FACTOR = 2;
    private static final int CLOSED_MODE = 0;
    private static final int WRITE_MODE = 1;
    private static final int READ_MODE = 2;

    private byte[] buf;
    private StreamBufferOutputStream out = new StreamBufferOutputStream();
    private StreamBufferInputStream in = new StreamBufferInputStream();
    private int mode = WRITE_MODE;

    public StreamBuffer() {
        this(START_BUFSIZE);
    }

    public StreamBuffer(int size) {
        buf = new byte[size];
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public InputStream getInputStream() {
        return in;
    }

    public void reset() {
        in.pos = out.pos = 0;
        mode = WRITE_MODE;
    }

    private void grow() {
        byte[] newbuf = new byte[buf.length * GROW_FACTOR];
        System.arraycopy(buf, 0, newbuf, 0, buf.length);
        buf = newbuf;
    }
}

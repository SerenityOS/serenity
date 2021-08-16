/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

import java.io.OutputStream;
import java.io.IOException;

/**
 * This class implements a stream filter for writing compressed data in
 * the GZIP file format.
 * @author      David Connelly
 * @since 1.1
 *
 */
public class GZIPOutputStream extends DeflaterOutputStream {
    /**
     * CRC-32 of uncompressed data.
     */
    protected CRC32 crc = new CRC32();

    /*
     * GZIP header magic number.
     */
    private static final int GZIP_MAGIC = 0x8b1f;

    /*
     * Trailer size in bytes.
     *
     */
    private static final int TRAILER_SIZE = 8;

    // Represents the default "unknown" value for OS header, per RFC-1952
    private static final byte OS_UNKNOWN = (byte) 255;

    /**
     * Creates a new output stream with the specified buffer size.
     *
     * <p>The new output stream instance is created as if by invoking
     * the 3-argument constructor GZIPOutputStream(out, size, false).
     *
     * @param out the output stream
     * @param size the output buffer size
     * @throws    IOException If an I/O error has occurred.
     * @throws    IllegalArgumentException if {@code size <= 0}
     */
    public GZIPOutputStream(OutputStream out, int size) throws IOException {
        this(out, size, false);
    }

    /**
     * Creates a new output stream with the specified buffer size and
     * flush mode.
     *
     * @param out the output stream
     * @param size the output buffer size
     * @param syncFlush
     *        if {@code true} invocation of the inherited
     *        {@link DeflaterOutputStream#flush() flush()} method of
     *        this instance flushes the compressor with flush mode
     *        {@link Deflater#SYNC_FLUSH} before flushing the output
     *        stream, otherwise only flushes the output stream
     * @throws    IOException If an I/O error has occurred.
     * @throws    IllegalArgumentException if {@code size <= 0}
     *
     * @since 1.7
     */
    public GZIPOutputStream(OutputStream out, int size, boolean syncFlush)
        throws IOException
    {
        super(out, out != null ? new Deflater(Deflater.DEFAULT_COMPRESSION, true) : null,
              size,
              syncFlush);
        usesDefaultDeflater = true;
        writeHeader();
        crc.reset();
    }


    /**
     * Creates a new output stream with a default buffer size.
     *
     * <p>The new output stream instance is created as if by invoking
     * the 2-argument constructor GZIPOutputStream(out, false).
     *
     * @param out the output stream
     * @throws    IOException If an I/O error has occurred.
     */
    public GZIPOutputStream(OutputStream out) throws IOException {
        this(out, 512, false);
    }

    /**
     * Creates a new output stream with a default buffer size and
     * the specified flush mode.
     *
     * @param out the output stream
     * @param syncFlush
     *        if {@code true} invocation of the inherited
     *        {@link DeflaterOutputStream#flush() flush()} method of
     *        this instance flushes the compressor with flush mode
     *        {@link Deflater#SYNC_FLUSH} before flushing the output
     *        stream, otherwise only flushes the output stream
     *
     * @throws    IOException If an I/O error has occurred.
     *
     * @since 1.7
     */
    public GZIPOutputStream(OutputStream out, boolean syncFlush)
        throws IOException
    {
        this(out, 512, syncFlush);
    }

    /**
     * Writes array of bytes to the compressed output stream. This method
     * will block until all the bytes are written.
     * @param buf the data to be written
     * @param off the start offset of the data
     * @param len the length of the data
     * @throws    IOException If an I/O error has occurred.
     */
    public synchronized void write(byte[] buf, int off, int len)
        throws IOException
    {
        super.write(buf, off, len);
        crc.update(buf, off, len);
    }

    /**
     * Finishes writing compressed data to the output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     * @throws    IOException if an I/O error has occurred
     */
    public void finish() throws IOException {
        if (!def.finished()) {
            def.finish();
            while (!def.finished()) {
                int len = def.deflate(buf, 0, buf.length);
                if (def.finished() && len <= buf.length - TRAILER_SIZE) {
                    // last deflater buffer. Fit trailer at the end
                    writeTrailer(buf, len);
                    len = len + TRAILER_SIZE;
                    out.write(buf, 0, len);
                    return;
                }
                if (len > 0)
                    out.write(buf, 0, len);
            }
            // if we can't fit the trailer at the end of the last
            // deflater buffer, we write it separately
            byte[] trailer = new byte[TRAILER_SIZE];
            writeTrailer(trailer, 0);
            out.write(trailer);
        }
    }

    /*
     * Writes GZIP member header.
     */
    private void writeHeader() throws IOException {
        out.write(new byte[] {
                      (byte) GZIP_MAGIC,        // Magic number (short)
                      (byte)(GZIP_MAGIC >> 8),  // Magic number (short)
                      Deflater.DEFLATED,        // Compression method (CM)
                      0,                        // Flags (FLG)
                      0,                        // Modification time MTIME (int)
                      0,                        // Modification time MTIME (int)
                      0,                        // Modification time MTIME (int)
                      0,                        // Modification time MTIME (int)
                      0,                        // Extra flags (XFLG)
                      OS_UNKNOWN                // Operating system (OS)
                  });
    }

    /*
     * Writes GZIP member trailer to a byte array, starting at a given
     * offset.
     */
    private void writeTrailer(byte[] buf, int offset) throws IOException {
        writeInt((int)crc.getValue(), buf, offset); // CRC-32 of uncompr. data
        writeInt(def.getTotalIn(), buf, offset + 4); // Number of uncompr. bytes
    }

    /*
     * Writes integer in Intel byte order to a byte array, starting at a
     * given offset.
     */
    private void writeInt(int i, byte[] buf, int offset) throws IOException {
        writeShort(i & 0xffff, buf, offset);
        writeShort((i >> 16) & 0xffff, buf, offset + 2);
    }

    /*
     * Writes short integer in Intel byte order to a byte array, starting
     * at a given offset
     */
    private void writeShort(int s, byte[] buf, int offset) throws IOException {
        buf[offset] = (byte)(s & 0xff);
        buf[offset + 1] = (byte)((s >> 8) & 0xff);
    }
}

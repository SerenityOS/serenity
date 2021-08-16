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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Resource Interchange File Format (RIFF) stream encoder.
 *
 * @author Karl Helgason
 */
public final class RIFFWriter extends OutputStream {

    private interface RandomAccessWriter {

        void seek(long chunksizepointer) throws IOException;

        long getPointer() throws IOException;

        void close() throws IOException;

        void write(int b) throws IOException;

        void write(byte[] b, int off, int len) throws IOException;

        void write(byte[] bytes) throws IOException;

        long length() throws IOException;

        void setLength(long i) throws IOException;
    }

    private static class RandomAccessFileWriter implements RandomAccessWriter {

        RandomAccessFile raf;

        RandomAccessFileWriter(File file) throws FileNotFoundException {
            this.raf = new RandomAccessFile(file, "rw");
        }

        RandomAccessFileWriter(String name) throws FileNotFoundException {
            this.raf = new RandomAccessFile(name, "rw");
        }

        @Override
        public void seek(long chunksizepointer) throws IOException {
            raf.seek(chunksizepointer);
        }

        @Override
        public long getPointer() throws IOException {
            return raf.getFilePointer();
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }

        @Override
        public void write(int b) throws IOException {
            raf.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            raf.write(b, off, len);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            raf.write(bytes);
        }

        @Override
        public long length() throws IOException {
            return raf.length();
        }

        @Override
        public void setLength(long i) throws IOException {
            raf.setLength(i);
        }
    }

    private static class RandomAccessByteWriter implements RandomAccessWriter {

        byte[] buff = new byte[32];
        int length = 0;
        int pos = 0;
        byte[] s;
        final OutputStream stream;

        RandomAccessByteWriter(OutputStream stream) {
            this.stream = stream;
        }

        @Override
        public void seek(long chunksizepointer) throws IOException {
            pos = (int) chunksizepointer;
        }

        @Override
        public long getPointer() throws IOException {
            return pos;
        }

        @Override
        public void close() throws IOException {
            stream.write(buff, 0, length);
            stream.close();
        }

        @Override
        public void write(int b) throws IOException {
            if (s == null)
                s = new byte[1];
            s[0] = (byte)b;
            write(s, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int newsize = pos + len;
            if (newsize > length)
                setLength(newsize);
            int end = off + len;
            for (int i = off; i < end; i++) {
                buff[pos++] = b[i];
            }
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            write(bytes, 0, bytes.length);
        }

        @Override
        public long length() throws IOException {
            return length;
        }

        @Override
        public void setLength(long i) throws IOException {
            length = (int) i;
            if (length > buff.length) {
                int newlen = Math.max(buff.length << 1, length);
                byte[] newbuff = new byte[newlen];
                System.arraycopy(buff, 0, newbuff, 0, buff.length);
                buff = newbuff;
            }
        }
    }
    private int chunktype = 0; // 0=RIFF, 1=LIST; 2=CHUNK
    private RandomAccessWriter raf;
    private final long chunksizepointer;
    private final long startpointer;
    private RIFFWriter childchunk = null;
    private boolean open = true;
    private boolean writeoverride = false;

    public RIFFWriter(String name, String format) throws IOException {
        this(new RandomAccessFileWriter(name), format, 0);
    }

    public RIFFWriter(File file, String format) throws IOException {
        this(new RandomAccessFileWriter(file), format, 0);
    }

    public RIFFWriter(OutputStream stream, String format) throws IOException {
        this(new RandomAccessByteWriter(stream), format, 0);
    }

    private RIFFWriter(RandomAccessWriter raf, String format, int chunktype)
            throws IOException {
        if (chunktype == 0)
            if (raf.length() != 0)
                raf.setLength(0);
        this.raf = raf;
        if (raf.getPointer() % 2 != 0)
            raf.write(0);

        if (chunktype == 0)
            raf.write("RIFF".getBytes(US_ASCII));
        else if (chunktype == 1)
            raf.write("LIST".getBytes(US_ASCII));
        else
            raf.write((format + "    ").substring(0, 4).getBytes(US_ASCII));

        chunksizepointer = raf.getPointer();
        this.chunktype = 2;
        writeUnsignedInt(0);
        this.chunktype = chunktype;
        startpointer = raf.getPointer();
        if (chunktype != 2)
            raf.write((format + "    ").substring(0, 4).getBytes(US_ASCII));
    }

    public void seek(long pos) throws IOException {
        raf.seek(pos);
    }

    public long getFilePointer() throws IOException {
        return raf.getPointer();
    }

    public void setWriteOverride(boolean writeoverride) {
        this.writeoverride = writeoverride;
    }

    public boolean getWriteOverride() {
        return writeoverride;
    }

    @Override
    public void close() throws IOException {
        if (!open)
            return;
        if (childchunk != null) {
            childchunk.close();
            childchunk = null;
        }

        int bakchunktype = chunktype;
        long fpointer = raf.getPointer();
        raf.seek(chunksizepointer);
        chunktype = 2;
        writeUnsignedInt(fpointer - startpointer);

        if (bakchunktype == 0)
            raf.close();
        else
            raf.seek(fpointer);
        open = false;
        raf = null;
    }

    @Override
    public void write(int b) throws IOException {
        if (!writeoverride) {
            if (chunktype != 2) {
                throw new IllegalArgumentException(
                        "Only chunks can write bytes!");
            }
            if (childchunk != null) {
                childchunk.close();
                childchunk = null;
            }
        }
        raf.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!writeoverride) {
            if (chunktype != 2) {
                throw new IllegalArgumentException(
                        "Only chunks can write bytes!");
            }
            if (childchunk != null) {
                childchunk.close();
                childchunk = null;
            }
        }
        raf.write(b, off, len);
    }

    public RIFFWriter writeList(String format) throws IOException {
        if (chunktype == 2) {
            throw new IllegalArgumentException(
                    "Only LIST and RIFF can write lists!");
        }
        if (childchunk != null) {
            childchunk.close();
            childchunk = null;
        }
        childchunk = new RIFFWriter(this.raf, format, 1);
        return childchunk;
    }

    public RIFFWriter writeChunk(String format) throws IOException {
        if (chunktype == 2) {
            throw new IllegalArgumentException(
                    "Only LIST and RIFF can write chunks!");
        }
        if (childchunk != null) {
            childchunk.close();
            childchunk = null;
        }
        childchunk = new RIFFWriter(this.raf, format, 2);
        return childchunk;
    }

    // Write ASCII chars to stream
    public void writeString(String string) throws IOException {
        byte[] buff = string.getBytes();
        write(buff);
    }

    // Write ASCII chars to stream
    public void writeString(String string, int len) throws IOException {
        byte[] buff = string.getBytes();
        if (buff.length > len)
            write(buff, 0, len);
        else {
            write(buff);
            for (int i = buff.length; i < len; i++)
                write(0);
        }
    }

    // Write 8 bit signed integer to stream
    public void writeByte(int b) throws IOException {
        write(b);
    }

    // Write 16 bit signed integer to stream
    public void writeShort(short b) throws IOException {
        write((b >>> 0) & 0xFF);
        write((b >>> 8) & 0xFF);
    }

    // Write 32 bit signed integer to stream
    public void writeInt(int b) throws IOException {
        write((b >>> 0) & 0xFF);
        write((b >>> 8) & 0xFF);
        write((b >>> 16) & 0xFF);
        write((b >>> 24) & 0xFF);
    }

    // Write 64 bit signed integer to stream
    public void writeLong(long b) throws IOException {
        write((int) (b >>> 0) & 0xFF);
        write((int) (b >>> 8) & 0xFF);
        write((int) (b >>> 16) & 0xFF);
        write((int) (b >>> 24) & 0xFF);
        write((int) (b >>> 32) & 0xFF);
        write((int) (b >>> 40) & 0xFF);
        write((int) (b >>> 48) & 0xFF);
        write((int) (b >>> 56) & 0xFF);
    }

    // Write 8 bit unsigned integer to stream
    public void writeUnsignedByte(int b) throws IOException {
        writeByte((byte) b);
    }

    // Write 16 bit unsigned integer to stream
    public void writeUnsignedShort(int b) throws IOException {
        writeShort((short) b);
    }

    // Write 32 bit unsigned integer to stream
    public void writeUnsignedInt(long b) throws IOException {
        writeInt((int) b);
    }
}

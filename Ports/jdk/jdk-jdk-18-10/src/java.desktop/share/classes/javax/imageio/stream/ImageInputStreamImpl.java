/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.stream;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Stack;
import javax.imageio.IIOException;

/**
 * An abstract class implementing the {@code ImageInputStream} interface.
 * This class is designed to reduce the number of methods that must
 * be implemented by subclasses.
 *
 * <p> In particular, this class handles most or all of the details of
 * byte order interpretation, buffering, mark/reset, discarding,
 * closing, and disposing.
 */
public abstract class ImageInputStreamImpl implements ImageInputStream {

    private Stack<Long> markByteStack = new Stack<>();

    private Stack<Integer> markBitStack = new Stack<>();

    private boolean isClosed = false;

    // Length of the buffer used for readFully(type[], int, int)
    private static final int BYTE_BUF_LENGTH = 8192;

    /**
     * Byte buffer used for readFully(type[], int, int).  Note that this
     * array is also used for bulk reads in readShort(), readInt(), etc, so
     * it should be large enough to hold a primitive value (i.e. >= 8 bytes).
     * Also note that this array is package protected, so that it can be
     * used by ImageOutputStreamImpl in a similar manner.
     */
    byte[] byteBuf = new byte[BYTE_BUF_LENGTH];

    /**
     * The byte order of the stream as an instance of the enumeration
     * class {@code java.nio.ByteOrder}, where
     * {@code ByteOrder.BIG_ENDIAN} indicates network byte order
     * and {@code ByteOrder.LITTLE_ENDIAN} indicates the reverse
     * order.  By default, the value is
     * {@code ByteOrder.BIG_ENDIAN}.
     */
    protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    /**
     * The current read position within the stream.  Subclasses are
     * responsible for keeping this value current from any method they
     * override that alters the read position.
     */
    protected long streamPos;

    /**
     * The current bit offset within the stream.  Subclasses are
     * responsible for keeping this value current from any method they
     * override that alters the bit offset.
     */
    protected int bitOffset;

    /**
     * The position prior to which data may be discarded.  Seeking
     * to a smaller position is not allowed.  {@code flushedPos}
     * will always be {@literal >= 0}.
     */
    protected long flushedPos = 0;

    /**
     * Constructs an {@code ImageInputStreamImpl}.
     */
    public ImageInputStreamImpl() {
    }

    /**
     * Throws an {@code IOException} if the stream has been closed.
     * Subclasses may call this method from any of their methods that
     * require the stream not to be closed.
     *
     * @exception IOException if the stream is closed.
     */
    protected final void checkClosed() throws IOException {
        if (isClosed) {
            throw new IOException("closed");
        }
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    /**
     * Reads a single byte from the stream and returns it as an
     * {@code int} between 0 and 255.  If EOF is reached,
     * {@code -1} is returned.
     *
     * <p> Subclasses must provide an implementation for this method.
     * The subclass implementation should update the stream position
     * before exiting.
     *
     * <p> The bit offset within the stream must be reset to zero before
     * the read occurs.
     *
     * @return the value of the next byte in the stream, or {@code -1}
     * if EOF is reached.
     *
     * @exception IOException if the stream has been closed.
     */
    public abstract int read() throws IOException;

    /**
     * A convenience method that calls {@code read(b, 0, b.length)}.
     *
     * <p> The bit offset within the stream is reset to zero before
     * the read occurs.
     *
     * @return the number of bytes actually read, or {@code -1}
     * to indicate EOF.
     *
     * @exception NullPointerException if {@code b} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to {@code len} bytes from the stream, and stores
     * them into {@code b} starting at index {@code off}.
     * If no bytes can be read because the end of the stream has been
     * reached, {@code -1} is returned.
     *
     * <p> The bit offset within the stream must be reset to zero before
     * the read occurs.
     *
     * <p> Subclasses must provide an implementation for this method.
     * The subclass implementation should update the stream position
     * before exiting.
     *
     * @param b an array of bytes to be written to.
     * @param off the starting position within {@code b} to write to.
     * @param len the maximum number of bytes to read.
     *
     * @return the number of bytes actually read, or {@code -1}
     * to indicate EOF.
     *
     * @exception IndexOutOfBoundsException if {@code off} is
     * negative, {@code len} is negative, or {@code off + len}
     * is greater than {@code b.length}.
     * @exception NullPointerException if {@code b} is
     * {@code null}.
     * @exception IOException if an I/O error occurs.
     */
    public abstract int read(byte[] b, int off, int len) throws IOException;

    public void readBytes(IIOByteBuffer buf, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException("len < 0!");
        }
        if (buf == null) {
            throw new NullPointerException("buf == null!");
        }

        byte[] data = new byte[len];
        len = read(data, 0, len);

        buf.setData(data);
        buf.setOffset(0);
        buf.setLength(len);
    }

    public boolean readBoolean() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (ch != 0);
    }

    public byte readByte() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte)ch;
    }

    public int readUnsignedByte() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    public short readShort() throws IOException {
        if (read(byteBuf, 0, 2) != 2) {
            throw new EOFException();
        }

        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return (short)
                (((byteBuf[0] & 0xff) << 8) | ((byteBuf[1] & 0xff) << 0));
        } else {
            return (short)
                (((byteBuf[1] & 0xff) << 8) | ((byteBuf[0] & 0xff) << 0));
        }
    }

    public int readUnsignedShort() throws IOException {
        return ((int)readShort()) & 0xffff;
    }

    public char readChar() throws IOException {
        return (char)readShort();
    }

    public int readInt() throws IOException {
        if (read(byteBuf, 0, 4) !=  4) {
            throw new EOFException();
        }

        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return
                (((byteBuf[0] & 0xff) << 24) | ((byteBuf[1] & 0xff) << 16) |
                 ((byteBuf[2] & 0xff) <<  8) | ((byteBuf[3] & 0xff) <<  0));
        } else {
            return
                (((byteBuf[3] & 0xff) << 24) | ((byteBuf[2] & 0xff) << 16) |
                 ((byteBuf[1] & 0xff) <<  8) | ((byteBuf[0] & 0xff) <<  0));
        }
    }

    public long readUnsignedInt() throws IOException {
        return ((long)readInt()) & 0xffffffffL;
    }

    public long readLong() throws IOException {
        // REMIND: Once 6277756 is fixed, we should do a bulk read of all 8
        // bytes here as we do in readShort() and readInt() for even better
        // performance (see 6347575 for details).
        int i1 = readInt();
        int i2 = readInt();

        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return ((long)i1 << 32) + (i2 & 0xFFFFFFFFL);
        } else {
            return ((long)i2 << 32) + (i1 & 0xFFFFFFFFL);
        }
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public String readLine() throws IOException {
        StringBuilder input = new StringBuilder();
        int c = -1;
        boolean eol = false;

        while (!eol) {
            switch (c = read()) {
            case -1:
            case '\n':
                eol = true;
                break;
            case '\r':
                eol = true;
                long cur = getStreamPosition();
                if ((read()) != '\n') {
                    seek(cur);
                }
                break;
            default:
                input.append((char)c);
                break;
            }
        }

        if ((c == -1) && (input.length() == 0)) {
            return null;
        }
        return input.toString();
    }

    public String readUTF() throws IOException {
        this.bitOffset = 0;

        // Fix 4494369: method ImageInputStreamImpl.readUTF()
        // does not work as specified (it should always assume
        // network byte order).
        ByteOrder oldByteOrder = getByteOrder();
        setByteOrder(ByteOrder.BIG_ENDIAN);

        String ret;
        try {
            ret = DataInputStream.readUTF(this);
        } catch (IOException e) {
            // Restore the old byte order even if an exception occurs
            setByteOrder(oldByteOrder);
            throw e;
        }

        setByteOrder(oldByteOrder);
        return ret;
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > b.length!");
        }

        while (len > 0) {
            int nbytes = read(b, off, len);
            if (nbytes == -1) {
                throw new EOFException();
            }
            off += nbytes;
            len -= nbytes;
        }
    }

    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(short[] s, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > s.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > s.length!");
        }

        while (len > 0) {
            int nelts = Math.min(len, byteBuf.length/2);
            readFully(byteBuf, 0, nelts*2);
            toShorts(byteBuf, s, off, nelts);
            off += nelts;
            len -= nelts;
        }
    }

    public void readFully(char[] c, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > c.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > c.length!");
        }

        while (len > 0) {
            int nelts = Math.min(len, byteBuf.length/2);
            readFully(byteBuf, 0, nelts*2);
            toChars(byteBuf, c, off, nelts);
            off += nelts;
            len -= nelts;
        }
    }

    public void readFully(int[] i, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > i.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > i.length!");
        }

        while (len > 0) {
            int nelts = Math.min(len, byteBuf.length/4);
            readFully(byteBuf, 0, nelts*4);
            toInts(byteBuf, i, off, nelts);
            off += nelts;
            len -= nelts;
        }
    }

    public void readFully(long[] l, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > l.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > l.length!");
        }

        while (len > 0) {
            int nelts = Math.min(len, byteBuf.length/8);
            readFully(byteBuf, 0, nelts*8);
            toLongs(byteBuf, l, off, nelts);
            off += nelts;
            len -= nelts;
        }
    }

    public void readFully(float[] f, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > f.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > f.length!");
        }

        while (len > 0) {
            int nelts = Math.min(len, byteBuf.length/4);
            readFully(byteBuf, 0, nelts*4);
            toFloats(byteBuf, f, off, nelts);
            off += nelts;
            len -= nelts;
        }
    }

    public void readFully(double[] d, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > d.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > d.length!");
        }

        while (len > 0) {
            int nelts = Math.min(len, byteBuf.length/8);
            readFully(byteBuf, 0, nelts*8);
            toDoubles(byteBuf, d, off, nelts);
            off += nelts;
            len -= nelts;
        }
    }

    private void toShorts(byte[] b, short[] s, int off, int len) {
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff];
                int b1 = b[boff + 1] & 0xff;
                s[off + j] = (short)((b0 << 8) | b1);
                boff += 2;
            }
        } else {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff + 1];
                int b1 = b[boff] & 0xff;
                s[off + j] = (short)((b0 << 8) | b1);
                boff += 2;
            }
        }
    }

    private void toChars(byte[] b, char[] c, int off, int len) {
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff];
                int b1 = b[boff + 1] & 0xff;
                c[off + j] = (char)((b0 << 8) | b1);
                boff += 2;
            }
        } else {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff + 1];
                int b1 = b[boff] & 0xff;
                c[off + j] = (char)((b0 << 8) | b1);
                boff += 2;
            }
        }
    }

    private void toInts(byte[] b, int[] i, int off, int len) {
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff];
                int b1 = b[boff + 1] & 0xff;
                int b2 = b[boff + 2] & 0xff;
                int b3 = b[boff + 3] & 0xff;
                i[off + j] = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                boff += 4;
            }
        } else {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff + 3];
                int b1 = b[boff + 2] & 0xff;
                int b2 = b[boff + 1] & 0xff;
                int b3 = b[boff] & 0xff;
                i[off + j] = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                boff += 4;
            }
        }
    }

    private void toLongs(byte[] b, long[] l, int off, int len) {
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff];
                int b1 = b[boff + 1] & 0xff;
                int b2 = b[boff + 2] & 0xff;
                int b3 = b[boff + 3] & 0xff;
                int b4 = b[boff + 4];
                int b5 = b[boff + 5] & 0xff;
                int b6 = b[boff + 6] & 0xff;
                int b7 = b[boff + 7] & 0xff;

                int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;

                l[off + j] = ((long)i0 << 32) | (i1 & 0xffffffffL);
                boff += 8;
            }
        } else {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff + 7];
                int b1 = b[boff + 6] & 0xff;
                int b2 = b[boff + 5] & 0xff;
                int b3 = b[boff + 4] & 0xff;
                int b4 = b[boff + 3];
                int b5 = b[boff + 2] & 0xff;
                int b6 = b[boff + 1] & 0xff;
                int b7 = b[boff]     & 0xff;

                int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;

                l[off + j] = ((long)i0 << 32) | (i1 & 0xffffffffL);
                boff += 8;
            }
        }
    }

    private void toFloats(byte[] b, float[] f, int off, int len) {
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff];
                int b1 = b[boff + 1] & 0xff;
                int b2 = b[boff + 2] & 0xff;
                int b3 = b[boff + 3] & 0xff;
                int i = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                f[off + j] = Float.intBitsToFloat(i);
                boff += 4;
            }
        } else {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff + 3];
                int b1 = b[boff + 2] & 0xff;
                int b2 = b[boff + 1] & 0xff;
                int b3 = b[boff + 0] & 0xff;
                int i = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                f[off + j] = Float.intBitsToFloat(i);
                boff += 4;
            }
        }
    }

    private void toDoubles(byte[] b, double[] d, int off, int len) {
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff];
                int b1 = b[boff + 1] & 0xff;
                int b2 = b[boff + 2] & 0xff;
                int b3 = b[boff + 3] & 0xff;
                int b4 = b[boff + 4];
                int b5 = b[boff + 5] & 0xff;
                int b6 = b[boff + 6] & 0xff;
                int b7 = b[boff + 7] & 0xff;

                int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
                long l = ((long)i0 << 32) | (i1 & 0xffffffffL);

                d[off + j] = Double.longBitsToDouble(l);
                boff += 8;
            }
        } else {
            for (int j = 0; j < len; j++) {
                int b0 = b[boff + 7];
                int b1 = b[boff + 6] & 0xff;
                int b2 = b[boff + 5] & 0xff;
                int b3 = b[boff + 4] & 0xff;
                int b4 = b[boff + 3];
                int b5 = b[boff + 2] & 0xff;
                int b6 = b[boff + 1] & 0xff;
                int b7 = b[boff] & 0xff;

                int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
                int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
                long l = ((long)i0 << 32) | (i1 & 0xffffffffL);

                d[off + j] = Double.longBitsToDouble(l);
                boff += 8;
            }
        }
    }

    public long getStreamPosition() throws IOException {
        checkClosed();
        return streamPos;
    }

    public int getBitOffset() throws IOException {
        checkClosed();
        return bitOffset;
    }

    public void setBitOffset(int bitOffset) throws IOException {
        checkClosed();
        if (bitOffset < 0 || bitOffset > 7) {
            throw new IllegalArgumentException("bitOffset must be betwwen 0 and 7!");
        }
        this.bitOffset = bitOffset;
    }

    public int readBit() throws IOException {
        checkClosed();

        // Compute final bit offset before we call read() and seek()
        int newBitOffset = (this.bitOffset + 1) & 0x7;

        int val = read();
        if (val == -1) {
            throw new EOFException();
        }

        if (newBitOffset != 0) {
            // Move byte position back if in the middle of a byte
            seek(getStreamPosition() - 1);
            // Shift the bit to be read to the rightmost position
            val >>= 8 - newBitOffset;
        }
        this.bitOffset = newBitOffset;

        return val & 0x1;
    }

    public long readBits(int numBits) throws IOException {
        checkClosed();

        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException();
        }
        if (numBits == 0) {
            return 0L;
        }

        // Have to read additional bits on the left equal to the bit offset
        int bitsToRead = numBits + bitOffset;

        // Compute final bit offset before we call read() and seek()
        int newBitOffset = (this.bitOffset + numBits) & 0x7;

        // Read a byte at a time, accumulate
        long accum = 0L;
        while (bitsToRead > 0) {
            int val = read();
            if (val == -1) {
                throw new EOFException();
            }

            accum <<= 8;
            accum |= val;
            bitsToRead -= 8;
        }

        // Move byte position back if in the middle of a byte
        if (newBitOffset != 0) {
            seek(getStreamPosition() - 1);
        }
        this.bitOffset = newBitOffset;

        // Shift away unwanted bits on the right.
        accum >>>= (-bitsToRead); // Negative of bitsToRead == extra bits read

        // Mask out unwanted bits on the left
        accum &= (-1L >>> (64 - numBits));

        return accum;
    }

    /**
     * Returns {@code -1L} to indicate that the stream has unknown
     * length.  Subclasses must override this method to provide actual
     * length information.
     *
     * @return -1L to indicate unknown length.
     */
    public long length() {
        return -1L;
    }

    /**
     * Advances the current stream position by calling
     * {@code seek(getStreamPosition() + n)}.
     *
     * <p> The bit offset is reset to zero.
     *
     * @param n the number of bytes to seek forward.
     *
     * @return an {@code int} representing the number of bytes
     * skipped.
     *
     * @exception IOException if {@code getStreamPosition}
     * throws an {@code IOException} when computing either
     * the starting or ending position.
     */
    public int skipBytes(int n) throws IOException {
        long pos = getStreamPosition();
        seek(pos + n);
        return (int)(getStreamPosition() - pos);
    }

    /**
     * Advances the current stream position by calling
     * {@code seek(getStreamPosition() + n)}.
     *
     * <p> The bit offset is reset to zero.
     *
     * @param n the number of bytes to seek forward.
     *
     * @return a {@code long} representing the number of bytes
     * skipped.
     *
     * @exception IOException if {@code getStreamPosition}
     * throws an {@code IOException} when computing either
     * the starting or ending position.
     */
    public long skipBytes(long n) throws IOException {
        long pos = getStreamPosition();
        seek(pos + n);
        return getStreamPosition() - pos;
    }

    public void seek(long pos) throws IOException {
        checkClosed();

        // This test also covers pos < 0
        if (pos < flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }

        this.streamPos = pos;
        this.bitOffset = 0;
    }

    /**
     * Pushes the current stream position onto a stack of marked
     * positions.
     */
    public void mark() {
        try {
            markByteStack.push(Long.valueOf(getStreamPosition()));
            markBitStack.push(Integer.valueOf(getBitOffset()));
        } catch (IOException e) {
        }
    }

    /**
     * Resets the current stream byte and bit positions from the stack
     * of marked positions.
     *
     * <p> An {@code IOException} will be thrown if the previous
     * marked position lies in the discarded portion of the stream.
     *
     * @exception IOException if an I/O error occurs.
     */
    public void reset() throws IOException {
        if (markByteStack.empty()) {
            return;
        }

        long pos = markByteStack.pop().longValue();
        if (pos < flushedPos) {
            throw new IIOException
                ("Previous marked position has been discarded!");
        }
        seek(pos);

        int offset = markBitStack.pop().intValue();
        setBitOffset(offset);
    }

    public void flushBefore(long pos) throws IOException {
        checkClosed();
        if (pos < flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        if (pos > getStreamPosition()) {
            throw new IndexOutOfBoundsException("pos > getStreamPosition()!");
        }
        // Invariant: flushedPos >= 0
        flushedPos = pos;
    }

    public void flush() throws IOException {
        flushBefore(getStreamPosition());
    }

    public long getFlushedPosition() {
        return flushedPos;
    }

    /**
     * Default implementation returns false.  Subclasses should
     * override this if they cache data.
     */
    public boolean isCached() {
        return false;
    }

    /**
     * Default implementation returns false.  Subclasses should
     * override this if they cache data in main memory.
     */
    public boolean isCachedMemory() {
        return false;
    }

    /**
     * Default implementation returns false.  Subclasses should
     * override this if they cache data in a temporary file.
     */
    public boolean isCachedFile() {
        return false;
    }

    public void close() throws IOException {
        checkClosed();

        isClosed = true;
    }

    /**
     * Finalizes this object prior to garbage collection.  The
     * {@code close} method is called to close any open input
     * source.  This method should not be called from application
     * code.
     *
     * @exception Throwable if an error occurs during superclass
     * finalization.
     *
     * @deprecated The {@code finalize} method has been deprecated.
     *     Subclasses that override {@code finalize} in order to perform cleanup
     *     should be modified to use alternative cleanup mechanisms and
     *     to remove the overriding {@code finalize} method.
     *     When overriding the {@code finalize} method, its implementation must explicitly
     *     ensure that {@code super.finalize()} is invoked as described in {@link Object#finalize}.
     *     See the specification for {@link Object#finalize()} for further
     *     information about migration options.
     */
    @Deprecated(since="9")
    protected void finalize() throws Throwable {
        if (!isClosed) {
            try {
                close();
            } catch (IOException e) {
            }
        }
        super.finalize();
    }
}

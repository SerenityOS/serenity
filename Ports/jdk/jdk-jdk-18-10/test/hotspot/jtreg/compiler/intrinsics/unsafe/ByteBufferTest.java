/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, 2016, Red Hat Inc. All rights reserved.
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

import jdk.test.lib.Utils;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Random;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

// A wrapper for a ByteBuffer which maintains a backing array and a
// position.  Whenever this wrapper is written the backing array and
// the wrapped byte buffer are updated together, and whenever it is
// read we check that the ByteBuffer and the backing array are identical.

class MyByteBuffer {
    final ByteBuffer buf;
    final byte[] bytes;
    int pos;
    ByteOrder byteOrder = BIG_ENDIAN;

    MyByteBuffer(ByteBuffer buf) {
        this.buf = buf;
        this.bytes = new byte[buf.capacity()];
        pos = 0;
    }

    public final MyByteBuffer order(ByteOrder bo) {
        byteOrder = bo;
        buf.order(bo);
        return this;
    }

    static MyByteBuffer allocate(int capacity) {
        return new MyByteBuffer(ByteBuffer.allocate(capacity));
    }

    static MyByteBuffer allocateDirect(int capacity) {
        return new MyByteBuffer(ByteBuffer.allocateDirect(capacity));
    }

    int capacity() { return bytes.length; }
    int position() {
        if (buf.position() != pos)
            throw new RuntimeException();
        return buf.position();
    }

    byte[] actualArray() {
        buf.rewind();
        byte[] actual = new byte[bytes.length];
        buf.get(actual, 0, actual.length);
        buf.rewind();

        return actual;
    }
    byte[] expectedArray() { return bytes; }

    private static byte long7(long x) { return (byte)(x >> 56); }
    private static byte long6(long x) { return (byte)(x >> 48); }
    private static byte long5(long x) { return (byte)(x >> 40); }
    private static byte long4(long x) { return (byte)(x >> 32); }
    private static byte long3(long x) { return (byte)(x >> 24); }
    private static byte long2(long x) { return (byte)(x >> 16); }
    private static byte long1(long x) { return (byte)(x >>  8); }
    private static byte long0(long x) { return (byte)(x      ); }

    private static byte int3(int x) { return (byte)(x >> 24); }
    private static byte int2(int x) { return (byte)(x >> 16); }
    private static byte int1(int x) { return (byte)(x >>  8); }
    private static byte int0(int x) { return (byte)(x      ); }

    private static byte short1(short x) { return (byte)(x >> 8); }
    private static byte short0(short x) { return (byte)(x     ); }

    byte _get(long i) { return bytes[(int)i]; }
    void _put(long i, byte x) { bytes[(int)i] = x; }

    private void putLongX(long a, long x) {
        if (byteOrder == BIG_ENDIAN) {
            x = Long.reverseBytes(x);
        }
        _put(a + 7, long7(x));
        _put(a + 6, long6(x));
        _put(a + 5, long5(x));
        _put(a + 4, long4(x));
        _put(a + 3, long3(x));
        _put(a + 2, long2(x));
        _put(a + 1, long1(x));
        _put(a    , long0(x));
    }

    private void putIntX(long a, int x) {
        if (byteOrder == BIG_ENDIAN) {
            x = Integer.reverseBytes(x);
        }
        _put(a + 3, int3(x));
        _put(a + 2, int2(x));
        _put(a + 1, int1(x));
        _put(a    , int0(x));
    }

    private void putShortX(int bi, short x) {
        if (byteOrder == BIG_ENDIAN) {
            x = Short.reverseBytes(x);
        }
        _put(bi    , short0(x));
        _put(bi + 1, short1(x));
    }

    static private int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3       ) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }
    int getIntX(long a) {
        int x = makeInt(_get(a + 3),
                _get(a + 2),
                _get(a + 1),
                _get(a));
        if (byteOrder == BIG_ENDIAN) {
            x = Integer.reverseBytes(x);
        }
        return x;
    }

    static private long makeLong(byte b7, byte b6, byte b5, byte b4,
                                 byte b3, byte b2, byte b1, byte b0)
    {
        return ((((long)b7       ) << 56) |
                (((long)b6 & 0xff) << 48) |
                (((long)b5 & 0xff) << 40) |
                (((long)b4 & 0xff) << 32) |
                (((long)b3 & 0xff) << 24) |
                (((long)b2 & 0xff) << 16) |
                (((long)b1 & 0xff) <<  8) |
                (((long)b0 & 0xff)      ));
    }

    long getLongX(long a) {
        long x = makeLong(_get(a + 7),
                _get(a + 6),
                _get(a + 5),
                _get(a + 4),
                _get(a + 3),
                _get(a + 2),
                _get(a + 1),
                _get(a));
        if (byteOrder == BIG_ENDIAN) {
            x = Long.reverseBytes(x);
        }
        return x;
    }

    static private short makeShort(byte b1, byte b0) {
        return (short)((b1 << 8) | (b0 & 0xff));
    }

    short getShortX(long a) {
        short x = makeShort(_get(a + 1),
                            _get(a    ));
        if (byteOrder == BIG_ENDIAN) {
            x = Short.reverseBytes(x);
        }
        return x;
    }

    double getDoubleX(long a) {
        long x = getLongX(a);
        return Double.longBitsToDouble(x);
    }

    double getFloatX(long a) {
        int x = getIntX(a);
        return Float.intBitsToFloat(x);
    }

    void ck(long x, long y) {
        if (x != y) {
            throw new RuntimeException(" x = " + Long.toHexString(x) + ", y = " + Long.toHexString(y));
        }
    }

    void ck(double x, double y) {
        if (x == x && y == y && x != y) {
            ck(x, y);
        }
    }

    // Method accessors

    long getLong(int i) { ck(buf.getLong(i), getLongX(i)); return buf.getLong(i); }
    int getInt(int i) { ck(buf.getInt(i), getIntX(i)); return buf.getInt(i); }
    short getShort(int i) { ck(buf.getShort(i), getShortX(i)); return buf.getShort(i); }
    char getChar(int i) { ck(buf.getChar(i), (char)getShortX(i)); return buf.getChar(i); }
    double getDouble(int i) { ck(buf.getDouble(i), getDoubleX(i)); return buf.getDouble(i); }
    float getFloat(int i) { ck(buf.getFloat(i), getFloatX(i)); return buf.getFloat(i); }

    void putLong(int i, long x) { buf.putLong(i, x); putLongX(i, x); }
    void putInt(int i, int x) { buf.putInt(i, x); putIntX(i, x); }
    void putShort(int i, short x) { buf.putShort(i, x); putShortX(i, x); }
    void putChar(int i, char x) { buf.putChar(i, x); putShortX(i, (short)x); }
    void putDouble(int i, double x) { buf.putDouble(i, x); putLongX(i, Double.doubleToRawLongBits(x)); }
    void putFloat(int i, float x) { buf.putFloat(i, x); putIntX(i, Float.floatToRawIntBits(x)); }

    long getLong() { ck(buf.getLong(buf.position()), getLongX(pos)); long x = buf.getLong(); pos += 8; return x; }
    int getInt() { ck(buf.getInt(buf.position()), getIntX(pos)); int x = buf.getInt(); pos += 4; return x; }
    short getShort() { ck(buf.getShort(buf.position()), getShortX(pos)); short x = buf.getShort(); pos += 2; return x; }
    char getChar() {  ck(buf.getChar(buf.position()), (char)getShortX(pos)); char x = buf.getChar(); pos += 2; return x; }
    double getDouble() { ck(buf.getDouble(buf.position()), getDoubleX(pos)); double x = buf.getDouble(); pos += 8; return x; }
    float getFloat() { ck(buf.getFloat(buf.position()), getFloatX(pos)); float x = buf.getFloat(); pos += 4; return x; }

    void putLong(long x) { putLongX(pos, x); pos += 8; buf.putLong(x); }
    void putInt(int x) { putIntX(pos, x); pos += 4; buf.putInt(x); }
    void putShort(short x) { putShortX(pos, x); pos += 2; buf.putShort(x); }
    void putChar(char x) { putShortX(pos, (short)x); pos += 2; buf.putChar(x); }
    void putDouble(double x) { putLongX(pos, Double.doubleToRawLongBits(x)); pos += 8; buf.putDouble(x); }
    void putFloat(float x) { putIntX(pos, Float.floatToRawIntBits(x)); pos += 4; buf.putFloat(x); }

    // View accessors

    long getLong(LongBuffer vb, int i) { ck(vb.get(i / 8), getLongX(i)); return vb.get(i / 8); }
    int getInt(IntBuffer vb, int i) { ck(vb.get(i / 4), getIntX(i)); return vb.get(i / 4); }
    short getShort(ShortBuffer vb, int i) { ck(vb.get(i / 2), getShortX(i)); return vb.get(i / 2); }
    char getChar(CharBuffer vb, int i) { ck(vb.get(i / 2), (char)getShortX(i)); return vb.get(i / 2); }
    double getDouble(DoubleBuffer vb, int i) { ck(vb.get(i / 8), getDoubleX(i)); return vb.get(i / 8); }
    float getFloat(FloatBuffer vb, int i) { ck(vb.get(i / 4), getFloatX(i)); return vb.get(i / 4); }

    void putLong(LongBuffer vb, int i, long x) { vb.put(i / 8, x); putLongX(i, x); }
    void putInt(IntBuffer vb, int i, int x) { vb.put(i / 4, x); putIntX(i, x); }
    void putShort(ShortBuffer vb, int i, short x) { vb.put(i / 2, x); putShortX(i, x); }
    void putChar(CharBuffer vb, int i, char x) { vb.put(i / 2, x); putShortX(i, (short)x); }
    void putDouble(DoubleBuffer vb, int i, double x) { vb.put(i / 8, x); putLongX(i, Double.doubleToRawLongBits(x)); }
    void putFloat(FloatBuffer vb, int i, float x) { vb.put(i / 4, x); putIntX(i, Float.floatToRawIntBits(x)); }

    long getLong(LongBuffer v) { ck(v.get(v.position()), getLongX(pos)); long x = v.get(); pos += 8; return x; }
    int getInt(IntBuffer v) { ck(v.get(v.position()), getIntX(pos)); int x = v.get(); pos += 4; return x; }
    short getShort(ShortBuffer v) { ck(v.get(v.position()), getShortX(pos)); short x = v.get(); pos += 2; return x; }
    char getChar(CharBuffer v) { ck(v.get(v.position()), (char)getShortX(pos)); char x = v.get(); pos += 2; return x; }
    double getDouble(DoubleBuffer v) { ck(v.get(v.position()), getDoubleX(pos)); double x = v.get(); pos += 8; return x; }
    float getFloat(FloatBuffer v) { ck(v.get(v.position()), getFloatX(pos)); float x = v.get(); pos += 4; return x; }

    void putLong(LongBuffer v, long x) { putLongX(pos, x); pos += 8; v.put(x); }
    void putInt(IntBuffer v, int x) { putIntX(pos, x); pos += 4; v.put(x); }
    void putShort(ShortBuffer v, short x) { putShortX(pos, x); pos += 2; v.put(x); }
    void putChar(CharBuffer v, char x) { putShortX(pos, (short)x); pos += 2; v.put(x); }
    void putDouble(DoubleBuffer v, double x) { putLongX(pos, Double.doubleToRawLongBits(x)); pos += 8; v.put(x); }
    void putFloat(FloatBuffer v, float x) { putIntX(pos, Float.floatToRawIntBits(x)); pos += 4; v.put(x); }

    void rewind() { pos = 0; buf.rewind(); }
}

public abstract class ByteBufferTest implements Runnable {

    Random random = Utils.getRandomInstance();
    MyByteBuffer data;

    static int randomOffset(Random r, MyByteBuffer buf, int size) {
        return r.nextInt(buf.capacity() - size);
    }

    static int randomAlignedOffset(Random r, MyByteBuffer buf, int unitSize) {
        return r.nextInt(buf.capacity() / unitSize) * unitSize;
    }

    long iterations;

    ByteBufferTest(long iterations, boolean direct) {
        this.iterations = iterations;
        data = direct
               ? MyByteBuffer.allocateDirect(1024)
               : MyByteBuffer.allocate(1024);
    }

    // The core of the test.  Walk over the buffer reading and writing
    // random data, XORing it as we go.  We can detect writes in the
    // wrong place, writes which are too long or too short, and reads
    // or writes of the wrong data,
    void step(Random r) {
        stepUsingAccessors(r);
        stepUsingViews(r);
    }

    void stepUsingAccessors(Random r) {
        data.order((r.nextInt() & 1) != 0 ? BIG_ENDIAN : LITTLE_ENDIAN);

        data.rewind();
        while (data.position() < data.capacity())
            data.putLong(data.getLong() ^ random.nextLong());

        data.rewind();
        while (data.position() < data.capacity())
            data.putInt(data.getInt() ^ random.nextInt());

        data.rewind();
        while (data.position() < data.capacity())
            data.putShort((short)(data.getShort() ^ random.nextInt()));

        data.rewind();
        while (data.position() < data.capacity())
            data.putChar((char)(data.getChar() ^ random.nextInt()));

        data.rewind();
        while (data.position() < data.capacity())
            data.putDouble(combine(data.getDouble(), random.nextLong()));

        data.rewind();
        while (data.position() < data.capacity())
            data.putFloat(combine(data.getFloat(), random.nextInt()));

        for (int i = 0; i < 100; i++) {
            int offset = randomOffset(r, data, Long.BYTES);
            data.putLong(offset, data.getLong(offset) ^ random.nextLong());
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomOffset(r, data, Integer.BYTES);
            data.putInt(offset, data.getInt(offset) ^ random.nextInt());
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomOffset(r, data, Short.BYTES);
            data.putShort(offset, (short)(data.getShort(offset) ^ random.nextInt()));
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomOffset(r, data, Character.BYTES);
            data.putChar(offset, (char)(data.getChar(offset) ^ random.nextInt()));
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomOffset(r, data, Double.BYTES);
            data.putDouble(offset, combine(data.getDouble(offset), random.nextLong()));
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomOffset(r, data, Float.BYTES);
            data.putFloat(offset, combine(data.getFloat(offset), random.nextInt()));
        }
    }

    void stepUsingViews(Random r) {
        data.order((r.nextInt() & 1) != 0 ? BIG_ENDIAN : LITTLE_ENDIAN);

        data.rewind();
        LongBuffer lbuf = data.buf.asLongBuffer();
        while (lbuf.position() < data.capacity() / Long.BYTES)
            data.putLong(lbuf, data.getLong(lbuf) ^ random.nextLong());

        data.rewind();
        IntBuffer ibuf = data.buf.asIntBuffer();
        while (ibuf.position() < data.capacity() / Integer.BYTES)
            data.putInt(ibuf, data.getInt(ibuf) ^ random.nextInt());

        data.rewind();
        ShortBuffer sbuf = data.buf.asShortBuffer();
        while (sbuf.position() < data.capacity() / Short.BYTES)
            data.putShort(sbuf, (short)(data.getShort(sbuf) ^ random.nextInt()));

        data.rewind();
        CharBuffer cbuf = data.buf.asCharBuffer();
        while (cbuf.position() < data.capacity() / Character.BYTES)
            data.putChar(cbuf, (char)(data.getChar(cbuf) ^ random.nextInt()));

        data.rewind();
        DoubleBuffer dbuf = data.buf.asDoubleBuffer();
        while (dbuf.position() < data.capacity() / Double.BYTES)
            data.putDouble(dbuf, combine(data.getDouble(dbuf), random.nextLong()));

        data.rewind();
        FloatBuffer fbuf = data.buf.asFloatBuffer();
        while (fbuf.position() < data.capacity() / Float.BYTES)
            data.putFloat(fbuf, combine(data.getFloat(fbuf), random.nextInt()));

        for (int i = 0; i < 100; i++) {
            int offset = randomAlignedOffset(r, data, Long.BYTES);
            data.putLong(lbuf, offset, data.getLong(lbuf, offset) ^ random.nextLong());
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomAlignedOffset(r, data, Integer.BYTES);
            data.putInt(ibuf, offset, data.getInt(ibuf, offset) ^ random.nextInt());
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomAlignedOffset(r, data, Short.BYTES);
            data.putShort(sbuf, offset, (short)(data.getShort(sbuf, offset) ^ random.nextInt()));
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomAlignedOffset(r, data, Character.BYTES);
            data.putChar(cbuf, offset, (char)(data.getChar(cbuf, offset) ^ random.nextInt()));
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomAlignedOffset(r, data, Double.BYTES);
            data.putDouble(dbuf, offset, combine(data.getDouble(dbuf, offset), random.nextLong()));
        }
        for (int i = 0; i < 100; i++) {
            int offset = randomAlignedOffset(r, data, Float.BYTES);
            data.putFloat(fbuf, offset, combine(data.getFloat(fbuf, offset), random.nextInt()));
        }
    }

    // XOR the bit pattern of a double and a long, returning the
    // result as a double.
    //
    // We convert signalling NaNs to quiet NaNs.  We need to do this
    // because some platforms (in particular legacy 80x87) do not
    // provide transparent conversions between integer and
    // floating-point types even when using raw conversions but
    // quietly convert sNaN to qNaN.  This causes spurious test
    // failures when the template interpreter uses 80x87 and the JITs
    // use XMM registers.
    //
    public double combine(double prev, long bits) {
        bits ^= Double.doubleToRawLongBits(prev);
        double result = Double.longBitsToDouble(bits);
        if (Double.isNaN(result)) {
            result = Double.longBitsToDouble(bits | 0x8000000000000l);
        }
        return result;
    }

    // XOR the bit pattern of a float and an int, returning the result
    // as a float.  Convert sNaNs to qNaNs.
    public Float combine(float prev, int bits) {
        bits ^= Float.floatToRawIntBits(prev);
        Float result = Float.intBitsToFloat(bits);
        if (Float.isNaN(result)) {
            result = Float.intBitsToFloat(bits | 0x400000);
        }
        return result;
    }

    enum PrimitiveType {
        BYTE(1), CHAR(2), SHORT(2), INT(4), LONG(8), FLOAT(4), DOUBLE(8);

        public final int size;
        PrimitiveType(int size) {
            this.size = size;
        }
    }

    Buffer asView(ByteBuffer b, PrimitiveType t) {
        switch (t) {
            case BYTE: return b;
            case CHAR: return b.asCharBuffer();
            case SHORT: return b.asShortBuffer();
            case INT: return b.asIntBuffer();
            case LONG: return b.asLongBuffer();
            case FLOAT: return b.asFloatBuffer();
            case DOUBLE: return b.asDoubleBuffer();
        }
        throw new InternalError("Should not reach here");
    }

    void getOne(ByteBuffer b, PrimitiveType t) {
        switch (t) {
        case BYTE: b.get(); break;
        case CHAR: b.getChar(); break;
        case SHORT: b.getShort(); break;
        case INT: b.getInt(); break;
        case LONG: b.getLong(); break;
        case FLOAT: b.getFloat(); break;
        case DOUBLE: b.getDouble(); break;
        }
    }

    void putOne(ByteBuffer b, PrimitiveType t) {
        switch (t) {
        case BYTE: b.put((byte)0); break;
        case CHAR: b.putChar('0'); break;
        case SHORT: b.putShort((short)0); break;
        case INT: b.putInt(0); break;
        case LONG: b.putLong(0); break;
        case FLOAT: b.putFloat(0); break;
        case DOUBLE: b.putDouble(0); break;
        }
    }

    void asViewGetOne(ByteBuffer b, PrimitiveType t) {
        switch (t) {
            case BYTE: b.get(); break;
            case CHAR: b.asCharBuffer().get(); break;
            case SHORT: b.asShortBuffer().get(); break;
            case INT: b.asIntBuffer().get(); break;
            case LONG: b.asLongBuffer().get(); break;
            case FLOAT: b.asFloatBuffer().get(); break;
            case DOUBLE: b.asDoubleBuffer().get(); break;
        }
    }

    void asViewPutOne(ByteBuffer b, PrimitiveType t) {
        switch (t) {
            case BYTE: b.put((byte)0); break;
            case CHAR: b.asCharBuffer().put('0'); break;
            case SHORT: b.asShortBuffer().put((short)0); break;
            case INT: b.asIntBuffer().put(0); break;
            case LONG: b.asLongBuffer().put(0); break;
            case FLOAT: b.asFloatBuffer().put(0); break;
            case DOUBLE: b.asDoubleBuffer().put(0); break;
        }
    }

    void getOne(ByteBuffer b, PrimitiveType t, int index) {
        switch (t) {
        case BYTE: b.get(index); break;
        case CHAR: b.getChar(index); break;
        case SHORT: b.getShort(index); break;
        case INT: b.getInt(index); break;
        case LONG: b.getLong(index); break;
        case FLOAT: b.getFloat(index); break;
        case DOUBLE: b.getDouble(index); break;
        }
    }

    void putOne(ByteBuffer b, PrimitiveType t, int index) {
        switch (t) {
        case BYTE: b.put(index, (byte)0); break;
        case CHAR: b.putChar(index, '0'); break;
        case SHORT: b.putShort(index, (short)0); break;
        case INT: b.putInt(index, 0); break;
        case LONG: b.putLong(index, 0); break;
        case FLOAT: b.putFloat(index, 0); break;
        case DOUBLE: b.putDouble(index, 0); break;
        }
    }

    void asViewGetOne(Buffer v, PrimitiveType t, int index) {
        switch (t) {
            case BYTE: ((ByteBuffer) v).get(index); break;
            case CHAR: ((CharBuffer) v).get(index); break;
            case SHORT: ((ShortBuffer) v).get(index); break;
            case INT: ((IntBuffer) v).get(index); break;
            case LONG: ((LongBuffer) v).get(index); break;
            case FLOAT: ((FloatBuffer) v).get(index); break;
            case DOUBLE: ((DoubleBuffer) v).get(index); break;
        }
    }

    void asViewPutOne(Buffer v, PrimitiveType t, int index) {
        switch (t) {
            case BYTE: ((ByteBuffer) v).put(index, (byte)0); break;
            case CHAR: ((CharBuffer) v).put(index, '0'); break;
            case SHORT: ((ShortBuffer) v).put(index, (short)0); break;
            case INT: ((IntBuffer) v).put(index, 0); break;
            case LONG: ((LongBuffer) v).put(index, 0); break;
            case FLOAT: ((FloatBuffer) v).put(index, 0); break;
            case DOUBLE: ((DoubleBuffer) v).put(index, 0); break;
        }
    }

    void checkBoundaryConditions() {
        for (int i = 0; i < 100; i++) {
            int bufSize = random.nextInt(16);
            ByteBuffer buf = data.buf.isDirect()
                    ? ByteBuffer.allocateDirect(bufSize)
                    : ByteBuffer.allocate(bufSize);
            for (PrimitiveType t : PrimitiveType.values()) {
                buf.rewind();
                Buffer viewBuf = asView(buf, t);
                for (int j = 0; j < 100; j++) {
                    int offset = random.nextInt(32) - 8;
                    int threw = 0;
                    int checks = 6;
                    try {
                        try {
                            buf.position(offset);
                            getOne(buf, t);
                        }
                        catch (BufferUnderflowException e) {
                            if (offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }
                        catch (IllegalArgumentException e) {
                            if (offset >= 0 && offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }

                        try {
                            buf.position(offset);
                            asViewGetOne(buf, t);
                        }
                        catch (BufferUnderflowException e) {
                            if (offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }
                        catch (IllegalArgumentException e) {
                            if (offset >= 0 && offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }

                        try {
                            buf.position(offset);
                            putOne(buf, t);
                        }
                        catch (BufferOverflowException e) {
                            if (offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }
                        catch (IllegalArgumentException e) {
                            if (offset >= 0 && offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }

                        try {
                            buf.position(offset);
                            asViewPutOne(buf, t);
                        }
                        catch (BufferOverflowException e) {
                            if (offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }
                        catch (IllegalArgumentException e) {
                            if (offset >= 0 && offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }

                        try {
                            putOne(buf, t, offset);
                        }
                        catch (IndexOutOfBoundsException e) {
                            if (offset >= 0 && offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }

                        try {
                            getOne(buf, t, offset);
                        }
                        catch (IndexOutOfBoundsException e) {
                            if (offset >= 0 && offset + t.size < bufSize)
                                throw new RuntimeException
                                        ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                            threw++;
                        }

                        // If offset is aligned access using the view
                        if (offset % t.size == 0) {
                            checks = 8;
                            int viewOffset = offset / t.size;


                            try {
                                asViewPutOne(viewBuf, t, viewOffset);
                            }
                            catch (IndexOutOfBoundsException e) {
                                if (offset >= 0 && offset + t.size < bufSize)
                                    throw new RuntimeException
                                            ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                                threw++;
                            }

                            try {
                                asViewGetOne(viewBuf, t, viewOffset);
                            }
                            catch (IndexOutOfBoundsException e) {
                                if (offset >= 0 && offset + t.size < bufSize)
                                    throw new RuntimeException
                                            ("type = " + t + ", offset = " + offset + ", bufSize = " + bufSize, e);
                                threw++;
                            }
                        }

                        if (threw == 0) {
                            // Make sure that we should not have thrown.
                            if (offset < 0 || offset + t.size > bufSize) {
                                throw new RuntimeException
                                        ("should have thrown but did not, type = " + t
                                         + ", offset = " + offset + ", bufSize = " + bufSize);
                            }
                        }
                        else if (threw != checks) {
                            // If one of the {get,put} operations threw
                            // due to an invalid offset then all four of
                            // them should have thrown.
                            throw new RuntimeException
                                    ("should have thrown but at least one did not, type = " + t
                                     + ", offset = " + offset + ", bufSize = " + bufSize);
                        }
                    }
                    catch (Throwable th) {
                        throw new RuntimeException
                                ("unexpected throw: type  = " + t + ", offset = " + offset + ", bufSize = " + bufSize, th);

                    }
                }
            }
        }
    }

    public void run() {
        checkBoundaryConditions();

        for (int i = 0; i < data.capacity(); i += 8) {
            data.putLong(i, random.nextLong());
        }

        for (int i = 0; i < iterations; i++) {
            step(random);
        }

        if (!Arrays.equals(data.actualArray(), data.expectedArray())) {
            throw new RuntimeException();
        }
    }
}

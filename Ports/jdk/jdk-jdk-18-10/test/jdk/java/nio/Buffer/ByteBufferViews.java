/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary Binary data and view tests for byte buffers
 * @bug 8159257 8258955
 * @run testng ByteBufferViews
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class ByteBufferViews {
    static final int SIZE = 32;

    // List of buffer allocator functions
    static final List<Map.Entry<String, IntFunction<ByteBuffer>>> BYTE_BUFFER_ALLOCATE_FUNCTIONS = List.of(
            // Heap
            Map.entry("ByteBuffer.allocate(ba)",
                      size -> ByteBuffer.allocate(size)),
            // Aligned
            Map.entry("ByteBuffer.allocate(size).position(8)",
                      size -> ByteBuffer.allocate(size).position(8)),
            Map.entry("ByteBuffer.allocate(size).position(8).slice()",
                      size -> ByteBuffer.allocate(size).position(8).slice()),
            Map.entry("ByteBuffer.allocate(size).position(8).slice().duplicate()",
                      size -> ByteBuffer.allocate(size).position(8).slice().duplicate()),
            Map.entry("ByteBuffer.allocate(size).slice(8,size-8)",
                      size -> ByteBuffer.allocate(size).slice(8,size-8)),
            // Unaligned
            Map.entry("ByteBuffer.allocate(size).position(1)",
                      size -> ByteBuffer.allocate(size).position(1)),
            Map.entry("ByteBuffer.allocate(size).position(1).slice()",
                      size -> ByteBuffer.allocate(size).position(1).slice()),
            Map.entry("ByteBuffer.allocate(size).position(1).slice().duplicate()",
                      size -> ByteBuffer.allocate(size).position(1).slice().duplicate()),
            Map.entry("ByteBuffer.allocate(size).slice(1,size-1)",
                      size -> ByteBuffer.allocate(size).slice(1,size-1)),

            // Off-heap
            Map.entry("ByteBuffer.allocateDirect(size)",
                      size -> ByteBuffer.allocateDirect(size)),
            // Aligned
            Map.entry("ByteBuffer.allocateDirect(size).position(8)",
                      size -> ByteBuffer.allocateDirect(size).position(8)),
            Map.entry("ByteBuffer.allocateDirect(size).position(8).slice()",
                      size -> ByteBuffer.allocateDirect(size).position(8).slice()),
            Map.entry("ByteBuffer.allocateDirect(size).position(8).slice().duplicate()",
                      size -> ByteBuffer.allocateDirect(size).position(8).slice().duplicate()),
            Map.entry("ByteBuffer.allocateDirect(size).slice(8,size-8)",
                      size -> ByteBuffer.allocateDirect(size).slice(8,size-8)),
            // Unaligned
            Map.entry("ByteBuffer.allocateDirect(size).position(1)",
                      size -> ByteBuffer.allocateDirect(size).position(1)),
            Map.entry("ByteBuffer.allocateDirect(size).position(1).slice()",
                      size -> ByteBuffer.allocateDirect(size).position(1).slice()),
            Map.entry("ByteBuffer.allocateDirect(size).position(1).slice().duplicate()",
                      size -> ByteBuffer.allocateDirect(size).position(1).slice().duplicate()),
            Map.entry("ByteBuffer.allocateDirect(size).slice(1,size-1)",
                      size -> ByteBuffer.allocateDirect(size).slice(1,size-1))
    );

    // List of buffer byte order functions
    static final List<Map.Entry<String, UnaryOperator<ByteBuffer>>> BYTE_BUFFER_ORDER_FUNCTIONS = List.of(
            Map.entry("order(ByteOrder.BIG_ENDIAN)",
                      (ByteBuffer bb) -> bb.order(ByteOrder.BIG_ENDIAN)),
            Map.entry("order(ByteOrder.LITTLE_ENDIAN)",
                      (ByteBuffer bb) -> bb.order(ByteOrder.LITTLE_ENDIAN))
    );

    // Produce a composition of allocation and byte order buffer functions
    static List<Map.Entry<String, IntFunction<ByteBuffer>>> composeBufferFunctions(
            List<Map.Entry<String, IntFunction<ByteBuffer>>> af,
            List<Map.Entry<String, UnaryOperator<ByteBuffer>>> of) {
        return af.stream().flatMap(afe -> of.stream().
                map(ofe -> {
                    String s = afe.getKey() + "." + ofe.getKey();
                    IntFunction<ByteBuffer> f = size -> ofe.getValue().
                            apply(afe.getValue().apply(size));
                    return Map.entry(s, f);
                })
        ).collect(Collectors.toList());
    }

    // List of buffer allocator functions to test
    static final List<Map.Entry<String, IntFunction<ByteBuffer>>> BYTE_BUFFER_FUNCTIONS =
            composeBufferFunctions(BYTE_BUFFER_ALLOCATE_FUNCTIONS, BYTE_BUFFER_ORDER_FUNCTIONS);

    // Creates a cross product of test arguments for
    // buffer allocator functions and buffer view functions
    static Object[][] product(List<? extends Map.Entry<String, ?>> la,
                              List<? extends Map.Entry<String, ?>> lb) {
        return la.stream().flatMap(lae -> lb.stream().
                map(lbe -> List.of(
                        lae.getKey() + " -> " + lbe.getKey(),
                        lae.getValue(),
                        lbe.getValue()).toArray()
                )).toArray(Object[][]::new);
    }

    static void assertValues(int i, Object bValue, Object bbValue, ByteBuffer bb) {
        if (!bValue.equals(bbValue)) {
            fail(String.format("Values %s and %s differ at index %d for %s",
                               bValue, bbValue, i, bb));
        }
    }

    static void assertValues(int i, Object bbValue, Object bvValue, ByteBuffer bb, Buffer bv) {
        if (!bbValue.equals(bvValue)) {
            fail(String.format("Values %s and %s differ at index %d for %s and %s",
                               bbValue, bvValue, i, bb, bv));
        }
    }

    static ByteBuffer allocate(IntFunction<ByteBuffer> f) {
        return allocate(f, i -> i);
    }

    static ByteBuffer allocate(IntFunction<ByteBuffer> f, IntUnaryOperator o) {
        return fill(f.apply(SIZE), o);
    }

    static ByteBuffer fill(ByteBuffer bb, IntUnaryOperator o) {
        for (int i = 0; i < bb.limit(); i++) {
            bb.put(i, (byte) o.applyAsInt(i));
        }
        return bb;
    }


    @DataProvider
    public static Object[][] shortViewProvider() {
        List<Map.Entry<String, Function<ByteBuffer, ShortBuffer>>> bfs = List.of(
                Map.entry("bb.asShortBuffer()",
                          bb -> bb.asShortBuffer()),
                Map.entry("bb.asShortBuffer().slice()",
                          bb -> bb.asShortBuffer().slice()),
                Map.entry("bb.asShortBuffer().slice(index,length)",
                          bb -> { var sb = bb.asShortBuffer();
                                  sb =  sb.slice(1, sb.limit() - 1);
                                  bb.position(bb.position() + 2);
                                  return sb; }),
                Map.entry("bb.asShortBuffer().slice().duplicate()",
                          bb -> bb.asShortBuffer().slice().duplicate())
        );

        return product(BYTE_BUFFER_FUNCTIONS, bfs);
    }

    @Test(dataProvider = "shortViewProvider")
    public void testShortGet(String desc, IntFunction<ByteBuffer> fbb,
                             Function<ByteBuffer, ShortBuffer> fbi) {
        ByteBuffer bb = allocate(fbb);
        ShortBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            short fromBytes = getShortFromBytes(bb, o + i * 2);
            short fromMethodView = bb.getShort(o + i * 2);
            assertValues(i, fromBytes, fromMethodView, bb);

            short fromBufferView = vb.get(i);
            assertValues(i, fromMethodView, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            short v = getShortFromBytes(bb, o + i * 2);
            short a = bb.getShort();
            assertValues(i, v, a, bb);

            short b = vb.get();
            assertValues(i, a, b, bb, vb);
        }

    }

    @Test(dataProvider = "shortViewProvider")
    public void testShortPut(String desc, IntFunction<ByteBuffer> fbb,
                             Function<ByteBuffer, ShortBuffer> fbi) {
        ByteBuffer bbfilled = allocate(fbb);
        ByteBuffer bb = allocate(fbb, i -> 0);
        ShortBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            short fromFilled = bbfilled.getShort(o + i * 2);

            vb.put(i, fromFilled);
            short fromMethodView = bb.getShort(o + i * 2);
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            short fromFilled = bbfilled.getShort(o + i * 2);

            vb.put(fromFilled);
            short fromMethodView = bb.getShort();
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }


        fill(bb, i -> 0);
        bb.clear().position(o);
        vb.clear();

        for (int i = 0; i < vb.limit(); i++) {
            short fromFilled = bbfilled.getShort(o + i * 2);

            bb.putShort(o + i * 2, fromFilled);
            short fromBufferView = vb.get(i);
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            short fromFilled = bbfilled.getShort(o + i * 2);

            bb.putShort(fromFilled);
            short fromBufferView = vb.get();
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }
    }

    static short getShortFromBytes(ByteBuffer bb, int i) {
        int a = bb.get(i) & 0xFF;
        int b = bb.get(i + 1) & 0xFF;

        if (bb.order() == ByteOrder.BIG_ENDIAN) {
            return (short) ((a << 8) | b);
        }
        else {
            return (short) ((b << 8) | a);
        }
    }

    @DataProvider
    public static Object[][] charViewProvider() {
        List<Map.Entry<String, Function<ByteBuffer, CharBuffer>>> bfs = List.of(
                Map.entry("bb.asCharBuffer()",
                          bb -> bb.asCharBuffer()),
                Map.entry("bb.asCharBuffer().slice()",
                          bb -> bb.asCharBuffer().slice()),
                Map.entry("bb.asCharBuffer().slice(index,length)",
                          bb -> { var cb = bb.asCharBuffer();
                                  cb =  cb.slice(1, cb.limit() - 1);
                                  bb.position(bb.position() + 2);
                                  return cb; }),
                Map.entry("bb.asCharBuffer().slice().duplicate()",
                          bb -> bb.asCharBuffer().slice().duplicate())
        );

        return product(BYTE_BUFFER_FUNCTIONS, bfs);
    }

    @Test(dataProvider = "charViewProvider")
    public void testCharGet(String desc, IntFunction<ByteBuffer> fbb,
                            Function<ByteBuffer, CharBuffer> fbi) {
        ByteBuffer bb = allocate(fbb);
        CharBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            char fromBytes = getCharFromBytes(bb, o + i * 2);
            char fromMethodView = bb.getChar(o + i * 2);
            assertValues(i, fromBytes, fromMethodView, bb);

            char fromBufferView = vb.get(i);
            assertValues(i, fromMethodView, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            char fromBytes = getCharFromBytes(bb, o + i * 2);
            char fromMethodView = bb.getChar();
            assertValues(i, fromBytes, fromMethodView, bb);

            char fromBufferView = vb.get();
            assertValues(i, fromMethodView, fromBufferView, bb, vb);
        }

    }

    @Test(dataProvider = "charViewProvider")
    public void testCharPut(String desc, IntFunction<ByteBuffer> fbb,
                            Function<ByteBuffer, CharBuffer> fbi) {
        ByteBuffer bbfilled = allocate(fbb);
        ByteBuffer bb = allocate(fbb, i -> 0);
        CharBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            char fromFilled = bbfilled.getChar(o + i * 2);

            vb.put(i, fromFilled);
            char fromMethodView = bb.getChar(o + i * 2);
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            char fromFilled = bbfilled.getChar(o + i * 2);

            vb.put(fromFilled);
            char fromMethodView = bb.getChar();
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }


        fill(bb, i -> 0);
        bb.clear().position(o);
        vb.clear();

        for (int i = 0; i < vb.limit(); i++) {
            char fromFilled = bbfilled.getChar(o + i * 2);

            bb.putChar(o + i * 2, fromFilled);
            char fromBufferView = vb.get(i);
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            char fromFilled = bbfilled.getChar(o + i * 2);

            bb.putChar(fromFilled);
            char fromBufferView = vb.get();
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }
    }

    static char getCharFromBytes(ByteBuffer bb, int i) {
        return (char) getShortFromBytes(bb, i);
    }


    @DataProvider
    public static Object[][] intViewProvider() {
        List<Map.Entry<String, Function<ByteBuffer, IntBuffer>>> bfs = List.of(
                Map.entry("bb.asIntBuffer()",
                          bb -> bb.asIntBuffer()),
                Map.entry("bb.asIntBuffer().slice()",
                          bb -> bb.asIntBuffer().slice()),
                Map.entry("bb.asIntBuffer().slice(index,length)",
                          bb -> { var ib = bb.asIntBuffer();
                                  ib =  ib.slice(1, ib.limit() - 1);
                                  bb.position(bb.position() + 4);
                                  return ib; }),
                Map.entry("bb.asIntBuffer().slice().duplicate()",
                          bb -> bb.asIntBuffer().slice().duplicate())
        );

        return product(BYTE_BUFFER_FUNCTIONS, bfs);
    }

    @Test(dataProvider = "intViewProvider")
    public void testIntGet(String desc, IntFunction<ByteBuffer> fbb,
                           Function<ByteBuffer, IntBuffer> fbi) {
        ByteBuffer bb = allocate(fbb);
        IntBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            int fromBytes = getIntFromBytes(bb, o + i * 4);
            int fromMethodView = bb.getInt(o + i * 4);
            assertValues(i, fromBytes, fromMethodView, bb);

            int fromBufferView = vb.get(i);
            assertValues(i, fromMethodView, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            int v = getIntFromBytes(bb, o + i * 4);
            int a = bb.getInt();
            assertValues(i, v, a, bb);

            int b = vb.get();
            assertValues(i, a, b, bb, vb);
        }

    }

    @Test(dataProvider = "intViewProvider")
    public void testIntPut(String desc, IntFunction<ByteBuffer> fbb,
                           Function<ByteBuffer, IntBuffer> fbi) {
        ByteBuffer bbfilled = allocate(fbb);
        ByteBuffer bb = allocate(fbb, i -> 0);
        IntBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            int fromFilled = bbfilled.getInt(o + i * 4);

            vb.put(i, fromFilled);
            int fromMethodView = bb.getInt(o + i * 4);
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            int fromFilled = bbfilled.getInt(o + i * 4);

            vb.put(fromFilled);
            int fromMethodView = bb.getInt();
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }


        fill(bb, i -> 0);
        bb.clear().position(o);
        vb.clear();

        for (int i = 0; i < vb.limit(); i++) {
            int fromFilled = bbfilled.getInt(o + i * 4);

            bb.putInt(o + i * 4, fromFilled);
            int fromBufferView = vb.get(i);
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            int fromFilled = bbfilled.getInt(o + i * 4);

            bb.putInt(fromFilled);
            int fromBufferView = vb.get();
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }
    }

    static int getIntFromBytes(ByteBuffer bb, int i) {
        int a = bb.get(i) & 0xFF;
        int b = bb.get(i + 1) & 0xFF;
        int c = bb.get(i + 2) & 0xFF;
        int d = bb.get(i + 3) & 0xFF;

        if (bb.order() == ByteOrder.BIG_ENDIAN) {
            return ((a << 24) | (b << 16) | (c << 8) | d);
        }
        else {
            return ((d << 24) | (c << 16) | (b << 8) | a);
        }
    }


    @DataProvider
    public static Object[][] longViewProvider() {
        List<Map.Entry<String, Function<ByteBuffer, LongBuffer>>> bfs = List.of(
                Map.entry("bb.asLongBuffer()",
                          bb -> bb.asLongBuffer()),
                Map.entry("bb.asLongBuffer().slice()",
                          bb -> bb.asLongBuffer().slice()),
                Map.entry("bb.asLongBuffer().slice(index,length)",
                          bb -> { var lb = bb.asLongBuffer();
                                  lb =  lb.slice(1, lb.limit() - 1);
                                  bb.position(bb.position() + 8);
                                  return lb; }),
                Map.entry("bb.asLongBuffer().slice().duplicate()",
                          bb -> bb.asLongBuffer().slice().duplicate())
        );

        return product(BYTE_BUFFER_FUNCTIONS, bfs);
    }

    @Test(dataProvider = "longViewProvider")
    public void testLongGet(String desc, IntFunction<ByteBuffer> fbb,
                            Function<ByteBuffer, LongBuffer> fbi) {
        ByteBuffer bb = allocate(fbb);
        LongBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            long fromBytes = getLongFromBytes(bb, o + i * 8);
            long fromMethodView = bb.getLong(o + i * 8);
            assertValues(i, fromBytes, fromMethodView, bb);

            long fromBufferView = vb.get(i);
            assertValues(i, fromMethodView, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            long v = getLongFromBytes(bb, o + i * 8);
            long a = bb.getLong();
            assertValues(i, v, a, bb);

            long b = vb.get();
            assertValues(i, a, b, bb, vb);
        }

    }

    @Test(dataProvider = "longViewProvider")
    public void testLongPut(String desc, IntFunction<ByteBuffer> fbb,
                            Function<ByteBuffer, LongBuffer> fbi) {
        ByteBuffer bbfilled = allocate(fbb);
        ByteBuffer bb = allocate(fbb, i -> 0);
        LongBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            long fromFilled = bbfilled.getLong(o + i * 8);

            vb.put(i, fromFilled);
            long fromMethodView = bb.getLong(o + i * 8);
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            long fromFilled = bbfilled.getLong(o + i * 8);

            vb.put(fromFilled);
            long fromMethodView = bb.getLong();
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }


        fill(bb, i -> 0);
        bb.clear().position(o);
        vb.clear();

        for (int i = 0; i < vb.limit(); i++) {
            long fromFilled = bbfilled.getLong(o + i * 8);

            bb.putLong(o + i * 8, fromFilled);
            long fromBufferView = vb.get(i);
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            long fromFilled = bbfilled.getLong(o + i * 8);

            bb.putLong(fromFilled);
            long fromBufferView = vb.get();
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }
    }

    static long getLongFromBytes(ByteBuffer bb, int i) {
        long a = bb.get(i) & 0xFF;
        long b = bb.get(i + 1) & 0xFF;
        long c = bb.get(i + 2) & 0xFF;
        long d = bb.get(i + 3) & 0xFF;
        long e = bb.get(i + 4) & 0xFF;
        long f = bb.get(i + 5) & 0xFF;
        long g = bb.get(i + 6) & 0xFF;
        long h = bb.get(i + 7) & 0xFF;

        if (bb.order() == ByteOrder.BIG_ENDIAN) {
            return ((a << 56) | (b << 48) | (c << 40) | (d << 32) |
                    (e << 24) | (f << 16) | (g << 8) | h);
        }
        else {
            return ((h << 56) | (g << 48) | (f << 40) | (e << 32) |
                    (d << 24) | (c << 16) | (b << 8) | a);
        }
    }


    @DataProvider
    public static Object[][] floatViewProvider() {
        List<Map.Entry<String, Function<ByteBuffer, FloatBuffer>>> bfs = List.of(
                Map.entry("bb.asFloatBuffer()",
                          bb -> bb.asFloatBuffer()),
                Map.entry("bb.asFloatBuffer().slice()",
                          bb -> bb.asFloatBuffer().slice()),
                Map.entry("bb.asFloatBuffer().slice(index,length)",
                        bb -> { var fb = bb.asFloatBuffer();
                            fb =  fb.slice(1, fb.limit() - 1);
                            bb.position(bb.position() + 4);
                            return fb; }),
                Map.entry("bb.asFloatBuffer().slice().duplicate()",
                          bb -> bb.asFloatBuffer().slice().duplicate())
        );

        return product(BYTE_BUFFER_FUNCTIONS, bfs);
    }

    @Test(dataProvider = "floatViewProvider")
    public void testFloatGet(String desc, IntFunction<ByteBuffer> fbb,
                             Function<ByteBuffer, FloatBuffer> fbi) {
        ByteBuffer bb = allocate(fbb);
        FloatBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            float fromBytes = getFloatFromBytes(bb, o + i * 4);
            float fromMethodView = bb.getFloat(o + i * 4);
            assertValues(i, fromBytes, fromMethodView, bb);

            float fromBufferView = vb.get(i);
            assertValues(i, fromMethodView, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            float v = getFloatFromBytes(bb, o + i * 4);
            float a = bb.getFloat();
            assertValues(i, v, a, bb);

            float b = vb.get();
            assertValues(i, a, b, bb, vb);
        }

    }

    @Test(dataProvider = "floatViewProvider")
    public void testFloatPut(String desc, IntFunction<ByteBuffer> fbb,
                             Function<ByteBuffer, FloatBuffer> fbi) {
        ByteBuffer bbfilled = allocate(fbb);
        ByteBuffer bb = allocate(fbb, i -> 0);
        FloatBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            float fromFilled = bbfilled.getFloat(o + i * 4);

            vb.put(i, fromFilled);
            float fromMethodView = bb.getFloat(o + i * 4);
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            float fromFilled = bbfilled.getFloat(o + i * 4);

            vb.put(fromFilled);
            float fromMethodView = bb.getFloat();
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }


        fill(bb, i -> 0);
        bb.clear().position(o);
        vb.clear();

        for (int i = 0; i < vb.limit(); i++) {
            float fromFilled = bbfilled.getFloat(o + i * 4);

            bb.putFloat(o + i * 4, fromFilled);
            float fromBufferView = vb.get(i);
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            float fromFilled = bbfilled.getFloat(o + i * 4);

            bb.putFloat(fromFilled);
            float fromBufferView = vb.get();
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }
    }

    static float getFloatFromBytes(ByteBuffer bb, int i) {
        return Float.intBitsToFloat(getIntFromBytes(bb, i));
    }



    @DataProvider
    public static Object[][] doubleViewProvider() {
        List<Map.Entry<String, Function<ByteBuffer, DoubleBuffer>>> bfs = List.of(
                Map.entry("bb.asDoubleBuffer()",
                          bb -> bb.asDoubleBuffer()),
                Map.entry("bb.asDoubleBuffer().slice()",
                          bb -> bb.asDoubleBuffer().slice()),
                Map.entry("bb.asDoubleBuffer().slice(index,length)",
                        bb -> { var db = bb.asDoubleBuffer();
                            db =  db.slice(1, db.limit() - 1);
                            bb.position(bb.position() + 8);
                            return db; }),
                Map.entry("bb.asDoubleBuffer().slice().duplicate()",
                          bb -> bb.asDoubleBuffer().slice().duplicate())
        );

        return product(BYTE_BUFFER_FUNCTIONS, bfs);
    }

    @Test(dataProvider = "doubleViewProvider")
    public void testDoubleGet(String desc, IntFunction<ByteBuffer> fbb,
                              Function<ByteBuffer, DoubleBuffer> fbi) {
        ByteBuffer bb = allocate(fbb);
        DoubleBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            double fromBytes = getDoubleFromBytes(bb, o + i * 8);
            double fromMethodView = bb.getDouble(o + i * 8);
            assertValues(i, fromBytes, fromMethodView, bb);

            double fromBufferView = vb.get(i);
            assertValues(i, fromMethodView, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            double v = getDoubleFromBytes(bb, o + i * 8);
            double a = bb.getDouble();
            assertValues(i, v, a, bb);

            double b = vb.get();
            assertValues(i, a, b, bb, vb);
        }

    }

    @Test(dataProvider = "doubleViewProvider")
    public void testDoublePut(String desc, IntFunction<ByteBuffer> fbb,
                              Function<ByteBuffer, DoubleBuffer> fbi) {
        ByteBuffer bbfilled = allocate(fbb);
        ByteBuffer bb = allocate(fbb, i -> 0);
        DoubleBuffer vb = fbi.apply(bb);
        int o = bb.position();

        for (int i = 0; i < vb.limit(); i++) {
            double fromFilled = bbfilled.getDouble(o + i * 8);

            vb.put(i, fromFilled);
            double fromMethodView = bb.getDouble(o + i * 8);
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            double fromFilled = bbfilled.getDouble(o + i * 8);

            vb.put(fromFilled);
            double fromMethodView = bb.getDouble();
            assertValues(i, fromFilled, fromMethodView, bb, vb);
        }


        fill(bb, i -> 0);
        bb.clear().position(o);
        vb.clear();

        for (int i = 0; i < vb.limit(); i++) {
            double fromFilled = bbfilled.getDouble(o + i * 8);

            bb.putDouble(o + i * 8, fromFilled);
            double fromBufferView = vb.get(i);
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }

        for (int i = 0; i < vb.limit(); i++) {
            double fromFilled = bbfilled.getDouble(o + i * 8);

            bb.putDouble(fromFilled);
            double fromBufferView = vb.get();
            assertValues(i, fromFilled, fromBufferView, bb, vb);
        }
    }

    static double getDoubleFromBytes(ByteBuffer bb, int i) {
        return Double.longBitsToDouble(getLongFromBytes(bb, i));
    }
}

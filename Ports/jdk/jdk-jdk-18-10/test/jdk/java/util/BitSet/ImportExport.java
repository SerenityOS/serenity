/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 5037068
 * @summary Test import/export constructors and methods
 * @author Martin Buchholz
 * @key randomness
 */

import java.nio.*;
import java.util.*;

public class ImportExport {
    final Random rnd = new Random();

    void equal(byte[] x, byte[] y) {
        check(Arrays.equals(x, y));
    }

    void equal(long[] x, long[] y) {
        check(Arrays.equals(x, y));
    }

    void equal(byte[] bytes, BitSet s) {
        equal(s, BitSet.valueOf(bytes));
        equal(s, BitSet.valueOf(ByteBuffer.wrap(bytes)));
        equal(s, BitSet.valueOf(
                  ByteBuffer.wrap(
                      Arrays.copyOf(bytes, bytes.length + 8 + rnd.nextInt(8)))
                  .order(ByteOrder.LITTLE_ENDIAN)
                  .asLongBuffer()));
    }

    void checkEmptyBitSet(BitSet s) {
        equal(s.toByteArray(), new byte[0]);
        equal(s.toLongArray(), new long[0]);
        check(s.isEmpty());
    }

    void test(String[] args) throws Throwable {
        for (int i = 0; i < 17; i++) {
            byte[] bytes = new byte[i];
            BitSet s = new BitSet();
            equal(bytes, s);
            equal(BitSet.valueOf(bytes).toByteArray(), new byte[0]);
            if (i > 0) {
                int k = rnd.nextInt(i);
                for (int j = 0; j < 8; j++) {
                    bytes[k] |= 1 << j;
                    s.set(8*k+j);
                    equal(bytes, s);
                    byte[] expected = new byte[k+1]; expected[k] = bytes[k];
                    equal(BitSet.valueOf(bytes).toByteArray(), expected);
                    ByteBuffer bb = ByteBuffer.wrap(bytes);
                    bb.position(k);
                    equal(BitSet.valueOf(bb).toByteArray(),
                          new byte[]{bytes[k]});
                }
            }
        }
        for (int i = 0; i < 100; i++) {
            byte[] bytes = new byte[rnd.nextInt(17)];
            for (int j = 0; j < bytes.length; j++)
                bytes[j] = (byte) rnd.nextInt(0x100);
            BitSet s = BitSet.valueOf(bytes);
            byte[] expected = s.toByteArray();
            equal(expected.length, (s.length()+7)/8);
            if (bytes.length == 0)
                continue;
            if (expected.length > 0)
                check(expected[expected.length-1] != 0);
            if (bytes[bytes.length-1] != 0)
                equal(bytes, expected);
            int n = rnd.nextInt(8 * bytes.length);
            equal(s.get(n), ((bytes[n/8] & (1<<(n%8))) != 0));
        }

        for (int i = 0; i < 3; i++) {
            checkEmptyBitSet(BitSet.valueOf(new byte[i]));
            checkEmptyBitSet(BitSet.valueOf(ByteBuffer.wrap(new byte[i])));
            checkEmptyBitSet(BitSet.valueOf(new byte[i*64]));
            checkEmptyBitSet(BitSet.valueOf(ByteBuffer.wrap(new byte[i*64])));
            checkEmptyBitSet(BitSet.valueOf(new long[i]));
            checkEmptyBitSet(BitSet.valueOf(LongBuffer.wrap(new long[i])));
        }

        {
            long[] longs = new long[rnd.nextInt(10)];
            for (int i = 0; i < longs.length; i++)
                longs[i] = rnd.nextLong();
            LongBuffer b1 = LongBuffer.wrap(longs);
            LongBuffer b2 = LongBuffer.allocate(longs.length + 10);
            for (int i = 0; i < b2.limit(); i++)
                b2.put(i, rnd.nextLong());
            int beg = rnd.nextInt(10);
            b2.position(beg);
            b2.put(longs);
            b2.limit(b2.position());
            b2.position(beg);
            BitSet s1 = BitSet.valueOf(longs);
            BitSet s2 = BitSet.valueOf(b1);
            BitSet s3 = BitSet.valueOf(b2);
            equal(s1, s2);
            equal(s1, s3);
            if (longs.length > 0 && longs[longs.length -1] != 0) {
                equal(longs, s1.toLongArray());
                equal(longs, s2.toLongArray());
                equal(longs, s3.toLongArray());
            }
            for (int i = 0; i < 64 * longs.length; i++) {
                equal(s1.get(i), ((longs [i/64] & (1L<<(i%64))) != 0));
                equal(s2.get(i), ((b1.get(i/64) & (1L<<(i%64))) != 0));
                equal(s3.get(i), ((b2.get(b2.position()+i/64) & (1L<<(i%64))) != 0));
            }
        }
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new ImportExport().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}

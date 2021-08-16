/*
 * Copyright (c) 2021, Arm Limited. All rights reserved.
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

package compiler.vectorapi;

import java.util.Random;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8268966
 * @summary AArch64: 'bad AD file' in some vector conversion tests
 * @modules jdk.incubator.vector
 * @run testng/othervm -XX:-TieredCompilation compiler.vectorapi.VectorCastShape64Test
 */


public class VectorCastShape64Test {

    private static final VectorSpecies<Long> lspec = LongVector.SPECIES_64;
    private static final VectorSpecies<Integer> ispec = IntVector.SPECIES_64;
    private static final VectorSpecies<Short> sspec = ShortVector.SPECIES_64;
    private static final VectorSpecies<Byte> bspec = ByteVector.SPECIES_64;
    private static final VectorSpecies<Float> fspec = FloatVector.SPECIES_64;
    private static final VectorSpecies<Double> dspec = DoubleVector.SPECIES_64;

    private static final int NUM_ITER = 50000;
    private static final int LENGTH = 512;
    private static int[] ia;
    private static int[] ib;
    private static byte[] ba;
    private static byte[] bb;
    private static short[] sa;
    private static short[] sb;
    private static long[] la;
    private static long[] lb;
    private static double[] da;
    private static double[] db;
    private static float[] fa;
    private static float[] fb;

    private static void initialize() {
        ia = new int[LENGTH];
        ib = new int[LENGTH];
        la = new long[LENGTH];
        lb = new long[LENGTH];
        sa = new short[LENGTH];
        sb = new short[LENGTH];
        ba = new byte[LENGTH];
        bb = new byte[LENGTH];
        fa = new float[LENGTH];
        fb = new float[LENGTH];
        da = new double[LENGTH];
        db = new double[LENGTH];
        Random r = new Random();
        for (int i = 0; i < LENGTH; i++) {
            ia[i] = r.nextInt();
            la[i] = r.nextLong();
            sa[i] = (short) r.nextInt();
            ba[i] = (byte) r.nextInt();
            fa[i] = r.nextFloat();
            da[i] = r.nextDouble();
        }
    }


    private static void testDoubleToByte() {
        for (int i = 0; i < dspec.loopBound(LENGTH); i += dspec.length()) {
            DoubleVector va = DoubleVector.fromArray(dspec, da, i);
            ByteVector vb = (ByteVector) va.castShape(bspec, 0);
            vb.intoArray(bb, 0);

            for (int j = 0; j < Math.min(dspec.length(), bspec.length()); j++) {
                Assert.assertEquals(bb[j], (byte) da[j + i]);
            }
        }
    }

    private static void testDoubleToShort() {
        for (int i = 0; i < dspec.loopBound(LENGTH); i += dspec.length()) {
            DoubleVector va = DoubleVector.fromArray(dspec, da, i);
            ShortVector vb = (ShortVector) va.castShape(sspec, 0);
            vb.intoArray(sb, 0);

            for (int j = 0; j < Math.min(dspec.length(), sspec.length()); j++) {
                Assert.assertEquals(sb[j], (short) da[j + i]);
            }
        }
    }

    private static void testDoubleToInt() {
        for (int i = 0; i < dspec.loopBound(LENGTH); i += dspec.length()) {
            DoubleVector va = DoubleVector.fromArray(dspec, da, i);
            IntVector vb = (IntVector) va.castShape(ispec, 0);
            vb.intoArray(ib, 0);

            for (int j = 0; j < Math.min(dspec.length(), ispec.length()); j++) {
                Assert.assertEquals(ib[j], (int) da[j + i]);
            }
        }
    }

    private static void testDoubleToLong() {
        for (int i = 0; i < dspec.loopBound(LENGTH); i += dspec.length()) {
            DoubleVector va = DoubleVector.fromArray(dspec, da, i);
            LongVector vb = (LongVector) va.castShape(lspec, 0);
            vb.intoArray(lb, 0);

            for (int j = 0; j < Math.min(dspec.length(), lspec.length()); j++) {
                Assert.assertEquals(lb[j], (long) da[j + i]);
            }
        }
    }

    private static void testDoubleToFloat() {
        for (int i = 0; i < dspec.loopBound(LENGTH); i += dspec.length()) {
            DoubleVector va = DoubleVector.fromArray(dspec, da, i);
            FloatVector vb = (FloatVector) va.castShape(fspec, 0);
            vb.intoArray(fb, 0);

            for (int j = 0; j < Math.min(dspec.length(), fspec.length()); j++) {
                Assert.assertEquals(fb[j], (float) da[j + i]);
            }
        }
    }


    private static void testFloatToByte() {
        for (int i = 0; i < fspec.loopBound(LENGTH); i += fspec.length()) {
            FloatVector va = FloatVector.fromArray(fspec, fa, i);
            ByteVector vb = (ByteVector) va.castShape(bspec, 0);
            vb.intoArray(bb, 0);

            for (int j = 0; j < Math.min(fspec.length(), bspec.length()); j++) {
                Assert.assertEquals(bb[j], (byte) fa[j + i]);
            }
        }
    }

    private static void testFloatToShort() {
        for (int i = 0; i < fspec.loopBound(LENGTH); i += fspec.length()) {
            FloatVector va = FloatVector.fromArray(fspec, fa, i);
            ShortVector vb = (ShortVector) va.castShape(sspec, 0);
            vb.intoArray(sb, 0);

            for (int j = 0; j < Math.min(fspec.length(), sspec.length()); j++) {
                Assert.assertEquals(sb[j], (short) fa[j + i]);
            }
        }
    }

    private static void testFloatToInt() {
        for (int i = 0; i < fspec.loopBound(LENGTH); i += fspec.length()) {
            FloatVector va = FloatVector.fromArray(fspec, fa, i);
            IntVector vb = (IntVector) va.castShape(ispec, 0);
            vb.intoArray(ib, 0);

            for (int j = 0; j < Math.min(fspec.length(), ispec.length()); j++) {
                Assert.assertEquals(ib[j], (int) fa[j + i]);
            }
        }
    }

    private static void testFloatToLong() {
        for (int i = 0; i < fspec.loopBound(LENGTH); i += fspec.length()) {
            FloatVector va = FloatVector.fromArray(fspec, fa, i);
            LongVector vb = (LongVector) va.castShape(lspec, 0);
            vb.intoArray(lb, 0);

            for (int j = 0; j < Math.min(fspec.length(), lspec.length()); j++) {
                Assert.assertEquals(lb[j], (long) fa[j + i]);
            }
        }
    }

    private static void testFloatToDouble() {
        for (int i = 0; i < fspec.loopBound(LENGTH); i += fspec.length()) {
            FloatVector va = FloatVector.fromArray(fspec, fa, i);
            DoubleVector vb = (DoubleVector) va.castShape(dspec, 0);
            vb.intoArray(db, 0);

            for (int j = 0; j < Math.min(fspec.length(), dspec.length()); j++) {
                Assert.assertEquals(db[j], (double) fa[j + i]);
            }
        }
    }


    private static void testIntToByte() {
        for (int i = 0; i < ispec.loopBound(LENGTH); i += ispec.length()) {
            IntVector va = IntVector.fromArray(ispec, ia, i);
            ByteVector vb = (ByteVector) va.castShape(bspec, 0);
            vb.intoArray(bb, 0);

            for (int j = 0; j < Math.min(ispec.length(), bspec.length()); j++) {
                Assert.assertEquals(bb[j], (byte) ia[j + i]);
            }
        }
    }

    private static void testIntToShort() {
        for (int i = 0; i < ispec.loopBound(LENGTH); i += ispec.length()) {
            IntVector va = IntVector.fromArray(ispec, ia, i);
            ShortVector vb = (ShortVector) va.castShape(sspec, 0);
            vb.intoArray(sb, 0);

            for (int j = 0; j < Math.min(ispec.length(), sspec.length()); j++) {
                Assert.assertEquals(sb[j], (short) ia[j + i]);
            }
        }
    }

    private static void testIntToLong() {
        for (int i = 0; i < ispec.loopBound(LENGTH); i += ispec.length()) {
            IntVector va = IntVector.fromArray(ispec, ia, i);
            LongVector vb = (LongVector) va.castShape(lspec, 0);
            vb.intoArray(lb, 0);

            for (int j = 0; j < Math.min(ispec.length(), lspec.length()); j++) {
                Assert.assertEquals(lb[j], (long) ia[j + i]);
            }
        }
    }

    private static void testIntToFloat() {
        for (int i = 0; i < ispec.loopBound(LENGTH); i += ispec.length()) {
            IntVector va = IntVector.fromArray(ispec, ia, i);
            FloatVector vb = (FloatVector) va.castShape(fspec, 0);
            vb.intoArray(fb, 0);

            for (int j = 0; j < Math.min(ispec.length(), fspec.length()); j++) {
                Assert.assertEquals(fb[j], (float) ia[j + i]);
            }
        }
    }

    private static void testIntToDouble() {
        for (int i = 0; i < ispec.loopBound(LENGTH); i += ispec.length()) {
            IntVector va = IntVector.fromArray(ispec, ia, i);
            DoubleVector vb = (DoubleVector) va.castShape(dspec, 0);
            vb.intoArray(db, 0);

            for (int j = 0; j < Math.min(ispec.length(), dspec.length()); j++) {
                Assert.assertEquals(db[j], (double) ia[j + i]);
            }
        }
    }


    private static void testLongToByte() {
        for (int i = 0; i < lspec.loopBound(LENGTH); i += lspec.length()) {
            LongVector va = LongVector.fromArray(lspec, la, i);
            ByteVector vb = (ByteVector) va.castShape(bspec, 0);
            vb.intoArray(bb, 0);

            for (int j = 0; j < Math.min(lspec.length(), bspec.length()); j++) {
                Assert.assertEquals(bb[j], (byte) la[j + i]);
            }
        }
    }

    private static void testLongToShort() {
        for (int i = 0; i < lspec.loopBound(LENGTH); i += lspec.length()) {
            LongVector va = LongVector.fromArray(lspec, la, i);
            ShortVector vb = (ShortVector) va.castShape(sspec, 0);
            vb.intoArray(sb, 0);

            for (int j = 0; j < Math.min(lspec.length(), sspec.length()); j++) {
                Assert.assertEquals(sb[j], (short) la[j + i]);
            }
        }
    }

    private static void testLongToInt() {
        for (int i = 0; i < lspec.loopBound(LENGTH); i += lspec.length()) {
            LongVector va = LongVector.fromArray(lspec, la, i);
            IntVector vb = (IntVector) va.castShape(ispec, 0);
            vb.intoArray(ib, 0);

            for (int j = 0; j < Math.min(lspec.length(), ispec.length()); j++) {
                Assert.assertEquals(ib[j], (int) la[j + i]);
            }
        }
    }

    private static void testLongToFloat() {
        for (int i = 0; i < lspec.loopBound(LENGTH); i += lspec.length()) {
            LongVector va = LongVector.fromArray(lspec, la, i);
            FloatVector vb = (FloatVector) va.castShape(fspec, 0);
            vb.intoArray(fb, 0);

            for (int j = 0; j < Math.min(lspec.length(), fspec.length()); j++) {
                Assert.assertEquals(fb[j], (float) la[j + i]);
            }
        }
    }

    private static void testLongToDouble() {
        for (int i = 0; i < lspec.loopBound(LENGTH); i += lspec.length()) {
            LongVector va = LongVector.fromArray(lspec, la, i);
            DoubleVector vb = (DoubleVector) va.castShape(dspec, 0);
            vb.intoArray(db, 0);

            for (int j = 0; j < Math.min(lspec.length(), dspec.length()); j++) {
                Assert.assertEquals(db[j], (double) la[j + i]);
            }
        }
    }


    private static void testShortToByte() {
        for (int i = 0; i < sspec.loopBound(LENGTH); i += sspec.length()) {
            ShortVector va = ShortVector.fromArray(sspec, sa, i);
            ByteVector vb = (ByteVector) va.castShape(bspec, 0);
            vb.intoArray(bb, 0);

            for (int j = 0; j < Math.min(sspec.length(), bspec.length()); j++) {
                Assert.assertEquals(bb[j], (byte) sa[j + i]);
            }
        }
    }

    private static void testShortToInt() {
        for (int i = 0; i < sspec.loopBound(LENGTH); i += sspec.length()) {
            ShortVector va = ShortVector.fromArray(sspec, sa, i);
            IntVector vb = (IntVector) va.castShape(ispec, 0);
            vb.intoArray(ib, 0);

            for (int j = 0; j < Math.min(sspec.length(), ispec.length()); j++) {
                Assert.assertEquals(ib[j], (int) sa[j + i]);
            }
        }
    }

    private static void testShortToLong() {
        for (int i = 0; i < sspec.loopBound(LENGTH); i += sspec.length()) {
            ShortVector va = ShortVector.fromArray(sspec, sa, i);
            LongVector vb = (LongVector) va.castShape(lspec, 0);
            vb.intoArray(lb, 0);

            for (int j = 0; j < Math.min(sspec.length(), lspec.length()); j++) {
                Assert.assertEquals(lb[j], (long) sa[j + i]);
            }
        }
    }

    private static void testShortToFloat() {
        for (int i = 0; i < sspec.loopBound(LENGTH); i += sspec.length()) {
            ShortVector va = ShortVector.fromArray(sspec, sa, i);
            FloatVector vb = (FloatVector) va.castShape(fspec, 0);
            vb.intoArray(fb, 0);

            for (int j = 0; j < Math.min(sspec.length(), fspec.length()); j++) {
                Assert.assertEquals(fb[j], (float) sa[j + i]);
            }
        }
    }

    private static void testShortToDouble() {
        for (int i = 0; i < sspec.loopBound(LENGTH); i += sspec.length()) {
            ShortVector va = ShortVector.fromArray(sspec, sa, i);
            DoubleVector vb = (DoubleVector) va.castShape(dspec, 0);
            vb.intoArray(db, 0);

            for (int j = 0; j < Math.min(sspec.length(), dspec.length()); j++) {
                Assert.assertEquals(db[j], (double) sa[j + i]);
            }
        }
    }


    private static void testByteToShort() {
        for (int i = 0; i < bspec.loopBound(LENGTH); i += bspec.length()) {
            ByteVector va = ByteVector.fromArray(bspec, ba, i);
            ShortVector vb = (ShortVector) va.castShape(sspec, 0);
            vb.intoArray(sb, 0);

            for (int j = 0; j < Math.min(bspec.length(), sspec.length()); j++) {
                Assert.assertEquals(sb[j], (short) ba[j + i]);
            }
        }
    }

    private static void testByteToInt() {
        for (int i = 0; i < bspec.loopBound(LENGTH); i += bspec.length()) {
            ByteVector va = ByteVector.fromArray(bspec, ba, i);
            IntVector vb = (IntVector) va.castShape(ispec, 0);
            vb.intoArray(ib, 0);

            for (int j = 0; j < Math.min(bspec.length(), ispec.length()); j++) {
                Assert.assertEquals(ib[j], (int) ba[j + i]);
            }
        }
    }

    private static void testByteToLong() {
        for (int i = 0; i < bspec.loopBound(LENGTH); i += bspec.length()) {
            ByteVector va = ByteVector.fromArray(bspec, ba, i);
            LongVector vb = (LongVector) va.castShape(lspec, 0);
            vb.intoArray(lb, 0);

            for (int j = 0; j < Math.min(bspec.length(), lspec.length()); j++) {
                Assert.assertEquals(lb[j], (long) ba[j + i]);
            }
        }
    }

    private static void testByteToFloat() {
        for (int i = 0; i < bspec.loopBound(LENGTH); i += bspec.length()) {
            ByteVector va = ByteVector.fromArray(bspec, ba, i);
            FloatVector vb = (FloatVector) va.castShape(fspec, 0);
            vb.intoArray(fb, 0);

            for (int j = 0; j < Math.min(bspec.length(), fspec.length()); j++) {
                Assert.assertEquals(fb[j], (float) ba[j + i]);
            }
        }
    }

    private static void testByteToDouble() {
        for (int i = 0; i < bspec.loopBound(LENGTH); i += bspec.length()) {
            ByteVector va = ByteVector.fromArray(bspec, ba, i);
            DoubleVector vb = (DoubleVector) va.castShape(dspec, 0);
            vb.intoArray(db, 0);

            for (int j = 0; j < Math.min(bspec.length(), dspec.length()); j++) {
                Assert.assertEquals(db[j], (double) ba[j + i]);
            }
        }
    }


    @Test
    public void testCastShape64() {
        initialize();
        for (int i = 0; i < NUM_ITER; i++) {
            testDoubleToByte();
            testDoubleToShort();
            testDoubleToInt();
            testDoubleToLong();
            testDoubleToFloat();

            testFloatToByte();
            testFloatToShort();
            testFloatToInt();
            testFloatToLong();
            testFloatToDouble();

            testLongToByte();
            testLongToShort();
            testLongToInt();
            testLongToFloat();
            testLongToDouble();

            testIntToByte();
            testIntToShort();
            testIntToLong();
            testIntToFloat();
            testIntToDouble();

            testShortToByte();
            testShortToInt();
            testShortToLong();
            testShortToFloat();
            testShortToDouble();

            testByteToShort();
            testByteToInt();
            testByteToLong();
            testByteToFloat();
            testByteToDouble();
        }
    }
}

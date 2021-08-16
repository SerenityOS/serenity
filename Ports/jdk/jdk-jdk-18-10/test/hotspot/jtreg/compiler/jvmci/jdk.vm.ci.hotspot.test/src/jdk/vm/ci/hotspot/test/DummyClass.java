/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.vm.ci.hotspot.test;

import jdk.internal.vm.annotation.Stable;

public class DummyClass {

    public boolean booleanField = true;
    public byte byteField = 2;
    public short shortField = 3;
    public char charField = 'a';
    public int intField = 4;
    public long longField = 5L;
    public float floatField = 4.5f;
    public double doubleField = 53.2;
    public Object objectField = new Object();

    public final boolean finalBooleanField = true;
    public final byte finalByteField = -2;
    public final short finalShortField = -3;
    public final char finalCharField = 'b';
    public final int finalIntField = 8;
    public final long finalLongField = 888L;
    public final float finalFloatField = 77.8f;
    public final double finalDoubleField = -234.2;
    public final Object finalObjectField = new Object();

    @Stable public boolean stableBooleanField = true;
    @Stable public byte stableByteField = -2;
    @Stable public short stableShortField = -3;
    @Stable public char stableCharField = 'c';
    @Stable public int stableIntField = 8;
    @Stable public long stableLongField = 888L;
    @Stable public float stableFloatField = 77.8f;
    @Stable public double stableDoubleField = -234.2;
    @Stable public Object stableObjectField = new Object();

    @Stable public boolean stableDefaultBooleanField;
    @Stable public byte stableDefaultByteField;
    @Stable public short stableDefaultShortField;
    @Stable public char stableDefaultCharField;
    @Stable public int stableDefaultIntField;
    @Stable public long stableDefaultLongField;
    @Stable public float stableDefaultFloatField;
    @Stable public double stableDefaultDoubleField;
    @Stable public Object stableDefaultObjectField;

    public final boolean finalDefaultBooleanField = false;
    public final byte finalDefaultByteField = 0;
    public final short finalDefaultShortField = 0;
    public final char finalDefaultCharField = 0;
    public final int finalDefaultIntField = 0;
    public final long finalDefaultLongField = 0L;
    public final float finalDefaultFloatField = 0.0f;
    public final double finalDefaultDoubleField = 0.0;
    public final Object finalDefaultObjectField = null;

    public static boolean staticBooleanField = true;
    public static byte staticByteField = -1;
    public static short staticShortField = 11;
    public static char staticCharField = 'e';
    public static int staticIntField = 344;
    public static long staticLongField = 34231212L;
    public static float staticFloatField = -4.5f;
    public static double staticDoubleField = 453.2;
    public static Object staticObjectField = new Object();

    public static final boolean staticFinalBooleanField = true;
    public static final byte staticFinalByteField = -51;
    public static final short staticFinalShortField = 911;
    public static final char staticFinalCharField = 'g';
    public static final int staticFinalIntField = 9344;
    public static final long staticFinalLongField = 54231212L;
    public static final float staticFinalFloatField = -42.5f;
    public static final double staticFinalDoubleField = 5453.2;
    public static final Object staticFinalObjectField = new Object();

    @Stable public static boolean staticStableBooleanField = true;
    @Stable public static byte staticStableByteField = -61;
    @Stable public static short staticStableShortField = 661;
    @Stable public static char staticStableCharField = 'y';
    @Stable public static int staticStableIntField = 6574;
    @Stable public static long staticStableLongField = -2342L;
    @Stable public static float staticStableFloatField = -466.5f;
    @Stable public static double staticStableDoubleField = 4563.2;
    @Stable public static Object staticStableObjectField = new Object();

    @Stable public static boolean staticStableDefaultBooleanField;
    @Stable public static byte staticStableDefaultByteField;
    @Stable public static short staticStableDefaultShortField;
    @Stable public static char staticStableDefaultCharField;
    @Stable public static int staticStableDefaultIntField;
    @Stable public static long staticStableDefaultLongField;
    @Stable public static float staticStableDefaultFloatField;
    @Stable public static double staticStableDefaultDoubleField;
    @Stable public static Object staticStableDefaultObjectField;

    public boolean[] booleanArrayWithValues = new boolean[]{true, false};
    public byte[] byteArrayWithValues = new byte[]{43, 0};
    public short[] shortArrayWithValues = new short[]{9, 0};
    public char[] charArrayWithValues = new char[]{'a', 0};
    public int[] intArrayWithValues = new int[]{99, 0};
    public long[] longArrayWithValues = new long[]{868L, 0L};
    public float[] floatArrayWithValues = new float[]{75.8f, 0f};
    public double[] doubleArrayWithValues = new double[]{-294.66, 0.0};
    public Object[] objectArrayWithValues = new Object[]{new Object(), null};

    @Stable public boolean[] stableBooleanArrayWithValues = new boolean[]{true, false};
    @Stable public byte[] stableByteArrayWithValues = new byte[]{-2, 0};
    @Stable public short[] stableShortArrayWithValues = new short[]{-3, 0};
    @Stable public char[] stableCharArrayWithValues = new char[]{'c', 0};
    @Stable public int[] stableIntArrayWithValues = new int[]{8, 0};
    @Stable public long[] stableLongArrayWithValues = new long[]{888L, 0L};
    @Stable public float[] stableFloatArrayWithValues = new float[]{77.8f, 0f};
    @Stable public double[] stableDoubleArrayWithValues = new double[]{-234.2, 0.0};
    @Stable public Object[] stableObjectArrayWithValues = new Object[]{new Object(), null};

    public boolean[][] booleanArrayArrayWithValues = new boolean[][]{{true}, null};
    public byte[][] byteArrayArrayWithValues = new byte[][]{{43, 0}, null};
    public short[][] shortArrayArrayWithValues = new short[][]{{9, 0}, null};
    public char[][] charArrayArrayWithValues = new char[][]{{'a', 0}, null};
    public int[][] intArrayArrayWithValues = new int[][]{{99, 0}, null};
    public long[][] longArrayArrayWithValues = new long[][]{{868L, 0L}, null};
    public float[][] floatArrayArrayWithValues = new float[][]{{75.8f, 0f}, null};
    public double[][] doubleArrayArrayWithValues = new double[][]{{-294.66, 0.0}, null};
    public Object[][] objectArrayArrayWithValues = new Object[][]{{new Object(), null}, null};

    @Stable public boolean[][] stableBooleanArrayArrayWithValues = new boolean[][]{{true, false}, null};
    @Stable public byte[][] stableByteArrayArrayWithValues = new byte[][]{{-2, 0}, null};
    @Stable public short[][] stableShortArrayArrayWithValues = new short[][]{{-3, 0}, null};
    @Stable public char[][] stableCharArrayArrayWithValues = new char[][]{{'c', 0}, null};
    @Stable public int[][] stableIntArrayArrayWithValues = new int[][]{{8, 0}, null};
    @Stable public long[][] stableLongArrayArrayWithValues = new long[][]{{888L, 0L}, null};
    @Stable public float[][] stableFloatArrayArrayWithValues = new float[][]{{77.8f, 0f}, null};
    @Stable public double[][] stableDoubleArrayArrayWithValues = new double[][]{{-234.2, 0.0}, null};
    @Stable public Object[][] stableObjectArrayArrayWithValues = new Object[][]{{new Object(), null}, null};

    // Strings for testing "forString" method
    public final String stringField = "abc";
    public final String stringField2 = "xyz";
    public final String stringEmptyField = "";
}

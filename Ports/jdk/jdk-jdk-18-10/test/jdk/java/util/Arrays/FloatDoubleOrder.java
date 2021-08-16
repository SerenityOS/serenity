/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4143272 6548425
 * @summary The natural ordering on Float and Double was not even a partial
 *          order (i.e., it violated the contract of Comparable.compareTo).
 *          Now it's a total ordering.  Arrays.sort(double[])
 *          and Arrays.sort(double[]) reflect the new ordering.  Also,
 *          Arrays.equals(double[], double[]) and
 *          Arrays.equals(float[], float[]) reflect the definition of
 *          equality used by Float and Double.
 */

import java.util.*;

@SuppressWarnings("unchecked")
public class FloatDoubleOrder {
    void test(String[] args) throws Throwable {
        double[] unsortedDbl = new double[] {1.0d, 3.7d, Double.NaN, -2.0d,
           Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0d, -0.0d};

        double[] sortedDbl = new double[] {Double.NEGATIVE_INFINITY, -2.0d,
           -0.0d, 0.0d, 1.0d, 3.7d, Double.POSITIVE_INFINITY, Double.NaN};

        List list = new ArrayList();
        for (int i=0; i<unsortedDbl.length; i++)
            list.add(new Double(unsortedDbl[i]));
        Collections.sort(list);

        List sortedList = new ArrayList();
        for (int i=0; i<sortedDbl.length; i++)
            sortedList.add(new Double(sortedDbl[i]));

        check(list.equals(sortedList));

        Arrays.sort(unsortedDbl);
        check(Arrays.equals(unsortedDbl, sortedDbl));

        double negNan = Double.longBitsToDouble(0xfff8000000000000L);
        for (int i = 0; i < sortedDbl.length; i++) {
            equal(Arrays.binarySearch(sortedDbl, sortedDbl[i]), i);
            if (Double.isNaN(sortedDbl[i]))
                equal(Arrays.binarySearch(sortedDbl, negNan), i);
        }

        float[] unsortedFlt = new float[] {1.0f, 3.7f, Float.NaN, -2.0f,
           Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f};

        float[] sortedFlt = new float[] {Float.NEGATIVE_INFINITY, -2.0f,
           -0.0f, 0.0f, 1.0f, 3.7f, Float.POSITIVE_INFINITY, Float.NaN};

        list.clear();
        for (int i=0; i<unsortedFlt.length; i++)
            list.add(new Float(unsortedFlt[i]));
        Collections.sort(list);

        sortedList.clear();
        for (int i=0; i<sortedFlt.length; i++)
            sortedList.add(new Float(sortedFlt[i]));

        check(list.equals(sortedList));

        Arrays.sort(unsortedFlt);
        check(Arrays.equals(unsortedFlt, sortedFlt));

        float negNaN = Float.intBitsToFloat(0xFfc00000);
        for (int i = 0; i < sortedDbl.length; i++) {
            equal(Arrays.binarySearch(sortedFlt, sortedFlt[i]), i);
            if (Float.isNaN(sortedFlt[i]))
                equal(Arrays.binarySearch(sortedFlt, negNaN), i);
        }


        // 6548425: Arrays.sort incorrectly sorts a double array
        // containing negative zeros
        double[] da = {-0.0d, -0.0d, 0.0d, -0.0d};
        Arrays.sort(da, 1, 4);
        check(Arrays.equals(da, new double[] {-0.0d, -0.0d, -0.0d, 0.0d}));

        float[] fa = {-0.0f, -0.0f, 0.0f, -0.0f};
        Arrays.sort(fa, 1, 4);
        check(Arrays.equals(fa, new float[] {-0.0f, -0.0f, -0.0f, 0.0f}));
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
        new FloatDoubleOrder().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}

/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8074981
 * @summary Add C2 x86 Superword support for scalar product reduction optimizations : int test
 * @requires os.arch=="x86" | os.arch=="i386" | os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions
 *      -XX:LoopUnrollLimit=250 -XX:CompileThresholdScaling=0.1
 *      -XX:CompileCommand=exclude,compiler.loopopts.superword.ReductionPerf::main
 *      -XX:+SuperWordReductions
 *      compiler.loopopts.superword.ReductionPerf
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions
 *      -XX:LoopUnrollLimit=250 -XX:CompileThresholdScaling=0.1
 *      -XX:CompileCommand=exclude,compiler.loopopts.superword.ReductionPerf::main
 *      -XX:-SuperWordReductions
 *      compiler.loopopts.superword.ReductionPerf
 */

package compiler.loopopts.superword;

public class ReductionPerf {
    public static void main(String[] args) throws Exception {
        int[] a1 = new int[8 * 1024];
        int[] a2 = new int[8 * 1024];
        int[] a3 = new int[8 * 1024];
        long[] b1 = new long[8 * 1024];
        long[] b2 = new long[8 * 1024];
        long[] b3 = new long[8 * 1024];
        float[] c1 = new float[8 * 1024];
        float[] c2 = new float[8 * 1024];
        float[] c3 = new float[8 * 1024];
        double[] d1 = new double[8 * 1024];
        double[] d2 = new double[8 * 1024];
        double[] d3 = new double[8 * 1024];

        ReductionInit(a1, a2, a3, b1, b2, b3, c1, c2, c3, d1, d2, d3);

        int sumIv = sumInt(a1, a2, a3);
        long sumLv = sumLong(b1, b2, b3);
        float sumFv = sumFloat(c1, c2, c3);
        double sumDv = sumDouble(d1, d2, d3);
        int mulIv = prodInt(a1, a2, a3);
        long mulLv = prodLong(b1, b2, b3);
        float mulFv = prodFloat(c1, c2, c3);
        double mulDv = prodDouble(d1, d2, d3);

        int sumI = 0;
        long sumL = 0;
        float sumF = 0.f;
        double sumD = 0.;
        int mulI = 0;
        long mulL = 0;
        float mulF = 0.f;
        double mulD = 0.;

        System.out.println("Warmup ...");
        long start = System.currentTimeMillis();

        for (int j = 0; j < 2000; j++) {
            sumI = sumInt(a1, a2, a3);
            sumL = sumLong(b1, b2, b3);
            sumF = sumFloat(c1, c2, c3);
            sumD = sumDouble(d1, d2, d3);
            mulI = prodInt(a1, a2, a3);
            mulL = prodLong(b1, b2, b3);
            mulF = prodFloat(c1, c2, c3);
            mulD = prodDouble(d1, d2, d3);
        }

        long stop = System.currentTimeMillis();
        System.out.println(" Warmup is done in " + (stop - start) + " msec");

        if (sumIv != sumI) {
            System.out.println("sum int:    " + sumIv + " != " + sumI);
        }
        if (sumLv != sumL) {
            System.out.println("sum long:   " + sumLv + " != " + sumL);
        }
        if (sumFv != sumF) {
            System.out.println("sum float:  " + sumFv + " != " + sumF);
        }
        if (sumDv != sumD) {
            System.out.println("sum double: " + sumDv + " != " + sumD);
        }
        if (mulIv != mulI) {
            System.out.println("prod int:    " + mulIv + " != " + mulI);
        }
        if (mulLv != mulL) {
            System.out.println("prod long:   " + mulLv + " != " + mulL);
        }
        if (mulFv != mulF) {
            System.out.println("prod float:  " + mulFv + " != " + mulF);
        }
        if (mulDv != mulD) {
            System.out.println("prod double: " + mulDv + " != " + mulD);
        }

        start = System.currentTimeMillis();
        for (int j = 0; j < 5000; j++) {
            sumI = sumInt(a1, a2, a3);
        }
        stop = System.currentTimeMillis();
        System.out.println("sum int:    " + (stop - start));

        start = System.currentTimeMillis();
        for (int j = 0; j < 5000; j++) {
            sumL = sumLong(b1, b2, b3);
        }
        stop = System.currentTimeMillis();
        System.out.println("sum long:   " + (stop - start));

        start = System.currentTimeMillis();
        for (int j = 0; j < 5000; j++) {
            sumF = sumFloat(c1, c2, c3);
        }
        stop = System.currentTimeMillis();
        System.out.println("sum float:  " + (stop - start));

        start = System.currentTimeMillis();
        for (int j = 0; j < 5000; j++) {
            sumD = sumDouble(d1, d2, d3);
        }
        stop = System.currentTimeMillis();
        System.out.println("sum double: " + (stop - start));

        start = System.currentTimeMillis();
        for (int j = 0; j < 5000; j++) {
            mulI = prodInt(a1, a2, a3);
        }
        stop = System.currentTimeMillis();
        System.out.println("prod int:    " + (stop - start));

        start = System.currentTimeMillis();
        for (int j = 0; j < 5000; j++) {
            mulL = prodLong(b1, b2, b3);
        }
        stop = System.currentTimeMillis();
        System.out.println("prod long:   " + (stop - start));

        start = System.currentTimeMillis();
        for (int j = 0; j < 5000; j++) {
            mulF = prodFloat(c1, c2, c3);
        }
        stop = System.currentTimeMillis();
        System.out.println("prod float:  " + (stop - start));

        start = System.currentTimeMillis();
        for (int j = 0; j < 5000; j++) {
            mulD = prodDouble(d1, d2, d3);
        }
        stop = System.currentTimeMillis();
        System.out.println("prod double: " + (stop - start));

    }

    public static void ReductionInit(int[] a1, int[] a2, int[] a3,
                                     long[] b1, long[] b2, long[] b3,
                                     float[] c1, float[] c2, float[] c3,
                                     double[] d1, double[] d2, double[] d3) {
        for(int i = 0; i < a1.length; i++) {
            a1[i] =          (i + 0);
            a2[i] =          (i + 1);
            a3[i] =          (i + 2);
            b1[i] =   (long) (i + 0);
            b2[i] =   (long) (i + 1);
            b3[i] =   (long) (i + 2);
            c1[i] =  (float) (i + 0);
            c2[i] =  (float) (i + 1);
            c3[i] =  (float) (i + 2);
            d1[i] = (double) (i + 0);
            d2[i] = (double) (i + 1);
            d3[i] = (double) (i + 2);
        }
    }

    public static int sumInt(int[] a1, int[] a2, int[] a3) {
        int total = 0;
        for (int i = 0; i < a1.length; i++) {
            total += (a1[i] * a2[i]) + (a1[i] * a3[i]) + (a2[i] * a3[i]);
        }
        return total;
    }

    public static long sumLong(long[] b1, long[] b2, long[] b3) {
        long total = 0;
        for (int i = 0; i < b1.length; i++) {
            total += (b1[i] * b2[i]) + (b1[i] * b3[i]) + (b2[i] * b3[i]);
        }
        return total;
    }

    public static float sumFloat(float[] c1, float[] c2, float[] c3) {
        float total = 0;
        for (int i = 0; i < c1.length; i++) {
            total += (c1[i] * c2[i]) + (c1[i] * c3[i]) + (c2[i] * c3[i]);
        }
        return total;
    }

    public static double sumDouble(double[] d1, double[] d2, double[] d3) {
        double total = 0;
        for (int i = 0; i < d1.length; i++) {
            total += (d1[i] * d2[i]) + (d1[i] * d3[i]) + (d2[i] * d3[i]);
        }
        return total;
    }

    public static int prodInt(int[] a1, int[] a2, int[] a3) {
        int total = 1;
        for (int i = 0; i < a1.length; i++) {
            total *= (a1[i] * a2[i]) + (a1[i] * a3[i]) + (a2[i] * a3[i]);
        }
        return total;
    }

    public static long prodLong(long[] b1, long[] b2, long[] b3) {
        long total = 1;
        for (int i = 0; i < b1.length; i++) {
            total *= (b1[i] * b2[i]) + (b1[i] * b3[i]) + (b2[i] * b3[i]);
        }
        return total;
    }

    public static float prodFloat(float[] c1, float[] c2, float[] c3) {
        float total = 1;
        for (int i = 0; i < c1.length; i++) {
            total *= (c1[i] * c2[i]) + (c1[i] * c3[i]) + (c2[i] * c3[i]);
        }
        return total;
    }

    public static double prodDouble(double[] d1, double[] d2, double[] d3) {
        double total = 1;
        for (int i = 0; i < d1.length; i++) {
            total *= (d1[i] * d2[i]) + (d1[i] * d3[i]) + (d2[i] * d3[i]);
        }
        return total;
    }
}

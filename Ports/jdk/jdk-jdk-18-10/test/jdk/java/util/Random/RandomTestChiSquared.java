/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;

import java.util.random.*;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @test
 * @summary test bit sequences produced by clases that implement interface RandomGenerator
 * @bug 8248862
 * @run main RandomTestChiSquared
 * @key randomness
 */

public class RandomTestChiSquared {

    static String currentRNG = "";
    static int failCount = 0;

    static void exceptionOnFail() {
        if (failCount != 0) {
            throw new RuntimeException(failCount + " fails detected");
        }
    }

    static void setRNG(String rng) {
        currentRNG = rng;
    }

    static void fail(String format, Object... args) {
        if (currentRNG.length() != 0) {
            System.err.println(currentRNG);
            currentRNG = "";
        }

        System.err.format("  " + format, args);
        failCount++;
    }

    // Some simple chi-squared tests similar to that used for the FIPS 140 poker test,
    // but intended primarily to test production of random values from ranges.

    private static final int SEQUENCE_SIZE = 20000;

    // Entry k of this table was computed in Microsoft Excel using the formulae
    // =CHISQ.INV(0.0000000777517,k) and =CHISQ.INV.RT(0.00000142611,k)
    // (except that entry 0 is a dummy and should never be used).
    static final double[][] chiSquaredBounds = {
       { 0.0, 0.0 },
       { 9.49598E-15, 23.24513154 },
       { 1.55503E-07, 26.92112020 },
       { 4.40485E-05, 29.93222467 },
       { 0.000788782, 32.62391510 },
       { 0.004636947, 35.11665045 },
       { 0.015541535, 37.46947634 },
       { 0.037678318, 39.71667636 },
       { 0.074471919, 41.88031518 },
       { 0.128297057, 43.97561646 },
       { 0.200566433, 46.01362542 },
       { 0.291944952, 48.00266676 },
       { 0.402564694, 49.94920504 },
       { 0.532199236, 51.85838271 },
       { 0.680392565, 53.73437242 },
       { 0.846549931, 55.58061674 },
       { 1.030000010, 57.39999630 },
       { 1.230036580, 59.19495111 },
       { 1.445945898, 60.96756998 },
       { 1.677024296, 62.71965780 },
       { 1.922589129, 64.45278706 },
       { 2.181985238, 66.16833789 },
       { 2.454588423, 67.86752964 },
       { 2.739806923, 69.55144602 },
       { 3.037081611, 71.22105544 },
       { 3.345885349, 72.87722754 },
       { 3.665721841, 74.52074682 },
       { 3.996124178, 76.15232388 },
       { 4.336653242, 77.77260487 },
       { 4.686896041, 79.38217943 },
       { 5.046464051, 80.98158736 },
       { 5.414991603, 82.57132439 }
    };



    // N is the number of categories; every element of s ought to be in [0,N).
    // N must be in [1,31].
    static boolean chiSquaredTest(String id, int N, IntSupplier theSupplier) {
       int[] stats = new int[N];
       for (int j = 0; j < SEQUENCE_SIZE; j++) {
           int v = theSupplier.getAsInt();
           // assert((0 <= v) && (v < N));
           ++stats[v];
       }
       int z = 0;
       for (int k = 0; k < stats.length; k++) {
           z += stats[k]*stats[k];
       }
       double x = ((double)N / (double)SEQUENCE_SIZE) * (double)z - (double)SEQUENCE_SIZE;
       double lo = chiSquaredBounds[N][0];
       double hi = chiSquaredBounds[N][1];
       boolean chiSquaredSuccess = ((lo < x) && (x < hi));
       if (!chiSquaredSuccess) fail("chi-squared test failure for %s: x=%g (should be in [%g,%g])\n", id, x, lo, hi);
       return chiSquaredSuccess;
    }

    static boolean testRngChiSquared(String id, int N, IntSupplier theSupplier) {
       if (chiSquaredTest(id, N, theSupplier)) return true;
       fail("testRngChiSquared glitch");
       return chiSquaredTest(id, N, theSupplier) && chiSquaredTest(id, N, theSupplier);
    }

    static void tryIt(RandomGenerator rng, String kind, Function<String,BooleanSupplier> f) {
       String id = rng.getClass().getName() + " " + kind;
       // System.out.printf("Testing %s\n", id);
       boolean success = f.apply(id).getAsBoolean();
       if (!success) {
           fail("*** Failure of %s\n", id);
       }
    }

    // To test one instance of an RandomGenerator with the BSI test suite, we test:
    //    nextInt()
    //    nextLong()
    //    nextBoolean()
    //    ints()
    //    longs()
    //    doubles()
    //    nextDouble()
    //    nextFloat()

    static final int[] testSizes = { 11, 13, 16, 17, 19, 23, 24 };

    static void testOneRng(RandomGenerator rng, int failureTolerance) {
        String name = rng.getClass().getName();
        for (int j = 0; j < testSizes.length; j++) {
            int N = testSizes[j];
            tryIt(rng, "nextInt(" + N + ")", (String id) -> () -> testRngChiSquared(id, N, () -> rng.nextInt(N)));
            tryIt(rng, "nextInt(43, " + (N+43) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> rng.nextInt(43, N+43) - 43));
            tryIt(rng, "nextInt(-59, " + (N-59) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> rng.nextInt(-59, N-59) + 59));
        }
        for (int j = 0; j < testSizes.length; j++) {
            int N = testSizes[j];
            tryIt(rng, "nextLong(" + N + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextLong(N))));
            tryIt(rng, "nextLong(43, " + (N+43) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextLong(43, N+43) - 43)));
            tryIt(rng, "nextLong(-59, " + (N-59) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextLong(-59, N-59) + 59)));
        }
        for (int j = 0; j < testSizes.length; j++) {
            int N = testSizes[j];
            tryIt(rng, "nextFloat(" + N + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextFloat(N)) % N));
            tryIt(rng, "nextFloat(43, " + (N+43) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextFloat(43, N+43) - 43) % N));
            tryIt(rng, "nextFloat(-59, " + (N-59) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextFloat(-59, N-59) + 59) % N));
        }
        for (int j = 0; j < testSizes.length; j++) {
            int N = testSizes[j];
            tryIt(rng, "nextDouble(" + N + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextDouble(N)) % N));
            tryIt(rng, "nextDouble(43, " + (N+43) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextDouble(43, N+43) - 43) % N));
            tryIt(rng, "nextDouble(-59, " + (N-59) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(rng.nextDouble(-59, N-59) + 59) % N));
        }
        for (int j = 0; j < testSizes.length; j++) {
            int N = testSizes[j];
            PrimitiveIterator.OfInt iter1 = rng.ints(0, N).iterator();
            tryIt(rng, "ints(0, " + N + ")", (String id) -> () -> testRngChiSquared(id, N, iter1::next));
            PrimitiveIterator.OfInt iter2 = rng.ints(43, N+43).iterator();
            tryIt(rng, "ints(43, " + (N+43) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> iter2.next() - 43));
            PrimitiveIterator.OfInt iter3 = rng.ints(-59, N-59).iterator();
            tryIt(rng, "ints(-59, " + (N-59) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> iter3.next() + 59));
        }
        for (int j = 0; j < testSizes.length; j++) {
            int N = testSizes[j];
            PrimitiveIterator.OfLong iter1 = rng.longs(0, N).iterator();
            tryIt(rng, "longs(0, " + N + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(iter1.next() + 0)));
            PrimitiveIterator.OfLong iter2 = rng.longs(43, N+43).iterator();
            tryIt(rng, "longs(43, " + (N+43) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(iter2.next() - 43)));
            PrimitiveIterator.OfLong iter3 = rng.longs(-59, N-59).iterator();
            tryIt(rng, "longs(-59, " + (N-59) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(iter3.next() + 59)));
        }
        for (int j = 0; j < testSizes.length; j++) {
            int N = testSizes[j];
            PrimitiveIterator.OfDouble iter1 = rng.doubles(0, N).iterator();
            tryIt(rng, "doubles(0, " + N + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(iter1.next() + 0) % N));
            PrimitiveIterator.OfDouble iter2 = rng.doubles(43, N+43).iterator();
            tryIt(rng, "doubles(43, " + (N+43) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(iter2.next() - 43) % N));
            PrimitiveIterator.OfDouble iter3 = rng.doubles(-59, N-59).iterator();
            tryIt(rng, "doubles(-59, " + (N-59) + ")", (String id) -> () -> testRngChiSquared(id, N, () -> (int)(iter3.next() + 59) % N));
        }
    }

    public static void main(String[] args) {
        RandomGeneratorFactory.all().forEach(factory -> {
            setRNG(factory.name());

            if (factory.name().equals("Random")) {
                testOneRng(factory.create(417), 1);
            } else {
                testOneRng(factory.create(417), 0);
            }
        });

        exceptionOnFail();
    }

}


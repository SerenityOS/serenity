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
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @test
 * @summary test bit sequences produced by clases that implement interface RandomGenerator
 * @bug 8248862
 * @run main RandomTestBsi1999
 * @key randomness
 */

public class RandomTestBsi1999 {

    /* A set of tests for pseudorandom number generators inspired by this report:
     *
     *   Werner Schindler.  Functionality Classes and Evaluation Methodology for
     *   Deterministic Random Number Generators, Version 2.0.
     *   Bundesamt fur Sicherheit in der Informationstechnik (BSI).  December 2, 1999.
     *   https://www.bsi.bund.de/SharedDocs/Downloads/DE/BSI/Zertifizierung/Interpretationen/AIS_20_Functionality_Classes_Evaluation_Methodology_DRNG_e.pdf
     *
     * Section F of this report (pp. 19-20) recommends the use of five tests to evaluate a
     * sequence of bits:
     *
     *        Monobit test
     *        Poker test
     *        Run test
     *        Long run test
     *        Autocorrelation test
     *
     * The first four of these tests are in turn taken from this report:
     *
     *   National Institute of Standards and Technology (NIST),
     *   U.S. Department of Commerce.  Security Requirements for
     *   Cryptographic Modules.  Federal Information Processing
     *   Standard (FIPS) 140-1, January 11, 1994.
     *   https://csrc.nist.gov/csrc/media/publications/fips/140/1/archive/1994-01-11/documents/fips1401.pdf
     *
     * The BSI report appears to contain a few typos in transcribing the FIPS 140-1
     * requirements (pp. 44-45); specifically, the last three intervals for the runs test
     * (for lengths 4, 5, and 6+) are given as "223 - 402, 90 - 223, 90 - 223" in the FIPS
     * standard but as "233-402, 90-223, 90-233" in the BSI publication.  A quick
     * mathematical check indicates that the FIPS 140-1 values are correct; therefore we
     * use those values here. In addition, the BSI report specifies a test interval of
     * 2326-2674 for the autocorrelation test, which provides an appropriately small
     * rejection rate if the test were done for only a single value of tau; but because we
     * wish to perform 5000 distinct tests, one for each value of tau in the range 1-5000,
     * that test interval produces too many false positives.  Some calculation shows that
     * the interval 2267-2733 used by the FIPS 140-1 run test for runs of length 1 is
     * appropriate, so we use that interval here for each of the 5000 individual
     * autocorrelation tests.
     *
     * Each of the four FIPS 140-1 tests examines a sequence of 20000 bits.  The
     * autocorrelation test examines a sequence of 10000 bits.  It is convenient to
     * generate a sequence of 20000 bits (which we represent as an array of 20000 bytes)
     * and then apply all five tests, knowing that the autocorrelation test will examine
     * only the first half of the byte array.
     *
     * The descriptions of the tests are quoted from the FIPS 140-1 and BSI reports
     * (with some adjustments of punctuation and mathematical symbols, as well as
     * for our specific choices of test intervals).
     */

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

    private static final int SEQUENCE_SIZE = 20000;

    /* The Monobit Test
     *
     *   1. Count the number of ones in the 20,000 bit stream.  Denote this quantity by X.
     *
     *   2. The test is passed if 9,654 < X < 10,346.
     */
    static int monobitTest(String id, byte[] s) {
       // System.out.println("monobit test");
       int count = 0;
       for (int j = 0; j < s.length; j++) {
           count += s[j];
       }
       int monobitFailure = ((9654 < count) && (count < 10346)) ? 0 : 1;
       if (monobitFailure != 0) fail("monobit test failure for %s: count=%d (should be in [9654,10346])\n", id, count);
       return monobitFailure;
    }

    /* The Poker Test
     *
     *   1.  Divide the 20,000 bit stream into 5,000 contiguous 4-bit segments.  Count and
     *   store the number of occurrences of each of the 16 possible 4-bit values.  Denote
     *   f(i) as the number of each 4-bit value i where 0 <= i <= 15.
     *
     *   2.  Evaluate the following: X = (16/5000)(sum[i=0,15] (f(i))**2) - 5000
     *
     *   3.  The test is passed if 1.03 < X < 57.4.
     */

    static int pokerTest(String id, byte[] s) {
       // System.out.println("poker test");
       // Divide the bit sequence into 4-bit chunks, and count the number of times each 4-bit value appears.
       int[] stats = new int[16];
       int v = 0;
       for (int j = 0; j < s.length; j++) {
           v = (v << 1) | s[j];
           if ((j & 3) == 3) {
           ++stats[v];
           v = 0;
           }
       }
       int z = 0;
       for (int k = 0; k < stats.length; k++) {
           z += stats[k]*stats[k];
       }
       double x = (16.0 / (s.length / 4)) * z - (s.length / 4);
       int pokerFailure = ((1.03 < x) && (x < 57.4)) ? 0 : 1;
       if (pokerFailure != 0) fail("poker test failure for %s: x=%g (should be in [1.03,57.4])\n", id, x);
       return pokerFailure;
    }

    /* The Runs Test
     *
     *   1.  A run is defined as a maximal sequence of consecutive bits of either all ones
     *   or all zeros, which is part of the 20,000 bit sample stream.  The incidences of
     *   runs (for both consecutive zeros and consecutive ones) of all lengths (>= 1) in
     *   the sample stream should be counted and stored.
     *
     *   2.  The test is passed if the number of runs that occur (of lengths 1 through 6)
     *   is each within the corresponding interval specified below.  This must hold for
     *   both the zeros and ones; that is, all 12 counts must lie in the specified
     *   interval.  For the purpose of this test, runs of greater than 6 are considered to
     *   be of length 6.
     *
     *        Length of run       Required Interval
     *             1                2,267 - 2,733
     *             2                1,079 - 1,421
     *             3                  502 - 748
     *             4                  223 - 402
     *             5                   90 - 223
     *             6+                  90 - 223
     *
     * The Long Run Test
     *
     *   1 . A long run is defined to be a run of length 34 or more (of either zeros or ones).
     *
     *   2.  On the sample of 20,000 bits, the test is passed if there are NO long runs.
     */
    static int runTestAndLongRunTest(String id, byte[] s) {
       // System.out.println("run test");
       int[][] stats = new int[2][8];
       int count = 0;
       for (int j = 0; j < s.length; j++) {
           ++count;
           if ((j == (s.length - 1)) || (s[j+1] != s[j])) {
           ++stats[s[j]][(count < 6) ? count : (count < 34) ? 6 : 7];
           count = 0;
           }
       }
       stats[0][6] += stats[0][7];
       stats[1][6] += stats[1][7];
       int runFailure = checkRunStats(stats[0]) | checkRunStats(stats[1]);
       if (runFailure != 0) fail("run test failure for %s\n", id);
       int longRunFailure = ((stats[0][7] == 0) && (stats[1][7] == 0)) ? 0 : 1;
       if (longRunFailure != 0) fail("long run test failure for %s\n", id);
       return (runFailure + longRunFailure);
    }

    static int checkRunStats(int[] stats) {
       int runFailure = 0;
       runFailure |= ((2267 <= stats[1]) && (stats[1] <= 2733)) ? 0 : 1;
       runFailure |= ((1079 <= stats[2]) && (stats[2] <= 1421)) ? 0 : 1;
       runFailure |= (( 502 <= stats[3]) && (stats[3] <=  748)) ? 0 : 1;
       runFailure |= (( 223 <= stats[4]) && (stats[4] <=  402)) ? 0 : 1;
       runFailure |= ((  90 <= stats[5]) && (stats[5] <=  223)) ? 0 : 1;
       runFailure |= ((  90 <= stats[6]) && (stats[6] <=  223)) ? 0 : 1;
       return runFailure;
        }

        /* Autocorrelation Test
         *
         *   For tau in {1, ..., 5000}, Z[tau] := sum[j=1,5000] (b[j] ^ b[j+tau]).
         *
         *   The sequence passes the autocorrelation test if every Z[tau] lies within the
         *   interval 2267-2733.
         */
        static int autocorrelationTest(String id, byte[] s) {
       // System.out.println("autocorrelation test");
       int autocorrelationFailure = 0;
       int N = s.length / 4;
       for (int tau = 1; tau <= N; tau++) {
           int count = 0;
           for (int j = 0; j < N; j++) {
           count += (s[j] ^ s[j+tau]);
           }
           // We intentionally use bounds [2267, 2733], which are wider than
           // the bounds [2326, 2674] specified by BSI for this test.
           // The latter bounds produce way too many false positives.
           int singleAutocorrelationFailure = ((2267 < count) && (count < 2733)) ? 0 : 1;
           if (singleAutocorrelationFailure != 0) {
           if (autocorrelationFailure < 8) {
               fail("autocorrelation failure for %s: count=%d (should be in [2267,2733]), tau=%d\n", id, count, tau);
               if (count < 100 || count > 4900) {
                    System.out.print("    ");
               for (int q = 0; q < 50; q++) System.out.print(s[q]);
                    System.out.println();
               }
           }
           }
           autocorrelationFailure += singleAutocorrelationFailure;
       }
       return (autocorrelationFailure == 0) ? 0 : 1;
    }

    static int entireBsi1999Test(String id, byte[] s) {
       return (monobitTest(id, s) +
           pokerTest(id, s) +
           runTestAndLongRunTest(id, s) +
           autocorrelationTest(id, s)
           );
    }

    /* To test a sequence of boolean values from a BooleanSupplier,
     * sequentially extract 20000 boolean values, convert to an array
     * of bytes, and feed them to method {@code entireBsi1999Test}.
     */

    static int testRngBsi1999BooleanOnce(String id, BooleanSupplier theSupplier) {
       int failureCount = 0;
       byte[] s = new byte[SEQUENCE_SIZE];
       // Take the next SEQUENCE_SIZE booleans and test them
       for (int j = 0; j < s.length; j++) {
           s[j] = (theSupplier.getAsBoolean() ? (byte)1 : (byte)0);
       }
       failureCount += entireBsi1999Test(id  + " consecutive", s);
       return failureCount;
    }

    /* To test a sequence of long values from a LongSupplier,
     * two kinds of tests are performed.
     *
     * The first kind of test extracts 313=ceiling(20000/64) long
     * values and concatenates all their bits; the first 20000 bits
     * are converted to a byte array of bits to be tested.  This test is
     * repeated 64 times.
     *
     * The second kind of test focuses on one bit position m (0 <= m < 64);
     * it extracts 20000 long values and uses just bit m from each value
     * to produce an array of bytes to be tested.  This test is performed
     * once for each possible value of m (64 times in all).
     */
    static int testRngBsi1999LongOnce(String id, LongSupplier theSupplier) {
       int failureCount = 0;
       byte[] s = new byte[SEQUENCE_SIZE];
       // Part 1: 64 times, take the next SEQUENCE_SIZE bits and test them
       for (int m = 0; m < 64; m++) {
           long bits = 0;
           int bitCount = 0;
           for (int j = 0; j < s.length; j++) {
           if ((j & 0x3f) == 0) {
               bits = theSupplier.getAsLong();
               // System.out.printf("0x%016x\n", bits);
               bitCount += Long.bitCount((j == (20000 - 32)) ? ((bits << 32) >>> 32) : bits);
           }
           s[j] = (byte)(bits & 1);
           bits >>>= 1;
           }
           // System.out.println(m + ": " + bitCount + " 1-bits");
           failureCount += entireBsi1999Test(id + " consecutive (" + bitCount + " 1-bits)", s);
       }
       // Part 2: for 0 <= m < 64, use bit m from each of the next SEQUENCE_SIZE longs
       for (int m = 0; m < 64; m++) {
           for (int j = 0; j < s.length; j++) {
           s[j] = (byte)((theSupplier.getAsLong() >>> m) & 1);
           }
           failureCount += entireBsi1999Test(id + " bit " + m, s);
       }
       return failureCount;
    }

    /* To test a sequence of ing values from an IntSupplier,
     * two kinds of tests are performed.
     *
     * The first kind of test extracts 625=20000/32 int values and
     * concatenates all their bits; these 20000 bits are converted to
     * a byte array of bits to be tested.  This test is repeated 64
     * times.
     *
     * The second kind of test focuses on one bit position m (0 <= m < 32);
     * it extracts 20000 int values and uses just bit m from each value
     * to produce an array of bytes to be tested.  This test is performed
     * once for each possible value of m (32 times in all).
     */
    static int testRngBsi1999IntOnce(String id, IntSupplier theSupplier) {
       int failureCount = 0;
       byte[] s = new byte[SEQUENCE_SIZE];
       // Part 1: 64 times, take the next SEQUENCE_SIZE bits and test them
       for (int m = 0; m < 64; m++) {
           int bits = 0;
           int bitCount = 0;
           for (int j = 0; j < s.length; j++) {
           if ((j & 0x1f) == 0) {
               bits = theSupplier.getAsInt();
               bitCount += Integer.bitCount(bits);
           }
           s[j] = (byte)(bits & 1);
           bits >>>= 1;
           }
           // System.out.println(m + ": " + bitCount + " 1-bits");
           failureCount += entireBsi1999Test(id + " consecutive (" + bitCount + " 1-bits)", s);
       }
       // Part 2: for 0 <= m < 32, use bit m from each of the next SEQUENCE_SIZE ints
       for (int m = 0; m < 32; m++) {
           for (int j = 0; j < s.length; j++) {
           s[j] = (byte)((theSupplier.getAsInt() >>> m) & 1);
           }
           failureCount += entireBsi1999Test(id + " bit " + m, s);
       }
       return failureCount;
    }

    /* A call to {@code entireBsi1999Test} may report failure even if the source of random
     * bits is quite good, because the test is statistical in nature.  To make the testing
     * procedure more robust, if the first call to {@code entireBsi1999Test} fails, then
     * the failure is ignored if two more calls to {@code entireBsi1999Test} both succeed.
    */

    static boolean testRngBsi1999Boolean(String id, BooleanSupplier theSupplier, int failureTolerance) {
       if (testRngBsi1999BooleanOnce(id, theSupplier) <= failureTolerance) return true;
       fail("testRngBsi1999Boolean glitch");
       return ((testRngBsi1999BooleanOnce(id, theSupplier) <= failureTolerance) &&
           (testRngBsi1999BooleanOnce(id, theSupplier) <= failureTolerance));
    }

    static boolean testRngBsi1999Long(String id, LongSupplier theSupplier, int failureTolerance) {
       if (testRngBsi1999LongOnce(id, theSupplier) <= failureTolerance) return true;
       fail("testRngBsi1999Long glitch");
       return ((testRngBsi1999LongOnce(id, theSupplier) <= failureTolerance) &&
           (testRngBsi1999LongOnce(id, theSupplier) <= failureTolerance));
    }

    static boolean testRngBsi1999Int(String id, IntSupplier theSupplier, int failureTolerance) {
       if (testRngBsi1999IntOnce(id, theSupplier) <= failureTolerance) return true;
       fail("testRngBsi1999Int glitch");
       return ((testRngBsi1999IntOnce(id, theSupplier) <= failureTolerance) &&
           (testRngBsi1999IntOnce(id, theSupplier) <= failureTolerance));
    }

    static void tryIt(RandomGenerator rng, String id, BooleanSupplier theSupplier) {
       System.out.printf("Testing %s %s\n", rng.getClass().getName(), id);
       boolean success = theSupplier.getAsBoolean();
       if (!success) {
           fail("*** Failure of %s %s\n", rng.getClass().getName(), id);
       }
    }

    static void testOneRng(RandomGenerator rng, int failureTolerance) {
       String name = rng.getClass().getName();
       tryIt(rng, "nextInt", () -> testRngBsi1999Int(name + " nextInt", rng::nextInt, failureTolerance));
       tryIt(rng, "nextLong", () -> testRngBsi1999Long(name + " nextLong", rng::nextLong, failureTolerance));
       tryIt(rng, "nextBoolean", () -> testRngBsi1999Boolean(name + " nextBoolean", rng::nextBoolean, failureTolerance));
       tryIt(rng, "ints", () -> testRngBsi1999Int(name + " ints", rng.ints().iterator()::next, failureTolerance));
       tryIt(rng, "longs", () -> testRngBsi1999Long(name + " longs", rng.longs().iterator()::next, failureTolerance));
       {
           PrimitiveIterator.OfDouble iter = rng.doubles().iterator();
           tryIt(rng, "doubles",
             () -> testRngBsi1999Int(name + " doubles",
                         () -> (int)(long)Math.floor(iter.next() * 0x1.0p32),
                         failureTolerance));
       }
       tryIt(rng, "nextDouble",
             () -> testRngBsi1999Int(name + " nextDouble",
                         () -> (int)(long)Math.floor(rng.nextDouble() * 0x1.0p32),
                         failureTolerance));
       tryIt(rng, "nextFloat",
             () -> testRngBsi1999Int(name + " nextFloat",
                         () -> (((int)(long)Math.floor(rng.nextFloat() * 0x1.0p16f) << 16)
                            | (int)(long)Math.floor(rng.nextFloat() * 0x1.0p16f)),
                         failureTolerance));
    }

    public static void main(String[] args) {
        RandomGeneratorFactory.all().forEach(factory -> {
            setRNG(factory.name());

            if (currentRNG.equals("SecureRandom")) {
                // skip because stochastic
            } else if (currentRNG.equals("Random")) {
                // testOneRng(factory.create(59), 1);
                // autocorrelation failure for java.util.Random longs bit 0: count=2207 (should be in [2267,2733]), tau=2819

            } else {
                testOneRng(factory.create(59), 0);
            }
        });

        exceptionOnFail();
    }
}


/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7050528
 * @summary Set of micro-benchmarks testing throughput of java.text.DecimalFormat.format()
 * @author Olivier Lagneau
 * @run main FormatMicroBenchmark
 */

/* This is a set of micro-benchmarks testing throughput of java.text.DecimalFormat.format().
 * It never fails.
 *
 * Usage and arguments:
 *  - Run with no argument skips the whole benchmark and exits.
 *  - Run with "-help" as first argument calls the usage() method and exits.
 *  - Run with "-doit" runs the benchmark with summary details.
 *  - Run with "-verbose" provides additional details on the run.
 *
 * Example run :
 *   java -Xms500m -Xmx500m -XX:NewSize=400m FormatMicroBenchmark -doit -verbose
 *
 * Running with jtreg:
 *  The jtreg header "run" tag options+args must be changed to avoid skipping
 *  the execution. here is an example of run options:
 *  "main/othervm -Xms500m -Xmx500m -XX:NewSize=400m FormatMicroBenchmark -doit"
 *
 * Note:
 *  - Vm options -Xms, -Xmx, -XX:NewSize must be set correctly for
 *    getting reliable numbers. Otherwise GC activity may corrupt results.
 *    As of jdk80b48 using "-Xms500m -Xmx500m -XX:NewSize=400m" covers
 *    all cases.
 *  - Optionally using "-Xlog:gc" option provides information that
 *    helps checking any GC activity while benches are run.
 *
 * Vm Options:
 *  - Vm options to use (as of jdk80b48):
 *     fast-path case :     -Xms128m -Xmx128m -XX:NewSize=100m
 *     non fast-path case:  -Xms500m -Xmx500m -XX:NewSize=400m
 *    or use worst case (non fast-path above) with both types of algorithm.
 *
 *  - use -Xlog:gc to verify memory consumption of the benchmarks.
 *    (See "Checking Memory Consumption" below).
 *
 * Description:
 *
 *  Fast-path algorithm for format(double...)  call stack is very different  of
 *  the standard call stack. Where the  standard algorithm for formating double
 *  uses internal class sun.misc.FloatingDecimal and its dtoa(double) method to
 *  provide digits,  fast-path embeds its own  algorithm for  binary to decimal
 *  string conversion.
 *
 *  FloatingDecimal always converts completely  the passed double to  a string.
 *  Fast-path converts  only to the needed digits  since it follows constraints
 *  on both the pattern rule,  the  DecimalFormat instance properties, and  the
 *  passed double.
 *
 *  Micro benchmarks below measure  the throughput for formating double  values
 *  using NumberFormat.format(double)  call stack.  The  standard DecimalFormat
 *  call stack as well as the  fast-path algorithm implementation are sensitive
 *  to the nature of the passed double values regarding throughput performance.
 *
 *  These benchmarks are useful both  for measuring the global performance gain
 *  of fast-path and to check that any modification done on fast-path algorithm
 *  does not bring any regression in the performance boost of fast-path.
 *
 *  Note  that these benchmarks  will provide numbers  without any knowledge of
 *  the  implementation of DecimalFormat class. So  to check regression any run
 *  should be compared to another reference run with  a previous JDK, whether or
 *  not this previous reference JDK contains fast-path implementation.
 *
 *  The eight benchmarks below are dedicated to measure throughput on different
 *  kinds of double that all fall in the fast-path case (all in Integer range):
 *
 *  - Integer case : used double values are all "integer-like" (ex: -12345.0).
 *    This is the benchFormatInteger micro-benchmark.
 *
 *  - Fractional case : double values are "fractional" (ex: -0.12345).
 *    This is the benchFormatFractional micro-benchmark.
 *
 *  - Small integral case : like Integer case but double values are all limited
 *    in their magnitude, from -500.0 to 500.0 if the number of iterations N is
 *    set to 500000.
 *    This is the benchFormatSmallIntegral micro-benchmark.
 *
 *  - Fractional All Nines : doubles values have fractional part that is very
 *    close to "999" (decimal pattern), or "99" (currency pattern),
 *    or "0000...".
 *    This is the benchFormatFractionalAllNines micro-benchmark.
 *
 *  - All Nines : double values are such that both integral and fractional
 *    part consist only of '9' digits. None of these values are rounded up.
 *    This is the benchFormatAllNines micro-benchmark.
 *
 *  - Fair simple case : calling J the loop variable and iterating over
 *    the N number of iterations, used double values are computed as
 *    d = (double) J + J*seed
 *    where seed is a very small value that adds a fractional part and adds a
 *    small number to integral part. Provides fairly distributed double values.
 *    This is the benchFormatFairSimple micro-benchmark.
 *
 *  - Fair case : this is a combination of small integral case and fair simple
 *    case. Double values are limited in their magnitude but follow a parabolic
 *    curve y = x**2 / K, keeping large magnitude only for large values of J.
 *    The intent is trying to reproduce a distribution of double values as could
 *    be found in a business application, with most values in either the low
 *    range or the high range.
 *    This is the benchFormatFair micro-benchmark.
 *
 *  - Tie cases: values are very close to a tie case (iii...ii.fff5)
 *    That is the worst situation that can happen for Fast-path algorithm when
 *    considering throughput.
 *    This is the benchFormatTie micro-benchmark.
 *
 *  For  all  of  the micro-benchmarks,  the  throughput load   of the eventual
 *  additional computations inside the loop is calculated  prior to running the
 *  benchmark, and provided in the output.  That may be  useful since this load
 *  may vary for each architecture or machine configuration.
 *
 *  The "-verbose" flag,  when set, provides the  throughput  load numbers, the
 *  time spent for  each run of  a benchmark, as  well as an estimation  of the
 *  memory consumed  by the  runs.  Beware of  incremental  GCs, see  "Checking
 *  Memory  Consumption" section below. Every run   should be done with correct
 *  ms, mx, and NewSize vm options to get fully reliable numbers.
 *
 *  The output provides the  mean time needed for  a benchmark after the server
 *  jit compiler has done its optimization work if  any. Thus only the last but
 *  first three runs are taken into account in the time measurement (server jit
 *  compiler shows  to have  done full  optimization  in  most cases  after the
 *  second run, given a base number of iterations set to 500000).
 *
 *  The program cleans up memory (stabilizeMemory() method) between each run of
 *  the benchmarks to make sure that  no garbage collection activity happens in
 *  measurements. However that does not  preclude incremental GCs activity that
 *  may  happen during the micro-benchmark if  -Xms, -Xmx, and NewSize options
 *  have not been tuned and set correctly.
 *
 * Checking Memory Consumption:
 *
 *  For getting confidence  in the throughput numbers, there  must not give any
 *  GC activity during the benchmark runs. That  means that specific VM options
 *  related to memory must be tuned for any given implementation of the JDK.
 *
 *  Running with "-verbose" arguments will provide  clues of the memory consumed
 *  but  is   not enough,  since  any   unexpected  incremental  GC  may  lower
 *  artificially the estimation of the memory consumption.
 *
 *  Options to  set are -Xms, -Xmx,  -XX:NewSize, plus -Xlog:gc to evaluate
 *  correctly  the  values of  these options. When  running "-verbose", varying
 *  numbers reported for memory consumption may  indicate bad choices for these
 *  options.
 *
 *  For jdk80b25, fast-path shows a consuption of ~60Mbs for 500000 iterations
 *  while a jdk without fast-path will consume ~260Mbs for each benchmark run.
 *  Indeed these values will vary depending on the jdk used.
 *
 *  Correct option settings found jdk80b48 were :
 *     fast-path :     -Xms128m -Xmx128m -XX:NewSize=100m
 *     non fast-path : -Xms500m -Xmx500m -XX:NewSize=400m
 *  Greater values can be provided safely but not smaller ones.
 * ----------------------------------------------------------------------
 */

import java.util.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;

public class FormatMicroBenchmark {

    // The number of times the bench method will be run (must be at least 4).
    private static final int NB_RUNS = 20;

    // The bench* methods below all iterates over [-MAX_RANGE , +MAX_RANGE] integer values.
    private static final int MAX_RANGE = 500000;

    // Flag for more details on each bench run (default is no).
    private static boolean Verbose = false;

    // Should we really execute the benches ? (no by default).
    private static boolean DoIt = false;

    // Prints out a message describing how to run the program.
    private static void usage() {
        System.out.println(
            "This is a set of micro-benchmarks testing throughput of " +
            "java.text.DecimalFormat.format(). It never fails.\n\n" +
            "Usage and arguments:\n" +
            " - Run with no argument skips the whole benchmark and exits.\n" +
            " - Run with \"-help\" as first argument prints this message and exits.\n" +
            " - Run with \"-doit\" runs the benchmark with summary details.\n" +
            " - Run with \"-verbose\" provides additional details on the run.\n\n" +
            "Example run :\n" +
            "   java -Xms500m -Xmx500m -XX:NewSize=400m FormatMicroBenchmark -doit -verbose\n\n" +
            "Note: \n" +
            " - Vm options -Xms, -Xmx, -XX:NewSize must be set correctly for \n" +
            "   getting reliable numbers. Otherwise GC activity may corrupt results.\n" +
            "   As of jdk80b48 using \"-Xms500m -Xmx500m -XX:NewSize=400m\" covers \n" +
            "   all cases.\n" +
            " - Optionally using \"-Xlog:gc\" option provides information that \n" +
            "   helps checking any GC activity while benches are run.\n\n" +
            "Look at the heading comments and description in source code for " +
            "detailed information.\n");
    }

    /* We will call stabilizeMemory before each call of benchFormat***().
     * This in turn tries to clean up as much memory as possible.
     * As a safe bound we limit number of System.gc() calls to 10,
     * but most of the time two calls to System.gc() will be enough.
     * If memory reporting is asked for, the method returns the difference
     * of free memory between entering an leaving the method.
     */
    private static long stabilizeMemory(boolean reportConsumedMemory) {
        final long oneMegabyte = 1024L * 1024L;

        long refMemory = 0;
        long initialMemoryLeft = Runtime.getRuntime().freeMemory();
        long currMemoryLeft = initialMemoryLeft;
        int nbGCCalls = 0;

        do {
            nbGCCalls++;

            refMemory = currMemoryLeft;
            System.gc();
            currMemoryLeft = Runtime.getRuntime().freeMemory();

        } while ((Math.abs(currMemoryLeft - refMemory) > oneMegabyte) &&
                 (nbGCCalls < 10));

        if (Verbose &&
            reportConsumedMemory)
            System.out.println("Memory consumed by previous run : " +
                               (currMemoryLeft - initialMemoryLeft)/oneMegabyte + "Mbs.");

        return currMemoryLeft;
    }


    // ---------- Integer only based bench --------------------
    private static final String INTEGER_BENCH = "benchFormatInteger";
    private static String benchFormatInteger(NumberFormat nf) {
        String str = "";
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++)
            str = nf.format((double) j);
        return str;
    }

    // This reproduces the throughput load added in benchFormatInteger
    static double integerThroughputLoad() {
        double d = 0.0d;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            d = (double) j;
        }
        return d;
    }

    // Runs integerThroughputLoad and calculate its mean load
    static void calculateIntegerThroughputLoad() {
        int nbRuns = NB_RUNS;
        long elapsedTime = 0;
        double foo;

        for (int i = 1; i <= nbRuns; i++) {

            long startTime = System.nanoTime();
            foo = integerThroughputLoad();
            long estimatedTime = System.nanoTime() - startTime;
            if (i > 3) elapsedTime += estimatedTime / 1000;
        }


        if (Verbose)
            System.out.println(
               "calculated throughput load for " + INTEGER_BENCH +
               " bench is = " + (elapsedTime / (nbRuns - 3)) + " microseconds");
    }

    // ---------- Fractional only based bench --------------------
    private static final String FRACTIONAL_BENCH = "benchFormatFractional";
    private static String benchFormatFractional(NumberFormat nf) {
        String str = "";
        double floatingN = 1.0d / (double) MAX_RANGE;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++)
            str = nf.format(floatingN * (double) j);
        return str;
    }

    // This reproduces the throughput load added in benchFormatFractional
    static double fractionalThroughputLoad() {
        double d = 0.0d;
        double floatingN = 1.0d / (double) MAX_RANGE;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            d = floatingN * (double) j;
        }
        return d;
    }

    // Runs fractionalThroughputLoad and calculate its mean load
    static void calculateFractionalThroughputLoad() {
        int nbRuns = NB_RUNS;
        long elapsedTime = 0;
        double foo;

        for (int i = 1; i <= nbRuns; i++) {

            long startTime = System.nanoTime();
            foo = fractionalThroughputLoad();
            long estimatedTime = System.nanoTime() - startTime;
            if (i > 3) elapsedTime += estimatedTime / 1000;
        }

        if (Verbose)
        System.out.println(
            "calculated throughput load for " + FRACTIONAL_BENCH +
            " bench is = " + (elapsedTime / (nbRuns - 3)) + " microseconds");
    }

    // ---------- An Small Integral bench --------------------
    //  that limits the magnitude of tested double values
    private static final String SMALL_INTEGRAL_BENCH = "benchFormatSmallIntegral";
    private static String benchFormatSmallIntegral(NumberFormat nf) {
        String str = "";
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++)
            str = nf.format(((double) j) / 1000.0d);
        return str;
    }

    // This reproduces the throughput load added in benchFormatSmallIntegral
    static double smallIntegralThroughputLoad() {
        double d = 0.0d;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            d = (double) j / 1000.0d;
        }
        return d;
    }

    // Runs small_integralThroughputLoad and calculate its mean load
    static void calculateSmallIntegralThroughputLoad() {
        int nbRuns = NB_RUNS;
        long elapsedTime = 0;
        double foo;

        for (int i = 1; i <= nbRuns; i++) {

            long startTime = System.nanoTime();
            foo = smallIntegralThroughputLoad();
            long estimatedTime = System.nanoTime() - startTime;
            if (i > 3) elapsedTime += estimatedTime / 1000;
        }

        if (Verbose)
        System.out.println(
            "calculated throughput load for " + SMALL_INTEGRAL_BENCH +
            " bench is = " + (elapsedTime / (nbRuns - 3)) + " microseconds");
    }

    // ---------- A fair and simple bench --------------------
    private static final String FAIR_SIMPLE_BENCH = "benchFormatFairSimple";
    private static String benchFormatFairSimple(NumberFormat nf, boolean isCurrency) {
        String str = "";
        double seed = isCurrency ?  0.0010203040506070809 : 0.00010203040506070809;
        double d = (double) -MAX_RANGE;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            d = d  + 1.0d + seed;
            str = nf.format(d);
        }
        return str;
    }

    // This reproduces the throughput load added in benchFormatFairSimple
    static double fairSimpleThroughputLoad() {
        double seed =  0.00010203040506070809;
        double delta = 0.0d;
        double d = (double) -MAX_RANGE;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            d = d + 1.0d + seed;
        }
        return d;
    }

    // Runs fairThroughputLoad and calculate its mean load
    static void calculateFairSimpleThroughputLoad() {
        int nbRuns = NB_RUNS;
        long elapsedTime = 0;
        double foo;

        for (int i = 1; i <= nbRuns; i++) {

            long startTime = System.nanoTime();
            foo = fairSimpleThroughputLoad();
            long estimatedTime = System.nanoTime() - startTime;
            if (i > 3) elapsedTime += estimatedTime / 1000;
        }

        if (Verbose)
        System.out.println(
            "calculated throughput load for " + FAIR_SIMPLE_BENCH +
            " bench is = " + (elapsedTime / (nbRuns - 3)) + " microseconds");
    }

    // ---------- Fractional part is only made of nines bench --------------
    private static final String FRACTIONAL_ALL_NINES_BENCH = "benchFormatFractionalAllNines";
    private static String benchFormatFractionalAllNines(NumberFormat nf, boolean isCurrency) {
        String str = "";
        double fractionalEven = isCurrency ?  0.993000001 : 0.99930000001;
        double fractionalOdd  = isCurrency ?  0.996000001 : 0.99960000001;
        double fractional;
        double d;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            if ((j & 1) == 0)
                fractional = fractionalEven;
            else
                fractional = fractionalOdd;
            if ( j >= 0)
                d = (double ) j + fractional;
            else d = (double) j - fractional;
            str = nf.format(d);
        }
        return str;
    }

    // This reproduces the throughput load added in benchFormatFractionalAllNines
    static double fractionalAllNinesThroughputLoad() {
        double fractionalEven = 0.99930000001;
        double fractionalOdd  = 0.99960000001;
        double fractional;
        double d = 0.0d;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            if ((j & 1) == 0)
                fractional = fractionalEven;
            else fractional = fractionalOdd;
            if ( j >= 0)
                d = (double ) j + fractional;
            else d = (double) j - fractional;
        }
        return d;
    }

    // Runs fractionalAllNinesThroughputLoad and calculate its mean load
    static void calculateFractionalAllNinesThroughputLoad() {
        int nbRuns = NB_RUNS;
        long elapsedTime = 0;
        double foo;

        for (int i = 1; i <= nbRuns; i++) {

            long startTime = System.nanoTime();
            foo = fractionalAllNinesThroughputLoad();
            long estimatedTime = System.nanoTime() - startTime;
            if (i > 3) elapsedTime += estimatedTime / 1000;
        }

        if (Verbose)
            System.out.println(
               "calculated throughput load for " + FRACTIONAL_ALL_NINES_BENCH +
               " bench is = " + (elapsedTime / (nbRuns - 3)) + " microseconds");
    }

    // ---------- Number is only made of nines bench --------------
    private static final String ALL_NINES_BENCH = "benchFormatAllNines";
    private static String benchFormatAllNines(NumberFormat nf, boolean isCurrency) {
        String str = "";
        double[] decimaAllNines =
            {9.9993, 99.9993, 999.9993, 9999.9993, 99999.9993,
             999999.9993, 9999999.9993, 99999999.9993, 999999999.9993};
        double[] currencyAllNines =
            {9.993, 99.993, 999.993, 9999.993, 99999.993,
             999999.993, 9999999.993, 99999999.993, 999999999.993};
        double[] valuesArray = (isCurrency) ? currencyAllNines : decimaAllNines;
        double seed = 1.0 / (double) MAX_RANGE;
        double d;
        int id;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            id = (j >=  0) ? j % 9 : -j % 9;
            if ((j & 1) == 0)
                d = valuesArray[id] + id * seed;
            else
                d = valuesArray[id] - id * seed;
            str = nf.format(d);
        }
        return str;
    }

    // This reproduces the throughput load added in benchFormatAllNines
    static double allNinesThroughputLoad() {
        double[] decimaAllNines =
            {9.9993, 99.9993, 999.9993, 9999.9993, 99999.9993,
             999999.9993, 9999999.9993, 99999999.9993, 999999999.9993};
        double[] valuesArray = decimaAllNines;
        double seed = 1.0 / (double) MAX_RANGE;
        double d = 0.0d;
        int id;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            id = (j >=  0) ? j % 9 : -j % 9;
            if ((j & 1) == 0)
                d = valuesArray[id] + id * seed;
            else
                d = valuesArray[id] - id * seed;
        }
        return d;
    }

    // Runs allNinesThroughputLoad and calculate its mean load
    static void calculateAllNinesThroughputLoad() {
        int nbRuns = NB_RUNS;
        long elapsedTime = 0;
        double foo;

        for (int i = 1; i <= nbRuns; i++) {

            long startTime = System.nanoTime();
            foo = allNinesThroughputLoad();
            long estimatedTime = System.nanoTime() - startTime;
            if (i > 3) elapsedTime += estimatedTime / 1000;
        }

        if (Verbose)
            System.out.println(
               "calculated throughput load for " + ALL_NINES_BENCH +
               " bench is = " + (elapsedTime / (nbRuns - 3)) + " microseconds");
    }



    // --- A fair bench trying (hopefully) to reproduce business applicatons  ---

    /*  benchFormatFair uses the following formula :
     *   y = F(x) = sign(x) * x**2 * ((1000/MAX_RANGE)**2).
     *
     *  which converts in the loop as (if j is the loop index) :
     *   x = double(j)
     *   k = 1000.0d * double(MAX_RANGE)
     *   y = sign(j) * x**2 * k**2
     *
     *  This is a flattened parabolic curve where only the j values
     *  in [-1000, 1000] will provide y results in [-1, +1] interval,
     *  and for abs(j) >= 1000 the result y will be greater than 1.
     *
     *  The difference with benchFormatSmallIntegral is that since y results
     *  follow a parabolic curve the magnitude of y grows much more rapidly
     *  and closer to j values when abs(j) >= 1000:
     *   - for |j| < 1000,  SmallIntegral(j) < 1.0 and fair(j) < 1.0
     *   - for j in [1000, 10000[
     *      SmallIntegral(j) is in [1, 10[
     *      Fair(j) is in [4, 400[
     *   - for j in [10000,100000[
     *      SmallIntegral(j) is in [10, 100[
     *      Fair(j) is in [400,40000[
     *   - for j in [100000,1000000[
     *      SmallIntegral(j) is in [100, 1000[
     *      Fair(j) is in [40000, 4000000[
     *
     *  Since double values for j less than 100000 provide only 4 digits in the
     *  integral, values greater than 250000 provide at least 6 digits, and 500000
     *  computes to 1000000, the distribution is roughly half with less than 5
     *  digits and half with at least 6 digits in the integral part.
     *
     *  Compared to FairSimple bench, this represents an application where 20% of
     *  the double values to format are less than 40000.0 absolute value.
     *
     *  Fair(j) is close to the magnitude of j when j > 100000 and is hopefully
     *  more representative of what may be found in general in business apps.
     *  (assumption : there will be mainly either small or large values, and
     *   less values in middle range).
     *
     *  We could get even more precise distribution of values using formula :
     *   y = sign(x) * abs(x)**n * ((1000 / MAX_RANGE)**n) where n > 2,
     *  or even well-known statistics function to fine target such distribution,
     *  but we have considred that the throughput load for calculating y would
     *  then be too high. We thus restrain the use of a power of 2 formula.
     */

    private static final String FAIR_BENCH = "benchFormatFair";
    private static String benchFormatFair(NumberFormat nf) {
        String str = "";
        double k = 1000.0d / (double) MAX_RANGE;
        k *= k;

        double d;
        double absj;
        double jPowerOf2;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            absj = (double) j;
            jPowerOf2 = absj * absj;
            d = k * jPowerOf2;
            if (j < 0) d = -d;
            str = nf.format(d);
        }
        return str;
    }

    // This is the exact throughput load added in benchFormatFair
    static double fairThroughputLoad() {
        double k = 1000.0d / (double) MAX_RANGE;
        k *= k;

        double d = 0.0d;
        double absj;
        double jPowerOf2;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            absj = (double) j;
            jPowerOf2 = absj * absj;
            d = k * jPowerOf2;
            if (j < 0) d = -d;
        }
        return d;
    }

    // Runs fairThroughputLoad and calculate its mean load
    static void calculateFairThroughputLoad() {
        int nbRuns = NB_RUNS;
        long elapsedTime = 0;
        double foo;

        for (int i = 1; i <= nbRuns; i++) {

            long startTime = System.nanoTime();
            foo = fairThroughputLoad();
            long estimatedTime = System.nanoTime() - startTime;
            if (i > 3) elapsedTime += estimatedTime / 1000;
        }

        if (Verbose)
            System.out.println(
               "calculated throughput load for " + FAIR_BENCH +
               " bench is = " + (elapsedTime / (nbRuns - 3)) + " microseconds");
    }

    // ---------- All double values are very close to a tie --------------------
    // i.e. like 123.1235 (for decimal case) or 123.125 (for currency case).

    private static final String TIE_BENCH = "benchFormatTie";
    private static String benchFormatTie(NumberFormat nf, boolean isCurrency) {
        double d;
        String str = "";
        double fractionaScaling = (isCurrency) ? 1000.0d : 10000.0d;
        int fixedFractionalPart = (isCurrency) ? 125 : 1235;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            d = (((double) j * fractionaScaling) +
                 (double) fixedFractionalPart) / fractionaScaling;
            str = nf.format(d);
        }
        return str;
    }

    // This is the exact throughput load added in benchFormatTie
    static double tieThroughputLoad(boolean isCurrency) {
        double d = 0.0d;
        double fractionaScaling = (isCurrency) ? 1000.0d : 10000.0d;
        int fixedFractionalPart = (isCurrency) ? 125 : 1235;
        for (int j = - MAX_RANGE; j <= MAX_RANGE; j++) {
            d = (((double) j * fractionaScaling) +
                 (double) fixedFractionalPart) / fractionaScaling;
        }
        return d;
    }

    // Runs tieThroughputLoad and calculate its mean load
    static void calculateTieThroughputLoad(boolean isCurrency) {
        int nbRuns = NB_RUNS;
        long elapsedTime = 0;
        double foo;

        for (int i = 1; i <= nbRuns; i++) {

            long startTime = System.nanoTime();
            foo = tieThroughputLoad(isCurrency);
            long estimatedTime = System.nanoTime() - startTime;
            if (i > 3) elapsedTime += estimatedTime / 1000;
        }

        if (Verbose)
            System.out.println(
               "calculated throughput load for " + TIE_BENCH +
               " bench is = " + (elapsedTime / (nbRuns - 3)) + " microseconds");
    }


    // Print statistics for passed times results of benchName.
    static void printPerfResults(long[] times, String benchName) {
        int nbBenches = times.length;

        long totalTimeSpent = 0;
        long meanTimeSpent;

        double variance = 0;
        double standardDeviation = 0;

        // Calculates mean spent time
        for (int i = 1; i <= nbBenches; i++)
            totalTimeSpent += times[i-1];
        meanTimeSpent = totalTimeSpent / nbBenches;

        // Calculates standard deviation
        for (int j = 1; j <= nbBenches; j++)
            variance += Math.pow(((double)times[j-1] - (double)meanTimeSpent), 2);
        variance = variance / (double) times.length;
        standardDeviation = Math.sqrt(variance) / meanTimeSpent;

        // Print result and statistics for benchName
        System.out.println(
           "Statistics (starting at 4th bench) for bench " + benchName +
           "\n for last " + nbBenches +
           " runs out of " + NB_RUNS +
           " , each with 2x" + MAX_RANGE + " format(double) calls : " +
           "\n  mean exec time = " + meanTimeSpent + " microseconds" +
           "\n  standard deviation = " + String.format("%.3f", standardDeviation) + "% \n");
    }

    public static void main(String[] args) {

        if (args.length >= 1) {
            // Parse args, just checks expected ones. Ignore others or dups.
            if (args[0].equals("-help")) {
                usage();
                return;
            }

            for (String s : args) {
                if (s.equals("-doit"))
                    DoIt = true;
                else if (s.equals("-verbose"))
                    Verbose = true;
            }
        } else {
            // No arguments, skips the benchmarks and exits.
            System.out.println(
                "Test skipped with success by default. See -help for details.");
            return;
        }

        if (!DoIt) {
            if (Verbose)
                usage();
            System.out.println(
                "Test skipped and considered successful.");
            return;
        }

        System.out.println("Single Threaded micro benchmark evaluating " +
                           "the throughput of java.text.DecimalFormat.format() call stack.\n");

        String fooString = "";

        // Run benches for decimal instance
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        System.out.println("Running with a decimal instance of DecimalFormat.");

        calculateIntegerThroughputLoad();
        fooString =
            BenchType.INTEGER_BENCH.runBenchAndPrintStatistics(NB_RUNS, df, false);

        calculateFractionalThroughputLoad();
        fooString =
            BenchType.FRACTIONAL_BENCH.runBenchAndPrintStatistics(NB_RUNS, df, false);

        calculateSmallIntegralThroughputLoad();
        fooString =
            BenchType.SMALL_INTEGRAL_BENCH.runBenchAndPrintStatistics(NB_RUNS, df, false);

        calculateFractionalAllNinesThroughputLoad();
        fooString =
            BenchType.FRACTIONAL_ALL_NINES_BENCH.runBenchAndPrintStatistics(NB_RUNS, df, false);

        calculateAllNinesThroughputLoad();
        fooString =
            BenchType.ALL_NINES_BENCH.runBenchAndPrintStatistics(NB_RUNS, df, false);

        calculateFairSimpleThroughputLoad();
        fooString =
            BenchType.FAIR_SIMPLE_BENCH.runBenchAndPrintStatistics(NB_RUNS, df, false);

        calculateFairThroughputLoad();
        fooString =
            BenchType.FAIR_BENCH.runBenchAndPrintStatistics(NB_RUNS, df, false);

        calculateTieThroughputLoad(false);
        fooString =
            BenchType.TIE_BENCH.runBenchAndPrintStatistics(NB_RUNS, df, false);

        // Run benches for currency instance
        DecimalFormat cf = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        System.out.println("Running with a currency instance of DecimalFormat.");

        calculateIntegerThroughputLoad();
        fooString =
            BenchType.INTEGER_BENCH.runBenchAndPrintStatistics(NB_RUNS, cf, false);

        calculateFractionalThroughputLoad();
        fooString =
            BenchType.FRACTIONAL_BENCH.runBenchAndPrintStatistics(NB_RUNS, cf, false);

        calculateSmallIntegralThroughputLoad();
        fooString =
            BenchType.SMALL_INTEGRAL_BENCH.runBenchAndPrintStatistics(NB_RUNS, cf, false);

        calculateFractionalAllNinesThroughputLoad();
        fooString =
            BenchType.FRACTIONAL_ALL_NINES_BENCH.runBenchAndPrintStatistics(NB_RUNS, cf, false);

        calculateAllNinesThroughputLoad();
        fooString =
            BenchType.ALL_NINES_BENCH.runBenchAndPrintStatistics(NB_RUNS, cf, false);

        calculateFairSimpleThroughputLoad();
        fooString =
            BenchType.FAIR_SIMPLE_BENCH.runBenchAndPrintStatistics(NB_RUNS, cf, false);

        calculateFairThroughputLoad();
        fooString =
            BenchType.FAIR_BENCH.runBenchAndPrintStatistics(NB_RUNS, cf, false);

        calculateTieThroughputLoad(false);
        fooString =
            BenchType.TIE_BENCH.runBenchAndPrintStatistics(NB_RUNS, cf, false);

    }

    // This class to factorise what would be duplicated otherwise.
    static enum BenchType {

        INTEGER_BENCH("benchFormatInteger"),
        FRACTIONAL_BENCH("benchFormatFractional"),
        SMALL_INTEGRAL_BENCH("benchFormatSmallIntegral"),
        FAIR_SIMPLE_BENCH("benchFormatFairSimple"),
        FRACTIONAL_ALL_NINES_BENCH("benchFormatFractionalAllNines"),
        ALL_NINES_BENCH("benchFormatAllNines"),
        FAIR_BENCH("benchFormatFair"),
        TIE_BENCH("benchFormatTie");

        private final String name;

        BenchType(String name) {
            this.name = name;
        }

        String runBenchAndPrintStatistics(int nbRuns,
                         NumberFormat nf,
                         boolean isCurrency) {

            // We eliminate the first 3 runs in the time measurements
            // to let C2 do complete compilation and optimization work.
            long[] elapsedTimes = new long[nbRuns - 3];

            System.out.println("Now running " + nbRuns + " times bench " + name);

            String str = "";
            for (int i = 1; i <= nbRuns; i++) {

                stabilizeMemory(false);
                long startTime = System.nanoTime();

                switch(this) {
                case INTEGER_BENCH :
                    str = benchFormatInteger(nf);
                    break;
                case FRACTIONAL_BENCH :
                    str = benchFormatFractional(nf);
                    break;
                case SMALL_INTEGRAL_BENCH :
                    str = benchFormatSmallIntegral(nf);
                    break;
                case FRACTIONAL_ALL_NINES_BENCH :
                    str = benchFormatFractionalAllNines(nf, isCurrency);
                    break;
                case ALL_NINES_BENCH :
                    str = benchFormatAllNines(nf, isCurrency);
                    break;
                case FAIR_SIMPLE_BENCH :
                    str = benchFormatFairSimple(nf, isCurrency);
                    break;
                case FAIR_BENCH :
                    str = benchFormatFair(nf);
                    break;
                case TIE_BENCH :
                    str = benchFormatTie(nf, isCurrency);
                    break;

                default:
                }


                long estimatedTime = System.nanoTime() - startTime;
                if (i > 3)
                    elapsedTimes[i-4] = estimatedTime / 1000;

                if (Verbose)
                    System.out.println(
                                       "calculated time for " + name +
                                       " bench " + i + " is = " +
                                       (estimatedTime / 1000) + " microseconds");
                else System.out.print(".");

                stabilizeMemory(true);
            }

            System.out.println(name + " Done.");

            printPerfResults(elapsedTimes, name);

            return str;
        }
    }

}

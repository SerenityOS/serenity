/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM testbase nsk/stress/numeric/numeric005.
 * VM testbase keywords: [stress, slow, nonconcurrent, quick]
 * VM testbase readme:
 * DESCRIPTION
 *     This test calculates the product A*A for a square matrix A of the type
 *     double[][]. Elements of the matrix A are initiated with random numbers,
 *     so that optimizing compiler could not eliminate any essential portion
 *     of calculations.
 *     Calculation of the product A*A is iterated three times, and result of
 *     the 1st iteration is compared to result of the 3rd iteration. HotSpot
 *     releases 1.0 and 1.3 seem to fail to adjust itself for better performance
 *     in 1st iteration, while 3rd iteration usually runs much faster. So, the
 *     1st iteration is probably executed by HotSpot interpreter, and HotSpot
 *     compiler is probably involved to execute the 3rd iteration. The test
 *     just tries to check if HotSpot compiler produces the same results as the
 *     HotSpot interpreter.
 *     By the way, the test checks JVM performance. The test is treated failed
 *     due to poor performance, if 1st iteration is essentially slower than the
 *     3rd iteration. The calculations algorithm is encoded as compact 3-levels
 *     cycle like:
 *         for (int line=0; line<N; line++)
 *             for (int column=0; column<N; column++) {
 *                 double sum = 0;
 *                 for (int k=0; k<N; k++)
 *                     sum += A[line][k] * A[k][column];
 *                 AA[line][column] = sum;
 *            }
 *     In this test, N=300, so that A is 300x300 matrix; and multiplication
 *     A[line][k]*A[k][column] is executed 300**3=27 millions times in each
 *     execution of this cycle. I believe, that this is HotSpot bug to do not
 *     adjust itself for best performance during such a huge series of executions
 *     of the same portion of program code.
 * COMMENTS
 *     See the bug-report:
 *         #4242172 (P3/S5) 2.0: poor performance in matrix calculations
 *
 * @library /test/lib
 * @run main/othervm nsk.stress.numeric.numeric005.numeric005 300 3
 */

package nsk.stress.numeric.numeric005;

import java.io.PrintStream;
import java.util.Random;
import jdk.test.lib.Utils;

/**
 * This test calculates the product <code>A<sup>.</sup>A</code> for
 * a square matrix <code>A</code> of the type <code>double[][]</code>.
 * Elements of the matrix <code>A</code> are initiated with random numbers,
 * so that optimizing compiler could not eliminate any essential portion
 * of calculations.
 * <p>
 * <p>Calculation of the product <code>A<sup>.</sup>A</code> is iterated three
 * times, and result of the 1<sup>st</sup> iteration is compared to result of
 * the 3<sup>rd</sup> iteration. HotSpot 1.0 and 1.3 seem to fail to adjust
 * itself for better performance in 1<sup>st</sup> iteration, while 3<sup>rd</sup>
 * iteration usually runs much faster. So, 1<sup>st</sup> iteration is probably
 * executed by HotSpot interpreter, and HotSpot compiler is probably involved to
 * execute the 3<sup>rd</sup> iteration. The test just tries to check if HotSpot
 * compiler produces the same results as the HotSpot interpreter.
 * <p>
 * <p>By the way, the test checks JVM performance. The test is treated failed
 * due to poor performance, if 1<sup>st</sup> iteration is essentially slower
 * than the 3<sup>rd</sup> iteration. The calculations algorithm is encoded
 * as compact ``canonical'' 3-levels cycle like:
 * <pre>
 *     for (int line=0; line&lt;N; line++)
 *         for (int column=0; column&lt;N; column++) {
 *             double sum = 0;
 *             for (int k=0; k&lt;N; k++)
 *                 sum += A[line][k] * A[k][column];
 *             AA[line][column] = sum;
 *         }
 * </pre>
 * <p>
 * In this test, <code>N</code>=300, so that <code>A</code> is 300x300 matrix;
 * and multiplication <code>A[line][k]*A[k][column]</code> is executed
 * 300<sup>3</sup>=27 millions times in each iteration of execution of this
 * cycle. I believe, that this is HotSpot bug to do not adjust itself for best
 * performance during such a huge series of executions of the same portion of
 * program code.
 * <p>
 * <p>See the bug-report:
 * <br>&nbsp;&nbsp;
 * #4242172 (P3/S5) 2.0: poor performance in matrix calculations
 */
public class numeric005 {
    private static final Random RNG = Utils.getRandomInstance();
    /**
     * When testing performance, single thread calculation is allowed to
     * be 10% slower than multi-threads calculation (<code>TOLERANCE</code>
     * is assigned to 10 now).
     */
    public static final double TOLERANCE = 100; // 10;

    /**
     * Re-assign this value to <code>true</code> for better
     * diagnostics.
     *
     * @see #print(Object)
     * @see #println(Object)
     */
    private static boolean verbose = false;

    /**
     * Stream to print execution trace and/or error messages.
     * This stream usually equals to <code>System.out</code>
     */
    private static PrintStream out = null;

    /**
     * Print error-message.
     *
     * @see #out
     */
    private static void complain(Object x) {
        out.println("# " + x);
    }

    /**
     * Print to execution trace, if mode is <code>verbose</code>.
     *
     * @see #verbose
     * @see #out
     */
    private static void print(Object x) {
        if (verbose)
            out.print(x);
    }

    /**
     * Print line to execution trace, if mode is <code>verbose</code>.
     *
     * @see #verbose
     * @see #out
     */
    private static void println(Object x) {
        print(x + "\n");
    }

    /**
     * Re-invoke <code>run(args,out)</code> in order to simulate
     * JCK-like test interface.
     */
    public static void main(String args[]) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
        // JCK-like exit status
    }

    /**
     * Parse command-line parameters stored in <code>args[]</code> and run
     * the test.
     * <p>
     * <p>Command-line parameters are:
     * <br>&nbsp;&nbsp;
     * <code>java numeric005 [-verbose] [-performance] <i>matrixSize</i>
     * <i>iterations</i></code>
     * <p>
     * <p>Here:
     * <br>&nbsp;&nbsp;<code>-verbose</code> -
     * keyword, which alows to print execution trace
     * <br>&nbsp;&nbsp;<code>-performance</code> -
     * keyword, which alows performance testing
     * <br>&nbsp;&nbsp;<code><i>matrixSize</i></code> -
     * number of rows (and columns) in square matrix <code>A</code>
     * <br>&nbsp;&nbsp;<code><i>iterations</i></code> -
     * compute <code>A*A</code> several times
     *
     * @param args strings array containing command-line parameters
     * @param out  the test log, usually <code>System.out</code>
     */
    public static int run(String args[], PrintStream out) {
        numeric005.out = out;

        boolean testPerformance = false;
        int numberOfCPU = 1;

        // Parse parameters starting with "-" (like: "-verbose"):

        int argsShift = 0;
        for (; argsShift < args.length; argsShift++) {
            String argument = args[argsShift];

            if (!argument.startsWith("-"))
                break;

            if (argument.equals("-performance")) {
                testPerformance = true;
                continue;
            }

            if (argument.equals("-verbose")) {
                verbose = true;
                continue;
            }

            complain("Cannot recognize argument: args[" + argsShift + "]: " + argument);
            return 2; // failure
        }

        if (args.length != argsShift + 2) {
            complain("Illegal arguments. Execute:");
            complain(
                    "    java numeric005 [-verbose] [-performance] [-CPU:number] " +
                            "matrixSize iterations");
            return 2; // failure
        }

        int size = Integer.parseInt(args[argsShift]);
        if ((size < 100) || (size > 10000)) {
            complain("Matrix size should be 100 to 1000 lines & columns.");
            return 2; // failure
        }

        int iterations = Integer.parseInt(args[argsShift + 1]);
        if ((iterations < 1) || (iterations > 100)) {
            complain("Iterations number should be 1 to 100.");
            return 2; // failure
        }

        print("Preparing A[" + size + "," + size + "]:");
        double[][] A = newMatrix(size);
        double[][] A1 = new double[size][size];
        double[][] Ai = new double[size][size];
        println(" done.");

        println("Should try " + iterations + " iteration(s):");
        println("==========================" +
                ((iterations > 99) ? "==" : (iterations > 9) ? "=" : ""));
        println("");

        double overallTime = 0;
        double firstTime = 0;
        double lastTime = 0;

        for (int i = 1; i <= iterations; i++) {
            double seconds;

            if (i == 1) {
                seconds = elapsedTime(i, A, A1);
                firstTime = seconds;
            } else {
                seconds = elapsedTime(i, A, Ai);
                lastTime = seconds;
            }

            overallTime += seconds;
        }

        double averageTime = overallTime / iterations;
        double averagePerformance = size * size * (size + size) / averageTime / 1e6;

        println("");
        println("=======================" +
                ((iterations > 99) ? "==" : (iterations > 9) ? "=" : ""));
        println("Overall iteration(s): " + iterations);
        println("Overall elapsed time: " + overallTime + " seconds.");
        println("Average elapsed time: " + averageTime + " seconds.");
        println("Average performance: " + averagePerformance + " MFLOPS");

        println("========================");
        print("Checking accuracy:");
        for (int line = 0; line < size; line++)
            for (int column = 0; column < size; column++)
                if (A1[line][column] != Ai[line][column]) {
                    println("");
                    complain("Test failed:");
                    complain("Different results in 1st and last iterations:");
                    complain("  line=" + line + ", column=" + column);
                    return 2; // FAILED
                }
        println(" done.");

        if (testPerformance) {
            print("Checking performance: ");
            if (firstTime > lastTime * (1 + TOLERANCE / 100)) {
                println("");
                complain("Test failed:");
                complain("1st iterartion is essentially slower:");
                complain("Calculation time elapsed (seconds):");
                complain("  1-st iteration: " + firstTime);
                complain("  last iteration: " + lastTime);
                complain("  tolerance: " + TOLERANCE + "%");
                return 2; // FAILED
            }
            println("done.");
        }

        println("Test passed.");
        return 0; // PASSED
    }

    private static double elapsedTime(int i, double[][] A, double[][] AA) {
        int size = A.length;

        if (i > 1)
            println("");
        println("Iteration #" + i + ":");

        print("Computing A*A:");
        long mark1 = System.currentTimeMillis();
        setSquare(A, AA);
        long mark2 = System.currentTimeMillis();
        println(" done.");

        double sec = (mark2 - mark1) / 1000.0;
        double perf = size * size * (size + size) / sec;
        println("Elapsed time: " + sec + " seconds");
        println("Performance: " + perf / 1e6 + " MFLOPS");

        return sec;
    }

    /**
     * Compute <code>A*A</code> for the given square matrix <code>A</code>.
     */
    private static void setSquare(double[][] A, double[][] AA) {
        if (A.length != A[0].length)
            throw new IllegalArgumentException(
                    "the argument matrix A should be square matrix");
        if (AA.length != AA[0].length)
            throw new IllegalArgumentException(
                    "the resulting matrix AA should be square matrix");
        if (A.length != AA.length)
            throw new IllegalArgumentException(
                    "the matrices A and AA should have equal size");

        int size = A.length;

        for (int line = 0; line < size; line++)
            for (int column = 0; column < size; column++) {
                double sum = 0;
                for (int k = 0; k < size; k++)
                    sum += A[line][k] * A[k][line];
                AA[line][column] = sum;
            }
    }

    /**
     * Generate new square matrix of the given <code>size</code>
     * and with elements initiated with random numbers.
     */
    private static double[][] newMatrix(int size) {
        if ((size < 1) || (size > 1000))
            throw new IllegalArgumentException(
                    "matrix size should be 1 to 1000");

        double[][] A = new double[size][size];

        for (int line = 0; line < size; line++)
            for (int column = 0; column < size; column++)
                A[line][column] = (1 - 2 * RNG.nextDouble()) * size;

        return A;
    }

}

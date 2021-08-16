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
 * @summary converted from VM testbase nsk/stress/numeric/numeric009.
 * VM testbase keywords: [stress, slow, nonconcurrent, quick]
 * VM testbase readme:
 * DESCRIPTION
 *     This test calculates the product A*A for a square matrix A, and checks
 *     if such product is calculated correctly. Elements of the matrix A are
 *     initiated with integer numbers, so that A*A must be the same if calculated
 *     with double, float, long, or int precision. The test just checks, if
 *     double, float, long, and int variants of the product calculation result
 *     in the same A*A matrix.
 *     Calculation of the product A*A is iterated two times, because HotSpot
 *     releases 1.0 and 1.3 seem to do not adjust JVM for better performance
 *     in 1st iteration, while 2nd iteration usually runs much faster. I guess,
 *     that the 1st iteration is probably executed by HotSpot interpreter, and
 *     HotSpot compiler is probably involved to execute the 2nd iteration. So,
 *     the test apparently checks accuracy of A*A calculation in both compilation
 *     and interpretation modes.
 *     By the way, the test checks JVM performance. The test is treated failed
 *     due to poor performance, if 1st iteration is essentially slower than the
 *     2nd iteration. The calculations algorithm is encoded as rather compact
 *     3-levels cycle like:
 *         for (int line=0; line<N; line++)
 *             for (int column=0; column<N; column++) {
 *                 float sum = 0;
 *                 for (int k=0; k<N; k++)
 *                     sum += A[line][k] * A[k][column];
 *                 AA[line][column] = sum;
 *            }
 *     In this test, N=200, so that A is 200x200 matrix; and multiplication
 *     A[line][k]*A[k][column] is executed 200**3=8 millions times in each
 *     iteration of execution of this cycle. I believe, that this is HotSpot
 *     bug to do not adjust JVM for best performance during such a huge series
 *     of executions of the rather compact portion of program code.
 * COMMENTS
 *     See the bug-report:
 *         #4242172 (P3/S5) 2.0: poor performance in matrix calculations
 *
 * @library /test/lib
 * @run main/othervm nsk.stress.numeric.numeric009.numeric009 200 2
 */

package nsk.stress.numeric.numeric009;

import java.io.PrintStream;
import java.util.Random;
import jdk.test.lib.Utils;

/**
 * This test calculates the product <code>A<sup>.</sup>A</code> for a square
 * matrix <code>A</code>, and checks if such product is calculated correctly.
 * Elements of the matrix <code>A</code> are initiated with integer numbers,
 * so that <code>A<sup>.</sup>A</code> must be the same if calculated with
 * <code>double</code>, <code>float</code>, <code>long</code>, or
 * <code>int</code> precision. The test just checks, if <code>double</code>,
 * <code>float</code>, <code>long</code>, and <code>int</code> variants of
 * the product calculation result in the same <code>A<sup>.</sup>A</code>
 * matrix.
 * <p>
 * <p>Calculation of the product <code>A<sup>.</sup>A</code> is iterated two
 * times, because HotSpot releases 1.0 and 1.3 seem to do not adjust JVM for
 * better performance in 1<sup>st</sup> iteration, while 2<sup>nd</sup>
 * iteration usually runs much faster. I guess, that the 1<sup>st</sup> iteration
 * is probably executed by HotSpot interpreter, and HotSpot compiler is probably
 * involved to execute the 2<sup>nd</sup> iteration. So, the test apparently
 * checks accuracy of <code>A<sup>.</sup>A</code> calculation in both compilation
 * and interpretation modes.
 * <p>
 * <p>By the way, the test checks JVM performance. The test is treated failed
 * due to poor performance, if 1<sup>st</sup> iteration is essentially slower
 * than the 2<sup>nd</sup> iteration. The calculations algorithm is encoded
 * as compact ``canonical'' 3-levels cycle like:
 * <pre>
 *     for (int line=0; line&lt;N; line++)
 *         for (int column=0; column&lt;N; column++) {
 *             float sum = 0;
 *             for (int k=0; k&lt;N; k++)
 *                 sum += A[line][k] * A[k][column];
 *             AA[line][column] = sum;
 *         }
 * </pre>
 * <p>
 * In this test, <code>N</code>=200, so that <code>A</code> is 200x200 matrix;
 * and multiplication <code>A[line][k]*A[k][column]</code> is executed
 * 200<sup>3</sup>=8 millions times in each iteration of execution of this
 * cycle. I believe, that this is HotSpot bug to do not adjust JVM for best
 * performance during such a huge series of executions of the rather compact
 * portion of program code.
 * <p>
 * <p>See the bug-report:
 * <br>&nbsp;&nbsp;
 * #4242172 (P3/S5) 2.0: poor performance in matrix calculations
 */
public class numeric009 {
    private static final Random RNG = Utils.getRandomInstance();
    /**
     * When testing performance, 1st iteration is allowed to be 10% slower
     * than 2nd iteration (<code>tolerance</code> is assigned to 10 now).
     */
    public static double tolerance = 100; // 10;

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
     * <code>java numeric009 [-verbose] [-performance]
     * [-tolerance:<i>percents</i>]
     * <i>matrixSize</i> <i>iterations</i></code>
     * <p>
     * <p>Here:
     * <br>&nbsp;&nbsp;<code>-verbose</code> -
     * keyword alowing to print execution trace
     * <br>&nbsp;&nbsp;<code>-performance</code> -
     * keyword turning on performance testing
     * <br>&nbsp;&nbsp;<code>-tolerance</code> -
     * setup tolerance of performance checking
     * <br>&nbsp;&nbsp;<code><i>percents</i></code> -
     * 1<sup>st</sup> iteration is allowed to be
     * <code><i>percents</i></code>% slower
     * <br>&nbsp;&nbsp;<code><i>matrixSize</i></code> -
     * number of rows (and columns) in square matrix <code>A</code>
     * <br>&nbsp;&nbsp;<code><i>iterations</i></code> -
     * how many times to execute the test for stronger checking
     *
     * @param args strings array containing command-line parameters
     * @param out  the test log, usually <code>System.out</code>
     */
    public static int run(String args[], PrintStream out) {
        numeric009.out = out;

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

            if (argument.startsWith("-tolerance:")) {
                String percents =
                        argument.substring("-tolerance:".length(), argument.length());
                tolerance = Integer.parseInt(percents);
                if ((tolerance < 0) || (tolerance > 100)) {
                    complain("Tolerance should be 0 to 100%: " + argument);
                    return 2; // failure
                }
                continue;
            }

            complain("Cannot recognize argument: args[" + argsShift + "]: " + argument);
            return 2; // failure
        }

        if (args.length != argsShift + 2) {
            complain("Illegal arguments. Execute:");
            complain(
                    "    java numeric009 [-verbose] [-performance] " +
                            "[-tolerance:percents] matrixSize iterations");
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
        int[][] intA = newIntegerMatrix(size);
        int[][] intAA = new int[size][size];
        long[][] longA = newLongMatrix(intA);
        long[][] longAA = new long[size][size];
        double[][] doubleA = newDoubleMatrix(intA);
        double[][] doubleAA = new double[size][size];
        float[][] floatA = newFloatMatrix(intA);
        float[][] floatAA = new float[size][size];
        println(" done.");

        println("Should try " + iterations + " iteration(s):");
        println("==========================" +
                ((iterations > 99) ? "==" : (iterations > 9) ? "=" : ""));
        println("");

        double overallTime = 0;
        double firstTime = 0;
        double lastTime = 0;

        for (int i = 1; i <= iterations; i++) {
            double seconds = elapsedTime(i,
                    intA, intAA, longA, longAA, floatA, floatAA, doubleA, doubleAA);

            if (i == 1)
                firstTime = seconds;
            else
                lastTime = seconds;
            overallTime += seconds;

            print("Checking accuracy:");
            for (int line = 0; line < size; line++)
                for (int column = 0; column < size; column++) {
                    if (intAA[line][column] != longAA[line][column]) {
                        println("");
                        complain("Test failed:");
                        complain("Integer and Long results differ at:");
                        complain("  line=" + line + ", column=" + column);
                        return 2; // FAILED
                    }
                    if (intAA[line][column] != floatAA[line][column]) {
                        println("");
                        complain("Test failed:");
                        complain("Integer and Float results differ at:");
                        complain("  line=" + line + ", column=" + column);
                        return 2; // FAILED
                    }
                    if (intAA[line][column] != doubleAA[line][column]) {
                        println("");
                        complain("Test failed:");
                        complain("Integer and Double results differ at:");
                        complain("  line=" + line + ", column=" + column);
                        return 2; // FAILED
                    }
                }
            println(" done.");
        }

        double averageTime = overallTime / iterations / 4;
        double averagePerformance = size * size * (size + size) / averageTime / 1e6;

        println("");
        println("=======================" +
                ((iterations > 99) ? "==" : (iterations > 9) ? "=" : ""));
        println("Overall iteration(s): " + iterations);
        println("Overall elapsed time: " + overallTime + " seconds.");
        println("Average elapsed time: " + averageTime + " seconds.");
        println("Average performance: " + averagePerformance + " MFLOPS");

        if (testPerformance) {
            print("Checking performance: ");
            if (firstTime > lastTime * (1 + tolerance / 100)) {
                println("");
                complain("Test failed:");
                complain("1st iterartion is essentially slower:");
                complain("Calculation time elapsed (seconds):");
                complain("  1-st iteration: " + firstTime);
                complain("  last iteration: " + lastTime);
                complain("  tolerance: " + tolerance + "%");
                return 2; // FAILED
            }
            println("done.");
        }

        println("Test passed.");
        return 0; // PASSED
    }

    /**
     * Return time (in seconds) elapsed for calculation of matrix
     * product <code>A*A</code> with <code>int</code>, <code>long</code>,
     * <code>float</code>, and <code>double</code> representations.
     */
    private static double elapsedTime(int i,
                                      int[][] intA, int[][] intAA,
                                      long[][] longA, long[][] longAA,
                                      float[][] floatA, float[][] floatAA,
                                      double[][] doubleA, double[][] doubleAA) {

        int size = intA.length;

        if (i > 1)
            println("");
        println("Iteration #" + i + ":");

        print("Computing int, long, float, and double A*A:");
        long mark1 = System.currentTimeMillis();
        setSquare(intA, intAA);
        setSquare(longA, longAA);
        setSquare(floatA, floatAA);
        setSquare(doubleA, doubleAA);
        long mark2 = System.currentTimeMillis();
        println(" done.");

        double sec = (mark2 - mark1) / 1000.0;
        double perf = size * size * (size + size) / (sec / 4);
        println("Elapsed time: " + sec + " seconds");
        println("Performance: " + perf / 1e6 + " MOPS");

        return sec;
    }

    /**
     * Compute <code>A*A</code> for the given square matrix <code>A</code>.
     */
    private static void setSquare(int[][] A, int[][] AA) {
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
                int sum = 0;
                for (int k = 0; k < size; k++)
                    sum += A[line][k] * A[k][line];
                AA[line][column] = sum;
            }
    }

    /**
     * Compute <code>A*A</code> for the given square matrix <code>A</code>.
     */
    private static void setSquare(long[][] A, long[][] AA) {
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
                long sum = 0;
                for (int k = 0; k < size; k++)
                    sum += A[line][k] * A[k][line];
                AA[line][column] = sum;
            }
    }

    /**
     * Compute <code>A*A</code> for the given square matrix <code>A</code>.
     */
    private static void setSquare(float[][] A, float[][] AA) {
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
                float sum = 0;
                for (int k = 0; k < size; k++)
                    sum += A[line][k] * A[k][line];
                AA[line][column] = sum;
            }
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
    private static int[][] newIntegerMatrix(int size) {
        if ((size < 1) || (size > 1000))
            throw new IllegalArgumentException(
                    "matrix size should be 1 to 1000");

        int[][] A = new int[size][size];

        for (int line = 0; line < size; line++)
            for (int column = 0; column < size; column++)
                A[line][column] =
                        Math.round((float) ((1 - 2 * RNG.nextDouble()) * size));

        return A;
    }

    /**
     * Generate new square matrix with <code>long</code> elements,
     * and initiate them with the same values as in the given matrix
     * <code>intA</code>.
     */
    private static long[][] newLongMatrix(int[][] intA) {
        if (intA.length != intA[0].length)
            throw new IllegalArgumentException(
                    "need square argument matrix");

        int size = intA.length;
        long[][] longA = new long[size][size];

        for (int line = 0; line < size; line++)
            for (int column = 0; column < size; column++)
                longA[line][column] = intA[line][column];

        return longA;
    }

    /**
     * Generate new square matrix with <code>double</code> elements,
     * and initiate them with the same values as in the given matrix
     * <code>intA</code>.
     */
    private static double[][] newDoubleMatrix(int[][] intA) {
        if (intA.length != intA[0].length)
            throw new IllegalArgumentException(
                    "need square argument matrix");

        int size = intA.length;
        double[][] doubleA = new double[size][size];

        for (int line = 0; line < size; line++)
            for (int column = 0; column < size; column++)
                doubleA[line][column] = intA[line][column];

        return doubleA;
    }

    /**
     * Generate new square matrix with <code>float</code> elements,
     * and initiate them with the same values as in the given matrix
     * <code>intA</code>.
     */
    private static float[][] newFloatMatrix(int[][] intA) {
        if (intA.length != intA[0].length)
            throw new IllegalArgumentException(
                    "need square argument matrix");

        int size = intA.length;
        float[][] floatA = new float[size][size];

        for (int line = 0; line < size; line++)
            for (int column = 0; column < size; column++)
                floatA[line][column] = intA[line][column];

        return floatA;
    }

}

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
 * @summary converted from VM testbase nsk/stress/numeric/numeric003.
 * VM testbase keywords: [stress, slow, nonconcurrent, quick]
 * VM testbase readme:
 * DESCRIPTION
 *     This test calculates the product A*A for a square matrix A of the type
 *     long[][]. Elements of the matrix A are initiated with random numbers,
 *     so that optimizing compiler could not eliminate any essential portion
 *     of calculations.
 *     That product A*A is calculated twice: in a single thread, and in N
 *     separate threads, where NxN is the size of square matrix A. When executing
 *     in N threads, each thread calculate distinct row of the resulting matrix.
 *     The test checks if the resulting product A*A is the same when calculated
 *     in single thread and in N threads.
 *     By the way, the test checks JVM performance. The test is treated failed
 *     due to poor performance, if single-thread calculation is essentially
 *     slower than N-threads calculation (surely, the number of CPUs installed
 *     on the platform executing the test is taken into account for performance
 *     testing). Note, that HotSpot may fail to adjust itself for better
 *     performance in single-thread calculation.
 * COMMENTS
 *     The bug was filed referencing to the same numeric algorithm,
 *     which is used by this test:
 *         4242172 (P3/S5) 2.0: poor performance in matrix calculations
 *
 * @library /test/lib
 * @run main/othervm nsk.stress.numeric.numeric003.numeric003 300 300
 */

package nsk.stress.numeric.numeric003;

import java.io.PrintStream;
import java.util.Random;
import jdk.test.lib.Utils;

/**
 * This test calculates the product <b>A</b><sup>.</sup><b>A</b> for
 * a square matrix <b>A</b> of the type <code>long[][]</code>.
 * Elements of the matrix <b>A</b> are initiated with random numbers,
 * so that optimizing compiler could not eliminate any essential portion
 * of calculations.
 * <p>
 * <p>That product <b>A</b><sup>.</sup><b>A</b> is calculated twice: in
 * a single thread, and in <i>N</i> separate threads, where <i>N</i>x<i>N</i>
 * is the size of square matrix <b>A</b>. When executing in <i>N</i> threads,
 * each thread calculate distinct row of the resulting matrix. The test checks
 * if the resulting product <b>A</b><sup>.</sup><b>A</b> is the same when
 * calculated in single thread and in <i>N</i> threads.
 * <p>
 * <p>By the way, the test checks JVM performance. The test is treated failed
 * due to poor performance, if single-thread calculation is essentially
 * slower than <i>N</i>-threads calculation (surely, the number of CPUs
 * installed on the platform executing the test is taken into account for
 * performance testing). Note, that HotSpot may fail to adjust itself for
 * better performance in single-thread calculation.
 * <p>
 * <p>See the bug-report:
 * <br>&nbsp;&nbsp;
 * 4242172 (P3/S5) 2.0: poor performance in matrix calculations
 */
public class numeric003 {
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
     */
    private static boolean verbose = false;

    private static PrintStream out = null;

    /**
     * Print error-message to the <code>out<code>.
     */
    private static void complain(Object x) {
        out.println("# " + x);
    }

    private static void print(Object x) {
        if (verbose)
            out.print(x);
    }

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
     * <code>java numeric003 [-verbose] [-performance] [-CPU:<i>number</i>]
     * <i>matrixSize</i> [<i>threads</i>]</code>
     * <p>
     * <p>Here:
     * <br>&nbsp;&nbsp;<code>-verbose</code> -
     * keyword, which alows to print execution trace
     * <br>&nbsp;&nbsp;<code>-performance</code> -
     * keyword, which alows performance testing
     * <br>&nbsp;&nbsp;<code><i>number</i></code> -
     * number of CPU installed on the computer just executing the test
     * <br>&nbsp;&nbsp;<code><i>matrixSize</i></code> -
     * number of rows (and columns) in square matrix to be tested
     * <br>&nbsp;&nbsp;<code><i>threads</i></code> -
     * for multi-thread calculation
     * (default: <code><i>matrixSize</i></code>)
     *
     * @param args strings array containing command-line parameters
     * @param out  the test log, usually <code>System.out</code>
     */
    public static int run(String args[], PrintStream out) {
        numeric003.out = out;

        boolean testPerformance = false;
        int numberOfCPU = 1;

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

            if (argument.startsWith("-CPU:")) {
                String value =
                        argument.substring("-CPU:".length(), argument.length());
                numberOfCPU = Integer.parseInt(value);

                if (numberOfCPU < 1) {
                    complain("Illegal number of CPU: " + argument);
                    return 2; // failure
                }
                continue;
            }

            complain("Cannot recognize argument: args[" + argsShift + "]: " + argument);
            return 2; // failure
        }

        if ((args.length < argsShift + 1) || (args.length > argsShift + 2)) {
            complain("Illegal argument(s). Execute:");
            complain(
                    "    java numeric003 [-verbose] [-performance] [-CPU:number] " +
                            "matrixSize [threads]");
            return 2; // failure
        }

        int size = Integer.parseInt(args[argsShift]);
        if ((size < 100) || (size > 10000)) {
            complain("Matrix size should be 100 to 1000 lines & columns.");
            return 2; // failure
        }

        int threads = size;
        if (args.length >= argsShift + 2)
            threads = Integer.parseInt(args[argsShift + 1]);
        if ((threads < 1) || (threads > size)) {
            complain("Threads number should be 1 to matrix size.");
            return 2; // failure
        }
        if ((size % threads) != 0) {
            complain("Threads number should evenly divide matrix size.");
            return 2; // failure
        }

        print("Preparing A[" + size + "," + size + "]:");
        SquareMatrix A = new SquareMatrix(size);
        SquareMatrix A1 = new SquareMatrix(size);
        SquareMatrix Am = new SquareMatrix(size);
        println(" done.");

        double singleThread = elapsedTime(out, A, A1, size, 1);
        double multiThreads = elapsedTime(out, A, Am, size, threads);

        print("Checking accuracy:");
        for (int line = 0; line < size; line++)
            for (int column = 0; column < size; column++)
                if (A1.value[line][column] != Am.value[line][column]) {
                    println("");
                    complain("Test failed:");
                    complain("Different results by single- and multi-threads:");
                    complain("  line=" + line + ", column=" + column);
                    complain("A1.value[line][column]=" + A1.value[line][column]);
                    complain("Am.value[line][column]=" + Am.value[line][column]);
                    return 2; // FAILED
                }
        println(" done.");

        if (testPerformance) {
            print("Checking performance: ");
            double elapsed1 = singleThread;
            double elapsedM = multiThreads * numberOfCPU;
            if (elapsed1 > elapsedM * (1 + TOLERANCE / 100)) {
                println("");
                complain("Test failed:");
                complain("Single-thread calculation is essentially slower:");
                complain("Calculation time elapsed (seconds):");
                complain("  single thread: " + singleThread);
                complain("  multi-threads: " + multiThreads);
                complain("  number of CPU: " + numberOfCPU);
                complain("  tolerance: " + TOLERANCE + "%");
                return 2; // FAILED
            }
            println("done.");
        }

        println("Test passed.");
        return 0; // PASSED
    }

    private static double elapsedTime(PrintStream out,
                                      SquareMatrix A, SquareMatrix AA, int size, int threads) {

        print("Computing A*A with " + threads + " thread(s):");
        long mark1 = System.currentTimeMillis();
        AA.setSquareOf(A, threads);
        long mark2 = System.currentTimeMillis();
        println(" done.");

        double sec = (mark2 - mark1) / 1000.0;
        double perf = size * size * (size + size) / sec;
        println("Elapsed time: " + sec + " seconds");
        println("Performance: " + perf / 1e6 + " MFLOPS");

        return sec;
    }

    /**
     * This class computes <code>A*A</code> for square matrix <code>A</code>.
     */
    private static class SquareMatrix {
        volatile long value[][];

        /**
         * New square matrix with random elements.
         */
        public SquareMatrix(int size) {
            value = new long[size][size];
            for (int line = 0; line < size; line++)
                for (int column = 0; column < size; column++)
                    value[line][column] = Math.round(RNG.nextDouble() * size);
        }

        /**
         * Update <code>value[][]</code> of <code>this</code> matrix.
         *
         * @param threads Split computation into the given number of threads.
         */
        public void setSquareOf(SquareMatrix A, int threads) {
            if (this.value.length != A.value.length)
                throw new IllegalArgumentException(
                        "this.value.length != A.value.length");

            int size = value.length;
            if ((size % threads) != 0)
                throw new IllegalArgumentException("size%threads != 0");
            int bunch = size / threads;

            Thread task[] = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                int line0 = bunch * t;
                MatrixComputer computer =
                        new MatrixComputer(value, A.value, line0, bunch);
                task[t] = new Thread(computer);
            }

            for (int t = 0; t < threads; t++)
                task[t].start();

            for (int t = 0; t < threads; t++)
                if (task[t].isAlive())
                    try {
                        task[t].join();
                    } catch (InterruptedException exception) {
                        throw new RuntimeException(exception.toString());
                    }
        }

        /**
         * Thread to compute a bunch of lines of matrix square.
         */
        private static class MatrixComputer implements Runnable {
            private long result[][];
            private long source[][];
            private int line0;
            private int bunch;

            /**
             * Register a task for matrix multiplication.
             */
            public MatrixComputer(
                    long result[][], long source[][], int line0, int bunch) {

                this.result = result;   // reference to resulting matrix value
                this.source = source;   // reference to matrix to be squared
                this.line0 = line0;     // compute lines from line0 to ...
                this.bunch = bunch;     // number of resulting lines to compute
            }

            /**
             * Do execute the task just registered for <code>this</code> thread.
             */
            public void run() {
                int line1 = line0 + bunch;
                int size = result.length;
                for (int line = line0; line < line1; line++)
                    for (int column = 0; column < size; column++) {
                        long sum = 0;
                        for (int i = 0; i < size; i++)
                            sum += source[line][i] * source[i][column];
                        result[line][column] = sum;
                    }
            }

        }

    }

}

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
 * @summary converted from VM testbase nsk/stress/numeric/numeric010.
 * VM testbase keywords: [stress, slow, nonconcurrent, quick]
 * VM testbase readme:
 * DESCRIPTION
 *     This test calculates the product A*A for a square matrix A, and checks
 *     if such product is calculated correctly. Elements of the matrix A are
 *     initiated with integer numbers, so that A*A must be the same if calculated
 *     with double, float, long, or int precision. The test just checks, if
 *     double, float, long, and int variants of the product calculation result
 *     in the same A*A matrix.
 *     The product A*A is calculated twice: in a single thread, and in N separate
 *     threads, where NxN is the size of square matrix A. When executing in N
 *     threads, each thread calculate distinct row of the resulting matrix.
 *     HotSpot releases 1.0 and 1.3 seem to do not adjust JVM for better
 *     performance in single-thread calculation, while milti-threads calculation
 *     usually runs much faster. I guess, that the 1-thread calculation is probably
 *     executed by HotSpot interpreter, and HotSpot compiler is probably involved
 *     to execute N-threads calculation. So, the test apparently checks accuracy
 *     of A*A calculation in both compilation and interpretation modes.
 *     By the way, the test checks JVM performance. The test is treated failed
 *     due to poor performance, if single-thread calculation is essentially
 *     slower than N-threads calculation (surely, the number of CPUs installed
 *     on the platform executing the test is taken into account for performance
 *     testing). The calculation algorithm is encoded with 3-levels cycle like:
 *         for (int line=0; line<N; line++)
 *             for (int column=0; column<N; column++) {
 *                 float sum = 0;
 *                 for (int k=0; k<N; k++)
 *                     sum += A[line][k]    A[k][column];
 *                 AA[line][column] = sum;
 *             }
 *     In this test, N=200, so that A is 200x200 matrix; and multiplication
 *     A[line][k]*A[k][column] is executed 200**3=8 millions times in this
 *     cycle. I believe, that this is HotSpot bug to do not adjust JVM for
 *     best performance during such a huge series of executions of the rather
 *     compact portion of program code.
 * COMMENTS
 *     The bug was filed referencing to the same numeric algorithm,
 *     which is used by this test:
 *         4242172 (P3/S5) 2.0: poor performance in matrix calculations
 *     Note, that despite HotSpot works faster in milti-thread calculations,
 *     it still remains essentially slower than classic VM with JIT on.
 *
 * @library /test/lib
 * @run main/othervm nsk.stress.numeric.numeric010.numeric010 200 200
 */

package nsk.stress.numeric.numeric010;

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
 * <p>The product <code>A<sup>.</sup>A</code> is calculated twice: in a single
 * thread, and in <code>N</code> separate threads, where <code>NxN</code> is
 * the size of square matrix <code>A</code>. When executing in <code>N</code>
 * threads, each thread calculate distinct row of the resulting matrix. HotSpot
 * releases 1.0 and 1.3 seem to do not adjust JVM for better performance in
 * single-thread calculation, while milti-threads calculation usually runs much
 * faster. I guess, that the 1-thread calculation is probably executed by HotSpot
 * interpreter, and HotSpot compiler is probably involved to execute
 * <code>N</code>-threads calculation. So, the test apparently checks accuracy
 * of <code>A<sup>.</sup>A</code> calculation in both compilation and
 * interpretation modes.
 * <p>
 * <p>By the way, the test checks JVM performance. The test is treated failed
 * due to poor performance, if single-thread calculation is essentially
 * slower than <code>N</code>-threads calculation (surely, the number of CPUs
 * installed on the platform executing the test is taken into account for
 * performance testing). The calculation algorithm is encoded with 3-levels
 * cycle like:
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
 * 200<sup>3</sup>=8 millions times in this cycle. I believe, that this is HotSpot
 * bug to do not adjust JVM for best performance during such a huge series of
 * executions of the rather compact portion of program code.
 * <p>
 * <p>See the bug-report:
 * <br>&nbsp;&nbsp;
 * 4242172 (P3/S5) 2.0: poor performance in matrix calculations
 */
public class numeric010 {
    private static final Random RNG = Utils.getRandomInstance();
    /**
     * When testing performance, 1-thread calculation is allowed to be 10%
     * slower than multi-thread calculation (<code>tolerance</code> is
     * assigned to 10 now).
     */
    public static double tolerance = 100; // 10;

    /**
     * Re-assign this value to <code>true</code> for better diagnostics.
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
     * <code>java numeric010 [-verbose] [-performance]
     * [-tolerance:<i>percents</i>] [-CPU:<i>number</i>]
     * <i>matrixSize</i> [<i>threads</i>]</code>
     * <p>
     * <p>Here:
     * <br>&nbsp;&nbsp;<code>-verbose</code> -
     * keyword, which alows to print execution trace
     * <br>&nbsp;&nbsp;<code>-performance</code> -
     * keyword, which alows performance testing
     * <br>&nbsp;&nbsp;<code>-tolerance</code> -
     * setup tolerance of performance checking
     * <br>&nbsp;&nbsp;<code><i>percents</i></code> -
     * 1-thread calculation is allowed to be
     * <code><i>percents</i></code>% slower
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
        numeric010.out = out;

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
                    "    java numeric010 [-verbose] [-performance] " +
                            "[-tolerance:percents] [-CPU:number] matrixSize [threads]");
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
        IntegerMatrix intA = new IntegerMatrix(size);
        IntegerMatrix intAA = new IntegerMatrix(size);
        LongMatrix longA = new LongMatrix(intA);
        LongMatrix longAA = new LongMatrix(intA);
        FloatMatrix floatA = new FloatMatrix(intA);
        FloatMatrix floatAA = new FloatMatrix(intA);
        DoubleMatrix doubleA = new DoubleMatrix(intA);
        DoubleMatrix doubleAA = new DoubleMatrix(intA);
        println(" done.");

        double elapsed[] = {0, 0};

        for (int i = 0; i < 2; i++) {
            double seconds =
                    elapsedTime((i == 0 ? 1 : threads),
                            intA, intAA,
                            longA, longAA,
                            floatA, floatAA,
                            doubleA, doubleAA);
            elapsed[i] = seconds;

            print("Checking accuracy:");
            for (int line = 0; line < size; line++)
                for (int column = 0; column < size; column++) {
                    if (intAA.value[line][column] != longAA.value[line][column]) {
                        println("");
                        complain("Test failed:");
                        complain("Integer and Long results differ at:");
                        complain("  line=" + line + ", column=" + column);
                        complain(" intAA.value[line][column]=" + intAA.value[line][column]);
                        complain("longAA.value[line][column]=" + longAA.value[line][column]);
                        return 2; // FAILED
                    }
                    if (intAA.value[line][column] != floatAA.value[line][column]) {
                        println("");
                        complain("Test failed:");
                        complain("Integer and Float results differ at:");
                        complain("  line=" + line + ", column=" + column);
                        complain("  intAA.value[line][column]=" + intAA.value[line][column]);
                        complain("floatAA.value[line][column]=" + floatAA.value[line][column]);
                        return 2; // FAILED
                    }
                    if (intAA.value[line][column] != doubleAA.value[line][column]) {
                        println("");
                        complain("Test failed:");
                        complain("Integer and Double results differ at:");
                        complain("  line=" + line + ", column=" + column);
                        complain("   intAA.value[line][column]=" + intAA.value[line][column]);
                        complain("doubleAA.value[line][column]=" + doubleAA.value[line][column]);
                        return 2; // FAILED
                    }
                }
            println(" done.");
        }

        double overallTime = elapsed[0] + elapsed[1];
        double averageTime = overallTime / 2;   // 2 excutions
        double averagePerformance = 4 * size * size * (size + size) / averageTime / 1e6;
        println("");
        println("Overall elapsed time: " + overallTime + " seconds.");
        println("Average elapsed time: " + averageTime + " seconds.");
        println("Average performance: " + averagePerformance + " MOPS");

        if (testPerformance) {
            println("");
            print("Checking performance: ");
            double elapsed1 = elapsed[0];
            double elapsedM = elapsed[1] * numberOfCPU;
            if (elapsed1 > elapsedM * (1 + tolerance / 100)) {
                println("");
                complain("Test failed:");
                complain("Single-thread calculation is essentially slower:");
                complain("Calculation time elapsed (seconds):");
                complain("  single thread: " + elapsed[0]);
                complain("  multi-threads: " + elapsed[1]);
                complain("  number of CPU: " + numberOfCPU);
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
    private static double elapsedTime(int threads,
                                      IntegerMatrix intA, IntegerMatrix intAA,
                                      LongMatrix longA, LongMatrix longAA,
                                      FloatMatrix floatA, FloatMatrix floatAA,
                                      DoubleMatrix doubleA, DoubleMatrix doubleAA) {

        println("");
        print("Computing A*A with " + threads + " thread(s):");
        long mark1 = System.currentTimeMillis();
        intAA.setSquareOf(intA, threads);
        longAA.setSquareOf(longA, threads);
        floatAA.setSquareOf(floatA, threads);
        doubleAA.setSquareOf(doubleA, threads);
        long mark2 = System.currentTimeMillis();
        println(" done.");

        int size = intA.size();
        double sec = (mark2 - mark1) / 1000.0;
        double perf = 4 * size * size * (size + size) / sec;
        println("Elapsed time: " + sec + " seconds");
        println("Performance: " + perf / 1e6 + " MOPS");

        return sec;
    }

    /**
     * Compute <code>A*A</code> for <code>int</code> matrix <code>A</code>.
     */
    private static class IntegerMatrix {
        volatile int value[][];

        /**
         * Number of lines and columns in <code>this</code> square matrix.
         */
        public int size() {
            return value.length;
        }

        /**
         * New square matrix with random elements.
         */
        public IntegerMatrix(int size) {
            value = new int[size][size];
            for (int line = 0; line < size; line++)
                for (int column = 0; column < size; column++)
                    value[line][column] =
                            Math.round((float) ((1 - 2 * RNG.nextDouble()) * size));
        }

        /**
         * Assign <code>this</code> matrix with <code>A*A</code>.
         *
         * @param threads Split computation into the given number of threads.
         */
        public void setSquareOf(IntegerMatrix A, int threads) {
            if (this.size() != A.size())
                throw new IllegalArgumentException(
                        "this.size() != A.size()");

            if ((size() % threads) != 0)
                throw new IllegalArgumentException("size()%threads != 0");
            int bunch = size() / threads;

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
            private int result[][];
            private int source[][];
            private int line0;
            private int bunch;

            /**
             * Register a task for matrix multiplication.
             */
            public MatrixComputer(
                    int result[][], int source[][], int line0, int bunch) {

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
                        int sum = 0;
                        for (int i = 0; i < size; i++)
                            sum += source[line][i] * source[i][column];
                        result[line][column] = sum;
                    }
            }

        }

    }

    /**
     * Compute <code>A*A</code> for <code>long</code> matrix <code>A</code>.
     */
    private static class LongMatrix {
        volatile long value[][];

        /**
         * Number of lines and columns in <code>this</code> square matrix.
         */
        public int size() {
            return value.length;
        }


        /**
         * New square matrix with the given integer elements.
         */
        public LongMatrix(IntegerMatrix A) {
            int size = A.size();
            value = new long[size][size];
            for (int line = 0; line < size; line++)
                for (int column = 0; column < size; column++)
                    value[line][column] = A.value[line][column];
        }

        /**
         * Assign <code>this</code> matrix with <code>A*A</code>.
         *
         * @param threads Split computation into the given number of threads.
         */
        public void setSquareOf(LongMatrix A, int threads) {
            if (this.size() != A.size())
                throw new IllegalArgumentException(
                        "this.size() != A.size()");

            if ((size() % threads) != 0)
                throw new IllegalArgumentException("size()%threads != 0");
            int bunch = size() / threads;

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

    /**
     * Compute <code>A*A</code> for <code>float</code> matrix <code>A</code>.
     */
    private static class FloatMatrix {
        volatile float value[][];

        /**
         * Number of lines and columns in <code>this</code> square matrix.
         */
        public int size() {
            return value.length;
        }


        /**
         * New square matrix with the given integer elements.
         */
        public FloatMatrix(IntegerMatrix A) {
            int size = A.size();
            value = new float[size][size];
            for (int line = 0; line < size; line++)
                for (int column = 0; column < size; column++)
                    value[line][column] = A.value[line][column];
        }

        /**
         * Assign <code>this</code> matrix with <code>A*A</code>.
         *
         * @param threads Split computation into the given number of threads.
         */
        public void setSquareOf(FloatMatrix A, int threads) {
            if (this.size() != A.size())
                throw new IllegalArgumentException(
                        "this.size() != A.size()");

            if ((size() % threads) != 0)
                throw new IllegalArgumentException("size()%threads != 0");
            int bunch = size() / threads;

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
            private float result[][];
            private float source[][];
            private int line0;
            private int bunch;

            /**
             * Register a task for matrix multiplication.
             */
            public MatrixComputer(
                    float result[][], float source[][], int line0, int bunch) {

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
                        float sum = 0;
                        for (int i = 0; i < size; i++)
                            sum += source[line][i] * source[i][column];
                        result[line][column] = sum;
                    }
            }

        }

    }

    /**
     * Compute <code>A*A</code> for <code>float</code> matrix <code>A</code>.
     */
    private static class DoubleMatrix {
        volatile double value[][];

        /**
         * Number of lines and columns in <code>this</code> square matrix.
         */
        public int size() {
            return value.length;
        }


        /**
         * New square matrix with the given integer elements.
         */
        public DoubleMatrix(IntegerMatrix A) {
            int size = A.size();
            value = new double[size][size];
            for (int line = 0; line < size; line++)
                for (int column = 0; column < size; column++)
                    value[line][column] = A.value[line][column];
        }

        /**
         * Assign <code>this</code> matrix with <code>A*A</code>.
         *
         * @param threads Split computation into the given number of threads.
         */
        public void setSquareOf(DoubleMatrix A, int threads) {
            if (this.size() != A.size())
                throw new IllegalArgumentException(
                        "this.size() != A.size()");

            if ((size() % threads) != 0)
                throw new IllegalArgumentException("size()%threads != 0");
            int bunch = size() / threads;

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
            private double result[][];
            private double source[][];
            private int line0;
            private int bunch;

            /**
             * Register a task for matrix multiplication.
             */
            public MatrixComputer(
                    double result[][], double source[][], int line0, int bunch) {

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
                        double sum = 0;
                        for (int i = 0; i < size; i++)
                            sum += source[line][i] * source[i][column];
                        result[line][column] = sum;
                    }
            }

        }

    }

}

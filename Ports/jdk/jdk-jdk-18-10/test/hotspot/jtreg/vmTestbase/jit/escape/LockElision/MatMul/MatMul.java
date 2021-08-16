/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 *
 * @summary converted from VM Testbase jit/escape/LockElision/MatMul.
 * VM Testbase keywords: [jit, quick]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test multiplies 2 matrices, first, by directly calculating matrix product
 *     elements, and second, by calculating them parallelly in diffenent threads.
 *     The results are compared then.
 *     The test, in addition to required locks, introduces locks on local variables or
 *     variables not escaping from the executing thread, and nests them manifoldly.
 *     In case of a buggy compiler, during lock elimination some code, required by
 *     the calulation may be eliminated as well, or the code may be overoptimized in
 *     some other way, causing difference in the execution results.
 *     The test has one parameter, -dim, which specifies the dimensions of matrices.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.escape.LockElision.MatMul.MatMul -dim 30 -threadCount 10
 */

package jit.escape.LockElision.MatMul;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nsk.share.Consts;
import nsk.share.Log;
import nsk.share.Pair;
import nsk.share.test.StressOptions;
import vm.share.options.Option;
import vm.share.options.OptionSupport;
import vm.share.options.Options;

import jdk.test.lib.Utils;

public class MatMul {

    @Option(name = "dim", description = "dimension of matrices")
    int dim;

    @Option(name = "verbose", default_value = "false",
            description = "verbose mode")
    boolean verbose;

    @Option(name = "threadCount", description = "thread count")
    int threadCount;

    @Options
    StressOptions stressOptions = new StressOptions();

    private Log log;

    public static void main(String[] args) {
        MatMul test = new MatMul();
        OptionSupport.setup(test, args);
        System.exit(Consts.JCK_STATUS_BASE + test.run());
    }

    public int run() {
        log = new Log(System.out, verbose);
        log.display("Parallel matrix multiplication test");

        Matrix a = Matrix.randomMatrix(dim);
        Matrix b = Matrix.randomMatrix(dim);
        long t1, t2;

        t1 = System.currentTimeMillis();
        Matrix serialResult = serialMul(a, b);
        t2 = System.currentTimeMillis();
        log.display("serial time: " + (t2 - t1) + "ms");

        try {
            t1 = System.currentTimeMillis();
            Matrix parallelResult = parallelMul(a, b,
                    threadCount * stressOptions.getThreadsFactor());
            t2 = System.currentTimeMillis();
            log.display("parallel time: " + (t2 - t1) + "ms");

            if (!serialResult.equals(parallelResult)) {
                log.complain("a = \n" + a);
                log.complain("b = \n" + b);

                log.complain("serial: a * b = \n" + serialResult);
                log.complain("serial: a * b = \n" + parallelResult);
                return Consts.TEST_FAILED;
            }
            return Consts.TEST_PASSED;

        } catch (CounterIncorrectStateException e) {
            log.complain("incorrect state of counter " + e.counter.name);
            log.complain("expected = " + e.counter.expected);
            log.complain("actual " + e.counter.state());
            return Consts.TEST_FAILED;
        }
    }

    public static int convolution(Seq<Integer> one, Seq<Integer> two) {
        int res = 0;
        int upperBound = Math.min(one.size(), two.size());
        for (int i = 0; i < upperBound; i++) {
            res += one.get(i) * two.get(i);
        }
        return res;
    }

    /**
     * calculate chunked convolutuion of two sequences
     * <p/>
     * This special version of this method:
     * <pre>{@code
     * public static int chunkedConvolution(Seq<Integer> one, Seq<Integer> two, int from, int to) {
     * int res = 0;
     *  int upperBound = Math.min(Math.min(one.size(), two.size()), to + 1);
     *  for (int i = from; i < upperBound; i++) {
     *    res += one.get(i) * two.get(i);
     *   }
     *  return res;
     * }}</pre>
     * <p/>
     * that tries to fool the Lock Elision optimization:
     * Most lock objects in these lines are really thread local, so related synchronized blocks (dummy blocks) can be removed.
     * But several synchronized blocks (all that protected by Counter instances) are really necessary, and removing them we obtain
     * an incorrect result.
     *
     * @param one
     * @param two
     * @param from     - lower bound of sum
     * @param to       - upper bound of sum
     * @param local    - reference ThreadLocal that will be used for calculations
     * @param bCounter - Counter instance, need to perfom checks
     */
    public static int chunkedConvolutionWithDummy(Seq<Integer> one,
            Seq<Integer> two, int from, int to, ThreadLocals local,
            Counter bCounter) {
        ThreadLocals conv_local1 = new ThreadLocals(local, "conv_local1");
        ThreadLocals conv_local2 = new ThreadLocals(conv_local1, "conv_local2");
        ThreadLocals conv_local3 = new ThreadLocals(null, "conv_local3");
        int res = 0;
        synchronized (local) {
            local.updateHash();
            int upperBound = 0;
            synchronized (conv_local1) {
                upperBound = local.min(one.size(), two.size());
                synchronized (two) {
                    //int upperBound = Math.min(Math.min(one.size(), two.size()), to + 1) :
                    upperBound = conv_local1.min(upperBound, to + 1);
                    synchronized (bCounter) {
                        bCounter.inc();
                    }
                }
                for (int i = from; i < upperBound; i++) {
                    synchronized (conv_local2) {
                        conv_local1.updateHash();
                        int prod = 0;
                        synchronized (one) {
                            int t = conv_local2.mult(one.get(i), two.get(i));
                            synchronized (conv_local3) {
                                prod = t;

                            }
                            //res += one.get(i) * two.get(i)
                            res = conv_local3.sum(res, prod);
                        }
                    }
                }
            }
            return res;
        }
    }

    public boolean productCheck(Matrix a, Matrix b) {
        if (a == null || b == null) {
            log.complain("null matrix!");
            return false;
        }

        if (a.dim != b.dim) {
            log.complain("matrices dimension are differs");
            return false;
        }
        return true;
    }

    public Matrix serialMul(Matrix a, Matrix b) {
        if (!productCheck(a, b)) {
            throw new IllegalArgumentException();
        }

        Matrix result = Matrix.zeroMatrix(a.dim);
        for (int i = 0; i < a.dim; i++) {
            for (int j = 0; j < a.dim; j++) {
                result.set(i, j, convolution(a.row(i), b.column(j)));
            }
        }
        return result;
    }


    /**
     * Parallel multiplication of matrices.
     * <p/>
     * This special version of this method:
     * <pre>{@code
     *  public Matrix parallelMul1(final Matrix a, final Matrix b, int threadCount) {
     *   if (!productCheck(a, b)) {
     *       throw new IllegalArgumentException();
     *   }
     *   final int dim = a.dim;
     *   final Matrix result = Matrix.zeroMatrix(dim);
     * <p/>
     *   ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
     *   final CountDownLatch latch = new CountDownLatch(threadCount);
     *   List<Pair<Integer, Integer>> parts = splitInterval(Pair.of(0, dim - 1), threadCount);
     *   for (final Pair<Integer, Integer> part : parts) {
     *       threadPool.submit(new Runnable() {
     *           @Override
     *           public void run() {
     *               for (int i = 0; i < dim; i++) {
     *                   for (int j = 0; j < dim; j++) {
     *                       synchronized (result) {
     *                           int from = part.first;
     *                           int to = part.second;
     *                           result.add(i, j, chunkedConvolution(a.row(i), b.column(j), from, to));
     *                       }
     *                   }
     *               }
     *               latch.countDown();
     *           }
     *       });
     *   }
     * <p/>
     *   try {
     *       latch.await();
     *   } catch (InterruptedException e) {
     *       e.printStackTrace();
     *   }
     *   threadPool.shutdown();
     *   return result;
     * }}</pre>
     * Lines marked with NOP comments need to fool the Lock Elision optimization:
     * All lock objects in these lines are really thread local, so related synchronized blocks (dummy blocks) can be removed.
     * But several synchronized blocks (that are nested in dummy blocks) are really necessary, and removing them we obtain
     * an incorrect result.
     *
     * @param a           first operand
     * @param b           second operand
     * @param threadCount number of threads that will be used for calculations
     * @return product of matrices a and b
     */
    public Matrix parallelMul(final Matrix a, final Matrix b, int threadCount)
            throws CounterIncorrectStateException {
        if (!productCheck(a, b)) {
            throw new IllegalArgumentException();
        }
        final int dim = a.dim;
        final Matrix result = Matrix.zeroMatrix(dim);

        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        List<Pair<Integer, Integer>> parts = splitInterval(Pair.of(0, dim - 1),
                threadCount);

        final Counter lCounter1 = new Counter(threadCount, "lCounter1");
        final Counter lCounter2 = new Counter(threadCount, "lCounter2");
        final Counter lCounter3 = new Counter(threadCount, "lCounter3");

        final Counter bCounter1 = new Counter(threadCount * dim * dim,
                "bCounter1");
        final Counter bCounter2 = new Counter(threadCount * dim * dim,
                "bCounter2");
        final Counter bCounter3 = new Counter(threadCount * dim * dim,
                "bCounter3");

        final Counter[] counters = {lCounter1, lCounter2, lCounter3,
                bCounter1, bCounter2, bCounter3};

        final Map<Pair<Integer, Integer>, ThreadLocals> locals1
                = CollectionsUtils.newHashMap();
        final Map<Pair<Integer, Integer>, ThreadLocals> locals2
                = CollectionsUtils.newHashMap();
        final Map<Pair<Integer, Integer>, ThreadLocals> locals3
                = CollectionsUtils.newHashMap();

        for (final Pair<Integer, Integer> part : parts) {

            ThreadLocals local1 = new ThreadLocals(null,
                    "locals1[" + part + "]");
            ThreadLocals local2 = new ThreadLocals(local1,
                    "locals2[" + part + "]");
            ThreadLocals local3 = new ThreadLocals(local2,
                    "locals3[" + part + "]");

            locals1.put(part, local1);
            locals2.put(part, local2);
            locals3.put(part, local3);
        }

        for (final Pair<Integer, Integer> part : parts) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    ThreadLocals local1 = locals1.get(part);
                    ThreadLocals local2 = locals2.get(part);
                    ThreadLocals local3 = locals3.get(part);
                    ThreadLocals local4 = locals3.get(part);
                    synchronized (local1) {
                        local1.updateHash();
                        synchronized (lCounter1) {
                            lCounter1.inc();
                        }
                        synchronized (lCounter3) {
                            synchronized (local2) {
                                local2.updateHash();
                                lCounter3.inc();
                            }
                        }
                        synchronized (new Object()) {
                            synchronized (lCounter2) {
                                lCounter2.inc();
                            }
                            for (int i = 0; i < dim; i++) {
                                for (int j = 0; j < dim; j++) {
                                    synchronized (bCounter1) {
                                        synchronized (new Object()) {
                                            bCounter1.inc();
                                        }
                                    }
                                    synchronized (local3) {
                                        local3.updateHash();
                                        synchronized (bCounter2) {
                                            bCounter2.inc();
                                        }
                                        synchronized (result) {
                                            local1.updateHash();
                                            synchronized (local2) {
                                                local2.updateHash();
                                                int from = part.first;
                                                int to = part.second;
                                                result.add(i, j,
                                                        chunkedConvolutionWithDummy(
                                                                a.row(i),
                                                                b.column(j),
                                                                from, to,
                                                                local4,
                                                                bCounter3));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        threadPool.shutdown();
        for (final Pair<Integer, Integer> part : parts) {
            log.display(
                    "hash for " + part + " = " + locals1.get(part).getHash());
        }


        for (Counter counter : counters) {
            if (!counter.check()) {
                throw new CounterIncorrectStateException(counter);
            }
        }
        return result;
    }

    /**
     * Split interval into parts
     *
     * @param interval  - pair than encode bounds of interval
     * @param partCount - count of parts
     * @return list of pairs than encode bounds of parts
     */
    public static List<Pair<Integer, Integer>> splitInterval(
            Pair<Integer, Integer> interval, int partCount) {
        if (partCount == 0) {
            throw new IllegalArgumentException();
        }

        if (partCount == 1) {
            return CollectionsUtils.asList(interval);
        }

        int intervalSize = interval.second - interval.first + 1;
        int partSize = intervalSize / partCount;

        List<Pair<Integer, Integer>> init = splitInterval(
                Pair.of(interval.first, interval.second - partSize),
                partCount - 1);
        Pair<Integer, Integer> lastPart = Pair
                .of(interval.second - partSize + 1, interval.second);

        return CollectionsUtils.append(init, lastPart);
    }

    public static class Counter {
        private int state;

        public final int expected;
        public final String name;

        public void inc() {
            state++;
        }

        public int state() {
            return state;
        }

        public boolean check() {
            return state == expected;
        }

        public Counter(int expected, String name) {
            this.expected = expected;
            this.name = name;
        }
    }

    private static class CounterIncorrectStateException extends Exception {
        public final Counter counter;

        public CounterIncorrectStateException(Counter counter) {
            this.counter = counter;
        }
    }

    private static abstract class Seq<E> implements Iterable<E> {
        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private int p = 0;

                @Override
                public boolean hasNext() {
                    return p < size();
                }

                @Override
                public E next() {
                    return get(p++);
                }

                @Override
                public void remove() {
                }
            };
        }

        public abstract E get(int i);

        public abstract int size();
    }

    private static class CollectionsUtils {

        public static <K, V> Map<K, V> newHashMap() {
            return new HashMap<K, V>();
        }

        public static <E> List<E> newArrayList() {
            return new ArrayList<E>();
        }

        public static <E> List<E> newArrayList(Collection<E> collection) {
            return new ArrayList<E>(collection);
        }

        public static <E> List<E> asList(E e) {
            List<E> result = newArrayList();
            result.add(e);
            return result;
        }

        public static <E> List<E> append(List<E> init, E last) {
            List<E> result = newArrayList(init);
            result.add(last);
            return result;
        }
    }

    private static class Matrix {

        public final int dim;
        private int[] coeffs;

        private Matrix(int dim) {
            this.dim = dim;
            this.coeffs = new int[dim * dim];
        }

        public void set(int i, int j, int value) {
            coeffs[i * dim + j] = value;
        }

        public void add(int i, int j, int value) {
            coeffs[i * dim + j] += value;
        }

        public int get(int i, int j) {
            return coeffs[i * dim + j];
        }

        public Seq<Integer> row(final int i) {
            return new Seq<Integer>() {
                @Override
                public Integer get(int j) {
                    return Matrix.this.get(i, j);
                }

                @Override
                public int size() {
                    return Matrix.this.dim;
                }
            };
        }

        public Seq<Integer> column(final int j) {
            return new Seq<Integer>() {
                @Override
                public Integer get(int i) {
                    return Matrix.this.get(i, j);
                }

                @Override
                public int size() {
                    return Matrix.this.dim;
                }
            };
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    builder.append((j == 0) ? "" : "\t\t");
                    builder.append(get(i, j));
                }
                builder.append("\n");
            }
            return builder.toString();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Matrix)) {
                return false;
            }

            Matrix b = (Matrix) other;
            if (b.dim != this.dim) {
                return false;
            }
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    if (this.get(i, j) != b.get(i, j)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static Random random = Utils.getRandomInstance();

        public static Matrix randomMatrix(int dim) {
            Matrix result = new Matrix(dim);
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    result.set(i, j, random.nextInt(50));
                }
            }
            return result;
        }

        public static Matrix zeroMatrix(int dim) {
            Matrix result = new Matrix(dim);
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    result.set(i, j, 0);
                }
            }
            return result;
        }
    }

    /**
     * All instances of this class will be used in thread local context
     */
    private static class ThreadLocals {
        private static final int HASH_BOUND = 424242;

        private ThreadLocals parent;
        private int hash = 42;
        public final String name;

        public ThreadLocals(ThreadLocals parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        public int min(int a, int b) {
            updateHash(a + b + 1);
            return Math.min(a, b);
        }

        public int mult(int a, int b) {
            updateHash(a + b + 2);
            return a * b;
        }

        public int sum(int a, int b) {
            updateHash(a + b + 3);
            return a + b;
        }


        public int updateHash() {
            return updateHash(42);
        }

        public int updateHash(int data) {
            hash = (hash + data) % HASH_BOUND;
            if (parent != null) {
                hash = parent.updateHash(hash) % HASH_BOUND;
            }
            return hash;
        }

        public int getHash() {
            return hash;
        }
    }
}

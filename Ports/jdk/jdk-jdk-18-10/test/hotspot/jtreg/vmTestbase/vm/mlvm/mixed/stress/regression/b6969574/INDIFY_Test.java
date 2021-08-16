/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6969574
 *
 * @summary converted from VM Testbase vm/mlvm/mixed/stress/regression/b6969574.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.mixed.stress.regression.b6969574.INDIFY_Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.mixed.stress.regression.b6969574.INDIFY_Test
 */

package vm.mlvm.mixed.stress.regression.b6969574;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.LinkedList;

import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;
import vm.share.options.Option;

/**
 * Test for CR 6969574: Verify that MethodHandles is faster than reflection and comparable
 * in order of magnitude to direct calls.
 * The test is supposed to run in -Xcomp/-Xmixed modes.
 * It can fail in -Xint.

 */

public class INDIFY_Test extends MlvmTest {

    @Option(name="warmups", default_value="5", description="Number of warm-up cycles")
    private int warmups;

    @Option(name="measurements", default_value="10", description="Number of test run cycles")
    private int measurements;

    @Option(name="iterations", default_value="1000000", description="Number iterations per test run")
    private int iterations;

    @Option(name="micro.iterations", default_value="5", description="Number micro-iterations per iteration")
    private int microIterations;

    private static final int MICRO_TO_NANO = 1000000;

    private static final String TESTEE_ARG2 = "abc";
    private static final long TESTEE_ARG3 = 123;

    //
    // Test method and its stuff
    //
    private static int sMicroIterations;

    private static class TestData {
        int i;
    }

    private static final String TESTEE_METHOD_NAME = "testee";

    static long testee;
    /**
     * A testee method. Declared public due to Reflection API requirements.
     * Not intended for external use.
     */
    public static void testee(TestData d, String y, long x) {
        for (int i = 0; i < INDIFY_Test.sMicroIterations; i++) {
            testee /= 1 + (d.i | 1);
        }
    }

    //
    // Indify stubs for invokedynamic
    //
    private static MethodType MT_bootstrap() {
        return MethodType.methodType(Object.class, Object.class, Object.class, Object.class);
    }

    private static MethodHandle MH_bootstrap() throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap", MT_bootstrap());
    }

    private static MethodType MT_target() {
        return MethodType.methodType(void.class, TestData.class, String.class, long.class);
    }

    private static MethodHandle INDY_call;
    private static MethodHandle INDY_call() throws Throwable {
        if (INDY_call != null) {
            return INDY_call;
        }

        return ((CallSite) MH_bootstrap().invokeWithArguments(MethodHandles.lookup(), "hello", MT_target())).dynamicInvoker();
    }

    private static Object bootstrap(Object l, Object n, Object t) throws Throwable {
        trace("BSM called");
        return new ConstantCallSite(MethodHandles.lookup().findStatic(INDIFY_Test.class, TESTEE_METHOD_NAME, MT_target()));
    }

    // The function below contains invokedynamic instruction after processing
    // with Indify
    private static void indyWrapper(TestData d) throws Throwable {
        INDY_call().invokeExact(d, TESTEE_ARG2, TESTEE_ARG3);
    }

    //
    // Benchmarking infrastructure
    //
    private abstract static class T {
        public abstract void run() throws Throwable;
    }

    private static class Measurement {
        Benchmark benchmark;
        long time;
        long iterations;
        double timePerIteration;

        Measurement(Benchmark b, long t, long iter) {
            benchmark = b;
            time = t;
            iterations = iter;
            timePerIteration = (double) time / iterations;
        }

        void report(Measurement compareToThis) {
            String line = String.format("%40s: %7.1f ns", benchmark.name, timePerIteration * MICRO_TO_NANO);

            if (compareToThis != null && compareToThis != this) {
                double ratio = (double) timePerIteration / compareToThis.timePerIteration;
                String er = "slower";

                if (ratio < 1) {
                    er = "FASTER";
                    ratio = 1 / ratio;
                }

                line += String.format(" // %.1f times %s than %s", ratio, er, compareToThis.benchmark.name);
            }

            print(line);
        }
    }

    private static class Result {
        Benchmark benchmark;
        double mean;
        double stdDev;

        public Result(Benchmark b, double mean, double stdDev) {
            benchmark = b;
            this.mean = mean;
            this.stdDev = stdDev;
        }

        public void report(Result compareToThis) {
            String line = String.format(
                    "%40s: %7.1f ns (stddev: %5.1f = %2d%%)",
                    benchmark.name,
                    mean * MICRO_TO_NANO,
                    stdDev * MICRO_TO_NANO,
                    (int) (100 * stdDev / mean));

            if (compareToThis != null && compareToThis != this) {
                double ratio = mean / compareToThis.mean;
                String er = "slower";

                if (ratio < 1) {
                    er = "FASTER";
                    ratio = 1 / ratio;
                }

                line += String.format(" // %.1f times %s than %s", ratio, er, compareToThis.benchmark.name);
            }

            print(line);
        }

        public static Result calculate(Measurement[] measurements, Result substractThis) {
            if (measurements.length == 0) {
                throw new IllegalArgumentException("No measurements!");
            }

            double meanToSubstract = 0;
            if (substractThis != null) {
                meanToSubstract = substractThis.mean;
            }

            long timeSum = 0;
            long iterationsSum = 0;
            for (Measurement m : measurements) {
                timeSum += m.time;
                iterationsSum += m.iterations;
            }

            double mean = (double) timeSum / iterationsSum - meanToSubstract;

            double stdDev = 0;
            for (Measurement m : measurements) {
                double result = (double) m.time / m.iterations - meanToSubstract;
                stdDev += Math.pow(result - mean, 2);
            }
            stdDev = Math.sqrt(stdDev / measurements.length);

            return new Result(measurements[0].benchmark, mean, stdDev);
        }

        public String getMeanStr() {
            return String.format("%.1f ns", mean * MICRO_TO_NANO);
        }

        public Benchmark getBenchmark() {
            return benchmark;
        }
    }

    private static class Benchmark {
        String name;
        T runnable;
        LinkedList<Measurement> runResults = new LinkedList<Measurement>();

        public Benchmark(String name, T runnable) {
            this.name = name;
            this.runnable = runnable;
        }

        public Measurement run(int iterations, boolean warmingUp) throws Throwable {
            long start = System.currentTimeMillis();

            for (int i = iterations; i > 0; --i) {
                runnable.run();
            }

            long duration = System.currentTimeMillis() - start;

            Measurement measurement = new Measurement(this, duration, iterations);

            if (!warmingUp) {
                runResults.add(measurement);
            }

            return measurement;
        }

        public void shortWarmup() throws Throwable {
            runnable.run();
        }

        public String getName() {
            return name;
        }
    }

    private static double relativeOrder(double value, double base) {
        return Math.log10(Math.abs(value - base) / base);
    }

    private void verifyTimeOrder(Result value, Result base) {
        double timeOrder = relativeOrder(value.mean, base.mean);

        if (timeOrder > 1) {
            markTestFailed(value.getBenchmark().getName() + " invocation time order ("
                    + value.getMeanStr()
                    + ") is greater than of " + base.getBenchmark().getName() + "("
                    + base.getMeanStr() + ")!");
        }

        print(value.getBenchmark().getName()
            + " <= "
            + base.getBenchmark().getName()
            + ": Good.");
    }

    // The numbers below are array indexes + size of array (the last constant).
    // They should be consecutive, starting with 0
    private final static int DIRECT_CALL = 0;
    private final static int REFLECTION_CALL = 1;
    private final static int INVOKE_EXACT = 2;
    private final static int INVOKE = 3;
    private final static int INDY = 4;
    private final static int BENCHMARK_COUNT = 5;

    //
    // Test body
    //
    @Override
    public boolean run() throws Throwable {
        sMicroIterations = microIterations;

        final MethodHandle mhTestee = MethodHandles.lookup().findStatic(INDIFY_Test.class, TESTEE_METHOD_NAME, MT_target());
        final Method refTestee = getClass().getMethod(TESTEE_METHOD_NAME, new Class<?>[] { TestData.class, String.class, long.class });

        final TestData testData = new TestData();

        final Benchmark[] benchmarks = new Benchmark[BENCHMARK_COUNT];

        benchmarks[DIRECT_CALL] = new Benchmark("Direct call", new T() {
                    public void run() throws Throwable {
                        testee(testData, TESTEE_ARG2, TESTEE_ARG3);
                    }
                });

        benchmarks[REFLECTION_CALL] =  new Benchmark("Reflection API Method.invoke()", new T() {
                    public void run() throws Throwable {
                        refTestee.invoke(null, testData, TESTEE_ARG2, TESTEE_ARG3);
                    }
                });

        benchmarks[INVOKE_EXACT] = new Benchmark("MH.invokeExact()", new T() {
                    public void run() throws Throwable {
                        mhTestee.invokeExact(testData, TESTEE_ARG2, TESTEE_ARG3);
                    }
                });

        benchmarks[INVOKE] = new Benchmark("MH.invoke()", new T() {
                    public void run() throws Throwable {
                        mhTestee.invokeExact(testData, TESTEE_ARG2, TESTEE_ARG3);
                    }
                });

        benchmarks[INDY] = new Benchmark("invokedynamic instruction", new T() {
                    public void run() throws Throwable {
                        indyWrapper(testData);
                    }
                });

        for (int w = 0; w < warmups; w++) {
            trace("\n======== Warming up, iteration #" + w);

            for (int i = iterations; i > 0; i--) {
                for (int r = 0; r < benchmarks.length; r++)
                    benchmarks[r].shortWarmup();
            }
        }

        final int compareToIdx = REFLECTION_CALL;
        for (int i = 0; i < measurements; i++) {
            trace("\n======== Measuring, iteration #" + i);

            for (int r = 0; r < benchmarks.length; r++) {
                benchmarks[r].run(iterations, false).report(
                        r > compareToIdx ? benchmarks[compareToIdx].runResults.getLast() : null);
            }
        }

        final Result[] results = new Result[benchmarks.length];

        print("\n======== Results (absolute)" + "; warmups: " + warmups
                + "; measurements: " + measurements + "; iterations/run: " + iterations
                + "; micro iterations: " + microIterations);

        for (int r = 0; r < benchmarks.length; r++) {
            results[r] = Result.calculate(benchmarks[r].runResults.toArray(new Measurement[0]), null);
        }

        for (int r = 0; r < benchmarks.length; r++) {
            results[r].report(r != compareToIdx ? results[compareToIdx] : null);
        }

        print("\n======== Conclusions");

        // TODO: exclude GC time, compilation time (optionally) from measurements

        print("Comparing invocation time orders");
        verifyTimeOrder(results[INDY],                    results[REFLECTION_CALL]);
        verifyTimeOrder(results[INVOKE_EXACT],            results[DIRECT_CALL]);
        verifyTimeOrder(results[INVOKE],                  results[DIRECT_CALL]);
        verifyTimeOrder(results[INVOKE_EXACT],            results[INDY]);

        return true;
    }

    // Below are routines for converting this test to a standalone one
    // This is useful if you want to run the test with JDK7 b103 release
    // where the regression can be seen
    static void print(String s) {
        Env.traceImportant(s);
    }

    static void trace(String s) {
        Env.traceNormal(s);
    }

    //boolean testFailed;
    //static void markTestFailed(String reason) {
    //    testFailed = true;
    //}

    public static void main(String[] args) {
        MlvmTest.launch(args);
    }
}

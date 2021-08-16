/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.intrinsics.bmi;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Test runner that invokes all methods implemented by particular Expr
 * with random arguments in two different JVM processes and compares output.
 * JVMs being started in different modes - one in int and other in comp
 * with C2 and disabled tiered compilation.
 */
public class BMITestRunner {

    enum VMMode {
        COMP, INT;
    };

    public static int DEFAULT_ITERATIONS_COUNT = 4000;

    /**
     * Execute all methods implemented by <b>expr</b> in int and comp modes
     * and compare output.
     * Test pass only of output obtained with different VM modes is equal.
     * To control behaviour of test following options could be passed:
     * <ul>
     *   <li>-iterations=&lt;N&gt; each operation implemented by
     *       <b>expr</b> will be executed <i>N</i> times. Default value
     *       is 4000.</li>
     * </ul>
     *
     * @param expr operation that should be tested
     * @param testOpts options to control test behaviour
     * @param additionalVMOpts additional options for VM
     *
     * @throws Throwable if test failed.
     */
    public static void runTests(Class<? extends Expr> expr,
                                String testOpts[],
                                String... additionalVMOpts)
                         throws Throwable {

        // ensure seed got printed out
        Utils.getRandomInstance();
        long seed = Utils.SEED;
        int iterations = DEFAULT_ITERATIONS_COUNT;

        for (String testOption : testOpts) {
            if (testOption.startsWith("-iterations=")) {
                iterations = Integer.valueOf(testOption.
                                             replace("-iterations=", ""));
            }
        }

        OutputAnalyzer intOutput = runTest(expr, VMMode.INT,
                                           additionalVMOpts,
                                           seed, iterations);
        OutputAnalyzer compOutput = runTest(expr, VMMode.COMP,
                                            additionalVMOpts,
                                            seed, iterations);

        dumpOutput(intOutput, "int");
        dumpOutput(compOutput, "comp");

        Asserts.assertStringsEqual(intOutput.getStdout(),
                                   compOutput.getStdout(),
                                   "Results obtained in -Xint and " +
                                   "-Xcomp should be the same.");
    }

    /**
     * Execute tests on methods implemented by <b>expr</b> in new VM
     * started in <b>testVMMode</b> mode.
     *
     * @param expr operation that should be tested
     * @param testVMMode VM mode for test
     * @param additionalVMOpts additional options for VM
     * @param seed for RNG used it tests
     * @param iterations that will be used to invoke <b>expr</b>'s methods.
     *
     * @return OutputAnalyzer for executed test.
     * @throws Throwable when something goes wrong.
     */
    public static OutputAnalyzer runTest(Class<? extends Expr> expr,
                                         VMMode testVMMode,
                                         String additionalVMOpts[],
                                         long seed, int iterations)
                                  throws Throwable {

        List<String> vmOpts = new LinkedList<String>();

        Collections.addAll(vmOpts, additionalVMOpts);

        //setup mode-specific options
        switch (testVMMode) {
        case INT:
            Collections.addAll(vmOpts, new String[] { "-Xint" });
            break;
        case COMP:
            Collections.addAll(vmOpts, new String[] {
                    "-Xcomp",
                    "-XX:-TieredCompilation",
                    String.format("-XX:CompileCommand=compileonly,%s::*",
                                  expr.getName())
                });
            break;
        }

        Collections.addAll(vmOpts, new String[] {
                "-XX:+DisplayVMOutputToStderr",
                "-D" + Utils.SEED_PROPERTY_NAME + "=" + seed,
                Executor.class.getName(),
                expr.getName(),
                new Integer(iterations).toString()
            });

        OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(vmOpts);

        outputAnalyzer.shouldHaveExitValue(0);

        return outputAnalyzer;
    }

    /**
     * Dump stdout and stderr of test process to <i>prefix</i>.test.out
     * and <i>prefix</i>.test.err respectively.
     *
     * @param outputAnalyzer OutputAnalyzer whom output should be dumped
     * @param prefix Prefix that will be used in file names.
     * @throws IOException if unable to dump output to file.
     */
    protected static void dumpOutput(OutputAnalyzer outputAnalyzer,
                                     String prefix)
                              throws IOException {
        Files.write(Paths.get(prefix + ".test.out"),
                    outputAnalyzer.getStdout().getBytes());

        Files.write(Paths.get(prefix + ".test.err"),
                    outputAnalyzer.getStderr().getBytes());
    }


    /**
     * Executor that invoke all methods implemented by particular
     * Expr instance.
     */
    public static class Executor {

        /**
         * Usage: BMITestRunner$Executor &lt;ExprClassName&gt; &lt;iterations&gt;
         */
        public static void main(String args[]) throws Exception {
            @SuppressWarnings("unchecked")
            Class<? extends Expr> exprClass =
                (Class<? extends Expr>)Class.forName(args[0]);
            Expr expr = exprClass.getConstructor().newInstance();
            int iterations = Integer.valueOf(args[1]);
            runTests(expr, iterations, Utils.getRandomInstance());
        }


        public static int[] getIntBitShifts() {
            //SIZE+1 shift is for zero.
            int data[] = new int[Integer.SIZE+1];
            for (int s = 0; s < data.length; s++) {
                data[s] = 1<<s;
            }
            return data;
        }

        public static long[] getLongBitShifts() {
            //SIZE+1 shift is for zero.
            long data[] = new long[Long.SIZE+1];
            for (int s = 0; s < data.length; s++) {
                data[s] = 1L<<s;
            }
            return data;
        }

        public static void log(String format, Object... args) {
            System.out.println(String.format(format, args));
        }

        public static void runTests(Expr expr, int iterations, Random rng) {
            runUnaryIntRegTest(expr, iterations, rng);
            runUnaryIntMemTest(expr, iterations, rng);
            runUnaryLongRegTest(expr, iterations, rng);
            runUnaryLongMemTest(expr, iterations, rng);
            runUnaryIntToLongRegTest(expr, iterations, rng);
            runBinaryRegRegIntTest(expr, iterations, rng);
            runBinaryRegMemIntTest(expr, iterations, rng);
            runBinaryMemRegIntTest(expr, iterations, rng);
            runBinaryMemMemIntTest(expr, iterations, rng);
            runBinaryRegRegLongTest(expr, iterations, rng);
            runBinaryRegMemLongTest(expr, iterations, rng);
            runBinaryMemRegLongTest(expr, iterations, rng);
            runBinaryMemMemLongTest(expr, iterations, rng);
        }

        public static void runUnaryIntRegTest(Expr expr, int iterations,
                                              Random rng) {
            if (!(expr.isUnaryArgumentSupported()
                  && expr.isIntExprSupported())) {
                return;
            }

            for (int value : getIntBitShifts()) {
                log("UnaryIntReg(0X%x) -> 0X%x",
                    value, expr.intExpr(value));
            }

            for (int i = 0; i < iterations; i++) {
                int value = rng.nextInt();
                log("UnaryIntReg(0X%x) -> 0X%x",
                    value, expr.intExpr(value));
            }
        }

        public static void runUnaryIntMemTest(Expr expr, int iterations,
                                              Random rng) {
            if (!(expr.isUnaryArgumentSupported()
                  && expr.isIntExprSupported()
                  && expr.isMemExprSupported())) {
                return;
            }

            for (int value : getIntBitShifts()) {
                log("UnaryIntMem(0X%x) -> 0X%x",
                    value, expr.intExpr(new Expr.MemI(value)));
            }

            for (int i = 0; i < iterations; i++) {
                int value = rng.nextInt();
                log("UnaryIntMem(0X%x) -> 0X%x",
                    value, expr.intExpr(new Expr.MemI(value)));
            }
        }

        public static void runUnaryLongRegTest(Expr expr, int iterations,
                                               Random rng) {
            if (!(expr.isUnaryArgumentSupported()
                  && expr.isLongExprSupported())) {
                return;
            }

            for (long value : getLongBitShifts()) {
                log("UnaryLongReg(0X%x) -> 0X%x",
                    value, expr.longExpr(value));
            }

            for (int i = 0; i < iterations; i++) {
                long value = rng.nextLong();
                log("UnaryLongReg(0X%x) -> 0X%x",
                    value, expr.longExpr(value));
            }
        }

        public static void runUnaryLongMemTest(Expr expr, int iterations,
                                               Random rng) {
            if (!(expr.isUnaryArgumentSupported()
                  && expr.isLongExprSupported()
                  && expr.isMemExprSupported())) {
                return;
            }

            for (long value : getLongBitShifts()) {
                log("UnaryLongMem(0X%x) -> 0X%x",
                    value, expr.longExpr(new Expr.MemL(value)));
            }

            for (int i = 0; i < iterations; i++) {
                long value = rng.nextLong();
                log("UnaryLongMem(0X%x) -> 0X%x",
                    value, expr.longExpr(new Expr.MemL(value)));
            }
        }

        public static void runUnaryIntToLongRegTest(Expr expr, int iterations,
                                                    Random rng) {
            if (!(expr.isUnaryArgumentSupported()
                  && expr.isIntToLongExprSupported())) {
                return;
            }

            for (int value : getIntBitShifts()) {
                log("UnaryIntToLongReg(0X%x) -> 0X%x",
                    value, expr.intToLongExpr(value));
            }

            for (int i = 0; i < iterations; i++) {
                int value = rng.nextInt();
                log("UnaryIntToLongReg(0X%x) -> 0X%x",
                    value, expr.intToLongExpr(value));
            }
        }

        public static void runBinaryRegRegIntTest(Expr expr, int iterations,
                                                  Random rng) {
            if (!(expr.isIntExprSupported()
                  && expr.isBinaryArgumentSupported())) {
                return;
            }

            for (int i = 0; i < iterations; i++) {
                int aValue = rng.nextInt();
                int bValue = rng.nextInt();
                log("BinaryIntRegReg(0X%x, 0X%x) -> 0X%x",
                    aValue, bValue, expr.intExpr(aValue, bValue));
            }
        }

        public static void runBinaryRegMemIntTest(Expr expr, int iterations,
                                                  Random rng) {
            if (!(expr.isIntExprSupported()
                  && expr.isBinaryArgumentSupported()
                  && expr.isMemExprSupported())) {
                return;
            }

            for (int i = 0; i < iterations; i++) {
                int aValue = rng.nextInt();
                int bValue = rng.nextInt();
                log("BinaryIntRegMem(0X%x, 0X%x) -> 0X%x", aValue, bValue,
                    expr.intExpr(aValue, new Expr.MemI(bValue)));
            }
        }

        public static void runBinaryMemRegIntTest(Expr expr, int iterations,
                                                  Random rng) {
            if (!(expr.isIntExprSupported()
                  && expr.isBinaryArgumentSupported()
                  && expr.isMemExprSupported())) {
                return;
            }

            for (int i = 0; i < iterations; i++) {
                int aValue = rng.nextInt();
                int bValue = rng.nextInt();
                log("BinaryIntMemReg(0X%x, 0X%x) -> 0X%x", aValue, bValue,
                    expr.intExpr(new Expr.MemI(aValue), bValue));
            }
        }

        public static void runBinaryMemMemIntTest(Expr expr, int iterations,
                                                  Random rng) {
            if (!(expr.isIntExprSupported()
                  && expr.isBinaryArgumentSupported()
                  && expr.isMemExprSupported())) {
                return;
            }

            for (int i = 0; i < iterations; i++) {
                int aValue = rng.nextInt();
                int bValue = rng.nextInt();
                log("BinaryIntMemMem(0X%x, 0X%x) -> 0X%x", aValue, bValue,
                    expr.intExpr(new Expr.MemI(aValue),
                                 new Expr.MemI(bValue)));
            }
        }

        public static void runBinaryRegRegLongTest(Expr expr,
                                                   int iterations,
                                                   Random rng) {
            if (!(expr.isLongExprSupported()
                  && expr.isBinaryArgumentSupported())) {
                return;
            }

            for (int i = 0; i < iterations; i++) {
                long aValue = rng.nextLong();
                long bValue = rng.nextLong();
                log("BinaryLongRegReg(0X%x, 0X%x) -> 0X%x", aValue, bValue,
                    expr.longExpr(aValue, bValue));
            }
        }

        public static void runBinaryRegMemLongTest(Expr expr,
                                                   int iterations,
                                                   Random rng) {
            if (!(expr.isLongExprSupported()
                  && expr.isBinaryArgumentSupported()
                  && expr.isMemExprSupported())) {
                return;
            }

            for (int i = 0; i < iterations; i++) {
                long aValue = rng.nextLong();
                long bValue = rng.nextLong();
                log("BinaryLongRegMem(0X%x, 0X%x) -> 0X%x", aValue, bValue,
                    expr.longExpr(aValue, new Expr.MemL(bValue)));
            }
        }

        public static void runBinaryMemRegLongTest(Expr expr,
                                                   int iterations,
                                                   Random rng) {
            if (!(expr.isLongExprSupported()
                  && expr.isBinaryArgumentSupported()
                  && expr.isMemExprSupported())) {
                return;
            }

            for (int i = 0; i < iterations; i++) {
                long aValue = rng.nextLong();
                long bValue = rng.nextLong();
                log("BinaryLongMemReg(0X%x, 0X%x) -> 0X%x", aValue, bValue,
                    expr.longExpr(new Expr.MemL(aValue), bValue));
            }
        }

        public static void runBinaryMemMemLongTest(Expr expr,
                                                   int iterations,
                                                   Random rng) {
            if (!(expr.isLongExprSupported()
                  && expr.isBinaryArgumentSupported()
                  && expr.isMemExprSupported())) {
                return;
            }

            for (int i = 0; i < iterations; i++) {
                long aValue = rng.nextLong();
                long bValue = rng.nextLong();
                log("BinaryLongMemMem(0X%x, 0X%x) -> 0X%x", aValue, bValue,
                    expr.longExpr(new Expr.MemL(aValue),
                                  new Expr.MemL(bValue)));
            }
        }
    }
}

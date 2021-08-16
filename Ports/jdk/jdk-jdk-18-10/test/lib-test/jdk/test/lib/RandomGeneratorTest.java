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

/*
 * @test
 * @key randomness
 * @summary Verify correctnes of the random generator from Utils.java
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver RandomGeneratorTest SAME_SEED
 * @run driver RandomGeneratorTest NO_SEED
 * @run driver RandomGeneratorTest DIFFERENT_SEED
 */

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

/**
 * The test verifies correctness of work {@link jdk.test.lib.Utils#getRandomInstance()}.
 * Test works in three modes: same seed provided, no seed provided and
 * different seed provided.
 * In the first case, the test expects that all random numbers will be repeated in all next iterations.
 * In the second case, the numbers are expected to be the same for promotable builds and different for other builds.
 * In the last case, the test expects the randomly generated numbers differ from original.
 */
public class RandomGeneratorTest {
    private static final String SEED_VM_OPTION = "-D" + Utils.SEED_PROPERTY_NAME + "=";

    public static void main( String[] args) throws Throwable {
        if (args.length == 0) {
            throw new Error("TESTBUG: No test mode provided.");
        }
        SeedOption seedOpt = SeedOption.valueOf(args[0]);
        List<String> jvmArgs = new ArrayList<String>();
        String optStr = seedOpt.getSeedOption();
        if (optStr != null) {
            jvmArgs.add(optStr);
        }
        jvmArgs.add(RandomRunner.class.getName());
        String origFileName = seedOpt.name() + "_orig";
        jvmArgs.add(origFileName);
        int fileNameIndex = jvmArgs.size() - 1;
        String[] cmdLineArgs = jvmArgs.toArray(new String[jvmArgs.size()]);
        ProcessTools.executeTestJvm(cmdLineArgs).shouldHaveExitValue(0);
        String etalon = Utils.fileAsString(origFileName).trim();
        cmdLineArgs[fileNameIndex] = seedOpt.name();
        seedOpt.verify(etalon, cmdLineArgs);
    }

    /**
     * The utility enum helps to generate an appropriate string that should be passed
     * to the command line depends on the testing mode. It is also responsible for the result
     * validation.
     */
    private enum SeedOption {
        SAME_SEED {
            @Override
            public String getSeedOption() {
                return SEED_VM_OPTION + Utils.SEED;
            }

            @Override
            protected boolean isOutputExpected(String orig, String output) {
                return output.equals(orig);
            }
        },
        DIFFERENT_SEED {
            @Override
            public String getSeedOption() {
                return SEED_VM_OPTION + Utils.getRandomInstance().nextLong();
            }

            @Override
            public void verify(String orig, String[] cmdLine) {
                cmdLine[0] = getSeedOption();
                super.verify(orig, cmdLine);
            }

            @Override
            protected boolean isOutputExpected(String orig, String output) {
                return !output.equals(orig);
            }
        },
        NO_SEED {
            @Override
            public String getSeedOption() {
                return null;
            }

            @Override
            protected boolean isOutputExpected(String orig, String output) {
                return Runtime.version().build().orElse(0) > 0 ^ !output.equals(orig);
            }
        };

        /**
         * Generates a string to be added as a command line argument.
         * It contains "-D" prefix, system property name, '=' sign
         * and seed value.
         * @return command line argument
         */
        public abstract String getSeedOption();

        protected abstract boolean isOutputExpected(String orig, String output);

        /**
         * Verifies that the original output meets expectations
         * depending on the test mode. It compares the output of second execution
         * to original one.
         * @param orig original output
         * @param cmdLine command line arguments
         * @throws Throwable - Throws an exception in case test failure.
         */
        public void verify(String orig, String[] cmdLine) {
            String output;
            OutputAnalyzer oa;
            try {
                oa = ProcessTools.executeTestJvm(cmdLine);
            } catch (Throwable t) {
                throw new Error("TESTBUG: Unexpedted exception during jvm execution.", t);
            }
            oa.shouldHaveExitValue(0);
            try {
                output = Utils.fileAsString(name()).trim();
            } catch (IOException ioe) {
                throw new Error("TESTBUG: Problem during IO operation with file: " + name(), ioe);
            }
            if (!isOutputExpected(orig, output)) {
                System.err.println("Initial output: " + orig);
                System.err.println("Second run output: " + output);
                throw new AssertionError("Unexpected random number sequence for mode: " + this.name());
            }
        }
    }

    /**
     * The helper class generates several random numbers
     * and put results to a file. The file name came as first
     * command line argument.
     */
    public static class RandomRunner {
        private static final int COUNT = 10;
        public static void main(String[] args) {
            StringBuilder sb = new StringBuilder();
            Random rng = Utils.getRandomInstance();
            for (int i = 0; i < COUNT; i++) {
                sb.append(rng.nextLong()).append(' ');
            }
            try (PrintWriter pw = new PrintWriter(new FileWriter(args[0]))) {
                pw.write(sb.toString());
            } catch (IOException ioe) {
                throw new Error("TESTBUG: Problem during IO operation with file: " + args[0], ioe);
            }
        }
    }
}

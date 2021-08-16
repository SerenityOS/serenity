/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.startupargs;

import java.util.List;
import java.util.ArrayList;

import jdk.jfr.internal.Options;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.internal.misc.Unsafe;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 *                 java.base/jdk.internal.misc
 * @run main/timeout=900 jdk.jfr.startupargs.TestMemoryOptions
 */
public class TestMemoryOptions {

    public enum Unit {
        b('b'), k('k'), m('m'), g('g');

        private char unit;

        Unit(char unit) {
            this.unit = unit;
        }

        char getUnit() {
            return unit;
        }
    };

    static class Option implements Comparable<Option> {
        static final long MIN_GLOBAL_BUFFER_COUNT = 2;
        static final long DEFAULT_GLOBAL_BUFFER_COUNT = 20;
        static final long MIN_GLOBAL_BUFFER_SIZE = 64 * 1024;
        static long DEFAULT_GLOBAL_BUFFER_SIZE = 524288;
        static final long MIN_MEMORY_SIZE = 1024 * 1024;
        static final long DEFAULT_MEMORY_SIZE = DEFAULT_GLOBAL_BUFFER_COUNT * DEFAULT_GLOBAL_BUFFER_SIZE;
        static final long MIN_THREAD_BUFFER_SIZE = 4 * 1024;
        static long DEFAULT_THREAD_BUFFER_SIZE;
        static final long PAGE_SIZE;
        static final long UNDEFINED = -1;

        static {
            PAGE_SIZE = Unsafe.getUnsafe().pageSize();
            if (PAGE_SIZE == MIN_THREAD_BUFFER_SIZE) {
                DEFAULT_THREAD_BUFFER_SIZE = PAGE_SIZE * 2;
            } else {
                DEFAULT_THREAD_BUFFER_SIZE = PAGE_SIZE;
            }
            if (PAGE_SIZE > DEFAULT_GLOBAL_BUFFER_SIZE) {
                DEFAULT_GLOBAL_BUFFER_SIZE = PAGE_SIZE;
            }
        }

        final private long min;
        final private long max;
        final private String paramName;

        private long input;
        private Unit inputUnit;
        private long result;
        private Unit resultUnit;

        private long getValueAsUnit(long value, Unit unit) {
            switch (unit) {
                case b:
                    return value;
                case k:
                    return value / 1024;
                case m:
                    return value / (1024 * 1024);
                case g:
                    return value / (1024 * 1024 * 1024);
            }
            return value;
        }

        private long getRawValue(long value, Unit unit) {
            switch (unit) {
                case b:
                    return value;
                case k:
                    return value * 1024;
                case m:
                    return value * (1024 * 1024);
                case g:
                    return value * (1024 * 1024 * 1024);
            }
            return value;
        }

        private long parseValue(String valueString, char unit) {
            if (Character.isLetter(unit)) {
                unit = Character.toLowerCase(unit);
                return getRawValue(Long.parseLong(valueString.substring(0, valueString.length() - 1), 10),
                                                  Unit.valueOf(String.valueOf(unit)));
            }
            // does not have a unit, default is bytes
            return Long.parseLong(valueString.substring(0, valueString.length()), 10);
        }

        public Option(String minString, String maxString, String paramName) {
            if (minString == null) {
                this.min = UNDEFINED;
            } else {
                char unit = minString.charAt(minString.length() - 1);
                this.min = parseValue(minString, unit);
            }
            if (maxString == null) {
                this.max = UNDEFINED;
            } else {
                char unit = maxString.charAt(maxString.length() - 1);
                this.max = parseValue(maxString, unit);
            }
            this.input = UNDEFINED;
            this.result = UNDEFINED;
            this.paramName = paramName;
        }

        private Option(long value, char unit) {
            this.resultUnit = Unit.valueOf(String.valueOf(unit));
            this.result = getRawValue(value, this.resultUnit);
            this.min = UNDEFINED;
            this.max = UNDEFINED;
            this.paramName = "";
        }

        public void setInput(long value, char unit) {
            this.inputUnit = Unit.valueOf(String.valueOf(unit));
            this.input = getRawValue(value, this.inputUnit);
        }

        public long getInput() {
            return input;
        }

        public void setResult(long value, char unit) {
            this.resultUnit = Unit.valueOf(String.valueOf(unit));
            this.result = getRawValue(value, this.resultUnit);
        }

        public long getResult() {
            return result;
        }

        public long getResultAs(Unit unit) {
            return getValueAsUnit(this.result, unit);
        }

        public String getOptionParamName() {
            return paramName;
        }

        public String getOptionParamString() {
            if (input == UNDEFINED) {
                return null;
            }
            char unit = inputUnit.getUnit();
            String unitString = Character.compare(unit, 'b') == 0 ? "" : String.valueOf(unit);
            return getOptionParamName() + "=" + Long.toString(getValueAsUnit(input, inputUnit)) + unitString;
        }

        public boolean predictedToStartVM() {
            if (input == UNDEFINED) {
                // option not set
                return true;
            }
            if (input >= 0) {
                if (min != UNDEFINED) {
                    if (max != UNDEFINED) {
                        return input >= min && input <= max;
                    }
                    return input >= min;
                }
                if (max != UNDEFINED) {
                    if (min != UNDEFINED) {
                        return input <= max && input >= min;
                    }
                }
                return true;
            }
            return false;
        }

        public boolean isSet() {
            return input != UNDEFINED;
        }

        public boolean validate() throws IllegalArgumentException {
            // a result memory options should be page aligned
            if (getResult() > PAGE_SIZE) {
                if (getResult() % PAGE_SIZE != 0) {
                    throw new IllegalArgumentException("Result value: "
                    + getResult() + " for option " + getOptionParamName() + " is not aligned to page size " + PAGE_SIZE);
                }
            }
            // if min is defined, the result value should be gteq
            if (min != UNDEFINED) {
                if (getResult() < min) {
                    throw new IllegalArgumentException("Result value: "
                    + getResult() + " for option " + getOptionParamName() + " is less than min: " + min);
                }
            }
            // if max is defined, the result values should be lteq
            if (max != UNDEFINED) {
                if (getResult() > max) {
                    throw new IllegalArgumentException("Result value: "
                    + getResult() + " for option " + getOptionParamName() + " is greater than max: " + max);
                }
            }
            return true;
        }

        @Override
        public int compareTo(Option obj) {
            if (getResult() == obj.getResult()) {
                return 0;
            }
            return getResult() > obj.getResult() ? 1 : -1;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!Option.class.isAssignableFrom(obj.getClass())) {
                return false;
            }
            final Option other = (Option) obj;

            return getResult() == other.getResult();
        }

        @Override
        public int hashCode() {
            long hash = 3;
            hash = 53 * hash * getResult();
            return (int)hash;
        }

        @Override
        public String toString() {
            return Long.toString(getResult());
        }

        public Option multiply(Option rhsFactor) {
            return new Option(getResult() * rhsFactor.getResult(), 'b');
        }

        public Option divide(Option rhsDivisor) {
            long divisor = rhsDivisor.getResult();
            if (divisor == 0) {
                return new Option(0, 'b');
            }
            return new Option(getResult() / divisor, 'b');
        }

        boolean isLessThanOrEqual(Option rhs) {
            return getResult() <= rhs.getResult();
        }

        boolean isGreaterThanOrEqual(Option rhs) {
            return getResult() >= rhs.getResult();
        }
    }

    private static class TestCase {
        private static final int MEMORYSIZE = 0;
        private static final int GLOBALBUFFERSIZE = 1;
        private static final int GLOBALBUFFERCOUNT = 2;
        private static final int THREADBUFFERSIZE = 3;

        final private List<Option> optionList = new ArrayList<Option>();
        final private String testName;

        public TestCase(String testName, boolean runTest) {
            this.testName = testName;
            Option memorySize = new Option(Long.toString(Option.MIN_MEMORY_SIZE), null, "memorysize");
            optionList.add(memorySize);

            Option globalBufferSize = new Option(Long.toString(Option.MIN_GLOBAL_BUFFER_SIZE),
                                                 null, "globalbuffersize");
            optionList.add(globalBufferSize);

            Option globalBufferCount = new Option(Long.toString(Option.MIN_GLOBAL_BUFFER_COUNT), null, "numglobalbuffers");
            optionList.add(globalBufferCount);

            Option threadBufferSize = new Option(Long.toString(Option.MIN_THREAD_BUFFER_SIZE), null, "threadbuffersize");
            optionList.add(threadBufferSize);

            if (runTest) {
                populateResults();
                validateNodes();
                validateEdges();
            }
        }

        public String getTestString() {
            String optionString = "-XX:FlightRecorderOptions:";
            for (Option o : optionList) {
                String optionParamString = o.getOptionParamString();
                if (optionParamString == null) {
                    continue;
                }
                optionString = optionString.concat(optionParamString);
                optionString = optionString.concat(",");
            }
            if (optionString.equals("-XX:FlightRecorderOptions:")) {
                return null;
            }
            // strip last ","
            optionString = optionString.substring(0, optionString.length() - 1);
            return optionString;
        }

        public String getTestName() {
            return this.testName;
        }

        private void setInputForIndex(int index, long size, char unit) {
            optionList.get(index).setInput(size, unit);
        }

        private void setResultForIndex(int index, long size, char unit) {
            optionList.get(index).setResult(size, unit);
        }

        public void setMemorySizeTestParam(long size, char unit) {
            setInputForIndex(MEMORYSIZE, size, unit);
        }

        public void setMemorySizeTestResult(long size, char unit) {
            setResultForIndex(MEMORYSIZE, size, unit);
        }

        public void setGlobalBufferSizeTestParam(long size, char unit) {
            setInputForIndex(GLOBALBUFFERSIZE, size, unit);
        }

        public void setGlobalBufferSizeTestResult(long size, char unit) {
            setResultForIndex(GLOBALBUFFERSIZE, size, unit);
        }

        public void setGlobalBufferCountTestParam(long size, char unit) {
            setInputForIndex(GLOBALBUFFERCOUNT, size, unit);
        }

        public void setGlobalBufferCountTestResult(long size, char unit) {
            setResultForIndex(GLOBALBUFFERCOUNT, size, unit);
        }

        public void setThreadBufferSizeTestParam(long size, char unit) {
            setInputForIndex(THREADBUFFERSIZE, size, unit);
        }

        public void setThreadBufferSizeTestResult(long size, char unit) {
            setResultForIndex(THREADBUFFERSIZE, size, unit);
        }

        public void validateNodes() {
            for (Option o : optionList) {
                o.validate();
            }
        }

        public void validateEdges() {
            final Option memorySize = optionList.get(MEMORYSIZE);
            final Option globalBufferSize = optionList.get(GLOBALBUFFERSIZE);
            final Option globalBufferCount = optionList.get(GLOBALBUFFERCOUNT);
            final Option threadBufferSize = optionList.get(THREADBUFFERSIZE);

            if (!memorySize.divide(globalBufferCount).equals(globalBufferSize)) {
                throw new IllegalArgumentException(getTestName() + " failure: " + memorySize.getOptionParamName() + " (" + memorySize.getResult() + ") / " +
                                                   globalBufferCount.getOptionParamName() + " (" + globalBufferCount.getResult() +
                                                   ") (" + memorySize.divide(globalBufferCount).getResult() + ") != " +
                                                   globalBufferSize.getOptionParamName() + " (" + globalBufferSize.getResult() + ")");
            }

            if (!globalBufferSize.multiply(globalBufferCount).equals(memorySize)) {
                throw new IllegalArgumentException(getTestName() + " failure: " + globalBufferSize.getOptionParamName() + ": " +
                                                   globalBufferSize.getResult() +
                                                   " * " + globalBufferCount.getOptionParamName() + ": " + globalBufferCount.getResult() +
                                                   " != " + memorySize.getOptionParamName() + ": " + memorySize.getResult());
            }

            if (!threadBufferSize.isLessThanOrEqual(globalBufferSize)) {
                throw new IllegalArgumentException(getTestName() + " failure: " + threadBufferSize.getOptionParamName() + ": " + threadBufferSize.getResult() +
                                                   " is larger than " + globalBufferSize.getOptionParamName() + ": " + globalBufferSize.getResult());
            }
        }

        public void print() {
            System.out.println("Printing information for testcase : " + getTestName());
            for (Option o : optionList) {
                System.out.println("Parameter name: " + o.getOptionParamName());
                System.out.println("Parameter test value : " + o.getOptionParamString() != null ?  o.getOptionParamString() : "not enabled for input testing");
                String inputString = o.getInput() == Option.UNDEFINED ? "N/A" : Long.toString(o.getInput());
                System.out.println("Input value: " + inputString);
                System.out.println("Predicted to start VM: " + (o.predictedToStartVM() ? "true" : "false"));
            }
        }

        private void populateResults() {
            optionList.get(MEMORYSIZE).setResult(Options.getMemorySize(), 'b');
            optionList.get(GLOBALBUFFERSIZE).setResult(Options.getGlobalBufferSize(), 'b');
            optionList.get(GLOBALBUFFERCOUNT).setResult(Options.getGlobalBufferCount(), 'b');
            optionList.get(THREADBUFFERSIZE).setResult(Options.getThreadBufferSize(), 'b');
        }

        public boolean predictedToStartVM() {
            // check each individual option
            for (Option o : optionList) {
                if (!o.predictedToStartVM()) {
                    return false;
                }
            }

            // check known invalid combinations that will not allow the VM to start

            // GLOBALBUFFERSIZE * GLOBALBUFFERCOUNT == MEMORYSIZE
            if (optionList.get(GLOBALBUFFERSIZE).isSet() && optionList.get(GLOBALBUFFERCOUNT).isSet()
               && optionList.get(MEMORYSIZE).isSet()) {
               long calculatedMemorySize = optionList.get(GLOBALBUFFERSIZE).getInput() * optionList.get(GLOBALBUFFERCOUNT).getInput();
               if (optionList.get(MEMORYSIZE).getInput() != calculatedMemorySize) {
                   return false;
               }
            }
            // GLOBALBUFFERSIZE * GLOBALBUFFERCOUNT >= MIN_MEMORY_SIZE
            if (optionList.get(GLOBALBUFFERSIZE).isSet() && optionList.get(GLOBALBUFFERCOUNT).isSet()
                && !optionList.get(MEMORYSIZE).isSet()) {
                long calculatedMemorySize = optionList.get(GLOBALBUFFERSIZE).getInput() * optionList.get(GLOBALBUFFERCOUNT).getInput();
                if (Option.MIN_MEMORY_SIZE > calculatedMemorySize) {
                    return false;
                }
            }
            // GLOBALBUFFERSIZE >= THREADBUFFERSIZE
            if (optionList.get(GLOBALBUFFERSIZE).isSet() && optionList.get(THREADBUFFERSIZE).isSet()) {
                if (optionList.get(GLOBALBUFFERSIZE).getInput() < optionList.get(THREADBUFFERSIZE).getInput()) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class SUT {
        public static void main(String[] args) {
            TestCase tc = new TestCase(args[0], true);
        }
    }

    private static class Driver {
        private static void launchTestVM(TestCase tc) throws Exception {
            final String flightRecorderOptions = tc.getTestString();
            ProcessBuilder pb;
            if (flightRecorderOptions != null) {
                pb = ProcessTools.createTestJvm("--add-exports=jdk.jfr/jdk.jfr.internal=ALL-UNNAMED",
                                                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                                                flightRecorderOptions,
                                                "-XX:StartFlightRecording",
                                                SUT.class.getName(),
                                                tc.getTestName());
            } else {
                // default, no FlightRecorderOptions passed
                pb = ProcessTools.createTestJvm("--add-exports=jdk.jfr/jdk.jfr.internal=ALL-UNNAMED",
                                                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                                                "-XX:StartFlightRecording",
                                                SUT.class.getName(),
                                                tc.getTestName());
            }

            System.out.println("Driver launching SUT with string: " + flightRecorderOptions != null ? flightRecorderOptions : "default");
            System.out.println("SUT VM " + (tc.predictedToStartVM() ? "is" : "is not") + " expected to start");
            tc.print();

            OutputAnalyzer out = ProcessTools.executeProcess(pb);

            if (tc.predictedToStartVM()) {
                out.shouldHaveExitValue(0);
            } else {
                out.shouldContain("Failure when starting JFR");
                out.shouldHaveExitValue(1);
            }
        }

        public static void runTestCase(TestCase tc) throws Exception {
            launchTestVM(tc);
        }
    }

    static private List<TestCase> testCases = new ArrayList<TestCase>();

    static {
        // positive example-based tests
        TestCase tc = new TestCase("DefaultOptionsPositive", false);
        // defaults, no options explicitly set
        testCases.add(tc);

        // explicit defaults passed as parameters
        tc = new TestCase("MemorySizePositive", false);
        tc.setMemorySizeTestParam(10, 'm');
        testCases.add(tc);

        tc = new TestCase("GlobalBufferSizePositive", false);
        tc.setGlobalBufferSizeTestParam(512, 'k');
        testCases.add(tc);

        tc = new TestCase("BufferCountPositive", false);
        tc.setGlobalBufferCountTestParam(20, 'b');
        testCases.add(tc);

        tc = new TestCase("ThreadBufferSizePositive", false);
        tc.setThreadBufferSizeTestParam(Option.DEFAULT_THREAD_BUFFER_SIZE, 'b');
        testCases.add(tc);

        // negative example-based tests, each individual option below minimum size
        tc = new TestCase("MemorySizeBelowMinNegative", false);
        tc.setMemorySizeTestParam(Option.MIN_MEMORY_SIZE - 1, 'b');
        testCases.add(tc);

        tc = new TestCase("GlobalBufferSizeBelowMinNegative", false);
        tc.setGlobalBufferSizeTestParam(Option.MIN_GLOBAL_BUFFER_SIZE - 1, 'b');
        testCases.add(tc);

        tc = new TestCase("BufferCountBelowMinNegative", false);
        tc.setGlobalBufferCountTestParam(Option.MIN_GLOBAL_BUFFER_COUNT - 1, 'b');
        testCases.add(tc);

        tc = new TestCase("ThreadBufferSizeBelowMinNegative", false);
        tc.setThreadBufferSizeTestParam(Option.MIN_THREAD_BUFFER_SIZE - 1, 'b');
        testCases.add(tc);

        // Memory size permutations
        // a few single memorysize option for deduction
        tc = new TestCase("MemorySizeValue1Positive", false);
        tc.setMemorySizeTestParam(48128, 'k'); // 47mb
        testCases.add(tc);

        tc = new TestCase("MemorySizeValue2Positive", false);
        tc.setMemorySizeTestParam(17391, 'k'); // 17808384 bytes
        testCases.add(tc);

        tc = new TestCase("MemorySizeValue3Positive", false);
        tc.setMemorySizeTestParam(Option.MIN_MEMORY_SIZE + 17, 'b');
        testCases.add(tc);

        // positive example-based-tests, memory size combined with other options
        tc = new TestCase("MemorySizeGlobalBufferSizePositive", false);
        tc.setMemorySizeTestParam(14680064, 'b'); // 14mb
        tc.setGlobalBufferSizeTestParam(720, 'k');
        testCases.add(tc);

        tc = new TestCase("MemorySizeGlobalBufferCountPositive", false);
        tc.setMemorySizeTestParam(14674581, 'b'); // 14mb - 5483 bytes
        tc.setGlobalBufferCountTestParam(17, 'b');
        testCases.add(tc);

        tc = new TestCase("MemorySizeThreadBufferSizePositive", false);
        tc.setMemorySizeTestParam(14586853, 'b'); // 14mb - 93211 bytes
        tc.setThreadBufferSizeTestParam(Option.DEFAULT_THREAD_BUFFER_SIZE, 'b');
        testCases.add(tc);

        tc = new TestCase("MemorySizeGlobalBufferSizeBufferCountPositive", false);
        tc.setMemorySizeTestParam(12240, 'k');
        tc.setGlobalBufferSizeTestParam(720, 'k'); // 720 k * 17 == 12240 k
        tc.setGlobalBufferCountTestParam(17, 'b');
        testCases.add(tc);

        tc = new TestCase("MemorySizeGlobalBufferSizeBufferCountThreadBufferSizePositive", false);
        tc.setMemorySizeTestParam(12240, 'k');
        tc.setGlobalBufferSizeTestParam(720, 'k'); // 720 k * 17 == 12240 k
        tc.setGlobalBufferCountTestParam(17, 'b');
        tc.setThreadBufferSizeTestParam(600, 'k');
        testCases.add(tc);

        // negative example-based test, create an ambiguous situtation
        tc = new TestCase("MemorySizeGlobalBufferSizeBufferCountAmbiguousNegative", false);
        tc.setMemorySizeTestParam(12240, 'k');
        tc.setGlobalBufferSizeTestParam(720, 'k'); // 720 k * 19 != 12240 k
        tc.setGlobalBufferCountTestParam(19, 'b');
        testCases.add(tc);

        // Global buffer size permutations
        tc = new TestCase("GlobalBufferSizeBufferCountPositive", false);
        tc.setGlobalBufferSizeTestParam(917, 'k');
        tc.setGlobalBufferCountTestParam(31, 'b');
        testCases.add(tc);

        tc = new TestCase("GlobalBufferSizeBufferCountThreadBufferSizePositive", false);
        tc.setGlobalBufferSizeTestParam(917, 'k');
        tc.setGlobalBufferCountTestParam(31, 'b');
        tc.setThreadBufferSizeTestParam(760832, 'b'); // 743 k
        testCases.add(tc);

        // negative example-based test, let thread buffer size > global buffer size
        tc = new TestCase("GlobalBufferSizeLessThanThreadBufferSizeNegative", false);
        tc.setGlobalBufferSizeTestParam(917, 'k');
        tc.setThreadBufferSizeTestParam(1317, 'k');
        testCases.add(tc);

        // explicitly specifying global buffer size and global buffer count must be gteq min memorysize
        tc = new TestCase("GlobalBufferSizeTimesGlobalBufferCountLessThanMinMemorySizeNegative", false);
        tc.setGlobalBufferSizeTestParam(67857, 'b');
        tc.setGlobalBufferCountTestParam(3, 'b');
        testCases.add(tc);

        // absolute minimum size
        tc = new TestCase("GlobalBufferSizeTimesGlobalBufferCountEqualToMinMemorySizePositive", false);
        tc.setGlobalBufferSizeTestParam(64, 'k');
        tc.setGlobalBufferCountTestParam(16, 'b');
        testCases.add(tc);

        // threadbuffersize exceeds default memorysize
        tc = new TestCase("ThreadBufferSizeExceedMemorySize", false);
        tc.setThreadBufferSizeTestParam(30, 'm');
        testCases.add(tc);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Testing " + testCases.size() + " number of testcases");
        for (TestCase tc : testCases) {
            Driver.runTestCase(tc);
        }
    }
}

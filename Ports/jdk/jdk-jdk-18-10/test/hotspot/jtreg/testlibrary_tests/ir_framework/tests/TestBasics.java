/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package ir_framework.tests;

import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.test.TestVM;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

/*
 * @test
 * @requires vm.compiler2.enabled & vm.flagless
 * @summary Test basics of the framework. This test runs directly the test VM which normally does not happen.
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -Xbatch ir_framework.tests.TestBasics
 */

public class TestBasics {
    private static boolean wasExecuted = false;
    private boolean lastToggleBoolean = true;
    private final static int[] executed = new int[100];
    private final static int[] executedOnce = new int[5];
    private long[] nonFloatingRandomNumbers = new long[10];
    private double[] floatingRandomNumbers = new double[10];
    private Boolean[] randomBooleans = new Boolean[64];

    public static void main(String[] args) throws Exception {
        // Run on same VM to make this test easier as we are not interested in any output processing.
        Class<?> c = TestFramework.class; // Enable JTreg test to compile TestFramework
        Method runTestsOnSameVM = TestVM.class.getDeclaredMethod("runTestsOnSameVM", Class.class);
        runTestsOnSameVM.setAccessible(true);
        runTestsOnSameVM.invoke(null, new Object[]{ null });

        if (wasExecuted) {
            throw new RuntimeException("Executed non @Test method or a method that was not intended to be run");
        }
        for (int i = 0; i < executed.length; i++) {
            int value = executed[i];
            if (value != TestVM.WARMUP_ITERATIONS + 1) {
                // Warmups + 1 C2 compiled invocation
                throw new RuntimeException("Test " + i + "  was executed " + value + " times instead stead of "
                                           + (TestVM.WARMUP_ITERATIONS + 1) + " times." );
            }
        }

        for (int value : executedOnce) {
            if (value != 1) {
                throw new RuntimeException("Check function should have been executed exactly once");
            }
        }
    }

    private void clearNonFloatingRandomNumbers() {
        nonFloatingRandomNumbers = new long[10];
    }

    private void clearFloatingRandomNumbers() {
        floatingRandomNumbers = new double[10];
    }

    private void clearRandomBooleans() {
        randomBooleans = new Boolean[64];
    }

    // Base test, no arguments, directly invoked.
    @Test
    public void test() {
        executed[0]++;
    }

    // Not a test
    public void noTest() {
        wasExecuted = true;
    }

    // Not a test
    public void test2() {
        wasExecuted = true;
    }

    // Can overload non- @Test
    public static void test2(int i) {
        wasExecuted = true;
    }

    // Can overload a @Test if it is not a @Test itself.
    public static void test(double i) {
        wasExecuted = true;
    }

    @Test
    public static void staticTest() {
        executed[1]++;
    }

    @Test
    public final void finalTest() {
        executed[2]++;
    }

    @Test
    public int returnValueTest() {
        executed[3]++;
        return 4;
    }

    // Base test, with arguments, directly invoked.
    // Specify the argument values with @Arguments
    @Test
    @Arguments(Argument.DEFAULT)
    public void byteDefaultArgument(byte x) {
        executed[4]++;
        if (x != 0) {
            throw new RuntimeException("Must be 0");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void shortDefaultArgument(short x) {
        executed[5]++;
        if (x != 0) {
            throw new RuntimeException("Must be 0");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void intDefaultArgument(int x) {
        executed[6]++;
        if (x != 0) {
            throw new RuntimeException("Must be 0");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void longDefaultArgument(long x) {
        executed[7]++;
        if (x != 0L) {
            throw new RuntimeException("Must be 0");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void floatDefaultArgument(float x) {
        executed[8]++;
        if (x != 0.0f) {
            throw new RuntimeException("Must be 0.0");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void doubleDefaultArgument(double x) {
        executed[9]++;
        if (x != 0.0f) {
            throw new RuntimeException("Must be 0.0");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void charDefaultArgument(char x) {
        executed[10]++;
        if (x != '\u0000') {
            throw new RuntimeException("Must be \u0000");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void booleanDefaultArgument(boolean x) {
        executed[11]++;
        if (x) {
            throw new RuntimeException("Must be false");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void stringObjectDefaultArgument(String x) {
        executed[12]++;
        if (x == null || x.length() != 0) {
            throw new RuntimeException("Default string object must be non-null and having a length of zero");
        }
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void defaultObjectDefaultArgument(DefaultObject x) {
        executed[13]++;
        if (x == null || x.i != 4) {
            throw new RuntimeException("Default object must not be null and its i field must be 4");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_42)
    public void byte42(byte x) {
        executed[14]++;
        if (x != 42) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_42)
    public void short42(short x) {
        executed[15]++;
        if (x != 42) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_42)
    public void int42(int x) {
        executed[16]++;
        if (x != 42) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_42)
    public void long42(long x) {
        executed[17]++;
        if (x != 42) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_42)
    public void float42(float x) {
        executed[18]++;
        if (x != 42.0) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_42)
    public void double42(double x) {
        executed[19]++;
        if (x != 42.0) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(Argument.FALSE)
    public void booleanFalse(boolean x) {
        executed[20]++;
        if (x) {
            throw new RuntimeException("Must be false");
        }
    }

    @Test
    @Arguments(Argument.TRUE)
    public void booleanTrue(boolean x) {
        executed[21]++;
        if (!x) {
            throw new RuntimeException("Must be true");
        }
    }

    @Test
    @Arguments(Argument.RANDOM_ONCE)
    public void randomByte(byte x) {
        executed[22]++;
    }

    @Test
    @Arguments(Argument.RANDOM_ONCE)
    public void randomShort(short x) {
        executed[23]++;
    }

    @Test
    @Arguments(Argument.RANDOM_ONCE)
    public void randomInt(int x) {
        executed[24]++;
    }

    @Test
    @Arguments(Argument.RANDOM_ONCE)
    public void randomLong(long x) {
        executed[25]++;
    }

    @Test
    @Arguments(Argument.RANDOM_ONCE)
    public void randomFloat(float x) {
        executed[26]++;
    }

    @Test
    @Arguments(Argument.RANDOM_ONCE)
    public void randomDouble(double x) {
        executed[27]++;
    }

    // Not executed
    public void randomNotExecutedTest(double x) {
        wasExecuted = true;
    }

    @Test
    @Arguments(Argument.RANDOM_ONCE)
    public void randomBoolean(boolean x) {
        executed[28]++;
    }

    @Test
    @Arguments(Argument.BOOLEAN_TOGGLE_FIRST_FALSE)
    public void booleanToggleFirstFalse(boolean x) {
        if (executed[29] == 0) {
            // First invocation
            if (x) {
                throw new RuntimeException("BOOLEAN_TOGGLE_FIRST_FALSE must be false on first invocation");
            }
        } else if (x == lastToggleBoolean) {
            throw new RuntimeException("BOOLEAN_TOGGLE_FIRST_FALSE did not toggle");
        }
        lastToggleBoolean = x;
        executed[29]++;
    }

    @Test
    @Arguments(Argument.RANDOM_EACH)
    public void randomEachByte(byte x) {
        checkNonFloatingRandomNumber(x, executed[30]);
        executed[30]++;
    }

    @Test
    @Arguments(Argument.RANDOM_EACH)
    public void randomEachShort(short x) {
        checkNonFloatingRandomNumber(x, executed[31]);
        executed[31]++;
    }

    @Test
    @Arguments(Argument.RANDOM_EACH)
    public void randomEachInt(int x) {
        checkNonFloatingRandomNumber(x, executed[32]);
        executed[32]++;
    }

    @Test
    @Arguments(Argument.RANDOM_EACH)
    public void randomEachLong(long x) {
        checkNonFloatingRandomNumber(x, executed[33]);
        executed[33]++;
    }

    @Test
    @Arguments(Argument.RANDOM_EACH)
    public void randomEachChar(char x) {
        checkNonFloatingRandomNumber(x, executed[34]);
        executed[34]++;
    }

    @Test
    @Arguments(Argument.RANDOM_EACH)
    public void randomEachFloat(float x) {
        checkFloatingRandomNumber(x, executed[35]);
        executed[35]++;
    }

    @Test
    @Arguments(Argument.RANDOM_EACH)
    public void randomEachDouble(double x) {
        checkFloatingRandomNumber(x, executed[36]);
        executed[36]++;
    }

    @Test
    @Arguments(Argument.RANDOM_EACH)
    public void randomEachBoolean(boolean x) {
        checkRandomBoolean(x, executed[37]);
        executed[37]++;
    }

    private void checkNonFloatingRandomNumber(long x, int invocationCount) {
        int mod10 = invocationCount % 10;
        if (invocationCount > 0 && mod10 == 0) {
            // Not first invocation
            // Check the last 10 numbers and ensure that there are at least 2 different ones.
            // All numbers are equal? Very unlikely nd we should really consider to play the lottery...
            long first = nonFloatingRandomNumbers[0];
            if (Arrays.stream(nonFloatingRandomNumbers).allMatch(n -> n == first)) {
                throw new RuntimeException("RANDOM_EACH does not generate random integer numbers");
            }
            clearNonFloatingRandomNumbers();
        }
        nonFloatingRandomNumbers[mod10] = x;
    }

    private void checkFloatingRandomNumber(double x, int invocationCount) {
        int mod10 = invocationCount % 10;
        if (invocationCount > 0 && mod10 == 0) {
            // Not first invocation
            // Check the last 10 numbers and ensure that there are at least 2 different ones.
            // All numbers are equal? Very unlikely nd we should really consider to play the lottery...
            double first = floatingRandomNumbers[0];
            if (Arrays.stream(floatingRandomNumbers).allMatch(n -> n == first)) {
                throw new RuntimeException("RANDOM_EACH does not generate random floating point numbers");
            }
            clearFloatingRandomNumbers();
        }
        floatingRandomNumbers[mod10] = x;
    }

    private void checkRandomBoolean(boolean x, int invocationCount) {
        int mod64 = invocationCount % 64;
        if (invocationCount > 0 && mod64 == 0) {
            // Not first invocation
            // Check the last 64 booleans and ensure that there are at least one true and one false.
            // All booleans are equal? Very unlikely (chance of 2^64) and we should really consider
            // to play the lottery...
            if (Arrays.stream(randomBooleans).allMatch(b -> b == randomBooleans[0])) {
                throw new RuntimeException("RANDOM_EACH does not generate random booleans");
            }
            clearRandomBooleans();
        }
        randomBooleans[mod64] = x;
    }


    @Test
    @Arguments(Argument.NUMBER_MINUS_42)
    public void byteMinus42(byte x) {
        executed[38]++;
        if (x != -42) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_MINUS_42)
    public void shortMinus42(short x) {
        executed[39]++;
        if (x != -42) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_MINUS_42)
    public void intMinus42(int x) {
        executed[40]++;
        if (x != -42) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_MINUS_42)
    public void longMinus42(long x) {
        executed[41]++;
        if (x != -42) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_MINUS_42)
    public void floatMinus42(float x) {
        executed[42]++;
        if (x != -42.0) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(Argument.NUMBER_MINUS_42)
    public void doubleMinus42(double x) {
        executed[43]++;
        if (x != -42.0) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(Argument.MIN)
    public void byteMin(byte x) {
        executed[79]++;
        if (x != Byte.MIN_VALUE) {
            throw new RuntimeException("Must be MIN_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MIN)
    public void charMin(char x) {
        executed[80]++;
        if (x != Character.MIN_VALUE) {
            throw new RuntimeException("Must be MIN_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MIN)
    public void shortMin(short x) {
        executed[81]++;
        if (x != Short.MIN_VALUE) {
            throw new RuntimeException("Must be MIN_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MIN)
    public void intMin(int x) {
        executed[82]++;
        if (x != Integer.MIN_VALUE) {
            throw new RuntimeException("Must be MIN_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MIN)
    public void longMin(long x) {
        executed[83]++;
        if (x != Long.MIN_VALUE) {
            throw new RuntimeException("Must be MIN_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MIN)
    public void floatMin(float x) {
        executed[84]++;
        if (x != Float.MIN_VALUE) {
            throw new RuntimeException("Must be MIN_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MIN)
    public void doubleMin(double x) {
        executed[85]++;
        if (x != Double.MIN_VALUE) {
            throw new RuntimeException("Must be MIN_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MAX)
    public void byteMax(byte x) {
        executed[86]++;
        if (x != Byte.MAX_VALUE) {
            throw new RuntimeException("Must be MAX_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MAX)
    public void charMax(char x) {
        executed[87]++;
        if (x != Character.MAX_VALUE) {
            throw new RuntimeException("Must be MAX_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MAX)
    public void shortMax(short x) {
        executed[88]++;
        if (x != Short.MAX_VALUE) {
            throw new RuntimeException("Must be MAX_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MAX)
    public void intMax(int x) {
        executed[89]++;
        if (x != Integer.MAX_VALUE) {
            throw new RuntimeException("Must be MAX_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MAX)
    public void longMax(long x) {
        executed[90]++;
        if (x != Long.MAX_VALUE) {
            throw new RuntimeException("Must be MAX_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MAX)
    public void floatMax(float x) {
        executed[91]++;
        if (x != Float.MAX_VALUE) {
            throw new RuntimeException("Must be MAX_VALUE");
        }
    }

    @Test
    @Arguments(Argument.MAX)
    public void doubleMax(double x) {
        executed[78]++;
        if (x != Double.MAX_VALUE) {
            throw new RuntimeException("Must be MAX_VALUE");
        }
    }

    @Test
    @Arguments({Argument.DEFAULT, Argument.DEFAULT})
    public void twoArgsDefault1(byte x, short y) {
        executed[44]++;
        if (x != 0 || y != 0) {
            throw new RuntimeException("Both must be 0");
        }
    }

    @Test
    @Arguments({Argument.DEFAULT, Argument.DEFAULT})
    public void twoArgsDefault2(int x, short y) {
        executed[45]++;
        if (x != 0 || y != 0) {
            throw new RuntimeException("Both must be 0");
        }
    }

    @Test
    @Arguments({Argument.DEFAULT, Argument.DEFAULT})
    public void twoArgsDefault3(short x, long y) {
        executed[46]++;
        if (x != 0 || y != 0) {
            throw new RuntimeException("Both must be 0");
        }
    }

    @Test
    @Arguments({Argument.DEFAULT, Argument.DEFAULT})
    public void twoArgsDefault4(float x, boolean y) {
        executed[47]++;
        if (x != 0.0 || y) {
            throw new RuntimeException("Must be 0 and false");
        }
    }

    @Test
    @Arguments({Argument.DEFAULT, Argument.DEFAULT})
    public void twoArgsDefault5(boolean x, char y) {
        executed[48]++;
        if (x || y != '\u0000') {
            throw new RuntimeException("Must be false and \u0000");
        }
    }

    @Test
    @Arguments({Argument.DEFAULT, Argument.DEFAULT})
    public void twoArgsDefault6(char x, byte y) {
        executed[49]++;
        if (x != '\u0000' || y != 0) {
            throw new RuntimeException("Must be\u0000 and 0");
        }
    }

    @Test
    @Arguments({Argument.RANDOM_ONCE, Argument.RANDOM_ONCE})
    public void twoArgsRandomOnce(char x, byte y) {
        executed[50]++;
    }

    @Test
    @Arguments({Argument.RANDOM_ONCE, Argument.RANDOM_ONCE,
                Argument.RANDOM_ONCE, Argument.RANDOM_ONCE,
                Argument.RANDOM_ONCE, Argument.RANDOM_ONCE,
                Argument.RANDOM_ONCE, Argument.RANDOM_ONCE})
    public void checkRandomOnceDifferentArgs(int a, int b, int c, int d, int e, int f, int g, int h) {
        if (Stream.of(a, b, c, d, e, f, g, h).allMatch(i -> i == a)) {
            throw new RuntimeException("RANDOM_ONCE does not produce random values for different arguments");
        }
        executed[51]++;
    }

    @Test
    @Arguments({Argument.RANDOM_ONCE, Argument.RANDOM_ONCE,
                Argument.RANDOM_ONCE, Argument.RANDOM_ONCE,
                Argument.RANDOM_ONCE, Argument.RANDOM_ONCE,
                Argument.RANDOM_ONCE, Argument.RANDOM_ONCE})
    public void checkMixedRandoms1(byte a, short b, int c, long d, char e, boolean f, float g, double h) {
        executed[52]++;
    }

    @Test
    @Arguments({Argument.RANDOM_EACH, Argument.RANDOM_EACH,
                Argument.RANDOM_EACH, Argument.RANDOM_EACH,
                Argument.RANDOM_EACH, Argument.RANDOM_EACH,
                Argument.RANDOM_EACH, Argument.RANDOM_EACH})
    public void checkMixedRandoms2(byte a, short b, int c, long d, char e, boolean f, float g, double h) {
        executed[53]++;
    }

    @Test
    @Arguments({Argument.RANDOM_ONCE, Argument.RANDOM_ONCE,
                Argument.RANDOM_EACH, Argument.RANDOM_EACH,
                Argument.RANDOM_ONCE, Argument.RANDOM_EACH,
                Argument.RANDOM_EACH, Argument.RANDOM_ONCE})
    public void checkMixedRandoms3(byte a, short b, int c, long d, char e, boolean f, float g, double h) {
        executed[54]++;
    }

    @Test
    @Arguments({Argument.NUMBER_42, Argument.NUMBER_42,
                Argument.NUMBER_42, Argument.NUMBER_42,
                Argument.NUMBER_42, Argument.NUMBER_42})
    public void check42Mix1(byte a, short b, int c, long d, float e, double f) {
        if (a != 42 || b != 42 || c != 42 || d != 42 || e != 42.0 || f != 42.0) {
            throw new RuntimeException("Must all be 42");
        }
        executed[55]++;
    }

    @Test
    @Arguments({Argument.NUMBER_MINUS_42, Argument.NUMBER_MINUS_42,
                Argument.NUMBER_MINUS_42, Argument.NUMBER_MINUS_42,
                Argument.NUMBER_MINUS_42, Argument.NUMBER_MINUS_42})
    public void check42Mix2(byte a, short b, int c, long d, float e, double f) {
        if (a != -42 || b != -42 || c != -42 || d != -42 || e != -42.0 || f != -42.0) {
            throw new RuntimeException("Must all be -42");
        }
        executed[56]++;
    }

    @Test
    @Arguments({Argument.NUMBER_MINUS_42, Argument.NUMBER_42,
                Argument.NUMBER_MINUS_42, Argument.NUMBER_MINUS_42,
                Argument.NUMBER_42, Argument.NUMBER_MINUS_42})
    public void check42Mix3(byte a, short b, int c, long d, float e, double f) {
        if (a != -42 || b != 42 || c != -42 || d != -42 || e != 42.0 || f != -42.0) {
            throw new RuntimeException("Do not match the right 42 version");
        }
        executed[57]++;
    }


    @Test
    @Arguments(Argument.BOOLEAN_TOGGLE_FIRST_TRUE)
    public void booleanToggleFirstTrue(boolean x) {
        if (executed[58] == 0) {
            // First invocation
            if (!x) {
                throw new RuntimeException("BOOLEAN_TOGGLE_FIRST_FALSE must be false on first invocation");
            }
        } else if (x == lastToggleBoolean) {
            throw new RuntimeException("BOOLEAN_TOGGLE_FIRST_FALSE did not toggle");
        }
        lastToggleBoolean = x;
        executed[58]++;
    }

    @Test
    @Arguments({Argument.BOOLEAN_TOGGLE_FIRST_FALSE, Argument.BOOLEAN_TOGGLE_FIRST_TRUE})
    public void checkTwoToggles(boolean b1, boolean b2) {
        if (executed[59] == 0) {
            // First invocation
            if (b1 || !b2) {
                throw new RuntimeException("BOOLEAN_TOGGLES have wrong initial value");
            }
        } else if (b1 == b2) {
            throw new RuntimeException("Boolean values must be different");
        } else if (b1 == lastToggleBoolean) {
            throw new RuntimeException("Booleans did not toggle");
        }
        lastToggleBoolean = b1;
        executed[59]++;
    }

    @Test
    @Arguments({Argument.BOOLEAN_TOGGLE_FIRST_FALSE, Argument.FALSE,
                Argument.TRUE, Argument.BOOLEAN_TOGGLE_FIRST_TRUE})
    public void booleanMix(boolean b1, boolean b2, boolean b3, boolean b4) {
        if (executed[60] == 0) {
            // First invocation
            if (b1 || b2 || !b3 || !b4) {
                throw new RuntimeException("BOOLEAN_TOGGLES have wrong initial value");
            }
        } else if (b1 == b4) {
            throw new RuntimeException("Boolean values must be different");
        } else if (b1 == lastToggleBoolean) {
            throw new RuntimeException("Booleans did not toggle");
        }
        lastToggleBoolean = b1;
        executed[60]++;
    }

    /*
     * Checked tests.
     */

    @Test
    public int testCheck() {
        executed[63]++;
        return 1;
    }

    // Checked test. Check invoked after invoking "testCheck". Perform some more things after invocation.
    @Check(test = "testCheck")
    public void checkTestCheck() {
        executed[64]++; // Executed on each invocation
    }

    @Test
    public int testCheckReturn() {
        executed[65]++;
        return 2;
    }

    // Checked test with return value. Perform checks on it.
    @Check(test = "testCheckReturn")
    public void checkTestCheckReturn(int returnValue) {
        if (returnValue != 2) {
            throw new RuntimeException("Must be 2");
        }
        executed[66]++; // Executed on each invocation
    }

    @Test
    @Arguments(Argument.NUMBER_42)
    public short testCheckWithArgs(short x) {
        executed[94]++;
        return x;
    }

    @Check(test = "testCheckWithArgs")
    public void checkTestCheckWithArgs(short returnValue) {
        if (returnValue != 42) {
            throw new RuntimeException("Must be 42");
        }
        executed[95]++; // Executed on each invocation
    }

    @Test
    public int testCheckTestInfo() {
        executed[67]++;
        return 3;
    }

    // Checked test with info object about test.
    @Check(test = "testCheckTestInfo")
    public void checkTestCheckTestInfo(TestInfo testInfo) {
        executed[68]++; // Executed on each invocation
    }


    @Test
    public int testCheckBoth() {
        executed[69]++;
        return 4;
    }

    // Checked test with return value and info object about test.
    @Check(test = "testCheckBoth")
    public void checkTestCheckTestInfo(int returnValue, TestInfo testInfo) {
        if (returnValue != 4) {
            throw new RuntimeException("Must be 4");
        }
        executed[70]++; // Executed on each invocation
    }

    @Test
    public int testCheckOnce() {
        executed[71]++;
        return 1;
    }

    // Check method only invoked once after method is compiled after warm up.
    @Check(test = "testCheckOnce", when = CheckAt.COMPILED)
    public void checkTestCheckOnce() {
        executedOnce[0]++; // Executed once
    }

    @Test
    public int testCheckReturnOnce() {
        executed[72]++;
        return 2;
    }

    @Check(test = "testCheckReturnOnce", when = CheckAt.COMPILED)
    public void checkTestCheckReturnOnce(int returnValue) {
        if (returnValue != 2) {
            throw new RuntimeException("Must be 2");
        }
        executedOnce[1]++; // Executed once
    }

    @Test
    public int testCheckTestInfoOnce() {
        executed[73]++;
        return 3;
    }

    @Check(test = "testCheckTestInfoOnce", when = CheckAt.COMPILED)
    public void checkTestCheckTestInfoOnce(TestInfo testInfo) {
        executedOnce[2]++; // Executed once
    }

    @Test
    public int testCheckBothOnce() {
        executed[74]++;
        return 4;
    }

    @Check(test = "testCheckBothOnce", when = CheckAt.COMPILED)
    public void checkTestCheckBothOnce(int returnValue, TestInfo testInfo) {
        if (returnValue != 4) {
            throw new RuntimeException("Must be 4");
        }
        executedOnce[3]++; // Executed once
    }

    @Test
    public void sameName() {
        executed[76]++;
    }

    // Allowed to overload test method if not test method itself
    public void sameName(boolean a) {
        wasExecuted = true;
    }

    // Allowed to overload test method if not test method itself
    @Check(test = "sameName")
    public void sameName(TestInfo info) {
        executed[77]++;
    }


    /*
     * Custom run tests.
     */

    @Test
    public void sameName2() {
        executed[92]++;
    }

    // Allowed to overload test method if not test method itself
    @Run(test = "sameName2")
    public void sameName2(RunInfo info) {
        executed[93]++;
        sameName2();
    }

    @Test
    public void testRun() {
        executed[61]++;
    }

    // Custom run test. This method is invoked each time instead of @Test method. This method responsible for calling
    // the @Test method. @Test method is compiled after warm up. This is similar to the verifiers in the old Valhalla framework.
    @Run(test = "testRun")
    public void runTestRun(RunInfo info) {
        testRun();
    }

    @Test
    public void testRunNoTestInfo(int i) { // Argument allowed when run by @Run
        executed[62]++;
    }

    @Run(test = "testRunNoTestInfo")
    public void runTestRunNoTestInfo() {
        testRunNoTestInfo(3);
    }

    @Test
    public void testNotRun() {
        wasExecuted = true;
    }

    @Run(test = "testNotRun")
    public void runTestNotRun() {
        // Do not execute the test. Pointless but need to test that as well.
    }

    @Test
    public void testRunOnce() {
        executedOnce[4]++;
    }

    // Custom run test that is only invoked once. There is no warm up and no compilation. This method is responsible
    // for triggering compilation.
    @Run(test = "testRunOnce", mode = RunMode.STANDALONE)
    public void runTestRunOnce(RunInfo info) {
        testRunOnce();
    }

    @Test
    public void testRunOnce2() {
        executed[75]++;
    }

    @Run(test = "testRunOnce2", mode = RunMode.STANDALONE)
    public void runTestRunOnce2(RunInfo info) {
        for (int i = 0; i < TestVM.WARMUP_ITERATIONS + 1; i++) {
            testRunOnce2();
        }
    }

    @Test
    public void testRunMultiple() {
        executed[96]++;
    }

    @Test
    public void testRunMultiple2() {
        executed[97]++;
    }

    @Test
    public void testRunMultipleNotExecuted() {
        wasExecuted = true;
    }

    @Run(test = {"testRunMultiple", "testRunMultiple2", "testRunMultipleNotExecuted"})
    public void runTestRunMultiple() {
        testRunMultiple();
        testRunMultiple2();
    }


    @Test
    public void testRunMultiple3() {
        executed[98]++;
    }

    @Test
    public void testRunMultiple4() {
        executed[99]++;
    }

    @Test
    public void testRunMultipleNotExecuted2() {
        wasExecuted = true;
    }

    @Run(test = {"testRunMultiple3", "testRunMultiple4", "testRunMultipleNotExecuted2"}, mode = RunMode.STANDALONE)
    public void runTestRunMultipl2(RunInfo info) {
        for (int i = 0; i < TestVM.WARMUP_ITERATIONS + 1; i++) {
            testRunMultiple3();
            testRunMultiple4();
        }
    }
}

class DefaultObject {
    int i = 4;
}

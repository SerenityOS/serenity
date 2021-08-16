/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test InlineMatcherTest
 * @bug 8074095
 * @summary Testing of compiler/InlineMatcher
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      compiler.compilercontrol.InlineMatcherTest
 */

package compiler.compilercontrol;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class InlineMatcherTest {

    /** Instance of WhiteBox */
    protected static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    Method helper;
    Method getDate;
    Method inner;
    Method toString;

    final public static int FORCE_INLINE = 2;
    final public static int DONT_INLINE = 1;
    final public static int NO_MATCH = 0;
    final public static int PARSING_FAILURE = -1;

    public InlineMatcherTest() {

    }

    public void test() throws Exception {
        // instantiate before calling getMethod on innerHelper
        TestCases testCases = new TestCases();

        helper = getMethod(InlineMatcherTest.class, "helper");

        testCases.add(helper, "*.*", PARSING_FAILURE);
        testCases.add(helper, "+*.*", FORCE_INLINE);
        testCases.add(helper, "++*.*", NO_MATCH); // + is a valid part of the
                                                  // class name
        testCases.add(helper, "-*.*", DONT_INLINE);
        testCases.add(helper, "--*.*", NO_MATCH); // - is a valid part of the
                                                  // class name

        String className = this.getClass().getName().replace('.', '/');
        testCases.add(helper, "+" + className + ".*", FORCE_INLINE);
        testCases.add(helper, "+" + className + ".helper", FORCE_INLINE);
        testCases.add(helper, "+" + className + ".helper()", FORCE_INLINE);
        testCases.add(helper, "+" + className + ".helper()V", FORCE_INLINE);
        testCases.add(helper, "+" + className + ".helper(", FORCE_INLINE);

        testCases.add(helper, "-" + className + ".*", DONT_INLINE);
        testCases.add(helper, "-" + className + ".helper", DONT_INLINE);
        testCases.add(helper, "-" + className + ".helper()", DONT_INLINE);
        testCases.add(helper, "-" + className + ".helper()V", DONT_INLINE);
        testCases.add(helper, "-" + className + ".helper(", DONT_INLINE);

        testCases.add(helper, "+abc.*", NO_MATCH);
        testCases.add(helper, "+*.abc", NO_MATCH);
        testCases.add(helper, "-abc.*", NO_MATCH);
        testCases.add(helper, "-*.abcls ", NO_MATCH);

        int failures = 0;

        for (TestCase t : testCases) {
            System.out.println("Test case: " + t.pattern);
            if (!t.test()) {
                failures++;
                System.out.println(" * FAILED");
            }
        }
        if (failures != 0) {
            throw new Exception("There where " + failures + " failures in this test");
        }
    }

    public static void main(String... args) throws Exception {
        InlineMatcherTest test = new InlineMatcherTest();
        test.test();
    }

    public void helper() {

    }

    private static Method getMethod(Class klass, String name, Class<?>... parameterTypes) {
        try {
            return klass.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("exception on getting method Helper." + name, e);
        }
    }

    class TestCase {
        String pattern;
        Method testTarget;
        int expectedResult;

        public TestCase(Method testTarget, String pattern, int expectedResult) {
            this.testTarget = testTarget;
            this.pattern = pattern;
            this.expectedResult = expectedResult;
        }

        public String resultAsStr(int errorCode) {
            switch (errorCode) {
            case PARSING_FAILURE:
                return "Parsing failed";
            case NO_MATCH:
                return "No match";
            case DONT_INLINE:
                return "Dont Inline";
            case FORCE_INLINE:
                return "Force Inline";
            default:
                return "Unknown error";
            }
        }

        boolean test() {
            int result = WHITE_BOX.matchesInline(testTarget, pattern);
            if (result != expectedResult) {
                System.out
                        .println("FAIL Wrong result, Got: " + resultAsStr(result) + "\n TestCase: " + this.toString());
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Method: '" + testTarget.toString() + "' Pattern: '" + pattern + "' Expected: "
                    + resultAsStr(expectedResult);
        }

        public void innerHelper() {

        }
    }

    class TestCases extends ArrayList<TestCase> {
        private static final long serialVersionUID = 1L;

        public boolean add(Method testTarget, String pattern, int expectedResult) {
            return super.add(new TestCase(testTarget, pattern, expectedResult));
        }
    }
}

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
 * @test MethodMatcherTest
 * @summary Testing of compiler/MethodMatcher
 * @bug 8135068
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   compiler.oracle.MethodMatcherTest
 */

package compiler.oracle;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MethodMatcherTest {

    /** Instance of WhiteBox */
    protected static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    Method helper;
    Method getDate;
    Method inner;
    Method toString;

    static final int MATCH = 1;
    static final int NO_MATCH = 0;
    static final int PARSING_FAILURE = -1;

    public MethodMatcherTest() {
    }

    public void test() throws Exception {
        // instantiate before calling getMethod on innerHelper
        TestCases testCases = new TestCases();

        helper = getMethod(MethodMatcherTest.class, "helper");
        getDate = getMethod(java.util.Date.class, "getDate");
        inner = getMethod(TestCases.class, "innerHelper");
        toString = getMethod(String.class, "toString");

        testCases.add(helper, "pool/sub/Klass.method(I[Ljava/lang/String;Ljava/lang/Integer;[B[[D)V", NO_MATCH);

        // These should be improved to parsing failed in the future
        testCases.add(helper, "*Klass*,*$method*::", NO_MATCH);
        testCases.add(helper, "*Klass *+*", NO_MATCH);
        testCases.add(helper, "*Klass*::*method*", NO_MATCH);

        testCases.add(helper, "*,**", PARSING_FAILURE);
        testCases.add(helper, "*,*(I[Ljava/lang/String;Lj]ava/lang/Integer;[B[[D)V", PARSING_FAILURE);
        testCases.add(helper, "*,*)method*.", PARSING_FAILURE);
        testCases.add(helper, "{pool.subpack.Klass}* *", PARSING_FAILURE);
        testCases.add(helper, "*Klass met]hod/", PARSING_FAILURE);
        testCases.add(helper, "pool::su@%b::Klass* *)method.", PARSING_FAILURE);
        testCases.add(helper, "0pool/sub/Klass,*{method}*.(I[Ljava/lang/String;Lj]ava/lang/Integer;[B[[D)V", PARSING_FAILURE);
        testCases.add(helper, "*Klass nonexistent::)(I[Ljava/lang/String;Ljava/lang/Integer;[B[[D)V", PARSING_FAILURE);
        testCases.add(helper, "pool,su]b,Klass*,*)method*/", PARSING_FAILURE);
        testCases.add(helper, "_pool,sub,Klass*,met@%hod,(0)V", PARSING_FAILURE);

        testCases.add(helper, "*.*", MATCH);
        testCases.add(helper, "compiler/oracle/MethodMatcherTest.*", MATCH);
        testCases.add(helper, "compiler/oracle/MethodMatcherTest.helper", MATCH);
        testCases.add(helper, "compiler/oracle/MethodMatcherTest.helper()", MATCH);
        testCases.add(helper, "compiler/oracle/MethodMatcherTest.helper()V", MATCH);
        testCases.add(helper, "compiler/oracle/MethodMatcherTest.helper()V;", NO_MATCH);
        testCases.add(helper, "compiler/oracle/MethodMatcherTest.helper()I", NO_MATCH);
        testCases.add(helper, "compiler/oracle/MethodMatcherTest.helperX", NO_MATCH);
        testCases.add(helper, "compiler/oracle/MethodMatcherTest.helper;", NO_MATCH);
        testCases.add(helper, "abc.*", NO_MATCH);
        testCases.add(helper, "*.abc", NO_MATCH);

        testCases.add(getDate, "*.*", MATCH);
        testCases.add(getDate, "*.getDate", MATCH);
        testCases.add(getDate, "java/util/Date.getDate", MATCH);
        testCases.add(getDate, "java/util/Date.*", MATCH);

        testCases.add(inner, "*.*", MATCH);
        testCases.add(inner, "compiler/oracle/MethodMatcherTest$TestCases.innerHelper", MATCH);
        testCases.add(inner, "compiler/oracle/MethodMatcherTest*.innerHelper", MATCH);
        testCases.add(inner, "compiler/oracle/MethodMatcherTest$*.innerHelper", MATCH);
        testCases.add(inner, "*$TestCases.innerHelper", MATCH);
        testCases.add(inner, "*TestCases.innerHelper", MATCH);
        testCases.add(inner, "TestCases.innerHelper", NO_MATCH);
        testCases.add(inner, "compiler/oracle/MethodMatcherTest.innerHelper", NO_MATCH);

        testCases.add(toString, "*.*", MATCH);
        testCases.add(toString, "java/lang/String.toString", MATCH);
        testCases.add(toString, "java.lang.String::toString", MATCH);

        testCases.add(toString, "java/lang/String::toString", PARSING_FAILURE);
        testCases.add(toString, "java.lang/String::toString", PARSING_FAILURE);
        testCases.add(toString, "java.lang/String.toString", PARSING_FAILURE);
        testCases.add(toString, "java::lang::String::toString", PARSING_FAILURE);

        testCases.add(toString, "java/lang/String.toString(*)", PARSING_FAILURE);
        testCases.add(toString, "java/lang/String.toString(L*", PARSING_FAILURE);
        testCases.add(toString, "java/lang/String.toString*(lsd)l", NO_MATCH);
        testCases.add(toString, "java/lang/String.toString(lsd)l", NO_MATCH);
        testCases.add(toString, "java/lang/String.toString (", MATCH);
        testCases.add(toString, "java/lang/String.toString ()", MATCH);
        testCases.add(toString, "java/lang/String.toString ()L", MATCH);
        testCases.add(toString, "java/lang/String.toString ()Lj", MATCH);
        testCases.add(toString, "java/lang/String.toString ()Ls", NO_MATCH);
        testCases.add(toString, "java/lang/String.toString*(", MATCH);
        testCases.add(toString, "java/lang/String.toString* (", MATCH);
        testCases.add(toString, "java/lang/String.toString*(;", NO_MATCH);
        testCases.add(toString, "java/lang/String.toString*();sf", NO_MATCH);
        testCases.add(toString, "java/lang/String.toString*()Ljava/lang/String;", MATCH);
        testCases.add(toString, "java/lang/String.toString()Ljava/lang/String;", MATCH);
        testCases.add(toString, "java/lang/String.toString ()Ljava/lang/String;", MATCH);
        testCases.add(toString, "java/lang/String.toString ()Ljava/lang/String", MATCH);
        testCases.add(toString, "java/lang/String.toString ()L", MATCH);
        testCases.add(toString, "java/lang/String.toString ()I;", NO_MATCH);

        testCases.add(toString, "*Internal.*", NO_MATCH);
        testCases.add(toString, "*Internal.**", PARSING_FAILURE);
        testCases.add(toString, "*Internal.***", PARSING_FAILURE);
        testCases.add(toString, "*Internal.*a**", PARSING_FAILURE);
        testCases.add(toString, "*Internal.**a*", PARSING_FAILURE);

        testCases.add(toString, "java.lang.String::<init>(Ljava/lang/String;)V", NO_MATCH);
        testCases.add(toString, "java.lang.String::<clinit>(Ljava/lang/String;)V", NO_MATCH);
        testCases.add(toString, "java.lang.String::<init(Ljava/lang/String;)V", PARSING_FAILURE);
        testCases.add(toString, "java.lang.String::init>(Ljava/lang/String;)V", PARSING_FAILURE);

        testCases.add(toString, "java/lang/String.toString()Ljava/lang/String;", MATCH);
        testCases.add(toString, "java/lang/Str<ing.toString()Ljava/lang/String;", PARSING_FAILURE);
        testCases.add(toString, "java/lang/Str>ing.toString()Ljava/lang/String;", PARSING_FAILURE);
        testCases.add(toString, "java/lang/<init>.toString()Ljava/lang/String;", PARSING_FAILURE);
        testCases.add(toString, "java/lang/<clinit>.toString()Ljava/lang/String;", PARSING_FAILURE);

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
        MethodMatcherTest test = new MethodMatcherTest();
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
            case MATCH:
                return "Match";
            default:
                return "Unknown error";
            }
        }

        boolean test() {
            int result = WHITE_BOX.matchesMethod(testTarget, pattern);
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

        public void innerHelper() {
        }
    }
}

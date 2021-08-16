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
import compiler.lib.ir_framework.driver.TestVMException;
import compiler.lib.ir_framework.shared.TestFormatException;
import jdk.test.lib.Asserts;

/*
 * @test
 * @requires vm.flagless
 * @summary Test the framework with helper classes.
 * @library /test/lib /
 * @run driver ir_framework.tests.TestWithHelperClasses
 */

public class TestWithHelperClasses {

    public static void main(String[] args) {
        new TestFramework().addHelperClasses(Helper1.class, Helper2.class).start();
        try {
            new TestFramework().addHelperClasses(Helper1.class).start();
            shouldNotReach();
        } catch (TestVMException e) {
            Asserts.assertFalse(e.getExceptionInfo().contains("public static void ir_framework.tests.Helper1.foo() should have been C2 compiled"));
            Asserts.assertFalse(TestFramework.getLastTestVMOutput().contains("public static void ir_framework.tests.Helper1.foo() should have been C2 compiled"));
            Asserts.assertTrue(TestFramework.getLastTestVMOutput().contains("public static void ir_framework.tests.Helper2.foo() should have been C2 compiled"));
            Asserts.assertTrue(e.getExceptionInfo().contains("public static void ir_framework.tests.Helper2.foo() should have been C2 compiled"));
            Asserts.assertFalse(TestFramework.getLastTestVMOutput().contains("Should not be executed"));
            Asserts.assertFalse(e.getExceptionInfo().contains("Should not be executed"));
        }

        try {
            new TestFramework(BadHelperClass.class).addHelperClasses(BadHelper.class).start();
            shouldNotReach();
        } catch (TestFormatException e) {
            Asserts.assertTrue(e.getMessage().contains("Cannot use @Test annotation in helper class"));
            Asserts.assertTrue(e.getMessage().contains("Cannot use @Check annotation in helper class"));
            Asserts.assertTrue(e.getMessage().contains("Cannot use @Run annotation in helper class"));
            Asserts.assertTrue(e.getMessage().contains("noTestInHelper"));
            Asserts.assertTrue(e.getMessage().contains("test2"));
            Asserts.assertTrue(e.getMessage().contains("check2"));
            Asserts.assertTrue(e.getMessage().contains("test3"));
            Asserts.assertTrue(e.getMessage().contains("run3"));
        }

        try {
            new TestFramework(TestAsHelper.class).addHelperClasses(TestAsHelper.class).start();
            shouldNotReach();
        } catch (TestFormatException e) {
            Asserts.assertTrue(e.getMessage().contains("Cannot specify test class ir_framework.tests." +
                                                       "TestAsHelper as helper class, too"));
        }

        try {
            new TestFramework().addHelperClasses(NestedHelper.class).start();
            shouldNotReach();
        } catch (TestFormatException e) {
            Asserts.assertTrue(e.getMessage().contains("Nested class"));
            Asserts.assertTrue(e.getMessage().contains("TestWithHelperClasses$NestedHelper inside test class"));
        }
    }

    public static void shouldNotReach() {
        throw new RuntimeException("should not reach");
    }

    @Test
    public void test() throws NoSuchMethodException {
        TestFramework.assertCompiledByC2(Helper1.class.getMethod("foo"));
        TestFramework.assertCompiledByC2(Helper2.class.getMethod("foo"));
        TestFramework.assertCompiledByC2(NestedHelper.class.getMethod("foo"));
        TestFramework.assertCompiledByC2(StaticNestedHelper.class.getMethod("foo"));
    }

    class NestedHelper {
        @ForceCompile(CompLevel.C2)
        public void foo() {
            throw new RuntimeException("Should not be executed");
        }
    }


    static class StaticNestedHelper {
        @ForceCompile(CompLevel.C2)
        public void foo() {
            throw new RuntimeException("Should not be executed");
        }
    }
}

class TestAsHelper {

    @Test
    public void foo() {}
}

class Helper1 {

    @ForceCompile(CompLevel.C2)
    public static void foo() {
        throw new RuntimeException("Should not be executed");
    }
}

class Helper2 {

    @ForceCompile(CompLevel.C2)
    public static void foo() {
        throw new RuntimeException("Should not be executed");
    }
}

class BadHelperClass {
    @Test
    public void test1() {}
 }


class BadHelper {
    @Test
    public void noTestInHelper() {}

    @Test
    public void test2() {}

    @Check(test = "test2")
    public void check2() {}

    @Test
    public void test3() {}

    @Run(test = "test3")
    public void run3() {}
}

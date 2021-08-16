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
 * @test
 * @bug 8074980
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @requires vm.debug == true
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=option,compiler.oracle.GetMethodOptionTest::test,ccstrlist,TestOptionList,_foo,_bar
 *                   -XX:CompileCommand=option,compiler.oracle.GetMethodOptionTest::test,ccstr,TestOptionStr,_foo
 *                   -XX:CompileCommand=option,compiler.oracle.GetMethodOptionTest::test,bool,TestOptionBool,false
 *                   -XX:CompileCommand=option,compiler.oracle.GetMethodOptionTest::test,intx,TestOptionInt,-1
 *                   -XX:CompileCommand=option,compiler.oracle.GetMethodOptionTest::test,uintx,TestOptionUint,1
 *                   -XX:CompileCommand=option,compiler.oracle.GetMethodOptionTest::test,TestOptionBool2
 *                   -XX:CompileCommand=option,compiler.oracle.GetMethodOptionTest::test,double,TestOptionDouble,1.123
 *                   compiler.oracle.GetMethodOptionTest
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=option,compiler.oracle.GetMethodOptionTest::test,bool,TestOptionBool,false,intx,TestOptionInt,-1,uintx,TestOptionUint,1,bool,TestOptionBool2,true,ccstr,TestOptionStr,_foo,double,TestOptionDouble,1.123,ccstrlist,TestOptionList,_foo,_bar
 *                   compiler.oracle.GetMethodOptionTest
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=TestOptionList,compiler.oracle.GetMethodOptionTest::test,_foo,_bar
 *                   -XX:CompileCommand=TestOptionStr,compiler.oracle.GetMethodOptionTest::test,_foo
 *                   -XX:CompileCommand=TestOptionBool,compiler.oracle.GetMethodOptionTest::test,false
                     -XX:CompileCommand=TestOptionBool2,compiler.oracle.GetMethodOptionTest::test
 *                   -XX:CompileCommand=TestOptionInt,compiler.oracle.GetMethodOptionTest::test,-1
 *                   -XX:CompileCommand=TestOptionUint,compiler.oracle.GetMethodOptionTest::test,1
 *                   -XX:CompileCommand=TestOptionDouble,compiler.oracle.GetMethodOptionTest::test,1.123
 *                   compiler.oracle.GetMethodOptionTest
 */

package compiler.oracle;

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Executable;
import java.util.function.BiFunction;

public class GetMethodOptionTest {
    private static final  WhiteBox WB = WhiteBox.getWhiteBox();
    public static void main(String[] args) {
        Executable test = getMethod("test");
        Executable test2 = getMethod("test2");
        BiFunction<Executable, String, Object> getter = WB::getMethodOption;
        for (TestCase testCase : TestCase.values()) {
            Object expected = testCase.value;
            String name = testCase.name();
            Asserts.assertEQ(expected, getter.apply(test, name),
                    testCase + ": universal getter returns wrong value");
            Asserts.assertEQ(expected, testCase.getter.apply(test, name),
                    testCase + ": specific getter returns wrong value");
            Asserts.assertEQ(null, getter.apply(test2, name),
                    testCase + ": universal getter returns value for unused method");
            Asserts.assertEQ(null, testCase.getter.apply(test2, name),
                    testCase + ": type specific getter returns value for unused method");
        }
    }
    private static void test() { }
    private static void test2() { }

    private static enum TestCase {
        TestOptionBool(false, WB::getMethodBooleanOption),
        TestOptionStr("_foo", WB::getMethodStringOption),
        TestOptionInt(-1L, WB::getMethodIntxOption),
        TestOptionUint(1L, WB::getMethodUintxOption),
        TestOptionBool2(true, WB::getMethodBooleanOption),
        TestOptionDouble(1.123d, WB::getMethodDoubleOption),
        TestOptionList("_foo _bar", WB::getMethodStringOption);

        public final Object value;
        public final BiFunction<Executable, String, Object> getter;
        private TestCase(Object value, BiFunction<Executable, String, Object> getter) {
            this.value = value;
            this.getter = getter;
        }
    }

    private static Executable getMethod(String name) {
        Executable result;
        try {
            result = GetMethodOptionTest.class.getDeclaredMethod(name);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new Error("TESTBUG : can't get method " + name, e);
        }
        return result;
    }
}

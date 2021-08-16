/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8189335
 * @summary Synthetic anonymous class used as access constructor tag conflicting with a top level class.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @library /tools/lib /tools/javac/lib ../lib
 * @build toolbox.ToolBox InMemoryFileManager TestResult
 * @build AccessToPrivateInnerClassConstructorsTest SyntheticTestDriver ExpectedClass ExpectedClasses
 * @run main SyntheticTestDriver AccessToPrivateInnerClassConstructorsTest 0
 * @run main AccessToPrivateInnerClassConstructorsTest
 */

@ExpectedClass(className = "AccessToPrivateInnerClassConstructorsTest",
        expectedMethods = {"<init>()", "main(java.lang.String[])", "f()", "g()"})
@ExpectedClass(className = "AccessToPrivateInnerClassConstructorsTest$1",
        expectedMethods = {"<init>()"})
@ExpectedClass(className = "AccessToPrivateInnerClassConstructorsTest$A",
        expectedMethods = {
                "<init>(AccessToPrivateInnerClassConstructorsTest)",
                "<init>(AccessToPrivateInnerClassConstructorsTest, " +
                       "AccessToPrivateInnerClassConstructorsTest$1)"},
        expectedNumberOfSyntheticFields = 1,
        expectedNumberOfSyntheticMethods = 0)
@ExpectedClass(className = "AccessToPrivateInnerClassConstructorsTest$1Local",
        expectedMethods = {"<init>(AccessToPrivateInnerClassConstructorsTest)"},
        expectedNumberOfSyntheticFields = 1)
@ExpectedClass(className = "AccessToPrivateInnerClassConstructorsTest$2Local",
        expectedMethods = {"<init>(AccessToPrivateInnerClassConstructorsTest)"},
        expectedNumberOfSyntheticFields = 1)
public class AccessToPrivateInnerClassConstructorsTest {

    public static void main(String... args) {
        new AccessToPrivateInnerClassConstructorsTest().f();
    }

    private class A {
        private A() { }
        private A(AccessToPrivateInnerClassConstructorsTest$1 o) { }
    }

    void f() {
        new A();
        new A(null);

        class Local {};
        new Local();
    }

    void g() {
        class Local {};
        new Local();
    }
}
class AccessToPrivateInnerClassConstructorsTest$1 {}

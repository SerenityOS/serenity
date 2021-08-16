/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044537
 * @summary Checking ACC_SYNTHETIC flag is generated for access method
 *          generated to access to private methods and fields.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @library /tools/lib /tools/javac/lib ../lib
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build AccessToPrivateSiblingsTest SyntheticTestDriver ExpectedClass ExpectedClasses
 * @run main SyntheticTestDriver AccessToPrivateSiblingsTest
 */

/**
 * Access from sibling classes to sibling classes.
 * Synthetic members:
 * 1. inner classes for Inner*.
 * 2. getter/setter for private field var.
 * 3. access method for private method function().
 * 4. getter/setter for private field staticVar.
 * 5. access method for private method staticFunction().
 * 6. field this in Inner1.
 * 7. constructor for Inner*.
 */
@ExpectedClass(className = "AccessToPrivateSiblingsTest", expectedMethods = "<init>()")
@ExpectedClass(className = "AccessToPrivateSiblingsTest$Inner1",
        expectedMethods = {"function()", "<init>(AccessToPrivateSiblingsTest)"},
        expectedFields = "var",
        expectedNumberOfSyntheticFields = 1)
@ExpectedClass(className = "AccessToPrivateSiblingsTest$Inner2",
        expectedMethods = "<init>(AccessToPrivateSiblingsTest)",
        expectedNumberOfSyntheticFields = 1)
@ExpectedClass(className = "AccessToPrivateSiblingsTest$Inner3",
        expectedMethods = {"<init>()", "function()", "staticFunction()", "<clinit>()"},
        expectedFields = {"var", "staticVar"})
@ExpectedClass(className = "AccessToPrivateSiblingsTest$Inner4",
        expectedMethods = {"<init>()", "function()", "staticFunction()"},
        expectedFields = {"var", "staticVar"})
public class AccessToPrivateSiblingsTest {

    private class Inner1 {
        private Inner1() {}
        private int var;
        private void function() {}

        {
            Inner3 inner = new Inner3();
            inner.var = 0;
            int i = inner.var;
            inner.function();
        }
    }

    private class Inner2 {
        {
            Inner1 inner = new Inner1();
            inner.var = 0;
            int i = inner.var;
            inner.function();
        }
    }

    private static class Inner3 {
        private Inner3() {}
        private int var;
        private static int staticVar;
        private void function() {}
        private static void staticFunction() {}

        static {
            Inner4 inner = new Inner4();
            inner.var = 0;
            int i = inner.var;
            inner.function();
        }
    }

    private static class Inner4 {
        private Inner4() {}
        private int var;
        private static int staticVar;
        private void function() {}
        private static void staticFunction() {}
    }
}

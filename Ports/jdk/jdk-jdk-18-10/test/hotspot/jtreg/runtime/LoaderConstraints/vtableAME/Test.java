/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186092
 * @compile ../common/Foo.java
 *          ../common/J.java
 *          I.java
 *          ../common/C.jasm
 *          Task.java
 *          ../common/PreemptingClassLoader.java
 * @run main/othervm Test
 */

import java.io.PrintStream;
import java.lang.reflect.*;

public class Test {

    // Test that LinkageError exceptions are not thrown during vtable creation,
    // for loader constraint errors, if the target method is an overpass method.
    //
    // In this test, during vtable creation for class Task, the target method
    // "Task.m()LFoo;" is an overpass method (that throws an AME).  So, even
    // though it is inheriting the method from its super class C, and Task has
    // a different class Foo than C, no LinkageError exception should be thrown
    // because the loader constraint check that would cause the LinkageError
    // should not be done.
    public static void main(String args[]) throws Exception {
        Class<?> c = test.Foo.class; // forces standard class loader to load Foo
        ClassLoader l = new PreemptingClassLoader("test.Task", "test.Foo", "test.I", "test.J");
        l.loadClass("test.Foo");
        l.loadClass("test.Task").newInstance();
        test.Task t = new test.Task();
        try {
            t.m(); // Should get AME
            throw new RuntimeException("Missing AbstractMethodError exception");
        } catch (AbstractMethodError e) {
            if (!e.getMessage().contains("Method test/Task.m()Ltest/Foo; is abstract")) {
                throw new RuntimeException("Wrong AME exception thrown: " + e.getMessage());
            }
        }
    }

}

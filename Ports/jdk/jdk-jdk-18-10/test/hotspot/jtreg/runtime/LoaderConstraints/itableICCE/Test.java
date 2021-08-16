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
 *          I.java
 *          ../common/J.java
 *          ../common/C.jasm
 *          Task.java
 *          ../common/PreemptingClassLoader.java
 * @run main/othervm Test
 */

public class Test {

    // Test that LinkageError exceptions are not thrown during itable creation,
    // for loader constraint errors, if the target method is an overpass method.
    //
    // In this test, during itable creation for class C, method "m()LFoo;" for
    // C's super interface I has a different class Foo than the selected method's
    // type J.  But, the selected method is an overpass method (that throws an
    // ICCE). So, no LinkageError exception should be thrown because the loader
    // constraint check that would cause the LinkageError should not be done.
    public static void main(String... args) throws Exception {
        Class<?> c = test.Foo.class; // forces standard class loader to load Foo
        ClassLoader l = new PreemptingClassLoader("test.Task", "test.Foo", "test.C", "test.I");
        Runnable r = (Runnable) l.loadClass("test.Task").newInstance();
        try {
            r.run(); // Cause an ICCE because both I and J define m()LFoo;
            throw new RuntimeException("Expected ICCE exception not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("Conflicting default methods: test/I.m test/J.m")) {
                throw new RuntimeException("Wrong ICCE exception thrown: " + e.getMessage());
            }
        }
    }
}

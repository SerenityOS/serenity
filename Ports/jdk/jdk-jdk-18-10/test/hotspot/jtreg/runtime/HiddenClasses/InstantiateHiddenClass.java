/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test issues with instantiating hidden classes.
 * @library /test/lib
 * @modules jdk.compiler
 * @run main InstantiateHiddenClass
 */

import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class InstantiateHiddenClass {

    static byte klassbuf[] = InMemoryJavaCompiler.compile("TestClass",
        "public class TestClass { " +
        "    public static void concat(String one, String two) throws Throwable { " +
        "        System.out.println(one + two);" +
        " } } ");

    public static void main(String[] args) throws Throwable {

        // Test that a hidden class cannot be found through its name.
        try {
            Lookup lookup = MethodHandles.lookup();
            Class<?> cl = lookup.defineHiddenClass(klassbuf, false, NESTMATE).lookupClass();
            Class.forName(cl.getName()).newInstance();
            throw new RuntimeException("Expected ClassNotFoundException not thrown");
        } catch (ClassNotFoundException e ) {
            // Test passed
        }


        // Create two hidden classes and instantiate an object from each of them.
        // Verify that the references to these objects are different and references
        // to their classes are not equal either.
        Lookup lookup = MethodHandles.lookup();
        Class<?> c1 = lookup.defineHiddenClass(klassbuf, false, NESTMATE).lookupClass();
        Class<?> c2 = lookup.defineHiddenClass(klassbuf, false, NESTMATE).lookupClass();
        Object o1 = c1.newInstance();
        Object o2 = c2.newInstance();
        if (o1 == o2) {
            throw new RuntimeException("Objects should not be equal");
        }
        if (o1.getClass() == o2.getClass()) {
            throw new RuntimeException("Classes should not be equal");
        }
    }
}

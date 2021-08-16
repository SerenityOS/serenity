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
 * @summary Creates a hidden class inside of a hidden class.
 * @library /test/lib
 * @modules jdk.compiler
 * @run main p.NestedHidden
 */

package p;

import java.lang.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import jdk.test.lib.compiler.InMemoryJavaCompiler;


// Test that a hidden class can define its own hidden class by calling
// lookup.defineHiddenClass().
public class NestedHidden {
    static byte klassbuf[] = InMemoryJavaCompiler.compile("p.TestClass",
        "package p; " +
        "public class TestClass { " +
        "    public static void concat(String one, String two) throws Throwable { " +
        "        System.out.println(one + two);" +
        " } } ");

    public static void main(String args[]) throws Exception {
        // The hidden class calls lookup.defineHiddenClass(), creating a nested hidden class.
        byte klassbuf2[] = InMemoryJavaCompiler.compile("p.TestClass2",
            "package p; " +
            "import java.lang.invoke.MethodHandles; " +
            "import java.lang.invoke.MethodHandles.Lookup; " +
            "import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*; " +
            "public class TestClass2 { " +
            "    public static void doit() throws Throwable { " +
            "        Lookup lookup = MethodHandles.lookup(); " +
            "        Class<?> klass2 = lookup.defineHiddenClass(p.NestedHidden.klassbuf, true, NESTMATE).lookupClass(); " +
            "        Class[] dArgs = new Class[2]; " +
            "        dArgs[0] = String.class; " +
            "        dArgs[1] = String.class; " +
            "        try { " +
            "            klass2.getMethod(\"concat\", dArgs).invoke(null, \"CC\", \"DD\"); " +
            "        } catch (Throwable ex) { " +
            "            throw new RuntimeException(\"Exception: \" + ex.toString()); " +
            "        } " +
            "} } ");

        Lookup lookup = MethodHandles.lookup();
        Class<?> klass2 = lookup.defineHiddenClass(klassbuf2, true, NESTMATE).lookupClass();
        klass2.getMethod("doit").invoke(null);
    }
}

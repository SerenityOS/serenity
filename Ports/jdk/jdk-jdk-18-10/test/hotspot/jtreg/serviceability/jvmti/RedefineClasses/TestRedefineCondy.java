/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8228596
 * @summary Test redefining a class with a condy in its constant pool
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @compile RedefineCondy.jasm
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar TestRedefineCondy
 */

import jdk.test.lib.compiler.InMemoryJavaCompiler;

// Test redefining a class that has a constant dynamic in its constant pool
// to a class that does not have a constant dynamic in its constant pool.
public class TestRedefineCondy {

    static final String DEST = System.getProperty("test.classes");
    static String newClass =
        "public class RedefineCondy { " +
        "public RedefineCondy(java.lang.invoke.MethodHandles.Lookup l, java.lang.String s, java.lang.Class c) { } " +
    "public static void main(String argv[]) { } } ";

    public static void main(String[] args) throws Exception  {
        Class<?> classWithCondy = Class.forName("RedefineCondy");

        try {
            byte[] classBytes = InMemoryJavaCompiler.compile("RedefineCondy", newClass);
            RedefineClassHelper.redefineClass(classWithCondy, classBytes);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception: " + e.getMessage());
        }
    }
}

/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verifies the behaviour of Unsafe.defineClass
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 * @run main DefineClass
 */

import java.security.ProtectionDomain;

import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;

public class DefineClass {
    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        TestClassLoader classloader = new TestClassLoader();
        ProtectionDomain pd = new ProtectionDomain(null, null);

        byte klassbuf[] = InMemoryJavaCompiler.compile("TestClass", "class TestClass { }");

        // Invalid class data
        try {
            unsafe.defineClass(null, klassbuf, 4, klassbuf.length - 4, classloader, pd);
            throw new RuntimeException("defineClass did not throw expected ClassFormatError");
        } catch (ClassFormatError e) {
            // Expected
        }

        // Negative offset
        try {
            unsafe.defineClass(null, klassbuf, -1, klassbuf.length, classloader, pd);
            throw new RuntimeException("defineClass did not throw expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        // Negative length
        try {
            unsafe.defineClass(null, klassbuf, 0, -1, classloader, pd);
            throw new RuntimeException("defineClass did not throw expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        // Offset greater than klassbuf.length
        try {
            unsafe.defineClass(null, klassbuf, klassbuf.length + 1, klassbuf.length, classloader, pd);
            throw new RuntimeException("defineClass did not throw expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        // Length greater than klassbuf.length
        try {
            unsafe.defineClass(null, klassbuf, 0, klassbuf.length + 1, classloader, pd);
            throw new RuntimeException("defineClass did not throw expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        Class klass = unsafe.defineClass(null, klassbuf, 0, klassbuf.length, classloader, pd);
        assertEquals(klass.getClassLoader(), classloader);
        assertEquals(klass.getProtectionDomain(), pd);
    }

    private static class TestClassLoader extends ClassLoader {
        public TestClassLoader(ClassLoader parent) {
            super(parent);
        }

        public TestClassLoader() {
            super();
        }
    }
}

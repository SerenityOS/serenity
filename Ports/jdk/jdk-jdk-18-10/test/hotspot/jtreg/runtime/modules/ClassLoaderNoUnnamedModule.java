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

import jdk.internal.misc.Unsafe;

public class ClassLoaderNoUnnamedModule {
    static final Unsafe UNSAFE = Unsafe.getUnsafe();

    class TestClass extends ClassLoader {
        public boolean calledConstructor = false;

        public TestClass() {
            calledConstructor = true;
        }
    }

    static void testConstructorCall() throws Exception {
        // Use Unsafe allocateInstance to construct an instance of TestClass
        // which does not invoke its super's, java.lang.ClassLoader, constructor.
        // An unnamed module for this ClassLoader is not created.
        TestClass tc = (TestClass)UNSAFE.allocateInstance(TestClass.class);
        System.out.println("tc = " + tc + "tc's ctor called = " + tc.calledConstructor);
        Module unnamed_module = tc.getUnnamedModule();
        if (unnamed_module == null) {
            System.out.println("The unnamed module for this class loader is null");
        }

        tc.loadClass(String.class.getName());
        Class.forName(String.class.getName(), false, tc);
    }

    public static void main(String args[]) throws Exception {
        testConstructorCall();
    }
}

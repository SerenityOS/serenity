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
 * @summary Verifies the behaviour of Unsafe.allocateInstance
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main AllocateInstance
 */

import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;

public class AllocateInstance {
    static final Unsafe UNSAFE = Unsafe.getUnsafe();

    class TestClass {
        public boolean calledConstructor = false;

        public TestClass() {
            calledConstructor = true;
        }
    }

    static void testConstructorCall() throws InstantiationException {
        // allocateInstance() should not result in a call to the constructor
        TestClass tc = (TestClass)UNSAFE.allocateInstance(TestClass.class);
        assertFalse(tc.calledConstructor);
    }

    abstract class AbstractClass {
        public AbstractClass() {}
    }

    static void testAbstractClass() {
        try {
            AbstractClass ac = (AbstractClass) UNSAFE.allocateInstance(AbstractClass.class);
            throw new AssertionError("Should throw InstantiationException for an abstract class");
        } catch (InstantiationException e) {
            // Expected
        }
    }

    interface AnInterface {}

    static void testInterface() {
        try {
            AnInterface ai = (AnInterface) UNSAFE.allocateInstance(AnInterface.class);
            throw new AssertionError("Should throw InstantiationException for an interface");
        } catch (InstantiationException e) {
            // Expected
        }
    }

    public static void main(String args[]) throws Exception {
        for (int i = 0; i < 20_000; i++) {
            testConstructorCall();
            testAbstractClass();
            testInterface();
        }
    }
}

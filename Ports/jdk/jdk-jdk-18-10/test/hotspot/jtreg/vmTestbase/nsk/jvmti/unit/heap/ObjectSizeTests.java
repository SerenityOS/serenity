/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Unit tests for JVMTI GetObjectSize function.
 *
 * Note: The tests here just test the relative size of objects. It's not
 * possible to write tests that check for specific object sizes as this
 * would be tied into the implementation and version.
 *
 */

package nsk.jvmti.unit.heap;

import nsk.share.jvmti.unit.*;
import java.io.PrintStream;
import nsk.share.Consts;

public class ObjectSizeTests {

    static class A {
        static Object static_foo;              // static field
       Object foo;
    }

    static class B extends A {
        Object bar;                     // instance field
        Object more_bar;                     // instance field
        static Object static_bar;
    }

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {

        // sizeof objects

        long o_size = Heap.getObjectSize( new Object() );

        long a_size = Heap.getObjectSize( new A() );
        long b_size = Heap.getObjectSize( new B() );

       System.out.println("Object size: " + o_size + " bytes");
       System.out.println("A size: " + a_size + " bytes");
       System.out.println("B size: " + b_size + " bytes");

        if (o_size >= b_size) {
            throw new RuntimeException("Expected instance of A to be the same or " +
                "larger than java.lang.Object");
        }

        if (a_size >= b_size) {
            throw new RuntimeException("Expected instance of B to be larger than A");
        }

        // sizeof classes
        // - this is difficult to test so we just make a crude assumption

        a_size = Heap.getObjectSize( A.class );
        b_size = Heap.getObjectSize( B.class );

        if (a_size > b_size) {
            throw new RuntimeException("Expected class B to be larger than A");
        }

        return Consts.TEST_PASSED;
    }

}

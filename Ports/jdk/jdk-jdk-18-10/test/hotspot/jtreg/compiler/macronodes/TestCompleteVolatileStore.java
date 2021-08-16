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
 * @bug 8236140
 * @requires vm.gc.Serial
 * @summary Tests proper rehashing of a captured volatile field StoreL node when completing it.
 * @run main/othervm -Xbatch -XX:+UseSerialGC -XX:CompileCommand=compileonly,compiler.macronodes.TestCompleteVolatileStore::test
 *                   compiler.macronodes.TestCompleteVolatileStore
 */

package compiler.macronodes;

public class TestCompleteVolatileStore {
    int i1 = 4;

    public void test() {
        /*
         * The store to the volatile field 'l1' (StoreL) of 'a' is captured in the Initialize node of 'a'
         * (i.e. additional input to it) and completed in InitializeNode::complete_stores.
         * Since 'l1' is volatile, the hash of the StoreL is non-zero triggering the hash assertion failure.
         */
        A a = new A();

        // Make sure that the CheckCastPP node of 'a' is used in the input chain of the Intialize node of 'b'
        B b = new B(a);

        // Make sure 'b' is non-scalar-replacable to avoid eliminating all allocations
        B[] arr = new B[i1];
        arr[i1-3] = b;
    }

    public static void main(String[] strArr) {
        TestCompleteVolatileStore _instance = new TestCompleteVolatileStore();
        for (int i = 0; i < 10_000; i++ ) {
            _instance.test();
        }
    }
}

class A {
    // Needs to be volatile to have a non-zero hash for the StoreL node.
    volatile long l1;

    A() {
        // StoreL gets captured and is later processed in InitializeNode::complete_stores while expanding the allocation node.
        this.l1 = 256;
    }
}

class B {
    A a;

    B(A a) {
        this.a = a;
    }
}


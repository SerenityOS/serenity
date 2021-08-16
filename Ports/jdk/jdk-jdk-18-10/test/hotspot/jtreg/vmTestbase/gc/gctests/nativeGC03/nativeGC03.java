/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase gc/gctests/nativeGC03.
 * VM Testbase keywords: [gc]
 * VM Testbase readme:
 * The idea behind this test is that an array container pointers
 * to circular linked lists is created in nativeGC03.java.
 * Once the linkedlists are all created, a native function is called
 * that trashes all the linked list by setting the elements of the
 * array to null. After creating the garbage, a callback function
 * is invoked that refills the array with new linked lists.
 * The is goes on 10 times. The test passes if no OutofMemory exception
 * is thrown.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native gc.gctests.nativeGC03.nativeGC03
 */

package gc.gctests.nativeGC03;

import nsk.share.TestFailure;
import nsk.share.test.*;
import nsk.share.gc.*;
import java.util.Vector;

public class nativeGC03 extends GCTestBase {
        private Object[] listHolder;

        public native void nativeMethod03(Object[] listHolder);

        // This method is a callback that is invoked by a by the native
        // function "nativeMethod()" before the bowels of the nativeMethod()
        // function are executed.
        // build an array of 10 circular linked lists
        private void fillArray() {
                int i = 0;
                while (i < 5) {
                        listHolder[i] = buildBigCircularLinkedList();
                        i++;
                }
        }

        public void run() {
                listHolder = new Object[5];

                // Build an array of linked lists

                fillArray();
                try {
                        nativeMethod03(listHolder);
                } catch (OutOfMemoryError e) {
                        throw new TestFailure("Test Failed");
                }
                System.out.println("Test Passed");
        }

        // Builds a circular linked list of 0.4Meg

        private CircularLinkedList buildBigCircularLinkedList() {
                CircularLinkedList cl = new CircularLinkedList(100);
                for(int i = 0; i < 10000; i++)
                        cl.grow();
                return cl;
        }

        static {
                System.loadLibrary("nativeGC03");
        }

        public static void main(String args[]){
                Tests.runTest(new nativeGC03(), args);
        }
}

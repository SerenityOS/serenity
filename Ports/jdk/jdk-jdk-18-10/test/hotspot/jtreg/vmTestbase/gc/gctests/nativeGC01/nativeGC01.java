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
 * @summary converted from VM Testbase gc/gctests/nativeGC01.
 * VM Testbase keywords: [gc]
 * VM Testbase readme:
 * This test creates a large linked list of 10,000 elements. After creating the
 * linked list a native function is called. The native function calls a java
 * method that removes all java references to the linked list and forces a garbage
 * collection. The only live reference to the linked list if on the C stack . As
 * there is a live reference on the C stack, the linked list should not be garbage collected.
 * After forcing a garbage collection, the native code calls another java method
 * that traverses the linked list to verify that the linked list has not
 * been garbage collected.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native gc.gctests.nativeGC01.nativeGC01
 */

package gc.gctests.nativeGC01;

import nsk.share.test.*;
import nsk.share.gc.*;
import java.util.Vector;

public class nativeGC01 extends TestBase {
        private CircularLinkedList cl;

        public native int nativeMethod01(CircularLinkedList cl);

        // This method is a callback that is invoked by a by the native
        // function "nativeMethod()" before the bowels of the nativeMethod()
        // function are executed.

        public void callbackGC() {
                cl = null;
                System.gc();
        }

        public void run() {
                int elementCount;
                cl = buildBigCircularLinkedList(); // build a 2 meg Circular Linked list.
                // Determine number of elements in the linked list with a native method
                // after GC has been done.

                try {
                        elementCount = nativeMethod01(cl);

                        if (elementCount == 10000) {
                                log.info("Test Passed");
                        } else {
                                log.info("Test Failed");
                                setFailed(true);
                        }
                } catch (Exception e ) {
                        log.info(e);
                        log.info("broken test");
                        setFailed(true);
                }

        }

        // build a circular linked list of 0.4 Meg
        private CircularLinkedList  buildBigCircularLinkedList() {
                CircularLinkedList cl = new CircularLinkedList(100);
                for(int i = 0; i < 10000; i++)
                        cl.grow();
                return cl;
        }

        static {
                System.loadLibrary("nativeGC01");
        }

        public static void main(String args[]){
                Tests.runTest(new nativeGC01(), args);
        }
}

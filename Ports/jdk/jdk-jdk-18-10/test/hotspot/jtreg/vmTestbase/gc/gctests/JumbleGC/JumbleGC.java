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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/gctests/JumbleGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 * VM Testbase readme:
 * A vector of 10 elements is filled up with references to
 * CicrcularLinkedList and Binary Trees of 0.1 Meg. Once this
 * entire structure has been built, all elements in the Vecor are set to null
 * creating 1Meg of garbage. The Vector is repopulated once again.
 * With ineffective garbage collection, the heap will soon fill up.
 * If an OutofMemoryError is thrown, the test fails.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.JumbleGC.JumbleGC
 */

package gc.gctests.JumbleGC;

import nsk.share.test.*;
import nsk.share.gc.*;
import java.util.Vector;

public class JumbleGC extends TestBase {
        public void run() {
                int TreeSize = 1000;
                int gc_count;
                int randNum;
                int num = 0;

                Vector v = new Vector(10);

                // Build a tree containing 100 treeNodes occupying about
                // 1Meg of heap space.

                gc_count = 0;
                try {
                        for(int i = 0; i < 10 ; i++) {
                                if ( i % 2 == 0 )
                                        v.addElement(buildCircularLinkedList());
                                else
                                        v.addElement(buildTree());
                        }

                        while (gc_count < 10) {

                                for (int i = 0; i < 10 ; i++)
                                        v.setElementAt(null, i);

                                for (int i = 0; i < 10 ; i++) {
                                        if ( i % 2 == 0 )
                                                v.setElementAt(buildCircularLinkedList(),i);
                                        else
                                                v.setElementAt(buildTree(),i);
                                }
                                gc_count ++;
                                log.info("Finished iteration # " + gc_count);
                        }

                } catch (OutOfMemoryError e) {
                        log.error("Test Failed.");
                        setFailed(true);
                }
                log.info("Test Passed.");
        }

        public static void main(String args[]){
                GC.runTest(new JumbleGC(), args);
        }

        // build a binary tree of 0.1 Meg.(100 treeNodes in the three, each of 100 bytes

        private Tree buildTree() {
                int i, randNum;

                i = 0;
                Tree newTree = new Tree(100);
                while (i < 100) {
                        randNum = LocalRandom.nextInt(0, 1000000);
                        newTree.addElement(randNum);
                        i++;
                }
                return newTree;
        }

        // build a circular linked list of 0.1 Meg
        private CircularLinkedList  buildCircularLinkedList() {
                CircularLinkedList cl;
                cl = new CircularLinkedList(100);
                for(int i = 0; i < 1000; i++)
                        cl.grow();
                return cl;
        }
}

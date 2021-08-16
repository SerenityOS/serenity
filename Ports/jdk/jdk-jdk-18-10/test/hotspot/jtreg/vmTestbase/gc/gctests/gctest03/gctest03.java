/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*  stress testing
 Redthreads keep removing new nodes from a binary sort tree(
 all nodes of its left subtree is less than itself, all nodes
 of its right subtree is large than itself).
 Bluethreads keep adding nodes into the binary sort tree.
 YellowThreads search the binary sort tree.
 The nodes removed from the tree will become garbages immediately
 Create 10 Redthreads and 10 Bluethreads to manipulate the
 the same binary tree involving excessive memory allocation
 to test if memory management module and gc() crash.
 */


/*
 * @test
 * @key randomness
 *
 * @summary converted from VM Testbase gc/gctests/gctest03.
 * VM Testbase keywords: [gc]
 *
 * @library /vmTestbase
 *          /test/lib
 * @compile Tree.java appthread.java
 * @run main/othervm gc.gctests.gctest03.gctest03 10000
 */

package gc.gctests.gctest03;

import nsk.share.test.*;
import nsk.share.gc.*;
import nsk.share.TestFailure;
import nsk.share.TestBug;

//import Tree;
//import Redthread;
//import Bluethread;

public class gctest03 extends TestBase {
        private String[] args;

        public gctest03(String[] args) {
                this.args = args;
        }

        public void run() {
                int i = 1;
                int  dataNodeLimit = 100000;

                if (args.length > 0) {
                        try {
                                dataNodeLimit = Integer.valueOf(args[0]).intValue();
                        } catch (NumberFormatException e) {
                                throw new TestBug("Bad input to gctest03. Expected integer, " +
                                                " got: ->" + args[0] + "<-", e);
                        }
                }

                for (int j = 0; j < 10; j++) {
                        DataNode.setDataNodeLimit(dataNodeLimit);
                        DataNode.clearDataNodeCount();

                        Tree  tr = new Tree();
                        for (i =2; i < 100; i++) {
                                try {
                                        DataNode d = new DataNode(i);
                                        TreeNode t = new TreeNode(d);
                                        tr.insert(t);
                                } catch (DataNodeException e) {
                                        throw new TestFailure("DataNodeException caught in gctest03.main()", e);
                                }
                        }

                        int sz = 10;

                        //create 10 threads adding data into binary tree.
                        // each thread only adds the multiple of its key
                        //(1, 2, 3, 4, 5, 6, 7, 8, 9 , 10). No duplication

                        Redthread rth[] = new Redthread[sz];

                        for(i=0; i < sz; i++) {
                                rth[i] = new Redthread(tr, i+1);
                                rth[i].setName("Redthread" + i);
                                rth[i].start();
                        }

                        //create 10 threads removing data from the tree.

                        Bluethread bth[] = new Bluethread[sz];

                        for(i=0; i < sz; i++) {
                                bth[i] = new Bluethread(tr, i+1);
                                bth[i].setName("Bluethread" + i);
                                bth[i].start();
                        }


                        //create 10 threads inquiring data from the tree

                        Yellowthread yth[] = new Yellowthread[sz];
                        for(i=0; i < sz; i++) {
                                yth[i] = new Yellowthread(tr, i+1);
                                yth[i].setName("Yellowthread" + i);
                                yth[i].start();
                        }

                        for (i = 0; i < sz; i++) {
                                try {
                                        rth[i].join();
                                        bth[i].join();
                                        yth[i].join();
                                } catch (InterruptedException e) {
                                        System.err.println("Error joining with threads in gctest03.main()");
                                        System.err.println("Loop count: " + i);
                                }
                        }
                }

        }

        public static void main(String args[]) {
                GC.runTest(new gctest03(args), args);
        }
}

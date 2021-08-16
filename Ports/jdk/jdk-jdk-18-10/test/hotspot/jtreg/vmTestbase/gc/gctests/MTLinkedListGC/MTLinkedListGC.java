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
 * @key stress
 *
 * @summary converted from VM Testbase gc/gctests/MTLinkedListGC.
 * VM Testbase keywords: [gc, stress, nonconcurrent]
 * VM Testbase readme:
 * In this test 1000 threads contribute in the formation
 * of 1 Meg circular Linked list. Insertion into the linked list
 * is sequential. Once formed, the linked list is converted to
 * garbage and the process repeated 50 times. The test fails
 * if an OutofMemoryException is thrown and passes otherwise.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.MTLinkedListGC.MTLinkedListGC
 */

package gc.gctests.MTLinkedListGC;

import java.util.Vector;
import nsk.share.TestFailure;

class CircularLinkedList {
        synchronized void addElement(String info) {
                node newnode;
                int elementCount ; // Number of nodes in LinkedList
                elementCount = elementCount();
                // Do not allow the linked list to grow to more
                // than  100000 nodes
                if (elementCount >= MAXNODES)
                         return;
                newnode = new node(info);
                if (Root == null) {
                         Root = newnode;
                         Root.next = Root;
                         Root.prev = Root;
                }
                else {
                         newnode.next = Root.next;
                         Root.next.prev = newnode;
                         Root.next = newnode;
                         newnode.prev = Root;
                }
        }
        private synchronized int elementCount() {
                node p;
                int count;
                if (Root == null)
                        return 0;
                p = Root;
                count = 0;
                do {
                        p = p.prev;
                        count++;
                } while(p != Root);
                return count;
        }
        private node Root;
        private final int MAXNODES = 100000;
}

class LinkedListGrower extends Thread {
        LinkedListGrower(int ThreadNumber) {
                setName("Thread-" + ThreadNumber);
        }

        public void run() {
                LinkedListHolder.getCircularLinkedList().addElement(getName());
        }
}

class LinkedListHolder {
        private static CircularLinkedList cl = new CircularLinkedList();
        static CircularLinkedList getCircularLinkedList() { return cl; }
        static void getNewList() { cl = new CircularLinkedList(); }
}

public class MTLinkedListGC {
        public static void main(String args[]) {
                int memory_reserve[] = new int [1000];
                Thread ThreadsArray[] = new LinkedListGrower[1000];
                int count;
//              for(int i = 0; i < ThreadsArray.length; i++ )
//                      ThreadsArray[i] = new LinkedListGrower(i);
                count = 0;
                try {
                        while(count < 50 ){
                                for(int i = 0; i < ThreadsArray.length; i++ )
                                        ThreadsArray[i] = new LinkedListGrower(i);
                                for(int i = 0 ; i < ThreadsArray.length ; i++)
                                        ThreadsArray[i].start();
                                try {
                                        for(int i =0 ; i < ThreadsArray.length ; i++)
                                                ThreadsArray[i].join();
                                } catch(Exception e) { }
                                //turn the old linked list into garbage
                                LinkedListHolder.getNewList();
                                System.out.println("Finished iteration " + count);
                                count ++;
                        }
                } catch (OutOfMemoryError e) {
                        memory_reserve = null;
                        System.gc();
                        throw new TestFailure("Test Failed at " + count +"th iteration.");
                }
                System.out.println("Test Passed");
        }
}

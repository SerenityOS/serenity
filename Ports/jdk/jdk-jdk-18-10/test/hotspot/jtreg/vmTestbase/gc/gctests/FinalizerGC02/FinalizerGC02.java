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
 * @summary converted from VM Testbase gc/gctests/FinalizerGC02.
 * VM Testbase keywords: [gc]
 * VM Testbase readme:
 * In this contrived test, the finalizer creates more garbage. As the finalizer
 * is invoked prior to garbage collection, this puts more stress on the
 * garbage collector.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.FinalizerGC02.FinalizerGC02
 */

package gc.gctests.FinalizerGC02;

import nsk.share.TestFailure;

class node {
    byte [] arr;
    node next;
    node prev;
    node(int info){ arr = new byte[100]; }
}

class CircularLinkedList{
   node Root;

   private void addElement(int info) {
      node newnode;

      newnode = new node(info);
      if (Root == null){
        Root = newnode;
        Root.next = Root;
         Root.prev = Root;
      } else {
         newnode.next = Root.next;
         Root.next.prev = newnode;
         Root.next = newnode;
         newnode.prev = Root;
      }
   }

   int elementCount() {
      node p;
      int count;

       p = Root;
       count = 0;

       do {
          p = p.prev;
          count++;
       }while(p != Root);
       return count;
   }

   public void buildNMegList(int N) {
     for (int i = 0 ; i < N*10000 ; i++)
        addElement(i);
   }

   protected void finalize () {
     // generate 1Meg more garbage
     FinalizerGC02.listHolder = new CircularLinkedList();
     FinalizerGC02.listHolder.buildNMegList(1);
   }
}


public class FinalizerGC02 {

  static CircularLinkedList listHolder;
  static int count;
  static int returnCount() { return count; }

  public static void main(String args[]) {
    int memory_reserve[] = new int [1000];

    listHolder = new CircularLinkedList();
    listHolder.buildNMegList(1);

    try {
      while (count < 5) {
        listHolder = new CircularLinkedList();
        listHolder.buildNMegList(1);
        count ++;
      }
    } catch (OutOfMemoryError e) {
       memory_reserve = null;
       System.gc();
       throw new TestFailure("Test failed at " + count +"th iteration.");
    }
       System.out.println("Test Passed");
    }

    static void doComputation(){
      long i = 0;
      while ( i < 1000000) {
        i ++;
      }
    }
}

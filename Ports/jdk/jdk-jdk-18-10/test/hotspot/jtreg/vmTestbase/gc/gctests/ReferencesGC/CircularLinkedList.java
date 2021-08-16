/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package gc.gctests.ReferencesGC;

class node {
    byte [] arr;
    node next;
    node prev;
    node(){ arr = new byte[100]; }
}

public class CircularLinkedList implements Cloneable {
    private node Root;

    public void addElement() {
       node newnode;

       newnode = new node();
       if (Root == null){
          Root = newnode;
          Root.next = Root;
          Root.prev = Root;
       } else{
          newnode.next = Root.next;
          Root.next.prev = newnode;
          Root.next = newnode;
          newnode.prev = Root;
       }
    }

    public void addNelements(int n) {
       for (int i = 0; i < n ; i++)
          addElement();
    }

    public int elementCount() {
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

    public Object clone() {
       node p;
       p = Root;

       if ( p == null ) return null;
       CircularLinkedList clone = new CircularLinkedList();
       do {
          clone.addElement();
          p = p.prev;
       } while(p != Root);
       return clone;
    }
}

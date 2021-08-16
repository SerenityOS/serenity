/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc;

public class CircularLinkedList {
        private int objectSize;
        private LinkedMemoryObject root;

        /**
         * Create empty circular linked list.
         *
         * @param objectSize size of each node in the list
         */
        public CircularLinkedList(int objectSize) {
                this.objectSize = objectSize;
        }

        /**
         * Insert new node in the list.
         */
        public void grow() {
                LinkedMemoryObject newnode = new LinkedMemoryObject(objectSize);
                if (root == null){
                        root = newnode;
                        root.setNext(root);
                        root.setPrev(root);
                } else {
                        newnode.setNext(root.getNext());
                        root.getNext().setPrev(newnode);
                        root.setNext(newnode);
                        newnode.setPrev(root);
                }
        }

        /**
         * Get length of the list.
         *
         * @return length
         */
        public int getLength() {
                return Memory.getListLength(root);
        }

        /**
         * Get length of another list.
         *
         * @param list another list
         * @return length of list
         */
        public int getLength(CircularLinkedList list) {
                return Memory.getListLength(list.root);
        }
}

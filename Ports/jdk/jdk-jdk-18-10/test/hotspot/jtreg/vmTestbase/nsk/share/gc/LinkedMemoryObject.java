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

/**
 *  An object that occupies approximately given number of bytes in memory
 *  and has forward and backward links to other objects.
 */
public class LinkedMemoryObject extends MemoryObject {
        private LinkedMemoryObject next;
        private LinkedMemoryObject prev;

        /**
         *  Create an object that occupies given number of bytes.
         *
         *  @param arraySize size
         */
        public LinkedMemoryObject(int size) {
                super(size - 2 * Memory.getReferenceSize());
        }

        public LinkedMemoryObject(int size, LinkedMemoryObject next) {
                super(size - 2 * Memory.getReferenceSize());
                setNext(next);
        }

        public LinkedMemoryObject(int size, LinkedMemoryObject next, LinkedMemoryObject prev) {
                super(size - 2 * Memory.getReferenceSize());
                setNext(next);
                setPrev(prev);
        }

        public final void setNext(LinkedMemoryObject next) {
                this.next = next;
        }

        public final void setPrev(LinkedMemoryObject prev) {
                this.prev = prev;
        }

        public final LinkedMemoryObject getNext() {
                return next;
        }

        public final LinkedMemoryObject getPrev() {
                return prev;
        }
}

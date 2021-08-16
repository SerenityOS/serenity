/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc.gp.list;

import nsk.share.gc.LinkedMemoryObject;
import nsk.share.gc.Memory;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.MemoryStrategy;

/**
 * Garbage producer that produces linear linked lists.
 */
public class LinearListProducer implements GarbageProducer<LinkedMemoryObject> {
        private MemoryStrategy memoryStrategy;

        public LinearListProducer(MemoryStrategy memoryStrategy) {
                this.memoryStrategy = memoryStrategy;
        }

        public LinkedMemoryObject create(long memory) {
                long objectSize = memoryStrategy.getSize(memory);
                int objectCount = memoryStrategy.getCount(memory);
                return Memory.makeLinearList(objectCount, (int) objectSize);
        }

        public void validate(LinkedMemoryObject obj) {
                LinkedMemoryObject o = obj;
                while (o != null && o != obj)
                        o = o.getNext();
        }
}

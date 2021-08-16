/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.gc.containers;

import nsk.share.gc.Memory;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.MemoryStrategy;
import nsk.share.test.ExecutionController;

public interface Container {
    public void setExecutionController(ExecutionController stresser);
    public void initialize();
    public void update();
}

abstract class TypicalContainer implements Container {

    protected ExecutionController stresser;
    protected long count;
    protected long size;
    protected GarbageProducer garbageProducer;
    protected Speed speed;

    public TypicalContainer(long maximumSize, GarbageProducer garbageProducer,
            MemoryStrategy memoryStrategy, Speed speed) {
        this.count = memoryStrategy.getCount(maximumSize);
        this.size = memoryStrategy.getSize(maximumSize);
        // typical container have at least reference to other element
        // and to data which is really size of "size" really for 100 bytes
        // overhead is about 50%
        final int structureOverHead = 6 * Memory.getReferenceSize();
        if (this.size < structureOverHead * 100) {
            this.count = this.count * (this.size - structureOverHead) / this.size;
        }
        this.garbageProducer = garbageProducer;
        this.speed = speed;
        System.err.format("Creating container: size = %s count = %s\n", size, count);
    }

    public void setExecutionController(ExecutionController stresser) {
        this.stresser = stresser;
    }


}

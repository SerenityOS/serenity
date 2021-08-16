/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
 *
 */

import sun.hotspot.WhiteBox;

import java.util.concurrent.atomic.AtomicLong;

public class MetaspaceTestArena {

    long arena;

    final long allocationCeiling;

    // Number and word size of allocations
    long allocatedWords = 0;
    long numAllocated = 0;
    long deallocatedWords = 0;
    long numDeallocated = 0;
    long numAllocationFailures = 0;

    private synchronized boolean reachedCeiling() {
        return (allocatedWords - deallocatedWords) > allocationCeiling;
    }

    private synchronized void accountAllocation(long words) {
        numAllocated ++;
        allocatedWords += words;
    }

    private synchronized void accountDeallocation(long words) {
        numDeallocated ++;
        deallocatedWords += words;
    }

    MetaspaceTestArena(long arena0, long allocationCeiling) {
        this.allocationCeiling = allocationCeiling;
        this.arena = arena0;
    }

    public Allocation allocate(long words) {
        if (reachedCeiling()) {
            numAllocationFailures ++;
            return null;
        }
        WhiteBox wb = WhiteBox.getWhiteBox();
        long p = wb.allocateFromMetaspaceTestArena(arena, words);
        if (p == 0) {
            numAllocationFailures ++;
            return null;
        } else {
            accountAllocation(words);
        }
        return new Allocation(p, words);
    }

    public void deallocate(Allocation a) {
        WhiteBox wb = WhiteBox.getWhiteBox();
        wb.deallocateToMetaspaceTestArena(arena, a.p, a.word_size);
        accountDeallocation(a.word_size);
    }

    //// Convenience functions ////

    public Allocation allocate_expect_success(long words) {
        Allocation a = allocate(words);
        if (a.isNull()) {
            throw new RuntimeException("Allocation failed (" + words + ")");
        }
        return a;
    }

    public void allocate_expect_failure(long words) {
        Allocation a = allocate(words);
        if (!a.isNull()) {
            throw new RuntimeException("Allocation failed (" + words + ")");
        }
    }

    boolean isLive() {
        return arena != 0;
    }

    @Override
    public String toString() {
        return "arena=" + arena +
                ", ceiling=" + allocationCeiling +
                ", allocatedWords=" + allocatedWords +
                ", numAllocated=" + numAllocated +
                ", deallocatedWords=" + deallocatedWords +
                ", numDeallocated=" + numDeallocated +
                ", numAllocationFailures=" + numAllocationFailures +
                '}';
    }

}

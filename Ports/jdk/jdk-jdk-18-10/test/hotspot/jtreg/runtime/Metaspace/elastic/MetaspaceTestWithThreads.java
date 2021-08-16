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

import java.util.Set;

public class MetaspaceTestWithThreads {

    // The context to use.
    final MetaspaceTestContext context;

    // Total *word* size we allow for the test to allocation. The test may overshoot this a bit, but should not by much.
    final long testAllocationCeiling;

    // Number of parallel allocators
    final int numThreads;

    // Number of seconds for each test
    final int seconds;

    RandomAllocatorThread threads[];

    public MetaspaceTestWithThreads(MetaspaceTestContext context, long testAllocationCeiling, int numThreads, int seconds) {
        this.context = context;
        this.testAllocationCeiling = testAllocationCeiling;
        this.numThreads = numThreads;
        this.seconds = seconds;
        this.threads = new RandomAllocatorThread[numThreads];
    }

    protected void stopAllThreads() throws InterruptedException {
        // Stop all threads.
        for (Thread t: threads) {
            t.interrupt();
            t.join();
        }
    }

    void destroyArenasAndPurgeSpace() {

        for (RandomAllocatorThread t: threads) {
            if (t.allocator.arena.isLive()) {
                context.destroyArena(t.allocator.arena);
            }
        }

        context.checkStatistics();

        // After deleting all arenas, we should have no committed space left: all arena chunks have been returned to
        // the freelist amd should have been maximally merged to a bunch of root chunks, which should be uncommitted
        // in one go.
        // Exception: if reclamation policy is none.
        if (Settings.settings().doesReclaim()) {
            if (context.committedWords() > 0) {
                throw new RuntimeException("Expected no committed words after purging empty metaspace context (was: " + context.committedWords() + ")");
            }
        }

        context.purge();

        context.checkStatistics();

        // After purging - if all arenas had been deleted before - we should have no committed space left even in
        //   recmalation=none mode:
        // purging deletes all nodes with only free chunks, and in this case no node should still house in-use chunks,
        //  so all nodes would have been unmapped.
        // This is independent on reclamation policy. Only one exception: if the area was created with a reserve limit
        // (mimicking compressed class space), the underlying virtual space list cannot be purged.
        if (context.reserveLimit == 0) {
            if (context.committedWords() > 0) {
                throw new RuntimeException("Expected no committed words after purging empty metaspace context (was: " + context.committedWords() + ")");
            }
        }

    }

    @Override
    public String toString() {
        return "commitLimit=" + context.commitLimit +
                ", reserveLimit=" + context.reserveLimit +
                ", testAllocationCeiling=" + testAllocationCeiling +
                ", num_allocators=" + numThreads +
                ", seconds=" + seconds;
    }

}

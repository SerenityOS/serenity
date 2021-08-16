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

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class RandomAllocatorThread extends Thread {

    public final CyclicBarrier gate;
    public final RandomAllocator allocator;
    public final int id;

    public RandomAllocatorThread(CyclicBarrier gate, RandomAllocator allocator, int id) {
        this.gate = gate;
        this.allocator = allocator;
        this.id = id;
    }

    @Override
    public void run() {

       // System.out.println("* [" + id + "] " + allocator);

        try {
            if (gate != null) {
                gate.await();
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            // At this point, interrupt would be an error.
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        while (!Thread.interrupted()) {
            for (int i = 0; i < 1000; i++) {
                allocator.tick();
            }
        }

        // System.out.println("+ [" + id + "] " + allocator);

    }

}

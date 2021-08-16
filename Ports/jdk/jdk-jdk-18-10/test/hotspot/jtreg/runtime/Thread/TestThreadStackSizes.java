/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary Test user threads with various stack sizes.
 * @run main/othervm TestThreadStackSizes
 */

public class TestThreadStackSizes extends Thread {
    static final int K = 1024;

    public TestThreadStackSizes(long stackSize) {
        super(null, null, "TestThreadStackSizes" + stackSize, stackSize);
    }

    @Override
    public void run() {
    }

    public static void createThread(long stackSize) {
        System.out.println("StackSize: " + stackSize);
        try {
            TestThreadStackSizes testThreadStackSize = new TestThreadStackSizes(stackSize);
            testThreadStackSize.start();
            try {
                testThreadStackSize.join();
            } catch (InterruptedException e) {
                throw new Error("InterruptedException in main thread", e);
            }
        } catch (Error e) {  // we sometimes get OutOfMemoryError for very large stacks
            System.out.println("Got exception for stack size " + stackSize  + ": " + e);
        }
    }

    public static void main(String[] args) throws Error {
        // Try every stack size from 0k to 320k.
        for (int i = 0; i <= 320; i++) {
            createThread(i * K);
        }
        // Try a few other potentially problematic stack sizes.
        createThread(500*K);
        createThread(501*K);
        createThread(-1);
        createThread(500*K*K);
        createThread(9223372036854774784L);
    }
}

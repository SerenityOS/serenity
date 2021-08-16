/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.z;

/*
 * @test TestUncommit
 * @requires vm.gc.Z
 * @summary Test ZGC uncommit unused memory
 * @library /test/lib
 * @run main/othervm -XX:+UseZGC -Xlog:gc*,gc+heap=debug,gc+stats=off -Xms128M -Xmx512M -XX:ZUncommitDelay=10 gc.z.TestUncommit
 */

import java.util.ArrayList;
import jdk.test.lib.Utils;

public class TestUncommit {
    private static final int delay = 10 * 1000; // milliseconds
    private static final int allocSize = 200 * 1024 * 1024; // 200M
    private static final int smallObjectSize = 4 * 1024; // 4K
    private static final int mediumObjectSize = 2 * 1024 * 1024; // 2M
    private static final int largeObjectSize = allocSize;

    private static volatile ArrayList<byte[]> keepAlive;

    private static final long startTime = System.nanoTime();

    private static void log(String msg) {
        final String elapsedSeconds = String.format("%.3fs", (System.nanoTime() - startTime) / 1_000_000_000.0);
        System.out.println("[" + elapsedSeconds + "] (" + Thread.currentThread().getName() + ") " + msg);
    }

    private static long capacity() {
        return Runtime.getRuntime().totalMemory();
    }

    private static void allocate(int objectSize) {
        keepAlive = new ArrayList<>();
        for (int i = 0; i < allocSize; i+= objectSize) {
            keepAlive.add(new byte[objectSize]);
        }
    }

    private static void reclaim() {
        keepAlive = null;
        System.gc();
    }

    private static void test(int objectSize) throws Exception {
        final var beforeAlloc = capacity();
        final var timeBeforeAlloc = System.nanoTime();

        // Allocate memory
        log("Allocating");
        allocate(objectSize);

        final var afterAlloc = capacity();

        // Reclaim memory
        log("Reclaiming");
        reclaim();

        log("Waiting for uncommit to start");
        while (capacity() >= afterAlloc) {
            Thread.sleep(1000);
        }

        log("Uncommit started");
        final var timeUncommitStart = System.nanoTime();
        final var actualDelay = (timeUncommitStart - timeBeforeAlloc) / 1_000_000;

        log("Waiting for uncommit to complete");
        while (capacity() > beforeAlloc) {
            Thread.sleep(1000);
        }

        log("Uncommit completed");
        final var afterUncommit = capacity();

        log("        Uncommit Delay: " + delay);
        log("           Object Size: " + objectSize);
        log("            Alloc Size: " + allocSize);
        log("          Before Alloc: " + beforeAlloc);
        log("           After Alloc: " + afterAlloc);
        log("        After Uncommit: " + afterUncommit);
        log(" Actual Uncommit Delay: " + actualDelay);

        // Verify
        if (actualDelay < delay) {
            throw new Exception("Uncommitted too fast");
        }

        if (actualDelay > delay * 2 * Utils.TIMEOUT_FACTOR) {
            throw new Exception("Uncommitted too slow");
        }

        if (afterUncommit < beforeAlloc) {
            throw new Exception("Uncommitted too much");
        }

        if (afterUncommit > beforeAlloc) {
            throw new Exception("Uncommitted too little");
        }

        log("Success");
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 2; i++) {
            log("Iteration " + i);
            test(smallObjectSize);
            test(mediumObjectSize);
            test(largeObjectSize);
        }
    }
}

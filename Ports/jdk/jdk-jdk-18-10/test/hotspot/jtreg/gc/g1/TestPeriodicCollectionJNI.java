/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/* @test
 * @bug 8218880
 * @summary Test that issuing a periodic collection while the GC locker is
 * held does not crash the VM.
 * @requires vm.gc.G1
 * @run main/othervm/native
 *    -Xbootclasspath/a:.
 *    -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *    -XX:+UseG1GC -XX:G1PeriodicGCInterval=100
 *    -XX:+G1PeriodicGCInvokesConcurrent
 *    -Xlog:gc*,gc+periodic=debug
 *    gc.g1.TestPeriodicCollectionJNI
 * @run main/othervm/native
 *    -Xbootclasspath/a:.
 *    -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *    -XX:+UseG1GC -XX:G1PeriodicGCInterval=100
 *    -XX:-G1PeriodicGCInvokesConcurrent
 *    -Xlog:gc*,gc+periodic=debug
 *    gc.g1.TestPeriodicCollectionJNI
 */

public class TestPeriodicCollectionJNI {
    static { System.loadLibrary("TestPeriodicCollectionJNI"); }

    private static native boolean blockInNative(byte[] array);
    private static native void unblock();

    public static void block() {
        if (!blockInNative(new byte[0])) {
            throw new RuntimeException("failed to acquire lock to dummy object");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long timeout = 2000;
        long startTime = System.currentTimeMillis();

        // Start thread doing JNI call
        BlockInNative blocker = new BlockInNative();
        blocker.start();

        try {
            // Wait for periodic GC timeout to trigger
            while (System.currentTimeMillis() < startTime + timeout) {
                System.out.println("Sleeping to let periodic GC trigger...");
                Thread.sleep(200);
            }
        } finally {
            unblock();
        }
    }
}

class BlockInNative extends Thread {

    public void run() {
        TestPeriodicCollectionJNI.block();
    }

    native void unlock();
}

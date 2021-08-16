/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test OverloadCompileQueueTest
 * @key stress randomness
 * @summary stressing code cache by overloading compile queues
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 *        compiler.codecache.stress.TestCaseImpl
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,compiler.codecache.stress.Helper$TestCase::method
 *                   -XX:-SegmentedCodeCache
 *                   compiler.codecache.stress.OverloadCompileQueueTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,compiler.codecache.stress.Helper$TestCase::method
 *                   -XX:+SegmentedCodeCache
 *                   compiler.codecache.stress.OverloadCompileQueueTest
 */

package compiler.codecache.stress;

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;

import java.lang.reflect.Method;
import java.util.stream.IntStream;
import java.util.Random;

class LockUnlockThread extends Thread {
    private static final int MAX_SLEEP = 10000;
    private static final int DELAY_BETWEEN_LOCKS = 100;
    private final Random rng = Utils.getRandomInstance();

    public volatile boolean isActive = true;

    @Override
    public void run() {
        try {
            while (isActive) {
                int timeInLockedState = rng.nextInt(MAX_SLEEP);
                Helper.WHITE_BOX.lockCompilation();
                Thread.sleep(timeInLockedState);
                Helper.WHITE_BOX.unlockCompilation();
                Thread.sleep(DELAY_BETWEEN_LOCKS);
            }
        } catch (InterruptedException e) {
            if (isActive) {
                throw new Error("TESTBUG: LockUnlockThread was unexpectedly interrupted", e);
            }
        } finally {
            Helper.WHITE_BOX.unlockCompilation();
        }
    }
}

public class OverloadCompileQueueTest implements Runnable {
    private static final String METHOD_TO_ENQUEUE = "method";
    private static final int LEVEL_SIMPLE = 1;
    private static final int LEVEL_FULL_OPTIMIZATION = 4;
    private static final boolean TIERED_COMPILATION
            = Helper.WHITE_BOX.getBooleanVMFlag("TieredCompilation");
    private static final int TIERED_STOP_AT_LEVEL
            = Helper.WHITE_BOX.getIntxVMFlag("TieredStopAtLevel").intValue();
    private static final int[] AVAILABLE_LEVELS;
    static {
        if (TIERED_COMPILATION) {
            AVAILABLE_LEVELS = IntStream
                    .rangeClosed(LEVEL_SIMPLE, TIERED_STOP_AT_LEVEL)
                    .toArray();
        } else if (Platform.isServer() && !Platform.isEmulatedClient()) {
            AVAILABLE_LEVELS = new int[] { LEVEL_FULL_OPTIMIZATION };
        } else if (Platform.isClient() || Platform.isMinimal() || Platform.isEmulatedClient()) {
            AVAILABLE_LEVELS = new int[] { LEVEL_SIMPLE };
        } else {
            throw new Error("TESTBUG: unknown VM: " + Platform.vmName);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LockUnlockThread lockUnlockThread = new LockUnlockThread();
        lockUnlockThread.start();

        if (Platform.isInt()) {
            throw new Error("TESTBUG: test can not be run in interpreter");
        }
        new CodeCacheStressRunner(new OverloadCompileQueueTest()).runTest();

        lockUnlockThread.isActive = false;
        lockUnlockThread.interrupt();
        lockUnlockThread.join();
    }

    @Override
    public void run() {
        Helper.TestCase obj = Helper.TestCase.get();
        Class clazz = obj.getClass();
        Method mEnqueue;
        try {
            mEnqueue = clazz.getMethod(METHOD_TO_ENQUEUE);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new Error(String.format(
                    "TESTBUG: cannot get method '%s' of class %s",
                    METHOD_TO_ENQUEUE, clazz.getName()), e);
        }
        for (int compLevel : AVAILABLE_LEVELS) {
            Helper.WHITE_BOX.enqueueMethodForCompilation(mEnqueue, compLevel);
        }
    }

}

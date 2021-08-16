/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;
import org.testng.Assert;

import jdk.test.lib.process.OutputAnalyzer;

import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/*
 * @test
 * @summary Test of diagnostic command Thread.print
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng PrintTest
 */
public class PrintTest {
    protected boolean jucLocks = false;

    CyclicBarrier readyBarrier = new CyclicBarrier(3);
    CyclicBarrier doneBarrier = new CyclicBarrier(3);

    private void waitForBarrier(CyclicBarrier b) {
        try {
            b.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            Assert.fail("Test error: Caught unexpected exception:", e);
        }
    }

    class MonitorThread extends Thread {
        Object lock = new Object();

        public void run() {
            /* Hold lock on "lock" to show up in thread dump */
            synchronized (lock) {
                /* Signal that we're ready for thread dump */
                waitForBarrier(readyBarrier);

                /* Released when the thread dump has been taken */
                waitForBarrier(doneBarrier);
            }
        }
    }

    class LockThread extends Thread {
        ReentrantLock lock = new ReentrantLock();

        public void run() {
            /* Hold lock "lock" to show up in thread dump */
            lock.lock();

            /* Signal that we're ready for thread dump */
            waitForBarrier(readyBarrier);

            /* Released when the thread dump has been taken */
            waitForBarrier(doneBarrier);

            lock.unlock();
        }
    }

    public void run(CommandExecutor executor) {
        MonitorThread mThread = new MonitorThread();
        mThread.start();
        LockThread lThread = new LockThread();
        lThread.start();

        /* Wait for threads to get ready */
        waitForBarrier(readyBarrier);

        /* Execute */
        OutputAnalyzer output = executor.execute("Thread.print" + (jucLocks ? " -l=true" : ""));

        /* Signal that we've got the thread dump */
        waitForBarrier(doneBarrier);

        /*
         * Example output (trimmed) with arrows indicating the rows we are looking for:
         *
         *     ...
         *     "Thread-2" #24 prio=5 os_prio=0 tid=0x00007f913411f800 nid=0x4fc9 waiting on condition [0x00007f91fbffe000]
         *        java.lang.Thread.State: WAITING (parking)
         *       at sun.misc.Unsafe.park(Native Method)
         *       - parking to wait for  <0x000000071a0868a8> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
         *       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
         *       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)
         *       at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:234)
         *       at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
         *       at Print.waitForBarrier(Print.java:26)
         *       at Print.access$000(Print.java:18)
         *       at Print$LockThread.run(Print.java:58)
         *
         * -->    Locked ownable synchronizers:
         * -->    - <0x000000071a294930> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)
         *
         *     "Thread-1" #23 prio=5 os_prio=0 tid=0x00007f913411e800 nid=0x4fc8 waiting on condition [0x00007f9200113000]
         *        java.lang.Thread.State: WAITING (parking)
         *       at sun.misc.Unsafe.park(Native Method)
         *       - parking to wait for  <0x000000071a0868a8> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
         *       at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
         *       at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)
         *       at java.util.concurrent.CyclicBarrier.dowait(CyclicBarrier.java:234)
         *       at java.util.concurrent.CyclicBarrier.await(CyclicBarrier.java:362)
         *       at Print.waitForBarrier(Print.java:26)
         *       at Print.access$000(Print.java:18)
         *       at Print$MonitorThread.run(Print.java:42)
         * -->   - locked <0x000000071a294390> (a java.lang.Object)
         *
         *        Locked ownable synchronizers:
         *       - None
         *
         *     "MainThread" #22 prio=5 os_prio=0 tid=0x00007f923015b000 nid=0x4fc7 in Object.wait() [0x00007f9200840000]
         *        java.lang.Thread.State: WAITING (on object monitor)
         *       at java.lang.Object.wait(Native Method)
         *       - waiting on <0x000000071a70ad98> (a java.lang.UNIXProcess)
         *       at java.lang.Object.wait(Object.java:502)
         *        at java.lang.UNIXProcess.waitFor(UNIXProcess.java:397)
         *        - locked <0x000000071a70ad98> (a java.lang.UNIXProcess)
         *        at jdk.test.lib.dcmd.JcmdExecutor.executeImpl(JcmdExecutor.java:32)
         *       at jdk.test.lib.dcmd.CommandExecutor.execute(CommandExecutor.java:24)
         * -->   at Print.run(Print.java:74)
         *       at Print.file(Print.java:112)
         *     ...

         */
        output.shouldMatch(".*at " + Pattern.quote(PrintTest.class.getName()) + "\\.run.*");
        output.shouldMatch(".*- locked <0x\\p{XDigit}+> \\(a " + Pattern.quote(mThread.lock.getClass().getName()) + "\\).*");

        String jucLockPattern1 = ".*Locked ownable synchronizers:.*";
        String jucLockPattern2 = ".*- <0x\\p{XDigit}+> \\(a " + Pattern.quote(lThread.lock.getClass().getName()) + ".*";

        if (jucLocks) {
            output.shouldMatch(jucLockPattern1);
            output.shouldMatch(jucLockPattern2);
        } else {
            output.shouldNotMatch(jucLockPattern1);
            output.shouldNotMatch(jucLockPattern2);
        }
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }
}

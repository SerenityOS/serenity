/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug     6173675 8231209
 * @summary Basic test of ThreadMXBean.getThreadAllocatedBytes
 * @author  Paul Hohensee
 */

import java.lang.management.*;

public class ThreadAllocatedMemory {
    private static com.sun.management.ThreadMXBean mbean =
        (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    private static volatile boolean done = false;
    private static volatile boolean done1 = false;
    private static Object obj = new Object();
    private static final int NUM_THREADS = 10;
    private static Thread[] threads = new Thread[NUM_THREADS];
    private static long[] sizes = new long[NUM_THREADS];

    public static void main(String[] argv)
        throws Exception {

        testSupportEnableDisable();

        // Test current thread two ways
        testGetCurrentThreadAllocatedBytes();
        testCurrentThreadGetThreadAllocatedBytes();

        // Test a single thread that is not this one
        testGetThreadAllocatedBytes();

        // Test many threads that are not this one
        testGetThreadsAllocatedBytes();

        System.out.println("Test passed");
    }

    private static void testSupportEnableDisable() {
        if (!mbean.isThreadAllocatedMemorySupported()) {
            return;
        }

        // disable allocated memory measurement
        if (mbean.isThreadAllocatedMemoryEnabled()) {
            mbean.setThreadAllocatedMemoryEnabled(false);
        }

        if (mbean.isThreadAllocatedMemoryEnabled()) {
            throw new RuntimeException(
                "ThreadAllocatedMemory is expected to be disabled");
        }

        long s = mbean.getCurrentThreadAllocatedBytes();
        if (s != -1) {
            throw new RuntimeException(
                "Invalid ThreadAllocatedBytes returned = " +
                s + " expected = -1");
        }

        // enable allocated memory measurement
        if (!mbean.isThreadAllocatedMemoryEnabled()) {
            mbean.setThreadAllocatedMemoryEnabled(true);
        }

        if (!mbean.isThreadAllocatedMemoryEnabled()) {
            throw new RuntimeException(
                "ThreadAllocatedMemory is expected to be enabled");
        }
    }

    private static void testGetCurrentThreadAllocatedBytes() {
        long size = mbean.getCurrentThreadAllocatedBytes();
        ensureValidSize(size);

        // do some more allocation
        doit();

        checkResult(Thread.currentThread(), size,
                    mbean.getCurrentThreadAllocatedBytes());
    }

    private static void testCurrentThreadGetThreadAllocatedBytes() {
        Thread curThread = Thread.currentThread();
        long id = curThread.getId();

        long size = mbean.getThreadAllocatedBytes(id);
        ensureValidSize(size);

        // do some more allocation
        doit();

        checkResult(curThread, size, mbean.getThreadAllocatedBytes(id));
    }

    private static void testGetThreadAllocatedBytes()
        throws Exception {

        // start a thread
        done = false; done1 = false;
        Thread curThread = new MyThread("MyThread");
        curThread.start();
        long id = curThread.getId();

        // wait for thread to block after doing some allocation
        waitUntilThreadBlocked(curThread);

        long size = mbean.getThreadAllocatedBytes(id);
        ensureValidSize(size);

        // let thread go to do some more allocation
        synchronized (obj) {
            done = true;
            obj.notifyAll();
        }

        // wait for thread to get going again. we don't care if we
        // catch it in mid-execution or if it hasn't
        // restarted after we're done sleeping.
        goSleep(400);

        checkResult(curThread, size, mbean.getThreadAllocatedBytes(id));

        // let thread exit
        synchronized (obj) {
            done1 = true;
            obj.notifyAll();
        }

        try {
            curThread.join();
        } catch (InterruptedException e) {
            System.out.println("Unexpected exception is thrown.");
            e.printStackTrace(System.out);
        }
    }

    private static void testGetThreadsAllocatedBytes()
        throws Exception {

        // start threads
        done = false; done1 = false;
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new MyThread("MyThread-" + i);
            threads[i].start();
        }

        // wait for threads to block after doing some allocation
        waitUntilThreadsBlocked();

        for (int i = 0; i < NUM_THREADS; i++) {
            sizes[i] = mbean.getThreadAllocatedBytes(threads[i].getId());
            ensureValidSize(sizes[i]);
        }

        // let threads go to do some more allocation
        synchronized (obj) {
            done = true;
            obj.notifyAll();
        }

        // wait for threads to get going again. we don't care if we
        // catch them in mid-execution or if some of them haven't
        // restarted after we're done sleeping.
        goSleep(400);

        for (int i = 0; i < NUM_THREADS; i++) {
            checkResult(threads[i], sizes[i],
                        mbean.getThreadAllocatedBytes(threads[i].getId()));
        }

        // let threads exit
        synchronized (obj) {
            done1 = true;
            obj.notifyAll();
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                System.out.println("Unexpected exception is thrown.");
                e.printStackTrace(System.out);
                break;
            }
        }
    }

    private static void ensureValidSize(long size) {
        // implementation could have started measurement when
        // measurement was enabled, in which case size can be 0
        if (size < 0) {
            throw new RuntimeException(
                "Invalid allocated bytes returned = " + size);
        }
    }

    private static void checkResult(Thread curThread,
                                    long prev_size, long curr_size) {
        if (curr_size < prev_size) {
            throw new RuntimeException("Allocated bytes " + curr_size +
                                       " expected >= " + prev_size);
        }
        System.out.println(curThread.getName() +
                           " Previous allocated bytes = " + prev_size +
                           " Current allocated bytes = " + curr_size);
    }

    private static void goSleep(long ms) throws Exception {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.out.println("Unexpected exception is thrown.");
            throw e;
        }
    }

    private static void waitUntilThreadBlocked(Thread thread)
        throws Exception {
        while (true) {
            goSleep(100);
            ThreadInfo info = mbean.getThreadInfo(thread.getId());
            if (info.getThreadState() == Thread.State.WAITING) {
                break;
            }
        }
    }

    private static void waitUntilThreadsBlocked()
        throws Exception {
        int count = 0;
        while (count != NUM_THREADS) {
            goSleep(100);
            count = 0;
            for (int i = 0; i < NUM_THREADS; i++) {
                ThreadInfo info = mbean.getThreadInfo(threads[i].getId());
                if (info.getThreadState() == Thread.State.WAITING) {
                    count++;
                }
            }
        }
    }

    public static void doit() {
        String tmp = "";
        long hashCode = 0;
        for (int counter = 0; counter < 1000; counter++) {
            tmp += counter;
            hashCode = tmp.hashCode();
        }
        System.out.println(Thread.currentThread().getName() +
                           " hashcode: " + hashCode);
    }

    static class MyThread extends Thread {
        public MyThread(String name) {
            super(name);
        }

        public void run() {
            ThreadAllocatedMemory.doit();

            synchronized (obj) {
                while (!done) {
                    try {
                        obj.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Unexpected exception is thrown.");
                        e.printStackTrace(System.out);
                        break;
                    }
                }
            }

            long size1 = mbean.getThreadAllocatedBytes(getId());
            ThreadAllocatedMemory.doit();
            long size2 = mbean.getThreadAllocatedBytes(getId());

            System.out.println(getName() + ": " +
                "ThreadAllocatedBytes  = " + size1 +
                " ThreadAllocatedBytes  = " + size2);

            if (size1 > size2) {
                throw new RuntimeException(getName() +
                    " ThreadAllocatedBytes = " + size1 +
                    " > ThreadAllocatedBytes = " + size2);
            }

            synchronized (obj) {
                while (!done1) {
                    try {
                        obj.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Unexpected exception is thrown.");
                        e.printStackTrace(System.out);
                        break;
                    }
                }
            }
        }
    }
}

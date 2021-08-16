/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/*
 * @test
 * @bug     6588467
 * @summary Basic test of ThreadInfo.isDaemon
 * @author  Jeremy Manson
 */
public class ThreadDaemonTest {

    public static void main(String[] args) throws Exception {
        final int NUM_THREADS = 20;
        final String THREAD_PREFIX = "ThreadDaemonTest-";

        final CountDownLatch started = new CountDownLatch(NUM_THREADS);
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<Exception> fail = new AtomicReference<>(null);

        Thread[] allThreads = new Thread[NUM_THREADS];
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        Random rand = new Random();

        for (int i = 0; i < NUM_THREADS; i++) {
            allThreads[i] = new Thread(new Runnable() {
                    public void run() {
                        try {
                            started.countDown();
                            finished.await();
                        } catch (InterruptedException e) {
                            fail.set(new Exception(
                                "Unexpected InterruptedException"));
                        }
                    }
                }, THREAD_PREFIX + i);
            allThreads[i].setDaemon(rand.nextBoolean());
            allThreads[i].start();
        }

        started.await();
        try {
            ThreadInfo[] allThreadInfos = mbean.dumpAllThreads(false, false);
            int count = 0;
            for (int i = 0; i < allThreadInfos.length; i++) {
                String threadName = allThreadInfos[i].getThreadName();
                if (threadName.startsWith(THREAD_PREFIX)) {
                    count++;
                    String[] nameAndNumber = threadName.split("-");
                    int threadNum = Integer.parseInt(nameAndNumber[1]);
                    if (allThreads[threadNum].isDaemon() !=
                        allThreadInfos[i].isDaemon()) {
                        throw new RuntimeException(
                            allThreads[threadNum] + " is not like " +
                            allThreadInfos[i] + ". TEST FAILED.");
                    }
                }
            }
            if (count != NUM_THREADS) {
                throw new RuntimeException("Wrong number of threads examined");
            }
        }
        finally { finished.countDown(); }

        for (int i = 0; i < NUM_THREADS; i++) {
            allThreads[i].join();
        }
        if (fail.get() != null) {
            throw fail.get();
        }
    }
}

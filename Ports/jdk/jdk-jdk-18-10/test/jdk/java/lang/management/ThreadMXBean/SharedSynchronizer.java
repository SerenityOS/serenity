/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6337571
 * @summary Test if findDeadlockedThreads works for an ownable synchronizer
 *          in shared mode which has no owner when a thread is parked.
 * @author  Mandy Chung
 *
 * @run main/othervm SharedSynchronizer
 */


import java.util.concurrent.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class SharedSynchronizer {
    public static void main(String[] args) throws Exception {
        MyThread t = new MyThread();
        t.setDaemon(true);
        t.start();

        ThreadMXBean tmbean = ManagementFactory.getThreadMXBean();
        if (!tmbean.isSynchronizerUsageSupported()) {
            System.out.println("Monitoring of synchronizer usage not supported")
;
            return;
        }

        long[] result = tmbean.findDeadlockedThreads();
        if (result != null) {
             throw new RuntimeException("TEST FAILED: result should be null");
        }
    }

    static class MyThread extends Thread {
        public void run() {
            FutureTask f = new FutureTask(
                new Callable() {
                    public Object call() {
                        throw new RuntimeException("should never reach here");
                    }
                }
            );

            // A FutureTask uses the AbstractOwnableSynchronizer in a shared
            // mode (not exclusive mode). When the thread calls f.get(),
            // it will put to park on the ownable synchronizer that
            // is not owned by any thread.
            try {
                f.get();
            } catch (Exception e) {
                RuntimeException re = new RuntimeException(e.getMessage());
                re.initCause(e);
                throw re;
            }
        }
    }
}

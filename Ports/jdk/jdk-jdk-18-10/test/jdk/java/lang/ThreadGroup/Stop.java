/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4176355
 * @summary Stopping a ThreadGroup that contains the current thread has
 *          unpredictable results.
 */

import java.util.concurrent.CountDownLatch;

public class Stop {

    public static void main(String[] args) throws Exception {
        final CountDownLatch ready = new CountDownLatch(1);
        final ThreadGroup group = new ThreadGroup("");

        final Thread second = new Thread(group, () -> {
            ready.countDown();
            while (true) {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException shouldNotHappen) {
                }
            }
        });

        final Thread first = new Thread(group, () -> {
            // Wait until "second" is started
            try {
                ready.await();
            } catch (InterruptedException shouldNotHappen) {
            }
            // Now stop the group
            group.stop();
        });

        // Launch two threads as part of the same thread group
        first.start();
        second.start();

        // Check that the second thread is terminated when the
        // first thread terminates the thread group.
        second.join();
        // Test passed - if never get here the test times out and fails.
    }
}

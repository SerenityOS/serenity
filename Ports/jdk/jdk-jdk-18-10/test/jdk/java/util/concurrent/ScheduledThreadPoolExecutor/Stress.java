/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * This is not a regression test, but a stress benchmark test for
 * 6602600: Fast removal of cancelled scheduled thread pool tasks
 *
 * This runs in the same wall clock time, but much reduced cpu time,
 * with the changes for 6602600.
 */
public class Stress {

    public static void main(String[] args) throws Throwable {

        final CountDownLatch count = new CountDownLatch(1000);

        final ScheduledThreadPoolExecutor pool =
            new ScheduledThreadPoolExecutor(100);
        pool.prestartAllCoreThreads();

        final Runnable incTask = new Runnable() { public void run() {
            count.countDown();
        }};

        pool.scheduleAtFixedRate(incTask, 0, 10, TimeUnit.MILLISECONDS);

        count.await();

        pool.shutdown();
        pool.awaitTermination(1L, TimeUnit.DAYS);
    }
}

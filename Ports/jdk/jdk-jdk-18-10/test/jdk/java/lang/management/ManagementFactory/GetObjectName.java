/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 7068328
 * @summary Test if getObjectName handles properly when called by
 *          multiple threads simultaneously. Run in othervm mode to
 *          make sure the object name is not initialized to begin with.
 *
 * @run main/othervm GetObjectName
 */

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformLoggingMXBean;
import java.lang.management.PlatformManagedObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GetObjectName {
    private static boolean failed = false;
    public static void main(String[] args) throws Exception {
        int tasks = 10000;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        submitTasks(executor, tasks);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        if (!failed) {
            System.out.println("Test passed.");
        }
    }

    static void submitTasks(ExecutorService executor, int count) {
        for (int i=0; i < count && !failed; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    List<PlatformManagedObject> mbeans = new ArrayList<>();
                    mbeans.add(ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class));
                    mbeans.addAll(ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class));
                    for (PlatformManagedObject pmo : mbeans) {
                        // Name should not be null
                        if (pmo.getObjectName() == null) {
                            failed = true;
                            throw new RuntimeException("TEST FAILED: getObjectName() returns null");
                        }
                    }
                }
            });
        }
    }
}

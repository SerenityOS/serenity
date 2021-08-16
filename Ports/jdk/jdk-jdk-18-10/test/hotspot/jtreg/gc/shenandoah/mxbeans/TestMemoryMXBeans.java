/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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
 *
 */

/**
 * @test
 * @summary Test JMX memory beans
 * @requires vm.gc.Shenandoah
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC          -Xmx1g TestMemoryMXBeans   -1 1024
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms1g   -Xmx1g TestMemoryMXBeans 1024 1024
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms128m -Xmx1g TestMemoryMXBeans  128 1024
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms1g   -Xmx1g -XX:ShenandoahUncommitDelay=0 TestMemoryMXBeans 1024 1024
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -Xms128m -Xmx1g -XX:ShenandoahUncommitDelay=0 TestMemoryMXBeans  128 1024
 */

import java.lang.management.*;
import java.util.*;

public class TestMemoryMXBeans {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalStateException("Should provide expected heap sizes");
        }

        long initSize = 1L * Integer.parseInt(args[0]) * 1024 * 1024;
        long maxSize  = 1L * Integer.parseInt(args[1]) * 1024 * 1024;

        // wait for GC to uncommit
        Thread.sleep(1000);

        testMemoryBean(initSize, maxSize);
    }

    public static void testMemoryBean(long initSize, long maxSize) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapInit = memoryMXBean.getHeapMemoryUsage().getInit();
        long heapCommitted = memoryMXBean.getHeapMemoryUsage().getCommitted();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapInit = memoryMXBean.getNonHeapMemoryUsage().getInit();
        long nonHeapCommitted = memoryMXBean.getNonHeapMemoryUsage().getCommitted();
        long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();

        if (initSize > 0 && heapInit != initSize) {
            throw new IllegalStateException("Init heap size is wrong: " + heapInit + " vs " + initSize);
        }
        if (maxSize > 0 && heapMax != maxSize) {
            throw new IllegalStateException("Max heap size is wrong: " + heapMax + " vs " + maxSize);
        }
        if (initSize > 0 && maxSize > 0 && initSize != maxSize && heapCommitted == heapMax) {
            throw new IllegalStateException("Committed heap size is max: " + heapCommitted +
                                            " (init: " + initSize + ", max: " + maxSize + ")");
        }
        if (initSize > 0 && maxSize > 0 && initSize == maxSize && heapCommitted != heapMax) {
            throw new IllegalStateException("Committed heap size is not max: " + heapCommitted +
                                            " (init: " + initSize + ", max: " + maxSize + ")");
        }
        if (initSize > 0 && heapCommitted < initSize) {
            throw new IllegalStateException("Committed heap size is below min: " + heapCommitted +
                                            " (init: " + initSize + ", max: " + maxSize + ")");
        }
    }
}

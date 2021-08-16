/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestMemoryMXBean
 * @requires vm.gc.Z
 * @summary Test ZGC heap memory MXBean
 * @modules java.management
 * @run main/othervm -XX:+UseZGC -Xms128M -Xmx256M -Xlog:gc* TestMemoryMXBean 128 256
 * @run main/othervm -XX:+UseZGC -Xms256M -Xmx256M -Xlog:gc* TestMemoryMXBean 256 256
 */

import java.lang.management.ManagementFactory;

public class TestMemoryMXBean {
    public static void main(String[] args) throws Exception {
        final long M = 1024 * 1024;
        final long expectedInitialCapacity = Long.parseLong(args[0]) * M;
        final long expectedMaxCapacity = Long.parseLong(args[1]) * M;
        final var memoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        final long initialCapacity = memoryUsage.getInit();
        final long capacity = memoryUsage.getCommitted();
        final long maxCapacity = memoryUsage.getMax();

        System.out.println("expectedInitialCapacity: " + expectedInitialCapacity);
        System.out.println("    expectedMaxCapacity: " + expectedMaxCapacity);
        System.out.println("        initialCapacity: " + initialCapacity);
        System.out.println("               capacity: " + capacity);
        System.out.println("            maxCapacity: " + maxCapacity);

        if (initialCapacity != expectedInitialCapacity) {
            throw new Exception("Unexpected initial capacity");
        }

        if (maxCapacity != expectedMaxCapacity) {
            throw new Exception("Unexpected max capacity");
        }

        if (capacity < initialCapacity || capacity > maxCapacity) {
            throw new Exception("Unexpected capacity");
        }
    }
}

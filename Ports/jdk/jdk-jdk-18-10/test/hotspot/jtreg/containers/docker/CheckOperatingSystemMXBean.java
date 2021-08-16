/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import jdk.internal.platform.Metrics;

public class CheckOperatingSystemMXBean {

    public static void main(String[] args) {
        System.out.println("Checking OperatingSystemMXBean");
        Metrics metrics = jdk.internal.platform.Container.metrics();
        System.out.println("Metrics instance: " + (metrics == null ? "null" : "non-null"));
        if (metrics != null) {
            System.out.println("Metrics.getMemoryAndSwapLimit() == " + metrics.getMemoryAndSwapLimit());
            System.out.println("Metrics.getMemoryLimit() == " + metrics.getMemoryLimit());
            System.out.println("Metrics.getMemoryAndSwapUsage() == " + metrics.getMemoryAndSwapUsage());
            System.out.println("Metrics.getMemoryUsage() == " + metrics.getMemoryUsage());
        }

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        System.out.println(String.format("Runtime.availableProcessors: %d", Runtime.getRuntime().availableProcessors()));
        System.out.println(String.format("OperatingSystemMXBean.getAvailableProcessors: %d", osBean.getAvailableProcessors()));
        System.out.println(String.format("OperatingSystemMXBean.getTotalMemorySize: %d", osBean.getTotalMemorySize()));
        System.out.println(String.format("OperatingSystemMXBean.getTotalPhysicalMemorySize: %d", osBean.getTotalPhysicalMemorySize()));
        System.out.println(String.format("OperatingSystemMXBean.getFreeMemorySize: %d", osBean.getFreeMemorySize()));
        System.out.println(String.format("OperatingSystemMXBean.getFreePhysicalMemorySize: %d", osBean.getFreePhysicalMemorySize()));
        System.out.println(String.format("OperatingSystemMXBean.getTotalSwapSpaceSize: %d", osBean.getTotalSwapSpaceSize()));
        System.out.println(String.format("OperatingSystemMXBean.getFreeSwapSpaceSize: %d", osBean.getFreeSwapSpaceSize()));
        System.out.println(String.format("OperatingSystemMXBean.getCpuLoad: %f", osBean.getCpuLoad()));
        System.out.println(String.format("OperatingSystemMXBean.getSystemCpuLoad: %f", osBean.getSystemCpuLoad()));
    }

}

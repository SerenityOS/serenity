/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share.direct;

import java.lang.management.*;
import javax.management.*;
import nsk.monitoring.share.*;
import java.util.List;
import java.lang.reflect.Method;

/**
 * This is MonitoringFactory implementation, which obtains
 * MXBeans directly from ManagementFactory.
 *
 * @see nsk.monitoring.share.MonitoringFactory
 */
public class DirectMonitoringFactory implements MonitoringFactory {
        public ClassLoadingMXBean getClassLoadingMXBean() {
                return ManagementFactory.getClassLoadingMXBean();
        }

        public boolean hasCompilationMXBean() {
                return ManagementFactory.getCompilationMXBean() != null;
        }

        public CompilationMXBean getCompilationMXBean() {
                return ManagementFactory.getCompilationMXBean();
        }

        public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
                return ManagementFactory.getGarbageCollectorMXBeans();
        }

        public RuntimeMXBean getRuntimeMXBean() {
                return ManagementFactory.getRuntimeMXBean();
        }

        public MemoryMXBean getMemoryMXBean() {
                return ManagementFactory.getMemoryMXBean();
        }

        public NotificationEmitter getMemoryMXBeanNotificationEmitter() {
                return (NotificationEmitter) ManagementFactory.getMemoryMXBean();
        }

        public List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
                return ManagementFactory.getMemoryPoolMXBeans();
        }

        public ThreadMXBean getThreadMXBean() {
                return ManagementFactory.getThreadMXBean();
        }

        public boolean hasThreadMXBeanNew() {
            boolean supported = false;
            Class cl = ManagementFactory.getThreadMXBean().getClass();
            Method[] methods = cl.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++ ) {
                if (methods[i].getName().equals("isThreadAllocatedMemorySupported")) {
                    supported = true;
                    break;
                }
            }
            return supported;
        }

        public ThreadMXBean getThreadMXBeanNew() {
            return getThreadMXBean();
        }
        /*
        public OperatingSystemMXBean getOperatingSystemMXBean() {
                return ManagementFactory.getOperatingSystemMXBean();
        }

        */
}

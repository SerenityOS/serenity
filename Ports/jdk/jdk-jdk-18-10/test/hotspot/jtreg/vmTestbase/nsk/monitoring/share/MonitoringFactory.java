/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share;

import java.util.List;
import java.lang.management.*;
import javax.management.*;

/**
 * This factory encapsulates a way to obtain VM monitoring beans.
 *
 * There are currently three implementations.
 * {@link nsk.monitoring.share.direct.DirectMonitoringFactory} obtains
 * them directly through {@link java.lang.management.MonitoringFactory}.
 * {@link nsk.monitoring.share.proxy.ProxyMonitoringFactory} obtains
 * proxies for a given MBeanServer.
 * {@link nsk.monitoring.share.server.ServerMonitoringFactory} obtains
 * MXBeans that use MBeanServer property methods to do it's work.
 *
 * @see nsk.monitoring.share.direct.DirectMonitoringFactory
 * @see nsk.monitoring.share.proxy.ProxyMonitoringFactory
 * @see nsk.monitoring.share.server.ServerMonitoringFactory
 */
public interface MonitoringFactory {
        /**
         * Obtain ClassLoadingMXBean.
         */
        public ClassLoadingMXBean getClassLoadingMXBean();

        /**
         * Check if CompilationMXBean is available.
         *
         * It may be unavailable if VM does not have a compilation
         * system, for example when -Xint option is used.
         *
         * @return true if CompilationMXBean is available, false
         * otherwise
         */
        public boolean hasCompilationMXBean();

        /**
         * Obtain CompilationMXBean
         */
        public CompilationMXBean getCompilationMXBean();

        /**
         * Obtain GarbageCollectorMXBean's.
         */
        public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans();

        /**
         * Obtain RuntimeMXBean.
         */
        public RuntimeMXBean getRuntimeMXBean();

        /**
         * Obtain MemoryMXBean.
         */
        public MemoryMXBean getMemoryMXBean();

        /**
         * Obtain NotificationEmitter for MemoryMXBean.
         */
        public NotificationEmitter getMemoryMXBeanNotificationEmitter();

        /**
         * Obtain MemoryPoolMXBean's.
         */
        public List<MemoryPoolMXBean> getMemoryPoolMXBeans();

        /**
         * Obtain ThreadMXBean.
         */
        public ThreadMXBean getThreadMXBean();

         /**
         * Check if com.sun.managementThreadMXBean is available.
         *
         * It may be unavailable if corresponding API is not integrated
         * into JDK under test and ThreadImpl does not inherit c.s.m.ThreadMXBean
         *
         * @return true if c.s.m.ThreadMXBean is available, false
         * otherwise
         */
        public boolean hasThreadMXBeanNew();

         /**
         * Obtain com.sun.management.ThreadMXBean.
         */
        public ThreadMXBean getThreadMXBeanNew();

        /*
        public OperatingSystemMXBean getOperatingSystemMXBean();
        */
}

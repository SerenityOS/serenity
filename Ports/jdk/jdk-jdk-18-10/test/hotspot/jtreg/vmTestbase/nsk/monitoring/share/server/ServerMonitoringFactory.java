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

package nsk.monitoring.share.server;

import javax.management.*;
import java.lang.management.*;
import nsk.monitoring.share.*;
import java.util.*;
import java.lang.reflect.Method;

public class ServerMonitoringFactory implements MonitoringFactory {
        private MBeanServer mbeanServer;
        private List<GarbageCollectorMXBean> garbageCollectorMXBeans;
        private List<MemoryPoolMXBean> memoryPoolMXBeans;
        private ThreadMXBean threadMXBean;

        public ServerMonitoringFactory(MBeanServer mbeanServer) {
                this.mbeanServer = mbeanServer;
        }

        public ClassLoadingMXBean getClassLoadingMXBean() {
                return new ServerClassLoadingMXBean(mbeanServer);
        }

        public boolean hasCompilationMXBean() {
                try {
                        return mbeanServer.isRegistered(new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME));
                } catch (MalformedObjectNameException e) {
                        throw Monitoring.convertException(e);
                }
        }

        public CompilationMXBean getCompilationMXBean() {
                return new ServerCompilationMXBean(mbeanServer);
        }

        public synchronized List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
                if (garbageCollectorMXBeans == null) {
                        Collection<ObjectName> coll = Monitoring.queryNamesByStart(mbeanServer, ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",");
                        garbageCollectorMXBeans = new ArrayList<GarbageCollectorMXBean>(coll.size());
                        int i = 0;
                        for (ObjectName name : coll)
                                garbageCollectorMXBeans.add(new ServerGarbageCollectorMXBean(mbeanServer, name));
                }
                return garbageCollectorMXBeans;
        }

        public RuntimeMXBean getRuntimeMXBean() {
                return new ServerRuntimeMXBean(mbeanServer);
        }

        public MemoryMXBean getMemoryMXBean() {
                return new ServerMemoryMXBean(mbeanServer);
        }

        public NotificationEmitter getMemoryMXBeanNotificationEmitter() {
                return new ServerNotificationEmitter(mbeanServer, ManagementFactory.MEMORY_MXBEAN_NAME);
        }

        public List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
                if (memoryPoolMXBeans == null) {
                        Collection<ObjectName> coll = Monitoring.queryNamesByStart(mbeanServer, ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",");
                        memoryPoolMXBeans = new ArrayList<MemoryPoolMXBean>(coll.size());
                        int i = 0;
                        for (ObjectName name : coll)
                                memoryPoolMXBeans.add(new ServerMemoryPoolMXBean(mbeanServer, name));
                }
                return memoryPoolMXBeans;
        }

        public ThreadMXBean getThreadMXBean() {
                if (threadMXBean == null)
                        threadMXBean = new ServerThreadMXBean(mbeanServer);
                return threadMXBean;
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

        public ThreadMXBean getThreadMXBeanNew () {
            return new ServerThreadMXBeanNew(mbeanServer);
        }
}

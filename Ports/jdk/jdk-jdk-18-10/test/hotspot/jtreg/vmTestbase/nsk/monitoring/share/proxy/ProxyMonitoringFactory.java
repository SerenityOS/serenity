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

package nsk.monitoring.share.proxy;

import javax.management.MBeanServer;
import java.lang.management.*;
import javax.management.*;
import nsk.monitoring.share.*;
import java.util.*;
import java.lang.reflect.Method;

public class ProxyMonitoringFactory implements MonitoringFactory {
        private MBeanServer mbeanServer;
        private ClassLoadingMXBean classLoadingMXBean;
        private CompilationMXBean compilationMXBean;
        private List<GarbageCollectorMXBean> garbageCollectorMXBeans;
        private RuntimeMXBean runtimeMXBean;
        private MemoryMXBean memoryMXBean;
        private List<MemoryPoolMXBean> memoryPoolMXBeans;
        private ThreadMXBean threadMXBean;
        private com.sun.management.ThreadMXBean threadMXBeanNew;

        public ProxyMonitoringFactory(MBeanServer mbeanServer) {
                this.mbeanServer = mbeanServer;
        }

        protected <T> T getProxy(String name, Class<T> cl) {
                try {
                        return (T) ManagementFactory.newPlatformMXBeanProxy(
                                mbeanServer,
                                name,
                                cl
                        );
                } catch (Exception e) {
                        throw Monitoring.convertException(e);
                }
        }

        protected <T> T getProxy(ObjectName name, Class<T> cl) {
                return getProxy(name.toString(), cl);
        }

        protected <T> List<T> getProxies(String prefix, Class<T> cl) {
                Collection<ObjectName> coll = Monitoring.queryNamesByStart(mbeanServer, prefix + ",");
                List<T> list = new ArrayList<T>(coll.size());
                for (ObjectName name : coll) {
                        list.add(getProxy(name, cl));
                }
                return list;
        }

        public synchronized ClassLoadingMXBean getClassLoadingMXBean() {
                if (classLoadingMXBean == null)
                        classLoadingMXBean = getProxy(
                                ManagementFactory.CLASS_LOADING_MXBEAN_NAME,
                                ClassLoadingMXBean.class
                        );
                return classLoadingMXBean;
        }

        public boolean hasCompilationMXBean() {
                try {
                        return mbeanServer.isRegistered(new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME));
                } catch (MalformedObjectNameException e) {
                        throw Monitoring.convertException(e);
                }
        }

        public synchronized CompilationMXBean getCompilationMXBean() {
                if (compilationMXBean == null)
                        compilationMXBean = getProxy(
                                ManagementFactory.COMPILATION_MXBEAN_NAME,
                                CompilationMXBean.class
                        );
                return compilationMXBean;
        }

        public synchronized List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
                if (garbageCollectorMXBeans == null) {
                        Collection<ObjectName> coll = Monitoring.queryNamesByStart(mbeanServer, ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",");
                        garbageCollectorMXBeans = new ArrayList<GarbageCollectorMXBean>(coll.size());
                        for (ObjectName name : coll) {
                                garbageCollectorMXBeans.add(getProxy(
                                        name,
                                        GarbageCollectorMXBean.class
                                ));
                        }
                }
                return garbageCollectorMXBeans;
        }

        public synchronized RuntimeMXBean getRuntimeMXBean() {
                if (runtimeMXBean == null) {
                        runtimeMXBean = getProxy(
                                ManagementFactory.RUNTIME_MXBEAN_NAME,
                                RuntimeMXBean.class
                        );
                }
                return runtimeMXBean;
        }

        public synchronized MemoryMXBean getMemoryMXBean() {
                if (memoryMXBean == null) {
                        memoryMXBean = getProxy(
                                ManagementFactory.MEMORY_MXBEAN_NAME,
                                MemoryMXBean.class
                        );
                }
                return memoryMXBean;
        }

        public synchronized NotificationEmitter getMemoryMXBeanNotificationEmitter() {
                return new ServerNotificationEmitter(mbeanServer, ManagementFactory.MEMORY_MXBEAN_NAME);
        }

        public synchronized List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
                if (memoryPoolMXBeans == null)
                        memoryPoolMXBeans = getProxies(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE, MemoryPoolMXBean.class);
                return memoryPoolMXBeans;
        }

        public synchronized ThreadMXBean getThreadMXBean() {
                if (threadMXBean == null)
                        threadMXBean = getProxy(
                                ManagementFactory.THREAD_MXBEAN_NAME,
                                ThreadMXBean.class
                        );
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

        public ThreadMXBean getThreadMXBeanNew() {
            if (threadMXBeanNew == null) {
                threadMXBeanNew = getProxy(
                    ManagementFactory.THREAD_MXBEAN_NAME,
                    com.sun.management.ThreadMXBean.class
                );
            }
            return (ThreadMXBean) threadMXBeanNew;
        }
}

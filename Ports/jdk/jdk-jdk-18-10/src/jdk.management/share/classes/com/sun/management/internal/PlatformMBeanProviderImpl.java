/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.sun.management.internal;

import com.sun.management.DiagnosticCommandMBean;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.DynamicMBean;
import sun.management.ManagementFactoryHelper;
import sun.management.spi.PlatformMBeanProvider;

@SuppressWarnings("removal")
public final class PlatformMBeanProviderImpl extends PlatformMBeanProvider {
    final static String DIAGNOSTIC_COMMAND_MBEAN_NAME =
        "com.sun.management:type=DiagnosticCommand";

    private final List<PlatformComponent<?>> mxbeanList;
    private static HotSpotDiagnostic hsDiagMBean = null;
    private static OperatingSystemMXBean osMBean = null;

    static {
       AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
           System.loadLibrary("management_ext");
           return null;
       });
    }

    public PlatformMBeanProviderImpl() {
        mxbeanList = Collections.unmodifiableList(init());
    }

    @Override
    public List<PlatformComponent<?>> getPlatformComponentList() {
        return mxbeanList;
    }

    private List<PlatformComponent<?>> init() {
        ArrayList<PlatformComponent<?>> initMBeanList = new ArrayList<>();
        /**
         * Garbage Collector in the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<MemoryManagerMXBean>() {
            private final Set<String> garbageCollectorMXBeanInterfaceNames
                    = Collections.unmodifiableSet(
                            Stream.of("java.lang.management.MemoryManagerMXBean",
                                    "java.lang.management.GarbageCollectorMXBean",
                                    "com.sun.management.GarbageCollectorMXBean")
                            .collect(Collectors.toSet()));

            @Override
            public Set<Class<? extends MemoryManagerMXBean>> mbeanInterfaces() {
                return Stream.of(MemoryManagerMXBean.class,
                        java.lang.management.GarbageCollectorMXBean.class,
                        com.sun.management.GarbageCollectorMXBean.class)
                        .collect(Collectors.toSet());
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return garbageCollectorMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",name=*";
            }

            @Override
            public boolean isSingleton() {
                return false; // zero or more instances
            }

            @Override
            public Map<String, MemoryManagerMXBean> nameToMBeanMap() {
                List<java.lang.management.GarbageCollectorMXBean> list
                        = ManagementFactoryHelper.getGarbageCollectorMXBeans();
                Map<String, MemoryManagerMXBean> map;
                if (list.isEmpty()) {
                    map = Collections.emptyMap();
                } else {
                    map = new HashMap<>(list.size());
                    for (MemoryManagerMXBean gcm : list) {
                        map.put(gcm.getObjectName().getCanonicalName(),
                                gcm);
                    }
                }

                return map;
            }
        });

        /**
         * Threading system of the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<java.lang.management.ThreadMXBean>() {
            private final Set<String> threadMXBeanInterfaceNames
                    = Collections.unmodifiableSet(
                            Stream.of("java.lang.management.ThreadMXBean",
                                    "com.sun.management.ThreadMXBean")
                            .collect(Collectors.toSet()));
            private ThreadMXBean threadMBean = null;

            @Override
            public Set<Class<? extends java.lang.management.ThreadMXBean>> mbeanInterfaces() {
                return Stream.of(java.lang.management.ThreadMXBean.class,
                        com.sun.management.ThreadMXBean.class)
                        .collect(Collectors.toSet());
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return threadMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.THREAD_MXBEAN_NAME;
            }

            @Override
            public synchronized Map<String, java.lang.management.ThreadMXBean> nameToMBeanMap() {
                if (threadMBean == null) {
                    threadMBean = new HotSpotThreadImpl(ManagementFactoryHelper.getVMManagement());
                }
                return Collections.singletonMap(
                        ManagementFactory.THREAD_MXBEAN_NAME,
                        threadMBean);
            }
        });

        /**
         * OperatingSystemMXBean
         */
        initMBeanList.add(new PlatformComponent<OperatingSystemMXBean>() {
            private final Set<String> operatingSystemMXBeanInterfaceNames
                    = Collections.unmodifiableSet(
                            Stream.of("java.lang.management.OperatingSystemMXBean",
                                    "com.sun.management.OperatingSystemMXBean",
                                    "com.sun.management.UnixOperatingSystemMXBean")
                            .collect(Collectors.toSet()));

            @Override
            public Set<Class<? extends OperatingSystemMXBean>> mbeanInterfaces() {
                return Stream.of(java.lang.management.OperatingSystemMXBean.class,
                        com.sun.management.OperatingSystemMXBean.class,
                        com.sun.management.UnixOperatingSystemMXBean.class)
                        .collect(Collectors.toSet());
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return operatingSystemMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME;
            }

            @Override
            public Map<String, java.lang.management.OperatingSystemMXBean> nameToMBeanMap() {
                return Collections.<String, java.lang.management.OperatingSystemMXBean>singletonMap(
                        ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                        getOperatingSystemMXBean());
            }
        });

        /**
         * Diagnostic support for the HotSpot Virtual Machine.
         */
        initMBeanList.add(new PlatformComponent<com.sun.management.HotSpotDiagnosticMXBean>() {
            private final Set<String> hotSpotDiagnosticMXBeanInterfaceNames =
                    Collections.singleton("com.sun.management.HotSpotDiagnosticMXBean");

            @Override
            public Set<Class<? extends com.sun.management.HotSpotDiagnosticMXBean>> mbeanInterfaces() {
                return Collections.singleton(com.sun.management.HotSpotDiagnosticMXBean.class);
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return hotSpotDiagnosticMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return "com.sun.management:type=HotSpotDiagnostic";
            }

            @Override
            public Map<String, com.sun.management.HotSpotDiagnosticMXBean> nameToMBeanMap() {
                return Collections.<String, com.sun.management.HotSpotDiagnosticMXBean>singletonMap(
                        "com.sun.management:type=HotSpotDiagnostic",
                        getDiagnosticMXBean());
            }
        });

        /**
         * Diagnostic command MBean
         */
        DiagnosticCommandMBean diagMBean = DiagnosticCommandImpl.getDiagnosticCommandMBean();
        if (diagMBean != null) {
            initMBeanList.add(new PlatformComponent<DynamicMBean>() {
                final Set<String> dynamicMBeanInterfaceNames
                        = Collections.singleton("javax.management.DynamicMBean");

                @Override
                public Set<String> mbeanInterfaceNames() {
                    return dynamicMBeanInterfaceNames;
                }

                @Override
                public Set<Class<? extends DynamicMBean>> mbeanInterfaces() {
                    // DynamicMBean cannot be used to find an MBean by ManagementFactory
                    return Collections.emptySet();
                }

                @Override
                public String getObjectNamePattern() {
                    return DIAGNOSTIC_COMMAND_MBEAN_NAME;
                }

                @Override
                public Map<String, DynamicMBean> nameToMBeanMap() {
                    return Collections.<String, DynamicMBean>singletonMap(
                            DIAGNOSTIC_COMMAND_MBEAN_NAME,
                            diagMBean);
                }
            });
        }

        initMBeanList.trimToSize();
        return initMBeanList;
    }

    private static synchronized HotSpotDiagnosticMXBean getDiagnosticMXBean() {
        if (hsDiagMBean == null) {
            hsDiagMBean = new HotSpotDiagnostic();
        }
        return hsDiagMBean;
    }

    private static synchronized OperatingSystemMXBean getOperatingSystemMXBean() {
        if (osMBean == null) {
            osMBean = new OperatingSystemImpl(ManagementFactoryHelper.getVMManagement());
        }
        return osMBean;
    }
}

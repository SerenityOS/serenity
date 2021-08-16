/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.ObjectName;
import sun.management.ManagementFactoryHelper;
import sun.management.spi.PlatformMBeanProvider;

class DefaultPlatformMBeanProvider extends PlatformMBeanProvider {
    private final List<PlatformComponent<?>> mxbeanList;

    DefaultPlatformMBeanProvider() {
        mxbeanList = Collections.unmodifiableList(init());
    }

    @Override
    public List<PlatformComponent<?>> getPlatformComponentList() {
        return mxbeanList;
    }

    private List<PlatformComponent<?>> init() {
        ArrayList<PlatformComponent<?>> initMBeanList = new ArrayList<>();
        /**
         * Class loading system of the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<ClassLoadingMXBean>() {
            private final Set<String> classLoadingInterfaceNames =
                    Collections.singleton("java.lang.management.ClassLoadingMXBean");

            @Override
            public Set<Class<? extends ClassLoadingMXBean>> mbeanInterfaces() {
                return Collections.singleton(ClassLoadingMXBean.class);
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return classLoadingInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.CLASS_LOADING_MXBEAN_NAME;
            }

            @Override
            public Map<String, ClassLoadingMXBean> nameToMBeanMap() {
                return Collections.singletonMap(
                        ManagementFactory.CLASS_LOADING_MXBEAN_NAME,
                        ManagementFactoryHelper.getClassLoadingMXBean());
            }
        });

        /**
         * Compilation system of the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<CompilationMXBean>() {
            private final Set<String> compilationMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.CompilationMXBean");

            @Override
            public Set<Class<? extends CompilationMXBean>> mbeanInterfaces() {
                return Collections.singleton(CompilationMXBean.class);
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return compilationMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.COMPILATION_MXBEAN_NAME;
            }

            @Override
            public Map<String, CompilationMXBean> nameToMBeanMap() {
                CompilationMXBean m = ManagementFactoryHelper.getCompilationMXBean();
                if (m == null) {
                    return Collections.emptyMap();
                } else {
                    return Collections.singletonMap(
                            ManagementFactory.COMPILATION_MXBEAN_NAME,
                            ManagementFactoryHelper.getCompilationMXBean());
                }
            }
        });

        /**
         * Memory system of the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<MemoryMXBean>() {
            private final Set<String> memoryMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.MemoryMXBean");

            @Override
            public Set<Class<? extends MemoryMXBean>> mbeanInterfaces() {
                return Collections.singleton(MemoryMXBean.class);
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return memoryMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.MEMORY_MXBEAN_NAME;
            }

            @Override
            public Map<String, MemoryMXBean> nameToMBeanMap() {
                return Collections.singletonMap(
                        ManagementFactory.MEMORY_MXBEAN_NAME,
                        ManagementFactoryHelper.getMemoryMXBean());
            }
        });

        /**
         * Garbage Collector in the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<MemoryManagerMXBean>() {
            private final Set<String> garbageCollectorMXBeanInterfaceNames
                    = Collections.unmodifiableSet(
                            Stream.of("java.lang.management.MemoryManagerMXBean",
                                    "java.lang.management.GarbageCollectorMXBean")
                            .collect(Collectors.toSet()));
            @Override
            public Set<Class<? extends MemoryManagerMXBean>> mbeanInterfaces() {
                return Stream.of(MemoryManagerMXBean.class,
                        GarbageCollectorMXBean.class).collect(Collectors.toSet());
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
                List<GarbageCollectorMXBean> list
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
         * Memory manager in the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<MemoryManagerMXBean>() {
            private final Set<String> memoryManagerMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.MemoryManagerMXBean");

            @Override
            public Set<Class<? extends MemoryManagerMXBean>> mbeanInterfaces() {
                return Collections.singleton(MemoryManagerMXBean.class);
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return memoryManagerMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE + ",name=*";
            }

            @Override
            public boolean isSingleton() {
                return false; // zero or more instances
            }

            @Override
            public Map<String, MemoryManagerMXBean> nameToMBeanMap() {
                List<MemoryManagerMXBean> list
                        = ManagementFactoryHelper.getMemoryManagerMXBeans();
                return list.stream()
                        .filter(this::isMemoryManager)
                        .collect(Collectors.toMap(
                                pmo -> pmo.getObjectName().getCanonicalName(), Function.identity()));
            }

            // ManagementFactoryHelper.getMemoryManagerMXBeans() returns all
            // memory managers - we need to filter out those that do not match
            // the pattern for which we are registered
            private boolean isMemoryManager(MemoryManagerMXBean mbean) {
                final ObjectName name = mbean.getObjectName();
                return ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE.startsWith(name.getDomain())
                        && ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE.contains(
                                "type="+name.getKeyProperty("type"));
            }
        });

        /**
         * Memory pool in the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<MemoryPoolMXBean>() {
            private final Set<String> memoryPoolMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.MemoryPoolMXBean");

            @Override
            public Set<Class<? extends MemoryPoolMXBean>> mbeanInterfaces() {
                return Collections.singleton(MemoryPoolMXBean.class);
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return memoryPoolMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",name=*";
            }

            @Override
            public boolean isSingleton() {
                return false; // zero or more instances
            }

            @Override
            public Map<String, MemoryPoolMXBean> nameToMBeanMap() {
                List<MemoryPoolMXBean> list
                        = ManagementFactoryHelper.getMemoryPoolMXBeans();
                Map<String, MemoryPoolMXBean> map;
                if (list.isEmpty()) {
                    map = Collections.<String, MemoryPoolMXBean>emptyMap();
                } else {
                    map = new HashMap<>(list.size());
                    for (MemoryPoolMXBean mpm : list) {
                        map.put(mpm.getObjectName().getCanonicalName(),
                                mpm);
                    }
                }
                return map;
            }
        });

        /**
         * Runtime system of the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<RuntimeMXBean>() {
            private final Set<String> runtimeMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.RuntimeMXBean");

            @Override
            public Set<Class<? extends RuntimeMXBean>> mbeanInterfaces() {
                return Collections.singleton(RuntimeMXBean.class);
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return runtimeMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return ManagementFactory.RUNTIME_MXBEAN_NAME;
            }

            @Override
            public Map<String, RuntimeMXBean> nameToMBeanMap() {
                return Collections.singletonMap(
                        ManagementFactory.RUNTIME_MXBEAN_NAME,
                        ManagementFactoryHelper.getRuntimeMXBean());
            }
        });

        /**
         * Threading system of the Java virtual machine.
         */
        initMBeanList.add(new PlatformComponent<ThreadMXBean>() {
            private final Set<String> threadMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.ThreadMXBean");

            @Override
            public Set<Class<? extends ThreadMXBean>> mbeanInterfaces() {
                return Collections.singleton(ThreadMXBean.class);
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
            public Map<String, ThreadMXBean> nameToMBeanMap() {
                return Collections.singletonMap(
                        ManagementFactory.THREAD_MXBEAN_NAME,
                        ManagementFactoryHelper.getThreadMXBean());
            }
        });

        if (ManagementFactoryHelper.isPlatformLoggingMXBeanAvailable()) {
            /**
             * Logging facility.
             */
            initMBeanList.add(new PlatformComponent<PlatformLoggingMXBean>() {
                private final Set<String> platformLoggingMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.PlatformLoggingMXBean");

                @Override
                public Set<Class<? extends PlatformLoggingMXBean>> mbeanInterfaces() {
                    return Collections.singleton(PlatformLoggingMXBean.class);
                }

                @Override
                public Set<String> mbeanInterfaceNames() {
                    return platformLoggingMXBeanInterfaceNames;
                }

                @Override
                public String getObjectNamePattern() {
                    return "java.util.logging:type=Logging";
                }

                @Override
                public Map<String, PlatformLoggingMXBean> nameToMBeanMap() {
                    return Collections.singletonMap(
                        "java.util.logging:type=Logging",
                        ManagementFactoryHelper.getPlatformLoggingMXBean());
                }
            });
        }

        /**
         * Buffer pools.
         */
        initMBeanList.add(new PlatformComponent<BufferPoolMXBean>() {
            private final Set<String> bufferPoolMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.BufferPoolMXBean");

            @Override
            public Set<Class<? extends BufferPoolMXBean>> mbeanInterfaces() {
                return Collections.singleton(BufferPoolMXBean.class);
            }

            @Override
            public Set<String> mbeanInterfaceNames() {
                return bufferPoolMXBeanInterfaceNames;
            }

            @Override
            public String getObjectNamePattern() {
                return "java.nio:type=BufferPool,name=*";
            }

            @Override
            public boolean isSingleton() {
                return false; // zero or more instances
            }

            @Override
            public Map<String, BufferPoolMXBean> nameToMBeanMap() {
                List<BufferPoolMXBean> list
                        = ManagementFactoryHelper.getBufferPoolMXBeans();
                Map<String, BufferPoolMXBean> map;
                if (list.isEmpty()) {
                    map = Collections.<String, BufferPoolMXBean>emptyMap();
                } else {
                    map = new HashMap<>(list.size());
                    list.stream()
                        .forEach(mbean -> map.put(mbean.getObjectName().getCanonicalName(),mbean));
                }
                return map;
            }
        });

        /**
         * OperatingSystemMXBean
         */
        initMBeanList.add(new PlatformComponent<OperatingSystemMXBean>() {
            private final Set<String> operatingSystemMXBeanInterfaceNames
                    = Collections.singleton("java.lang.management.OperatingSystemMXBean");

            @Override
            public Set<Class<? extends OperatingSystemMXBean>> mbeanInterfaces() {
                return Collections.singleton(OperatingSystemMXBean.class);
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
            public Map<String, OperatingSystemMXBean> nameToMBeanMap() {
                return Collections.singletonMap(
                        ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                        ManagementFactoryHelper.getOperatingSystemMXBean());
            }

        });

        initMBeanList.trimToSize();
        return initMBeanList;
    }
}

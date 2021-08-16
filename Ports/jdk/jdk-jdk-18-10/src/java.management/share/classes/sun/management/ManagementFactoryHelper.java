/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;

import java.lang.invoke.MethodHandles;
import java.lang.management.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import jdk.internal.misc.VM;
import jdk.internal.misc.VM.BufferPool;

import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ManagementFactoryHelper provides static factory methods to create
 * instances of the management interface.
 */
public class ManagementFactoryHelper {
    static {
        // make sure that the management lib is loaded within
        // java.lang.management.ManagementFactory
        try {
            MethodHandles.lookup().ensureInitialized(ManagementFactory.class);
        } catch (IllegalAccessException e) {}
    }

    private static final VMManagement jvm = new VMManagementImpl();

    private ManagementFactoryHelper() {};

    public static VMManagement getVMManagement() {
        return jvm;
    }

    static final String LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";
    private static ClassLoadingImpl    classMBean = null;
    private static MemoryImpl          memoryMBean = null;
    private static ThreadImpl          threadMBean = null;
    private static RuntimeImpl         runtimeMBean = null;
    private static CompilationImpl     compileMBean = null;
    private static BaseOperatingSystemImpl osMBean = null;

    public static synchronized ClassLoadingMXBean getClassLoadingMXBean() {
        if (classMBean == null) {
            classMBean = new ClassLoadingImpl(jvm);
        }
        return classMBean;
    }

    public static synchronized MemoryMXBean getMemoryMXBean() {
        if (memoryMBean == null) {
            memoryMBean = new MemoryImpl(jvm);
        }
        return memoryMBean;
    }

    public static synchronized ThreadMXBean getThreadMXBean() {
        if (threadMBean == null) {
            threadMBean = new ThreadImpl(jvm);
        }
        return threadMBean;
    }

    public static synchronized RuntimeMXBean getRuntimeMXBean() {
        if (runtimeMBean == null) {
            runtimeMBean = new RuntimeImpl(jvm);
        }
        return runtimeMBean;
    }

    public static synchronized CompilationMXBean getCompilationMXBean() {
        if (compileMBean == null && jvm.getCompilerName() != null) {
            compileMBean = new CompilationImpl(jvm);
        }
        return compileMBean;
    }

    public static synchronized OperatingSystemMXBean getOperatingSystemMXBean() {
        if (osMBean == null) {
            osMBean = new BaseOperatingSystemImpl(jvm);
        }
        return osMBean;
    }

    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        MemoryPoolMXBean[] pools = MemoryImpl.getMemoryPools();
        List<MemoryPoolMXBean> list = new ArrayList<>(pools.length);
        for (MemoryPoolMXBean p : pools) {
            list.add(p);
        }
        return list;
    }

    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        MemoryManagerMXBean[]  mgrs = MemoryImpl.getMemoryManagers();
        List<MemoryManagerMXBean> result = new ArrayList<>(mgrs.length);
        for (MemoryManagerMXBean m : mgrs) {
            result.add(m);
        }
        return result;
    }

     public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        MemoryManagerMXBean[]  mgrs = MemoryImpl.getMemoryManagers();
        List<GarbageCollectorMXBean> result = new ArrayList<>(mgrs.length);
        for (MemoryManagerMXBean m : mgrs) {
            if (GarbageCollectorMXBean.class.isInstance(m)) {
                 result.add(GarbageCollectorMXBean.class.cast(m));
            }
        }
        return result;
    }

    public static PlatformLoggingMXBean getPlatformLoggingMXBean() {
        if (LoggingMXBeanAccess.isAvailable()) {
            return PlatformLoggingImpl.MBEAN;
        } else {
            return null;
        }
    }

    public static boolean isPlatformLoggingMXBeanAvailable() {
        return LoggingMXBeanAccess.isAvailable();
    }

    /**
     * Returns an array of the name of all memory pools.  The order of the memory pools is
     * significant and maintained in the VM.
     */
    public static String[] getAllMemoryPoolNames() {
        return Arrays.stream(MemoryImpl.getMemoryPools())
                .map(MemoryPoolMXBean::getName)
                .toArray(String[]::new);
    }

    // The LoggingMXBeanAccess class uses reflection to determine
    // whether java.util.logging is present, and load the actual LoggingMXBean
    // implementation.
    //
    static final class LoggingMXBeanAccess {

        static final String LOG_MANAGER_CLASS_NAME = "java.util.logging.LogManager";
        static final String LOGGING_MXBEAN_CLASS_NAME = "java.util.logging.LoggingMXBean";
        static final Class<?> LOG_MANAGER_CLASS = loadLoggingClass(LOG_MANAGER_CLASS_NAME);

        static boolean isAvailable() {
            return LOG_MANAGER_CLASS != null;
        }

        @SuppressWarnings("removal")
        private static Class<?> loadLoggingClass(String className) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Class<?> run() {
                    Optional<Module> logging = ModuleLayer.boot().findModule("java.logging");
                    if (logging.isPresent()) {
                        return Class.forName(logging.get(), className);
                    }
                    return null;
                }
            });
        }

        private Map<String, Method> initMethodMap(Object impl) {
            if (impl == null) {
                return Collections.emptyMap();
            }
            Class<?> intfClass = loadLoggingClass(LOGGING_MXBEAN_CLASS_NAME);
            final Map<String, Method> methodsMap = new HashMap<>();
            for (Method m : intfClass.getMethods()) {
                try {
                    // Sanity checking: all public methods present in
                    // java.util.logging.LoggingMXBean should
                    // also be in PlatformLoggingMXBean
                    Method specMethod = PlatformLoggingMXBean.class
                             .getMethod(m.getName(), m.getParameterTypes());
                    if (specMethod.getReturnType().isAssignableFrom(m.getReturnType())) {
                        if (methodsMap.putIfAbsent(m.getName(), m) != null) {
                            throw new RuntimeException("unexpected polymorphic method: "
                                     + m.getName());
                        }
                    }
                } catch (NoSuchMethodException x) {
                    // All methods in java.util.logging.LoggingMXBean should
                    // also be in PlatformLoggingMXBean
                    throw new InternalError(x);
                }
            }
            return Collections.unmodifiableMap(methodsMap);
        }

        private static Object getMXBeanImplementation() {
            if (!isAvailable()) {
                // should not happen
                throw new NoClassDefFoundError(LOG_MANAGER_CLASS_NAME);
            }
            try {
                final Method m = LOG_MANAGER_CLASS.getMethod("getLoggingMXBean");
                return m.invoke(null);
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException x) {
                throw new ExceptionInInitializerError(x);
            }
         }

        // The implementation object, which will be invoked through
        // reflection. The implementation does not need to implement
        // PlatformLoggingMXBean, but must declare the same methods
        // with same signatures, and they must be public, with one
        // exception:
        // getObjectName will not be called on the implementation object,
        // so the implementation object does not need to declare such
        // a method.
        final Object impl = getMXBeanImplementation();
        final Map<String, Method> methods = initMethodMap(impl);

        LoggingMXBeanAccess() {
        }

        <T> T invoke(String methodName, Object... args) {
            Method m = methods.get(methodName);
            if (m == null) {
                throw new UnsupportedOperationException(methodName);
            }
            try {
                @SuppressWarnings("unchecked")
                T result = (T) m.invoke(impl, args);
                return result;
            } catch (IllegalAccessException ex) {
                throw new UnsupportedOperationException(ex);
            } catch (InvocationTargetException ex) {
                throw unwrap(ex);
            }
        }

        private static RuntimeException unwrap(InvocationTargetException x) {
            Throwable t = x.getCause();
            if (t instanceof RuntimeException) {
                return (RuntimeException)t;
            }
            if (t instanceof Error) {
                throw (Error)t;
            }
            return new UndeclaredThrowableException(t == null ? x : t);
        }


    }

    static final class PlatformLoggingImpl implements PlatformLoggingMXBean {

        private final LoggingMXBeanAccess loggingAccess;
        private PlatformLoggingImpl(LoggingMXBeanAccess loggingAccess) {
            this.loggingAccess = loggingAccess;
        }

        private volatile ObjectName objname;  // created lazily
        @Override
        public ObjectName getObjectName() {
            ObjectName result = objname;
            if (result == null) {
                synchronized (this) {
                    result = objname;
                    if (result == null) {
                        result = Util.newObjectName(LOGGING_MXBEAN_NAME);
                        objname = result;
                    }
                }
            }
            return result;
        }

        @Override
        public java.util.List<String> getLoggerNames() {
            return loggingAccess.invoke("getLoggerNames");
        }

        @Override
        public String getLoggerLevel(String loggerName) {
            return loggingAccess.invoke("getLoggerLevel", loggerName);
        }

        @Override
        public void setLoggerLevel(String loggerName, String levelName) {
            loggingAccess.invoke("setLoggerLevel", loggerName, levelName);
        }

        @Override
        public String getParentLoggerName(String loggerName) {
            return loggingAccess.invoke("getParentLoggerName", loggerName);
        }

        private static PlatformLoggingImpl getInstance() {
            return new PlatformLoggingImpl(new LoggingMXBeanAccess());
         }

        static final PlatformLoggingMXBean MBEAN = getInstance();
    }

    private static volatile List<BufferPoolMXBean> bufferPools;
    public static List<BufferPoolMXBean> getBufferPoolMXBeans() {
        if (bufferPools == null) {
            synchronized (ManagementFactoryHelper.class) {
                if (bufferPools == null) {
                    bufferPools = VM.getBufferPools().stream()
                                    .map(ManagementFactoryHelper::createBufferPoolMXBean)
                                    .collect(Collectors.toList());
                }
            }
        }
        return bufferPools;
    }

    private static final String BUFFER_POOL_MXBEAN_NAME = "java.nio:type=BufferPool";

    /**
     * Creates management interface for the given buffer pool.
     */
    private static BufferPoolMXBean
        createBufferPoolMXBean(final BufferPool pool)
    {
        return new BufferPoolMXBean() {
            private volatile ObjectName objname;  // created lazily
            @Override
            public ObjectName getObjectName() {
                ObjectName result = objname;
                if (result == null) {
                    synchronized (this) {
                        result = objname;
                        if (result == null) {
                            result = Util.newObjectName(BUFFER_POOL_MXBEAN_NAME +
                                ",name=" + pool.getName());
                            objname = result;
                        }
                    }
                }
                return result;
            }
            @Override
            public String getName() {
                return pool.getName();
            }
            @Override
            public long getCount() {
                return pool.getCount();
            }
            @Override
            public long getTotalCapacity() {
                return pool.getTotalCapacity();
            }
            @Override
            public long getMemoryUsed() {
                return pool.getMemoryUsed();
            }
        };
    }

    private static HotspotRuntime hsRuntimeMBean = null;
    private static HotspotClassLoading hsClassMBean = null;
    private static HotspotThread hsThreadMBean = null;
    private static HotspotCompilation hsCompileMBean = null;
    private static HotspotMemory hsMemoryMBean = null;

    /**
     * This method is for testing only.
     */
    public static synchronized HotspotRuntimeMBean getHotspotRuntimeMBean() {
        if (hsRuntimeMBean == null) {
            hsRuntimeMBean = new HotspotRuntime(jvm);
        }
        return hsRuntimeMBean;
    }

    /**
     * This method is for testing only.
     */
    public static synchronized HotspotClassLoadingMBean getHotspotClassLoadingMBean() {
        if (hsClassMBean == null) {
            hsClassMBean = new HotspotClassLoading(jvm);
        }
        return hsClassMBean;
    }

    /**
     * This method is for testing only.
     */
    public static synchronized HotspotThreadMBean getHotspotThreadMBean() {
        if (hsThreadMBean == null) {
            hsThreadMBean = new HotspotThread(jvm);
        }
        return hsThreadMBean;
    }

    /**
     * This method is for testing only.
     */
    public static synchronized HotspotMemoryMBean getHotspotMemoryMBean() {
        if (hsMemoryMBean == null) {
            hsMemoryMBean = new HotspotMemory(jvm);
        }
        return hsMemoryMBean;
    }

    /**
     * This method is for testing only.
     */
    public static synchronized HotspotCompilationMBean getHotspotCompilationMBean() {
        if (hsCompileMBean == null) {
            hsCompileMBean = new HotspotCompilation(jvm);
        }
        return hsCompileMBean;
    }

    /**
     * Registers a given MBean if not registered in the MBeanServer;
     * otherwise, just return.
     */
    @SuppressWarnings("removal")
    private static void addMBean(MBeanServer mbs, Object mbean, String mbeanName) {
        try {
            final ObjectName objName = Util.newObjectName(mbeanName);

            // inner class requires these fields to be final
            final MBeanServer mbs0 = mbs;
            final Object mbean0 = mbean;
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws MBeanRegistrationException,
                                         NotCompliantMBeanException {
                    try {
                        mbs0.registerMBean(mbean0, objName);
                        return null;
                    } catch (InstanceAlreadyExistsException e) {
                        // if an instance with the object name exists in
                        // the MBeanServer ignore the exception
                    }
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw Util.newException(e.getException());
        }
    }

    private static final String HOTSPOT_CLASS_LOADING_MBEAN_NAME =
        "sun.management:type=HotspotClassLoading";

    private static final String HOTSPOT_COMPILATION_MBEAN_NAME =
        "sun.management:type=HotspotCompilation";

    private static final String HOTSPOT_MEMORY_MBEAN_NAME =
        "sun.management:type=HotspotMemory";

    private static final String HOTSPOT_RUNTIME_MBEAN_NAME =
        "sun.management:type=HotspotRuntime";

    private static final String HOTSPOT_THREAD_MBEAN_NAME =
        "sun.management:type=HotspotThreading";

    static void registerInternalMBeans(MBeanServer mbs) {
        // register all internal MBeans if not registered
        // No exception is thrown if a MBean with that object name
        // already registered
        addMBean(mbs, getHotspotClassLoadingMBean(),
            HOTSPOT_CLASS_LOADING_MBEAN_NAME);
        addMBean(mbs, getHotspotMemoryMBean(),
            HOTSPOT_MEMORY_MBEAN_NAME);
        addMBean(mbs, getHotspotRuntimeMBean(),
            HOTSPOT_RUNTIME_MBEAN_NAME);
        addMBean(mbs, getHotspotThreadMBean(),
            HOTSPOT_THREAD_MBEAN_NAME);

        // CompilationMBean may not exist
        if (getCompilationMXBean() != null) {
            addMBean(mbs, getHotspotCompilationMBean(),
                HOTSPOT_COMPILATION_MBEAN_NAME);
        }
    }

    @SuppressWarnings("removal")
    private static void unregisterMBean(MBeanServer mbs, String mbeanName) {
        try {
            final ObjectName objName = Util.newObjectName(mbeanName);

            // inner class requires these fields to be final
            final MBeanServer mbs0 = mbs;
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws MBeanRegistrationException,
                                           RuntimeOperationsException  {
                    try {
                        mbs0.unregisterMBean(objName);
                    } catch (InstanceNotFoundException e) {
                        // ignore exception if not found
                    }
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw Util.newException(e.getException());
        }
    }

    static void unregisterInternalMBeans(MBeanServer mbs) {
        // unregister all internal MBeans
        unregisterMBean(mbs, HOTSPOT_CLASS_LOADING_MBEAN_NAME);
        unregisterMBean(mbs, HOTSPOT_MEMORY_MBEAN_NAME);
        unregisterMBean(mbs, HOTSPOT_RUNTIME_MBEAN_NAME);
        unregisterMBean(mbs, HOTSPOT_THREAD_MBEAN_NAME);

        // CompilationMBean may not exist
        if (getCompilationMXBean() != null) {
            unregisterMBean(mbs, HOTSPOT_COMPILATION_MBEAN_NAME);
        }
    }

    public static boolean isThreadSuspended(int state) {
        return ((state & JMM_THREAD_STATE_FLAG_SUSPENDED) != 0);
    }

    public static boolean isThreadRunningNative(int state) {
        return ((state & JMM_THREAD_STATE_FLAG_NATIVE) != 0);
    }

    public static Thread.State toThreadState(int state) {
        // suspended and native bits may be set in state
        int threadStatus = state & ~JMM_THREAD_STATE_FLAG_MASK;
        return jdk.internal.misc.VM.toThreadState(threadStatus);
    }

    // These values are defined in jmm.h
    private static final int JMM_THREAD_STATE_FLAG_MASK = 0xFFF00000;
    private static final int JMM_THREAD_STATE_FLAG_SUSPENDED = 0x00100000;
    private static final int JMM_THREAD_STATE_FLAG_NATIVE = 0x00400000;

    // Invoked by the VM
    private static MemoryPoolMXBean createMemoryPool
        (String name, boolean isHeap, long uThreshold, long gcThreshold) {
        return new MemoryPoolImpl(name, isHeap, uThreshold, gcThreshold);
    }

    private static MemoryManagerMXBean createMemoryManager(String name) {
        return new MemoryManagerImpl(name);
    }

    private static GarbageCollectorMXBean
        createGarbageCollector(String name, String type) {

        // ignore type parameter which is for future extension
        return new GarbageCollectorImpl(name);
    }
}

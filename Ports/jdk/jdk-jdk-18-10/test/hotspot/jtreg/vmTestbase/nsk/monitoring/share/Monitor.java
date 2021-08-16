/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.*;
import javax.management.*;
import javax.management.openmbean.*;
import nsk.share.*;

/**
 * <code>Monitor</code> is a factory class for getting wrappers for monitoring
 * interfaces.
 * <p>This class consists of static methods that return an instance of a
 * wapper. Depending on command line arguments, a wrapper redirects invocations
 * interfaces directly, or accesses MBeans through MBeanServer</p>
 *
 * <p>At the same time <code>Monitor</code> is a base class for such wrappers.
 * Each wrapper must override {@link #Monitor
 * <code>Monitor(Log, ArgumentHandler)</code>} constructor, that receives
 * command line options and creates a proper MBean server based on that options.
 */
public class Monitor {

    /**
     * <code>Directly</code> mode -- the MBeans' methods are accessed directly
     * within the same JVM.
     */
    public static final int DIRECTLY_MODE = 0;

    /**
     * <code>Server</code> mode -- the MBeans' methods are accessed through
     * default MBeanServer.
     */
    public static final int SERVER_MODE = 1;

    /**
     * <code>Server</code> mode -- the MBeans' methods are accessed through
     * MBean proxy.
     */
    public static final int PROXY_MODE = 2;

    /**
     * Prefix string to print while logging.
     */
    protected static String logPrefix;

    /**
     * <code>Logger</code> object to print info in.
     */
    protected Log.Logger logger;

    /**
     * An <code>ObjectName</code> object to represent somespecific MBean.
     */
    protected ObjectName mbeanObjectName;

    // Some private variables
    private static int testMode;
    private static MBeanServer mbeanServer = null;

    /**
     * Creates a new <code>Monitor</code> object. The arguments that are passed
     * to the test are parsed.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     */
    protected Monitor(Log log, ArgumentHandler argumentHandler) {
        if (log != null)
            logger = new Log.Logger(log, logPrefix);
        readArguments(argumentHandler);
    }

    /**
     * Returns the test mode code.
     *
     * @return the test mode code.
     *
     * @see Monitor#DIRECTLY_MODE
     * @see Monitor#SERVER_MODE
     * @see Monitor#PROXY_MODE
     *
     */
    public static int getTestMode() {
        return testMode;
    }

    /**
     * Returns MBean server if <code>"server"</code> is assigned to
     * <code>-testMode</code>, i.e. the metrics are accessed through default
     * or custom MBeanServer.
     *
     * @return MBean server
     */
    public static MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    // Parse the arguments passed to the test
    private void readArguments(ArgumentHandler argumentHandler) {
        String tmp = argumentHandler.getTestMode();
        String tmp1 = "Test mode:\t";

        if (tmp.equals(ArgumentHandler.DIRECTLY_MODE)) {
            testMode = DIRECTLY_MODE;
            display(tmp1 + "DIRECTLY access to MBean");
        } else if (tmp.equals(ArgumentHandler.SERVER_MODE)) {
            testMode = SERVER_MODE;
            display(tmp1 + "access to MBean through MBeanServer");
        } else if (tmp.equals(ArgumentHandler.PROXY_MODE)) {
            testMode = PROXY_MODE;
            display(tmp1 + "access to MBean through proxy");
        } else {
            throw new Failure("UNKNOWN test mode.");
        }

        if (testMode == SERVER_MODE || testMode == PROXY_MODE)
            createMBeanServer(argumentHandler.isDefaultServer());
    }

    // Create a new either default, or custom MBeanServer
    private void createMBeanServer(boolean defaultServer) {
        String tmp = "MBeanServer:\t";
        if (defaultServer) {
            System.setProperty(CustomMBeanServer.SERVER_BUILDER_PROPERTY,
                               CustomMBeanServer.DEFAULT_SERVER_BUILDER);
            display(tmp + "DEFAULT");
        } else {
            System.setProperty(CustomMBeanServer.SERVER_BUILDER_PROPERTY,
                               CustomMBeanServer.CUSTOM_SERVER_BUILDER);
            display(tmp + "CUSTOM");
        }

        mbeanServer = ManagementFactory.getPlatformMBeanServer();
        if (!defaultServer)
            ((CustomMBeanServer) mbeanServer).setLog(logger.getLog());
    }

    /**
     * Creates an instance of the <code>ClassLoadingMonitor</code> class to
     * provide class loading/unloading monitoring.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     * @return New <code>ClassLoadingMonitor</code> instance.
     *
     * @see ArgumentHandler
     */
    public static ClassLoadingMonitor getClassLoadingMonitor(Log log,
                                              ArgumentHandler argumentHandler) {

        ClassLoadingMonitor monitor
            = new ClassLoadingMonitor(log, argumentHandler);
        createMBean(monitor, ManagementFactory.CLASS_LOADING_MXBEAN_NAME);
        return monitor;
    }

    /**
     * Creates an instance of the <code>MemoryMonitor</code> class to
     * provide control on heap and non-heap memory.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     * @return New <code>MemoryMonitor</code> instance.
     *
     * @see ArgumentHandler
     */
    public static MemoryMonitor getMemoryMonitor(Log log,
                                              ArgumentHandler argumentHandler) {

        MemoryMonitor monitor = new MemoryMonitor(log, argumentHandler);
        createMBean(monitor, ManagementFactory.MEMORY_MXBEAN_NAME);
        return monitor;
    }

    /**
     * Creates an instance of the <code>ThreadMonitor</code> class to
     * provide control on threads.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     * @return New <code>ThreadMonitor</code> instance.
     *
     * @see ArgumentHandler
     */
    public static ThreadMonitor getThreadMonitor(Log log,
                                              ArgumentHandler argumentHandler) {

        ThreadMonitor monitor = new ThreadMonitor(log, argumentHandler);
        createMBean(monitor, ManagementFactory.THREAD_MXBEAN_NAME);
        return monitor;
    }

    /**
     * Creates an instance of the <code>RuntimeMonitor</code> class to
     * provide control on runtime enviroment.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     * @return New <code>RuntimeMonitor</code> instance.
     *
     * @see ArgumentHandler
     */
    public static RuntimeMonitor getRuntimeMonitor(Log log,
                                              ArgumentHandler argumentHandler) {

        RuntimeMonitor monitor = new RuntimeMonitor(log, argumentHandler);
        createMBean(monitor, ManagementFactory.RUNTIME_MXBEAN_NAME);
        return monitor;
    }

    /**
     * Creates an instance of the <code>CompilationMonitor</code> class to
     * provide control on compilation system.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     * @return New <code>CompilationMonitor</code> instance.
     *
     * @see ArgumentHandler
     */
    public static CompilationMonitor getCompilationMonitor(Log log,
                                              ArgumentHandler argumentHandler) {

        CompilationMonitor monitor
            = new CompilationMonitor(log, argumentHandler);
        createMBean(monitor, ManagementFactory.COMPILATION_MXBEAN_NAME);
        return monitor;
    }

    /**
     * Creates an instance of the <code>LoggingMonitor</code> class to
     * provide control on logging system.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     * @return New <code>LoggingMonitor</code> instance.
     *
     * @see ArgumentHandler
     */
    public static LoggingMonitor getLoggingMonitor(Log log,
                                              ArgumentHandler argumentHandler) {

        LoggingMonitor monitor
            = new LoggingMonitor(log, argumentHandler);
        createMBean(monitor, LoggingMonitor.LOGGING_MXBEAN_NAME);
        return monitor;
    }

    /**
     * Creates an instance of the <code>GarbageCollectorMonitor</code> class to
     * provide control on logging system.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     * @return New <code>GarbageCollectorMonitor</code> instance.
     *
     * @see ArgumentHandler
     */
    public static GarbageCollectorMonitor getGarbageCollectorMonitor(Log log,
                                              ArgumentHandler argumentHandler) {

        GarbageCollectorMonitor monitor
            = new GarbageCollectorMonitor(log, argumentHandler);
        createMBean(monitor,
            GarbageCollectorMonitor.GARBAGECOLLECTOR_MXBEAN_NAME);
        return monitor;
    }

    /**
     * Displays <code>message</code> using <code>Log.Logger</code> object.
     */
    protected void display(String message) {
        if (logger != null)
            logger.display(message);
    }

    /**
     * Displays an error <code>message</code> using <code>Log.Logger</code>
     * object.
     */
    protected void complain(String message) {
        if (logger != null)
            logger.complain(message);
    }

    /**
     * Retrieves the <code>boolean</code> value of the specified attribute
     * from MBeanServer.
     *
     * @param object MBean's <code>ObjectName</code>
     * @param name name of the attribute.
     *
     * @return value of the attribute.
     */
    protected boolean getBooleanAttribute(ObjectName object, String name) {
        try {
            Boolean b = (Boolean) getMBeanServer().getAttribute(object, name);
            return b.booleanValue();
        } catch (Exception e) {
            throw new Failure(e);
        }
    } // getBooleanAttribute()

    /**
     * Retrieves the <code>int</code> value of the specified attribute
     * from MBeanServer.
     *
     * @param object MBean's <code>ObjectName</code>
     * @param name name of the attribute.
     *
     * @return value of the attribute.
     */
    protected int getIntAttribute(ObjectName object, String name) {
        try {
            Integer i = (Integer) getMBeanServer().getAttribute(object, name);
            return i.intValue();
        } catch (Exception e) {
            throw new Failure(e);
        }
    } // getIntAttribute()

    /**
     * Retrieves the <code>long</code> value of the specified attribute
     * from MBeanServer.
     *
     * @param object MBean's <code>ObjectName</code>
     * @param name name of the attribute.
     *
     * @return value of the attribute.
     */
    protected long getLongAttribute(ObjectName object, String name) {
        try {
            Long l = (Long) getMBeanServer().getAttribute(object, name);
            return l.longValue();
        } catch (Exception e) {
            throw new Failure(e);
        }
    } // getLongAttribute()

    /**
     * Retrieves the array of <code>long</code> -- value of the specified
     * attribute from MBeanServer.
     *
     * @param object MBean's <code>ObjectName</code>
     * @param name name of the attribute.
     *
     * @return value of the attribute.
     */
    protected long[] getLongArrayAttribute(ObjectName object, String name) {
        try {
            Object o = (Object) getMBeanServer().getAttribute(object, name);
            return (long[]) o;
        } catch (Exception e) {
            throw new Failure(e);
        }
    } // getLongArrayAttribute()

    /**
     * Retrieves the <code>MemoryUsage</code> value of the specified attribute
     * from MBeanServer.
     *
     * @param object MBean's <code>ObjectName</code>
     * @param name name of the attribute.
     *
     * @return value of the attribute.
     */
    protected MemoryUsage getMemoryUsageAttribute(ObjectName object,
                                                                   String name) {
        try {
                Object data = getMBeanServer().getAttribute(object, name);
                if (data instanceof MemoryUsage)
                        return (MemoryUsage) data;
                return MemoryUsage.from((CompositeData) data);
        } catch (Exception e) {
            throw new Failure(e);
        }
    } // getMemoryUsageAttribute()

    /**
     * Sets the <code>long</code> value to the specified attribute from
     * MBeanServer.
     *
     * @param object MBean's <code>ObjectName</code>
     * @param name name of the attribute.
     * @param value value of the attribute.
     */
    protected void setLongAttribute(ObjectName object, String name,
                                                                   long value) {
        Attribute attribute = new Attribute(name, Long.valueOf(value));

        try {
            getMBeanServer().setAttribute(object, attribute);
        } catch (Exception e) {
            throw new Failure(e);
        }
    } // setLongAttribute()

    /**
     * Sets the <code>boolean</code> value to the specified attribute from
     * MBeanServer.
     *
     * @param object MBean's <code>ObjectName</code>
     * @param name name of the attribute.
     * @param value value of the attribute.
     */
    protected void setBooleanAttribute(ObjectName object, String name,
                                                                boolean value) {
        Attribute attribute = new Attribute(name, Boolean.valueOf(value));

        try {
            getMBeanServer().setAttribute(object, attribute);
        } catch (Exception e) {
            throw new Failure(e);
        }
    } // setBooleanAttribute()

    // Create a new MBean with specified object name
    private static void createMBean(Monitor monitor, String name) {

        if (testMode == SERVER_MODE || testMode == PROXY_MODE) {
            try {
                monitor.mbeanObjectName = new ObjectName(name);
            } catch (MalformedObjectNameException e) {
                throw new Failure(e);
            }
        }
    }
} // Monitor

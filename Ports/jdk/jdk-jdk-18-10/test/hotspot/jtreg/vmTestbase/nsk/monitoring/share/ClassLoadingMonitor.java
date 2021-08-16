/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import nsk.share.*;

/**
 * <code>ClassLoadingMonitor</code> class is a wrapper of
 * <code>ClassLoadingMXBean</code>. Depending on command line arguments, an
 * instance of this class redirects invocations to the
 * <code>ClassLoadingMXBean</code> interface. If
 * <code>-testMode="directly"</code> option is set, this instance directly
 * invokes corresponding method of the <tt>ClassLoadingMXBean</tt> interface. If
 * <code>-testMode="server"</code> option is set, it makes invokations via
 * MBeanServer. If <code>-testMode="proxy"</code> option is set it will make
 * invocations via MBeanServer proxy.
 *
 * @see ArgumentHandler
 */
public class ClassLoadingMonitor extends Monitor {

    // Names of the attributes of ClassLoadingMXBean
    private static final String LOADED_CLASSES = "LoadedClassCount";
    private static final String TOTAL_CLASSES = "TotalLoadedClassCount";
    private static final String UNLOADED_CLASSES = "UnloadedClassCount";

    // Internal trace level
    private static final int TRACE_LEVEL = 10;

    // An instance of ClassLoadingMXBean
    private static ClassLoadingMXBean mbean
        = ManagementFactory.getClassLoadingMXBean();

    private ClassLoadingMXBean proxyInstance;

    static {
        Monitor.logPrefix = "ClassLoadingMonitor> ";
    }

    /**
     * Creates a new <code>ClassLoadingMonitor</code> object.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     */
    public ClassLoadingMonitor(Log log, ArgumentHandler argumentHandler) {
        super(log, argumentHandler);
    }

    /**
     *
     * Return a proxy instance for a platform
     * {@link java.lang.management.ClassLoadingMXBean
     * <code>ClassLoadingMXBean</code>} interface.
     *
     */
    synchronized ClassLoadingMXBean getProxy() {
        if (proxyInstance == null) {
            // create proxy instance
            try {
                proxyInstance = (ClassLoadingMXBean)
                ManagementFactory.newPlatformMXBeanProxy(
                    getMBeanServer(),
                    ManagementFactory.CLASS_LOADING_MXBEAN_NAME,
                    ClassLoadingMXBean.class
                );
            } catch (java.io.IOException e) {
                throw new Failure(e);
            }
        }
        return proxyInstance;
    }

    /**
     * Redirects the invocation to
     * {@link java.lang.management.ClassLoadingMXBean#getLoadedClassCount()
     * <code>ClassLoadingMXBean.getLoadedClassCount()</code>}.
     *
     * @return the number of currently loaded classes.
     */
    public int getLoadedClassCount() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL,"[getLoadedClassCount] "
                       + "getLoadedClassCount() directly invoked");
            return mbean.getLoadedClassCount();

        case SERVER_MODE:
            logger.trace(TRACE_LEVEL,"[getLoadedClassCount] "
                       + "getLoadedClassCount() invoked through MBeanServer");
            return getIntAttribute(mbeanObjectName, LOADED_CLASSES);

        case PROXY_MODE:
            logger.trace(TRACE_LEVEL,"[getLoadedClassCount] "
                       + "getLoadedClassCount() invoked through proxy");
            return getProxy().getLoadedClassCount();
        }

        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to {@link
     * java.lang.management.ClassLoadingMXBean#getTotalLoadedClassCount()
     * <code>ClassLoadingMXBean.getTotalLoadedClassCount()</code>}.
     *
     * @return the total number of classes loaded.
     */
    public long getTotalLoadedClassCount() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL,"[getTotalLoadedClassCount] "
                       + "getTotalLoadedClassCount() directly invoked");
            return mbean.getTotalLoadedClassCount();

        case SERVER_MODE:
            logger.trace(TRACE_LEVEL,"[getTotalLoadedClassCount] "
                      + "getTotalLoadedClassCount() invoked through "
                      + "MBeanServer");
            return getLongAttribute(mbeanObjectName, TOTAL_CLASSES);

        case PROXY_MODE:
            logger.trace(TRACE_LEVEL,"[getTotalLoadedClassCount] "
                       + "getTotalLoadedClassCount() invoked through proxy");
            return getProxy().getTotalLoadedClassCount();
        }
        throw new TestBug("Unknown testMode " + mode);
    }

    /**
     * Redirects the invocation to
     * {@link java.lang.management.ClassLoadingMXBean#getUnloadedClassCount()
     * <code>ClassLoadingMXBean.getUnloadedClassCount()</code>}.
     *
     * @return the number of unloaded classes.
     */
    public long getUnloadedClassCount() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL,"[getUnloadedClassCount] "
                       + "getUnloadedClassCount() directly invoked");
            return mbean.getUnloadedClassCount();

        case SERVER_MODE:
            logger.trace(TRACE_LEVEL,"[getUnloadedClassCount] "
                       + "getUnloadedClassCount() invoked through MBeanServer");
            return getLongAttribute(mbeanObjectName, UNLOADED_CLASSES);

        case PROXY_MODE:
            logger.trace(TRACE_LEVEL,"[getUnloadedClassCount] "
                       + "getUnloadedClassCount() invoked through proxy");
            return getProxy().getUnloadedClassCount();

        }

        throw new TestBug("Unknown testMode " + mode);
    }
} // ClassLoadingMonitor

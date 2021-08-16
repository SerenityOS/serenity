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
import nsk.share.*;

/**
 * <code>CompilationMonitor</code> class is a wrapper of
 * <tt>CompilationMXBean</tt>. Depending on command line arguments, an instance
 * of this class redirects invocations to the <tt>CompilationMXBean</tt>
 * interface. If <code>-testMode="directly"</code> option is set, this instance
 * directly invokes corresponding method of the <tt>CompilationMXBean</tt>
 * interface. If <code>-testMode="server"</code> option is set it will make
 * invocations via MBeanServer. If <code>-testMode="proxy"</code> option is set
 * it will make invocations via MBeanServer proxy.
 *
 * @see ArgumentHandler
 */
public class CompilationMonitor extends Monitor {

    // An instance of CompilationMXBean
    private static CompilationMXBean mbean
        = ManagementFactory.getCompilationMXBean();

    private CompilationMXBean proxyInstance;

    // An attribute of CompilationMXBean
    private static final String IS_COMP = "CompilationTimeMonitoringSupported";

    static {
        Monitor.logPrefix = "CompilationMonitor> ";
    }

    /**
     * Creates a new <code>CompilationMonitor</code> object.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     */
    public CompilationMonitor(Log log, ArgumentHandler argumentHandler) {
        super(log, argumentHandler);

    }

    /**
     *
     * Return a proxy instance for a platform
     * {@link java.lang.management.CompilationMXBean
     * <code>CompilationMXBean</code>} interface.
     *
     */
    synchronized CompilationMXBean getProxy() {
        if (proxyInstance == null) {
            // create proxy instance
            try {
                proxyInstance = (CompilationMXBean)
                ManagementFactory.newPlatformMXBeanProxy(
                    getMBeanServer(),
                    ManagementFactory.COMPILATION_MXBEAN_NAME,
                    CompilationMXBean.class
                );
            } catch (java.io.IOException e) {
                throw new Failure(e);
            }
        }
        return proxyInstance;
    }

    /**
     * Detects if the JVM has compilation system.
     *
     * @return <code>true</code>, if the JVM has compilation system,
     *         <code>false</code> otherwise.
     */
    public boolean isCompilationSystem() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return (mbean != null);

        case SERVER_MODE:
        case PROXY_MODE:
            try {
                return getMBeanServer().isRegistered(mbeanObjectName);
            } catch (RuntimeOperationsException e) {
                complain("Unexpected exception");
                e.printStackTrace(logger.getOutStream());
                throw new Failure(e);
            }
        }

        throw new TestBug("Unknown testMode " + mode);
    } // isCompilationSystem()

    /**
     * Redirects the invocation to
     * {@link CompilationMXBean#isCompilationTimeMonitoringSupported()
     * <code>CompilationMXBean.isCompilationTimeMonitoringSupported()</code>}.
     *
     * @return <code>true</code>, if the monitoring of compilation time is
     *         supported, <code>false</code> otherwise.
     */
    public boolean isCompilationTimeMonitoringSupported() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.isCompilationTimeMonitoringSupported();

        case SERVER_MODE:
            return getBooleanAttribute(mbeanObjectName, IS_COMP);

        case PROXY_MODE:
            return getProxy().isCompilationTimeMonitoringSupported();
        }

        throw new TestBug("Unknown testMode " + mode);
    } // isCompilationTimeMonitoringSupported()
} // CompilationMonitor

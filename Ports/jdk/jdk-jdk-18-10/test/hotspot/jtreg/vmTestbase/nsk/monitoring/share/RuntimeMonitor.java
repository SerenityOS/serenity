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
 * <code>RuntimeMonitor</code> class is a wrapper of <code>RuntimeMXBean</code>.
 * Depending on command line arguments, an instance of this class redirects
 * invocations to the <code>RuntimeMXBean</code> interface. If
 * <code>-testMode="directly"</code> option is set, this instance directly
 * invokes corresponding method of the <code>RuntimeMXBean</code> interface. If
 * <code>-testMode="server"</code> option is set it will make invocations via
 * MBeanServer. If <code>-testMode="proxy"</code> option is set it will make
 * invocations via MBeanServer proxy.
 *
 * @see ArgumentHandler
 */
public class RuntimeMonitor extends Monitor {
    // An instance of ClassLoadingMBean
    private final static RuntimeMXBean mbean
        = ManagementFactory.getRuntimeMXBean();

    // Name of an attribute of RuntimeMonitor
    private static final String IS_BOOT = "BootClassPathSupported";

    private RuntimeMXBean proxyInstance;

    static {
        Monitor.logPrefix = "RuntimeMonitor> ";
    }

    /**
     * Creates a new <code>RuntimeMonitor</code> object.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     */
    public RuntimeMonitor(Log log, ArgumentHandler argumentHandler) {
        super(log, argumentHandler);
    }

    /**
     *
     * Return a proxy instance for a platform
     * {@link java.lang.management.RuntimeMXBean
     * <code>RuntimeMXBean</code>} interface.
     *
     */
    RuntimeMXBean getProxy() {
        if (proxyInstance == null) {
            // create proxy instance
            try {
                proxyInstance = (RuntimeMXBean)
                ManagementFactory.newPlatformMXBeanProxy(
                    getMBeanServer(),
                    ManagementFactory.RUNTIME_MXBEAN_NAME,
                    RuntimeMXBean.class
                );
            } catch (java.io.IOException e) {
                throw new Failure(e);
            }
        }
        return proxyInstance;
    }

    /**
     * Redirects the invocation to
     * {@link java.lang.management.RuntimeMXBean#isBootClassPathSupported()
     * <code>RuntimeMXBean.isBootClassPathSupported()</code>}.
     *
     * @return <code>true</code>, if the JVM supports the class path mechanism;
     *         <code>flase</code> otherwise.
     */
    public boolean isBootClassPathSupported() {
        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            return mbean.isBootClassPathSupported();

        case SERVER_MODE:
            return getBooleanAttribute(mbeanObjectName, IS_BOOT);

        case PROXY_MODE:
            return getProxy().isBootClassPathSupported();
        }

        throw new TestBug("Unknown testMode " + mode);
    }
} // RuntimeMonitor

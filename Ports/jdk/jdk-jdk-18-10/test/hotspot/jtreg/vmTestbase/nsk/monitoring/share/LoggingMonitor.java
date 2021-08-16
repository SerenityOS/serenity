/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.logging.*;
import javax.management.*;
import java.io.IOException;
import java.util.List;

import nsk.share.*;

/**
 * <code>LoggingMonitor</code> class is a wrapper of <code>LoggingMXBean</code>.
 * Depending on command line arguments, an instance of this class redirects
 * invocations to the <code>LoggingMXBean</code> interface. If
 * <code>-testMode="directly"</code> option is set, this instance directly
 * invokes corresponding method of the <code>LoggingMXBean</code> interface. If
 * <code>-testMode="server"</code> option is set it will make invocations via
 * MBeanServer. If <code>-testMode="proxy"</code> option is set it will make
 * invocations via MBeanServer proxy.
 *
 * @see ArgumentHandler
 */

public class LoggingMonitor extends Monitor {

    public static final String LOGGING_MXBEAN_NAME =
        LogManager.LOGGING_MXBEAN_NAME;

    private static LoggingMXBean mbean = LogManager.getLoggingMXBean();
    private LoggingMXBean proxyInstance = null;

    // Internal trace level
    private static final int TRACE_LEVEL = 10;

    // Names of the attributes of ClassLoadingMXBean
    private static final String GET_LOGGER_LEVEL = "getLoggerLevel";
    private static final String SET_LOGGER_LEVEL = "setLoggerLevel";
    private static final String GET_PARENT_LOGGER_NAME = "getParentLoggerName";
    private static final String LOGGER_NAMES = "LoggerNames";

    static {
        Monitor.logPrefix = "LoggingMonitor> ";
    }

    /**
     * Creates a new <code>LoggingMonitor</code> object.
     *
     * @param log <code>Log</code> object to print info to.
     * @param argumentHandler <code>ArgumentHandler</code> object that saves
     *        all info about test's arguments.
     *
     */
    public LoggingMonitor(Log log, ArgumentHandler argumentHandler) {

        super(log, argumentHandler);
    }

    public ObjectName getMBeanObjectName() {

        return mbeanObjectName;
    }

    /**
     *
     * Return a proxy instance for a platform
     * {@link java.lang.management.LoggingMXBean
     * <code>LoggingMXBean</code>} interface.
     *
     */
    LoggingMXBean getProxy() {
        if (proxyInstance == null) {
            // create proxy instance
            try {
                proxyInstance = (LoggingMXBean)
                ManagementFactory.newPlatformMXBeanProxy(
                    getMBeanServer(),
                    LOGGING_MXBEAN_NAME,
                    LoggingMXBean.class
                );
            } catch (Exception e) {
                throw new Failure(e);
            }
        }
        return proxyInstance;
    }

    /**
     * Redirects the invocation to
     * {@link java.util.logging.LoggingMXBean#getLoggerLevel(String)
     * <code>LoggingMXBean.getLoggerLevel(String loggerName)</code>}.
     *
     * @param loggerName The name of the <tt>Logger</tt> to be retrieved.
     *
     * @return The name of the log level of the specified logger; or
     *         an empty string if the log level of the specified logger
     *         is <tt>null</tt>.  If the specified logger does not
     *         exist, <tt>null</tt> is returned.
     *
     * @see java.util.logging.LoggingMXBean#getLoggerLevel(String)
     */
     public String getLoggerLevel(String loggerName) {

        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL,"[getLoggerLevel] "
                       + "getLoggerLevel() directly invoked");
            return mbean.getLoggerLevel(loggerName);

        case SERVER_MODE:
            logger.trace(TRACE_LEVEL,"[getLoggerLevel] "
                       + "getLoggerLevel() invoked through MBeanServer");

            try {

                Object[] params = { loggerName };
                String[] signature = { String.class.getName() };

                String res = (String) getMBeanServer().invoke(
                    mbeanObjectName,
                    GET_LOGGER_LEVEL,
                    params,
                    signature );
                return res;

            } catch (Exception e) {

                throw new Failure(e);
            }
        case PROXY_MODE:
            logger.trace(TRACE_LEVEL,"[getLoggerLevel] "
                       + "getLoggerLevel() invoked through MBeanServer proxy");
            return getProxy().getLoggerLevel(loggerName);
        }

        throw new TestBug("Unknown testMode " + mode);
    }


    /**
     * Redirects the invocation to
     * {@link java.util.logging.LoggingMXBean#getLoggerNames()
     * <code>LoggingMXBean.getLoggerNames()</code>}.
     *
     * @return A list of <tt>String</tt> each of which is a
     *         currently registered <tt>Logger</tt> name.
     *
     * @see java.util.logging.LoggingMXBean#getLoggerNames()
     */
    public List<String> getLoggerNames() {

        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL,"[getLoggerNames] "
                       + "getLoggerNames() directly invoked");
            return mbean.getLoggerNames();

        case SERVER_MODE:
            logger.trace(TRACE_LEVEL,"[getLoggerNames] "
                       + "getLoggerNames() invoked through MBeanServer");

            try {

                    Object value = getMBeanServer().getAttribute(
                        mbeanObjectName, LOGGER_NAMES);
                    if (value instanceof List)
                            return (List<String>) value;
                    else if (value instanceof Object[]) {
                            Object[] names = (Object[]) value;
                            List<String> res = new java.util.ArrayList<String>(names.length);
                            for (int i=0; i < names.length; i++)
                                    res.add(names[i].toString());

                            return res;
                    } else {
                            String[] names = (String[]) getMBeanServer().getAttribute(
                                            mbeanObjectName, LOGGER_NAMES);

                            List<String> res = new java.util.ArrayList<String>(names.length);
                            for (int i=0; i<names.length; i++)
                                    res.add(names[i]);

                            return res;
                    }
                    } catch (Exception e) {
                            throw new Failure(e);
                    }
        case PROXY_MODE:
            logger.trace(TRACE_LEVEL,"[getLoggerNames] "
                       + "getLoggerNames() invoked through MBeanServer proxy");
            return getProxy().getLoggerNames();
        }

        throw new TestBug("Unknown testMode " + mode);
    }


    /**
     * Redirects the invocation to
     * {@link java.util.logging.LoggingMXBean#getParentLoggerName(String)
     * <code>LoggingMXBean.getParentLoggerName(String loggerName)</code>}.
     *
     * @param loggerName The name of a <tt>Logger</tt>.
     *
     * @return the name of the nearest existing parent logger;
     *         an empty string if the specified logger is the root logger.
     *         If the specified logger does not exist, <tt>null</tt>
     *         is returned.
     *
     * @see java.util.logging.LoggingMXBean#getParentLoggerName(String)
     */
    public String getParentLoggerName(String loggerName) {

        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL,"[getParentLoggerName] "
                       + "getParentLoggerName() directly invoked");
            return mbean.getParentLoggerName(loggerName);

        case SERVER_MODE:
            logger.trace(TRACE_LEVEL,"[getParentLoggerName] "
                       + "getParentLoggerName() invoked through MBeanServer");

            try {

                Object[] params = { loggerName };
                String[] signature = { String.class.getName() };

                String res = (String) getMBeanServer().invoke(
                    mbeanObjectName,
                    GET_PARENT_LOGGER_NAME,
                    params,
                    signature );
                return res;

            } catch (Exception e) {

                throw new Failure(e);
            }
        case PROXY_MODE:
            logger.trace(TRACE_LEVEL,"[getParentLoggerName] "
                       + "getParentLoggerName() invoked through MBeanServer proxy");
            return getProxy().getParentLoggerName(loggerName);
        }

        throw new TestBug("Unknown testMode " + mode);
    }


    /**
     * Redirects the invocation to
     * {@link java.util.logging.LoggingMXBean#setLoggerLevel(String,String)
     * <code>LoggingMXBean.setLoggerLevel(String loggerName, String levelName)</code>}.
     *
     * @param loggerName The name of the <tt>Logger</tt> to be set.
     *                   Must be non-null.
     * @param levelName The name of the level to set the specified logger to,
     *                 or <tt>null</tt> if to set the level to inherit
     *                 from its nearest ancestor.
     *
     * @see java.util.logging.LoggingMXBean#setLoggerLevel(String,String)
     */
    public void setLoggerLevel(String loggerName, String levelName) {

        int mode = getTestMode();

        switch (mode) {
        case DIRECTLY_MODE:
            logger.trace(TRACE_LEVEL,"[setLoggerLevel] "
                       + "setLoggerLevel() directly invoked");
            mbean.setLoggerLevel(loggerName, levelName);
            break;
        case SERVER_MODE:
            logger.trace(TRACE_LEVEL,"[setLoggerLevel] "
                       + "setLoggerLevel() invoked through MBeanServer");

            try {

                Object[] params = { loggerName, levelName };
                String[] signature = { String.class.getName(), String.class.getName() };

                getMBeanServer().invoke(
                    mbeanObjectName,
                    SET_LOGGER_LEVEL,
                    params,
                    signature );

            } catch (Exception e) {

                throw new Failure(e);
            }
            break;
        case PROXY_MODE:
            logger.trace(TRACE_LEVEL,"[setLoggerLevel] "
                       + "setLoggerLevel() invoked through MBeanServer proxy");
            getProxy().setLoggerLevel(loggerName, levelName);
            break;
        }

        throw new TestBug("Unknown testMode " + mode);
    }

} // LoggingMonitor

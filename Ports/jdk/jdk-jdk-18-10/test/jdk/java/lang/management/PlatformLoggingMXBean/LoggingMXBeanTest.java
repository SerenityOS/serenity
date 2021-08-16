/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 7024172 7067691
 * @summary Test if proxy for PlatformLoggingMXBean is equivalent
 *          to proxy for LoggingMXBean
 *
 * @build LoggingMXBeanTest
 * @run main LoggingMXBeanTest
 */

import java.lang.management.*;
import javax.management.MBeanServer;
import java.util.logging.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LoggingMXBeanTest
{
    static final String LOGGER_NAME_1 = "com.sun.management.Logger";
    static final String LOGGER_NAME_2 = "com.sun.management.Logger.Logger2";
    static final String UNKNOWN_LOGGER_NAME = "com.sun.management.Unknown";

    // These instance variables prevent premature logger garbage collection
    // See getLogger() weak reference warnings.
    Logger logger1;
    Logger logger2;

    static LoggingMXBeanTest test;

    public static void main(String[] argv) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        LoggingMXBean proxy =
            ManagementFactory.newPlatformMXBeanProxy(mbs,
                LogManager.LOGGING_MXBEAN_NAME,
                LoggingMXBean.class);

        // test LoggingMXBean proxy
        test = new LoggingMXBeanTest(proxy);

        // check if the attributes implemented by PlatformLoggingMXBean
        // and LoggingMXBean return the same value
        PlatformLoggingMXBean mxbean =
            ManagementFactory.getPlatformMXBean(mbs, PlatformLoggingMXBean.class);

        checkAttributes(proxy, mxbean);
    }

    // same verification as in java/util/logging/LoggingMXBeanTest2
    public LoggingMXBeanTest(LoggingMXBean mbean) throws Exception {

        logger1 = Logger.getLogger( LOGGER_NAME_1 );
        logger1.setLevel(Level.FINE);
        logger2 = Logger.getLogger( LOGGER_NAME_2 );
        logger2.setLevel(null);

        /*
         *  Check for the existence of our new Loggers
         */
        System.out.println("Test Logger Name retrieval (getLoggerNames)");
        boolean log1 = false, log2 = false;
        List<String> loggers = mbean.getLoggerNames();
        if (loggers == null || loggers.size() < 2) {
            throw new RuntimeException(
                "Could not Detect the presense of the new Loggers");
        }

        for (String logger : loggers) {
            if (logger.equals(LOGGER_NAME_1)) {
                log1 = true;
                System.out.println("  : Found new Logger : " + logger);
            }
            if (logger.equals(LOGGER_NAME_2)) {
                log2 = true;
                System.out.println("  : Found new Logger : " + logger);
            }
        }
        if ( log1 && log2 )
            System.out.println("  : PASSED." );
        else {
            System.out.println("  : FAILED.  Could not Detect the new Loggers." );
            throw new RuntimeException(
                "Could not Detect the presense of the new Loggers");
        }

        System.out.println("Test getLoggerLevel");
        String l1 = mbean.getLoggerLevel(LOGGER_NAME_1);
        System.out.println("  : Level for Logger " + LOGGER_NAME_1 + " : " + l1);
        if (!l1.equals(Level.FINE.getName())) {
            throw new RuntimeException(
                "Expected level for " + LOGGER_NAME_1 + " = " +
                 Level.FINE.getName() + " but got " + l1);
        }
        String l2 = mbean.getLoggerLevel(LOGGER_NAME_2);
        System.out.println("  : Level for Logger " + LOGGER_NAME_2 + " : " + l2);
        if (!l2.equals("")) {
            throw new RuntimeException(
                "Expected level for " + LOGGER_NAME_2 + " = \"\"" +
                 " but got " + l2);
        }
        String l3 = mbean.getLoggerLevel(UNKNOWN_LOGGER_NAME);
        System.out.println("  : Level for unknown logger : " + l3);
        if (l3 != null) {
            throw new RuntimeException(
                "Expected level for " + UNKNOWN_LOGGER_NAME + " = null" +
                 " but got " + l3);
        }

        System.out.println("Test setLoggerLevel");
        mbean.setLoggerLevel(LOGGER_NAME_1, "INFO");
        System.out.println("  : Set Level for Logger " + LOGGER_NAME_1 + " to: INFO");
        Level l = logger1.getLevel();
        if (l != Level.INFO) {
            throw new RuntimeException(
                "Expected level for " + LOGGER_NAME_1 + " = " +
                 Level.INFO + " but got " + l);
        }

        mbean.setLoggerLevel(LOGGER_NAME_2, "SEVERE");
        System.out.println("  : Set Level for Logger " + LOGGER_NAME_2 + " to: SERVER");
        l = logger2.getLevel();
        if (l != Level.SEVERE) {
            throw new RuntimeException(
                "Expected level for " + LOGGER_NAME_2 + " = " +
                 Level.SEVERE+ " but got " + l);
        }

        mbean.setLoggerLevel(LOGGER_NAME_1, null);
        System.out.println("  : Set Level for Logger " + LOGGER_NAME_1 + " to: null");
        l = logger1.getLevel();
        if (l != null) {
            throw new RuntimeException(
                "Expected level for " + LOGGER_NAME_1 + " = null " +
                 " but got " + l);
        }

        boolean iaeCaught = false;
        System.out.println("  : Set Level for unknown Logger to: FINE");
        try {
            mbean.setLoggerLevel(UNKNOWN_LOGGER_NAME, "FINE");
        } catch (IllegalArgumentException e) {
            // expected
            iaeCaught = true;
            System.out.println("      : IllegalArgumentException caught as expected");
        }
        if (!iaeCaught) {
            throw new RuntimeException(
                "Expected IllegalArgumentException for setting level for " +
                UNKNOWN_LOGGER_NAME + " not thrown");
        }
        iaeCaught = false;
        System.out.println("  : Set Level for Logger " + LOGGER_NAME_1 + " to: DUMMY");
        try {
            mbean.setLoggerLevel(LOGGER_NAME_1, "DUMMY");
        } catch (IllegalArgumentException e) {
            // expected
            iaeCaught = true;
            System.out.println("      : IllegalArgumentException caught as expected");
        }
        if (!iaeCaught) {
            throw new RuntimeException(
                "Expected IllegalArgumentException for invalid level.");
        }


        System.out.println("Test getParentLoggerName");
        String p1 = mbean.getParentLoggerName(LOGGER_NAME_2);
        System.out.println("  : Parent Logger for " + LOGGER_NAME_2 + " : " + p1);
        if (!p1.equals(LOGGER_NAME_1)) {
            throw new RuntimeException(
                "Expected parent for " + LOGGER_NAME_2 + " = " +
                LOGGER_NAME_1 + " but got " + p1);
        }
        String p2 = mbean.getParentLoggerName("");
        System.out.println("  : Parent Logger for \"\" : " + p2);
        if (!p2.equals("")) {
            throw new RuntimeException(
                "Expected parent for root logger \"\" = \"\"" +
                " but got " + p2);
        }
        String p3 = mbean.getParentLoggerName(UNKNOWN_LOGGER_NAME);
        System.out.println("  : Parent Logger for unknown logger : " + p3);
        if (p3 != null) {
            throw new RuntimeException(
                "Expected level for " + UNKNOWN_LOGGER_NAME + " = null" +
                 " but got " + p3);
        }
    }

    private static void checkAttributes(LoggingMXBean mxbean1,
                                        PlatformLoggingMXBean mxbean2) {
        // verify logger names
        List<String> loggers1 = mxbean1.getLoggerNames();
        System.out.println("Loggers: " + loggers1);

        // Retrieve the named loggers to prevent them from being
        // spontaneously gc'ed.
        Map<String, Logger> loggersMap = new HashMap<>();
        for (String n : loggers1) {
            loggersMap.put(n, Logger.getLogger(n));
        }

        List<String> loggers2 = mxbean2.getLoggerNames();

        // loggers1 and loggers2 should be identical - no new logger should
        // have been created in between (at least no new logger name)
        //
        if (loggers1.size() != loggers2.size())
            throw new RuntimeException("LoggerNames: unmatched number of entries");
        if (!loggers2.containsAll(loggersMap.keySet()))
            throw new RuntimeException("LoggerNames: unmatched loggers");


        // verify logger's level  and parent
        for (String logger : loggers1) {
            String level1 = mxbean1.getLoggerLevel(logger);
            String level2 = mxbean2.getLoggerLevel(logger);
            if (!java.util.Objects.equals(level1, level2)) {
                throw new RuntimeException(
                        "LoggerLevel: unmatched level for " + logger
                        + ", " + level1 + ", " + level2);
            }

            if (!mxbean1.getParentLoggerName(logger)
                    .equals(mxbean2.getParentLoggerName(logger)))
                throw new RuntimeException(
                    "ParentLoggerName: unmatched parent logger's name for " + logger);
        }
    }
}

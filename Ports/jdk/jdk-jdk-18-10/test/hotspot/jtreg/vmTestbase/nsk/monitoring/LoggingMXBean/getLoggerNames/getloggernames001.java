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

package nsk.monitoring.LoggingMXBean.getLoggerNames;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;
import nsk.monitoring.share.*;
import java.util.*;
import java.util.logging.*;

import javax.management.*;

public class getloggernames001 {

    private static boolean testFailed = false;

    public static void main(String[] args) {

        System.exit(Consts.JCK_STATUS_BASE + run(args, System.out));
    }

    private static Log log;

    private static final Level[] LogLevels = new Level[] {

        Level.ALL, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST,
        Level.INFO, Level.OFF, Level.SEVERE, Level.WARNING
    };

    static int run(String[] args, PrintStream out) {

        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        LoggingMonitor loggingMonitor = Monitor.getLoggingMonitor(log,
            argumentHandler);

        // Test case 1. check that
        // This method calls LogManager.getLoggerNames() and returns a
        // list of the logger names

        // create logger
        String testLog1Name = getloggernames001.class.getName();
        Logger testLog1 = Logger.getLogger(testLog1Name);

        Enumeration loggerNamesEn = LogManager.getLogManager().getLoggerNames();
        List loggerMXBeanNamesList = loggingMonitor.getLoggerNames();

        // check that all elements from loggerNamesEn contain in
        // loggerMXBeanNamesList
        while (loggerNamesEn.hasMoreElements()) {

            Object loggerName = loggerNamesEn.nextElement();
            //log.display("loggerName="+loggerName);

            boolean isInvolved = false;
            for (int i=0; i<loggerMXBeanNamesList.size(); i++) {

                Object loggerMXBeanName = loggerMXBeanNamesList.get(i);
                if ( loggerName.equals(loggerMXBeanName) ) {

                    isInvolved = true;
                    break;
                }
            }

            if (! isInvolved) {

                log.complain("FAILURE 1.");
                log.complain("LoggingMonitor.getLoggerNames() does not return "
                    + "the next logger name: "+loggerName);
                testFailed = true;
            }
        }

        // check that all elements from loggerMXBeanNamesList contain in
        // loggerNamesEn
        loggerNamesEn = LogManager.getLogManager().getLoggerNames();
        for (int i=0; i<loggerMXBeanNamesList.size(); i++) {

            Object loggerMXBeanName = loggerMXBeanNamesList.get(i);
            //log.display("loggerMXBeanName="+loggerMXBeanName);

            boolean isInvolved = false;

            while (loggerNamesEn.hasMoreElements()) {

                Object loggerName = loggerNamesEn.nextElement();
                if ( loggerName.equals(loggerMXBeanName) ) {

                    isInvolved = true;
                    break;
                }
            }

            if (! isInvolved) {

                log.complain("FAILURE 2.");
                log.complain("LoggingMonitor.getLoggerNames() does not return "
                    + "unknown logger name: "+loggerMXBeanName);
                testFailed = true;
            }
        }


        if (testFailed)
            log.complain("TEST FAILED");

        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

}

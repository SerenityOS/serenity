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

package nsk.monitoring.LoggingMXBean.setLoggerLevel;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;
import nsk.monitoring.share.*;
import java.util.logging.*;

public class setloggerlevel001 {

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
        LoggingMXBean loggingMXBean = LogManager.getLoggingMXBean();



        // Test case 1. check that
        // if the levelName is not null, the level of the specified
        // logger is set to the parsed Level matching the levelName
        String testLog1Name = setloggerlevel001.class.getName();
        Logger testLog1 = Logger.getLogger(testLog1Name);

        for (int i=0; i<LogLevels.length; i++) {

            loggingMXBean.setLoggerLevel(testLog1Name, LogLevels[i].toString());
            if (! LogLevels[i].equals(testLog1.getLevel())) {

                testFailed = true;
                log.complain("Failure 1.");
                log.complain("LogLevels[i] = "+LogLevels[i].toString());
                log.complain("testLog1.getLevel() = "+testLog1.getLevel());
                log.complain("loggingMXBean.setLoggerLevel() failed");
            }
        }

        // Test case 2. check
        // if the levelName is null, the level of the specified logger is set
        // to null
        loggingMXBean.setLoggerLevel(testLog1Name, null);
        if (testLog1.getLevel() != null) {

            testFailed = true;
            log.complain("Failure 2.");
            log.complain("Level of the specified logger was not set to null: "+
            testLog1.getLevel());
        }

        if (testFailed)
            log.complain("TEST FAILED");

        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

}

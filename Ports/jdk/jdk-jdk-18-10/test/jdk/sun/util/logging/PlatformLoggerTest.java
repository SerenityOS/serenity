/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6882376 6985460 8010309 8011638
 * @summary Test if java.util.logging.Logger is created before and after
 *          logging is enabled.  Also validate some basic PlatformLogger
 *          operations.  othervm mode to make sure java.util.logging
 *          is not initialized.
 *
 * @modules java.base/sun.util.logging
 *          java.logging/sun.util.logging.internal
 * @run main/othervm PlatformLoggerTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.*;
import sun.util.logging.PlatformLogger;
import static sun.util.logging.PlatformLogger.Level.*;

public class PlatformLoggerTest {

    static Logger logger;
    static PlatformLogger bar;
    static PlatformLogger goo;
    static PlatformLogger foo;

    public static void main(String[] args) throws Exception {
        final String FOO_PLATFORM_LOGGER = "test.platformlogger.foo";
        final String BAR_PLATFORM_LOGGER = "test.platformlogger.bar";
        final String GOO_PLATFORM_LOGGER = "test.platformlogger.goo";
        final String BAR_LOGGER = "test.logger.bar";
        goo = PlatformLogger.getLogger(GOO_PLATFORM_LOGGER);
        // test the PlatformLogger methods
        testLogMethods(goo);

        // Create a platform logger using the default
        foo = PlatformLogger.getLogger(FOO_PLATFORM_LOGGER);
        checkPlatformLogger(foo, FOO_PLATFORM_LOGGER);

        // create a java.util.logging.Logger
        // now java.util.logging.Logger should be created for each platform logger
        logger = Logger.getLogger(BAR_LOGGER);
        logger.setLevel(Level.WARNING);

        bar = PlatformLogger.getLogger(BAR_PLATFORM_LOGGER);
        checkPlatformLogger(bar, BAR_PLATFORM_LOGGER);

        // test the PlatformLogger methods
        testLogMethods(goo);
        testLogMethods(bar);

        checkLogger(FOO_PLATFORM_LOGGER, Level.FINER);
        checkLogger(BAR_PLATFORM_LOGGER, Level.FINER);

        checkLogger(GOO_PLATFORM_LOGGER, null);
        checkLogger(BAR_LOGGER, Level.WARNING);

        foo.setLevel(PlatformLogger.Level.SEVERE);
        checkLogger(FOO_PLATFORM_LOGGER, Level.SEVERE);

        checkPlatformLoggerLevels(foo, bar);
    }

    // don't use java.util.logging here to prevent it from initialized
    private static void checkPlatformLogger(PlatformLogger logger, String name) {
        if (!logger.getName().equals(name)) {
            throw new RuntimeException("Invalid logger's name " +
                logger.getName() + " but expected " + name);
        }

        if (logger.level() != null) {
            throw new RuntimeException("Invalid default level for logger " +
                logger.getName() + ": " + logger.level());
        }

        checkLoggable(logger, FINE, false);

        logger.setLevel(FINER);
        checkLevel(logger, FINER);
        checkLoggable(logger, FINER, true);
        checkLoggable(logger, FINE, true);
        checkLoggable(logger, FINEST, false);

        logger.info("OK: Testing log message");
    }

    private static void checkLoggable(PlatformLogger logger, PlatformLogger.Level level, boolean expected) {
        if (logger.isLoggable(level) != expected) {
            throw new RuntimeException("logger " + logger.getName() + ": " + level +
                (expected ? " not loggable" : " loggable"));
        }
    }

    private static void checkLevel(PlatformLogger logger, PlatformLogger.Level level) {
        if (logger.level() != level) {
            throw new RuntimeException("Invalid level for logger " +
                logger.getName() + ": " + logger.level() + " != " + level);
        }
    }

    private static void checkLogger(String name, Level level) {
        Logger logger = LogManager.getLogManager().getLogger(name);
        if (logger == null) {
            throw new RuntimeException("Logger " + name +
                " does not exist");
        }

        if (logger.getLevel() != level) {
            throw new RuntimeException("Invalid level for logger " +
                logger.getName() + " " + logger.getLevel());
        }
    }

    private static void testLogMethods(PlatformLogger logger) {
        logger.severe("Test severe(String, Object...) {0} {1}", new Long(1), "string");
        // test Object[]
        logger.severe("Test severe(String, Object...) {0}", (Object[]) getPoints());
        logger.warning("Test warning(String, Throwable)", new Throwable("Testing"));
        logger.info("Test info(String)");
    }

    private static void checkPlatformLoggerLevels(PlatformLogger... loggers) {
        final Level[] levels = new Level[] {
            Level.ALL, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST,
            Level.INFO, Level.OFF, Level.SEVERE, Level.WARNING
        };

        int count = PlatformLogger.Level.values().length;
        if (levels.length != count) {
            throw new RuntimeException("There are " + count +
                    " PlatformLogger.Level members, but " + levels.length +
                    " standard java.util.logging levels - the numbers should be equal.");
        }
        // check mappings
        for (Level level : levels) {
            checkPlatformLoggerLevelMapping(level);
        }

        for (Level level : levels) {
            PlatformLogger.Level platformLevel = PlatformLogger.Level.valueOf(level.getName());
            for (PlatformLogger logger : loggers) {
                logger.setLevel(platformLevel);       // setLevel(PlatformLogger.Level)
                checkLoggerLevel(logger, level);

                logger.setLevel(ALL);  // setLevel(int)
                checkLoggerLevel(logger, Level.ALL);
            }
        }

        Logger javaLogger = Logger.getLogger("foo.bar.baz");
        for (Level level : levels) {
            checkJavaLoggerLevel(javaLogger, level);
        }
    }

    private static void checkLoggerLevel(PlatformLogger logger, Level level) {
        PlatformLogger.Level plevel = PlatformLogger.Level.valueOf(level.getName());
        if (plevel != logger.level()) {
            throw new RuntimeException("Retrieved PlatformLogger level "
                    + logger.level()
                    + " is not the same as set level " + plevel);
        }

        // check the level set in java.util.logging.Logger
        Logger javaLogger = LogManager.getLogManager().getLogger(logger.getName());
        Level javaLevel = javaLogger.getLevel();
        if (javaLogger.getLevel() != level) {
            throw new RuntimeException("Retrieved backing java.util.logging.Logger level "
                    + javaLevel + " is not the expected " + level);
        }
    }

    private static void checkJavaLoggerLevel(Logger logger, Level level) {
        // This method exercise the mapping of java level to platform level
        // when the java level is not one of the standard levels...

        System.out.println("Testing Java Level with: " + level.getName());

        // create a brand new java logger
        Logger javaLogger = sun.util.logging.internal.LoggingProviderImpl.getLogManagerAccess()
                     .demandLoggerFor(LogManager.getLogManager(),
                          logger.getName()+"."+level.getName(), Thread.class.getModule());

        // Set a non standard java.util.logging.Level on the java logger
        // (except for OFF & ALL - which will remain unchanged)
        int intValue = level.intValue();
        if (level != Level.ALL && level != Level.OFF) {
            intValue -= 7;
        }
        javaLogger.setLevel(Level.parse(String.valueOf(intValue)));

        // check the level set in java.util.logging.Logger
        Level effectiveLevel = javaLogger.getLevel();
        System.out.println("Effective Java Level used is: " + effectiveLevel);

        if (effectiveLevel.intValue() != intValue) {
            throw new RuntimeException("Retrieved backing java.util.logging.Logger level.intValue() "
                    + effectiveLevel.intValue() + " is not the expected " + intValue);
        }
        if (intValue != level.intValue() && javaLogger.getLevel() == level) {
            throw new RuntimeException("Retrieved backing java.util.logging.Logger level "
                    + effectiveLevel + " is " + level);
        }
        if (intValue == level.intValue() && javaLogger.getLevel() != level) {
            throw new RuntimeException("Retrieved backing java.util.logging.Logger level "
                    + effectiveLevel + " is not " + level);
        }

        // check the level set in the PlatformLogger
        PlatformLogger plogger = PlatformLogger.getLogger(javaLogger.getName());
        PlatformLogger.Level expected = PlatformLogger.Level.valueOf(level.getName());
        if (plogger.level() != expected) {
            throw new RuntimeException("Retrieved backing PlatformLogger level "
                    + plogger.level() + " is not the expected " + expected);

        }
    }

    private static void checkPlatformLoggerLevelMapping(Level level) {
        // map the given level to PlatformLogger.Level of the same name and value
        PlatformLogger.Level platformLevel = PlatformLogger.Level.valueOf(level.getName());
        if (platformLevel.intValue() != level.intValue()) {
            throw new RuntimeException("Mismatched level: " + level
                    + " PlatformLogger.Level" + platformLevel);
        }
        if (!platformLevel.name().equals(level.getName())) {
            throw new RuntimeException("The value of PlatformLogger." + level.getName() + ".name() is "
                                       + platformLevel.name() + " but expected " + level.getName());
        }
    }

    static Point[] getPoints() {
        Point[] res = new Point[3];
        res[0] = new Point(0,0);
        res[1] = new Point(1,1);
        res[2] = new Point(2,2);
        return res;
    }

    static class Point {
        final int x;
        final int y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public String toString() {
            return "{x="+x + ", y=" + y + "}";
        }
    }

}

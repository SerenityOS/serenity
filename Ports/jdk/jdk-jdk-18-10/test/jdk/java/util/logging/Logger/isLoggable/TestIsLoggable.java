/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @test
 * @bug 8024525
 * @summary checks that isLoggable() can be overridden to control logging.
 * @author danielfuchs
 * @run main/othervm TestIsLoggable
 */
public class TestIsLoggable {

    // This logger can be configured to override its default level
    // for a particular set of thread ids
    public static final class ThreadLogger extends Logger {

       final Map<Long, Level> threadMap =
                Collections.synchronizedMap(new HashMap<Long, Level>());

        public ThreadLogger(String name) {
            super(name, null);
        }

        @Override
        public boolean isLoggable(Level level) {
            final Level threadLevel = threadMap.get(Thread.currentThread().getId());
            if (threadLevel == null) return super.isLoggable(level);
            final int levelValue = threadLevel.intValue();
            final int offValue = Level.OFF.intValue();
            if (level.intValue() < levelValue || levelValue == offValue) {
                return false;
            }
            return true;
        }

    }

    public static final class TestHandler extends Handler {

        final List<String> messages = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
            messages.clear();
        }

    }

    // Sorted list of standard levels
    static final List<Level> LEVELS = Collections.unmodifiableList(
            java.util.Arrays.asList(new Level[] {
                Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG,
                Level.FINE, Level.FINER, Level.FINEST
            }));

    // Test cases:
    //   LEV_   test logger.severe(msg) .. logger.finest(msg)
    //   LOG_   logger.log(Level.SEVERE, msg) ... logger.log(Level.FINEST, msg)
    //   LOG1_  logger.log(Level.SEVERE, msg, param1) ...
    //   LOG2_  logger.log(Level.SEVERE, msg, params[]) ...
    //   LOG3_  logger.log(Level.SEVERE, msg, throwable) ...
    //   LOGP_  logger.logp(Level.SEVERE, class, method, msg) ...
    //   LOGP1_ logger.logp(Level.SEVERE, class, method, msg, param1) ...
    //   LOGP2_ logger.logp(Level.SEVERE, class, method, msg, params[]) ...
    //   LOGP3_ logger.logp(Level.SEVERE, class, method, msg, throwable) ...
    public static enum LogTest {
        LEV_SEVERE, LEV_WARNING, LEV_INFO, LEV_CONFIG, LEV_FINE, LEV_FINER, LEV_FINEST,
        LOG_SEVERE, LOG_WARNING, LOG_INFO, LOG_CONFIG, LOG_FINE, LOG_FINER, LOG_FINEST,
        LOG1_SEVERE, LOG1_WARNING, LOG1_INFO, LOG1_CONFIG, LOG1_FINE, LOG1_FINER, LOG1_FINEST,
        LOG2_SEVERE, LOG2_WARNING, LOG2_INFO, LOG2_CONFIG, LOG2_FINE, LOG2_FINER, LOG2_FINEST,
        LOG3_SEVERE, LOG3_WARNING, LOG3_INFO, LOG3_CONFIG, LOG3_FINE, LOG3_FINER, LOG3_FINEST,
        LOGP_SEVERE, LOGP_WARNING, LOGP_INFO, LOGP_CONFIG, LOGP_FINE, LOGP_FINER, LOGP_FINEST,
        LOGP1_SEVERE, LOGP1_WARNING, LOGP1_INFO, LOGP1_CONFIG, LOGP1_FINE, LOGP1_FINER, LOGP1_FINEST,
        LOGP2_SEVERE, LOGP2_WARNING, LOGP2_INFO, LOGP2_CONFIG, LOGP2_FINE, LOGP2_FINER, LOGP2_FINEST,
        LOGP3_SEVERE, LOGP3_WARNING, LOGP3_INFO, LOGP3_CONFIG, LOGP3_FINE, LOGP3_FINER, LOGP3_FINEST;

        // call the method Logger.severe() ... Logger.finest() corresponding
        // to the given level 'l' (severe() for SEVERE etc...)
        public void loglevel(Level l, Logger logger, String message) {
            LogTest test = LogTest.valueOf("LEV_"+l.getName());
            switch(test) {
                case LEV_SEVERE:
                    logger.severe(message);
                    break;
                case LEV_WARNING:
                    logger.warning(message);
                    break;
                case LEV_INFO:
                    logger.info(message);
                    break;
                case LEV_CONFIG:
                    logger.config(message);
                    break;
                case LEV_FINE:
                    logger.fine(message);
                    break;
                case LEV_FINER:
                    logger.finer(message);
                    break;
                case LEV_FINEST:
                    logger.finest(message);
                    break;
            }
        }

        // The threshold at which the logger is expected to start logging.
        // trick: we derive the threshold level from the testcase name...
        public Level threshold() {
            for (Level l : LEVELS ) {
                if (this.toString().endsWith(l.getName())) {
                    return l;
                }
            }
            return Level.OFF;
        }

        // Levels for which the logger is expected to log something.
        public List<Level> loggable() {
            return LEVELS.subList(0, LEVELS.indexOf(threshold())+1);
        }

        // Levels which will be blocked because they are weaker than the
        // threshold()
        public List<Level> weaker() {
            return LEVELS.subList(LEVELS.indexOf(threshold())+1, LEVELS.size());
        }

        // Log a message at this testcase threshold, using this testcase method.
        public void log(Logger logger, String message) {
            log(threshold(), logger, message);
        }

        // Log a message at the given level, using this testcase method.
        public void log(Level level, Logger logger, String message) {
            if (this.toString().startsWith("LOG_")) {
                logger.log(level, message);
            } else if (this.toString().startsWith("LOG1_")) {
                logger.log(level, message, "dummy param");
            } else if (this.toString().startsWith("LOG2_")) {
                logger.log(level, message, new Object[] {"dummy", "param"});
            } else if (this.toString().startsWith("LOG3_")) {
                logger.log(level, message, new Exception("dummy exception"));
            } else if (this.toString().startsWith("LOGP_")) {
                logger.logp(level, "TestCase", "log", message);
            } else if (this.toString().startsWith("LOGP1_")) {
                logger.logp(level, "TestCase", "log", message, "dummy param");
            } else if (this.toString().startsWith("LOGP2_")) {
                logger.logp(level, "TestCase", "log", message,
                        new Object[] {"dummy", "param"});
            } else if (this.toString().startsWith("LOGP3_")) {
                logger.logp(level, "TestCase", "log", message,
                        new Exception("dummy exception"));
            } else if (this.toString().startsWith("LEV_")) {
                loglevel(level, logger, message);
            }
        }

        // String description of the logging method called.
        public String method() {
            if (this.toString().startsWith("LOG_")) {
                return "Logger.log(Level." + threshold().getName() +", msg): ";
            } else if (this.toString().startsWith("LOG1_")) {
                return "Logger.log(Level." + threshold().getName() +", msg, param1): ";
            } else if (this.toString().startsWith("LOG2_")) {
                return "Logger.log(Level." + threshold().getName() +", msg, params[]): ";
            } else if (this.toString().startsWith("LOG3_")) {
                return "Logger.log(Level." + threshold().getName() +", msg, throwable): ";
            } else if (this.toString().startsWith("LEV_")) {
                return "Logger."+threshold().getName().toLowerCase(Locale.ROOT)+"(): ";
            } else if (this.toString().startsWith("LOGP_")) {
                return "Logger.logp(Level." + threshold().getName() +", msg): ";
            } else if (this.toString().startsWith("LOGP1_")) {
                return "Logger.logp(Level." + threshold().getName() +", msg, param1): ";
            } else if (this.toString().startsWith("LOGP2_")) {
                return "Logger.logp(Level." + threshold().getName() +", msg, params[]): ";
            } else if (this.toString().startsWith("LOGP3_")) {
                return "Logger.logp(Level." + threshold().getName() +", msg, throwable): ";
            }
            throw new RuntimeException("Unknown test case: "+this);
        }
    }

    // The purpose of this test is to verify that the various log methods in
    // Logger now call Logger.isLoggable().
    // To do that - we're going to use a subclass of Logger, ThreadLogger, which
    // only overrides isLoggable() - and compare the level it is given to a level
    // it finds in a map indexed with the current thread id.
    // We will register a TestHandler with our ThreadLogger which will store
    // the messages in a messages map. This will allow us to verify whether the
    // logging method we're testing has or hasn't logged.
    //
    // The TestCase enum above allows us to test a combination of every possible
    // log method with every possible level inside a loop - with the
    // exception of exiting/entering/throwing that we will be testing
    // outside of that loop.
    //
    public static void main(String... args) {
        LogManager manager = LogManager.getLogManager();
        ThreadLogger logger = new ThreadLogger("foo.bar");
        //manager.addLogger(logger);
        TestHandler handler = new TestHandler();
        logger.addHandler(handler);

        //By default, logger's level is Level.INFO
        final List<Level> loggable = LEVELS.subList(0, LEVELS.indexOf(Level.INFO)+1);

        // Check our test implementation of logger.isLoggable();
        //
        // Since we haven't put anything in the threadMap, isLoggable() should
        // return true for all levels stronger or equals to Level.INFO.
        // here we're just checking that our implementation of
        // ThreadLogger.isLoggable() returns what we want - we're just testing
        // the test code...
        for (Level level : LEVELS) {
            if (logger.isLoggable(level) != loggable.contains(level)) {
                throw new RuntimeException(level +
                        ": unexpected result for isLoggable(): expected " +
                        (loggable.contains(level)));
            }
        }

        // Test that entering/exiting/throwing call isLoggable()

        // Here we test the default behavior: this call shouldn't log anything
        //   because by default the logger level is Level.INFO and these
        //   methods log at Level.FINER.
        // So by default - these methods don't log anything. We check it here.
        logger.entering("blah", "blah");
        logger.entering("blah", "blah", "blah");
        logger.entering("blah", "blah", new Object[] {"blah"});
        if (!handler.messages.isEmpty()) {
            throw new RuntimeException("Expected empty, got "+handler.messages);
        }

        logger.exiting("blah", "blah");
        logger.exiting("blah", "blah", "blah");
        logger.exiting("blah", "blah", new Object[] {"blah"});
        if (!handler.messages.isEmpty()) {
            throw new RuntimeException("Expected empty, got "+handler.messages);
        }

        logger.throwing("blah", "blah", new Exception("blah"));
        if (!handler.messages.isEmpty()) {
            throw new RuntimeException("Expected empty, got "+handler.messages);
        }

        // Now we're going to put each level in turn in the threadMap.
        // This means that isLoggable(Level.FINER) should now return true if the
        // level in the map is not one of the level in the 'stronger' list below
        // (here stronger=stronger than FINER)
        final List<Level> stronger = LEVELS.subList(0, LEVELS.indexOf(Level.FINER));
        for (Level l : LEVELS) {

            logger.threadMap.put(Thread.currentThread().getId(), l);

            // Check that our implementation of isLoggable(level) now returns true
            // if 'level' is stronger or equals to 'l' - here we're just checking
            // that our implementation of ThreadLogger.isLoggable() returns what
            // we want - we're just testing the test code...
            final List<Level> loggableLevels = LEVELS.subList(0, LEVELS.indexOf(l)+1);
            for (Level level : LEVELS) {
                if (logger.isLoggable(level) != loggableLevels.contains(level)) {
                    throw new RuntimeException(level +
                            ": unexpected result for isLoggable(): expected " +
                            (loggableLevels.contains(level)));
                }
            }

            // These methods should now start to log when the level we put in
            // the map is weaker or equals to Level.FINER.
            // This validates that these methods now call ThreadLogger.isLoggable()
            // since the default level for our logger is still Level.INFO.
            // If the methods didn't call ThreadLogger.isLoggable() they wouldn't
            // log anything, whatever we put in the threadMap...

            logger.entering("blah", "blah");
            logger.entering("blah", "blah", "blah");
            logger.entering("blah", "blah", new Object[] {"blah"});
            if (stronger.contains(l)) {
                if (!handler.messages.isEmpty()) {
                    throw new RuntimeException(l +
                            ": Expected empty, got " + handler.messages);
                }
            } else {
                if (handler.messages.size() != 3) {
                    throw new RuntimeException(l +
                            ": Expected size 3, got " + handler.messages);
                }
            }

            logger.exiting("blah", "blah");
            logger.exiting("blah", "blah", "blah");
            logger.exiting("blah", "blah", new Object[] {"blah"});
            if (stronger.contains(l)) {
                if (!handler.messages.isEmpty()) {
                    throw new RuntimeException(l +
                            ": Expected empty, got " + handler.messages);
                }
            } else {
                if (handler.messages.size() != 6) {
                    throw new RuntimeException(l +
                            ": Expected size 6, got " + handler.messages);
                }
            }

            logger.throwing("blah", "blah", new Exception("blah"));
            if (stronger.contains(l)) {
                if (!handler.messages.isEmpty()) {
                    throw new RuntimeException(l +
                            ": Expected empty, got " + handler.messages);
                }
            } else {
                if (handler.messages.size() != 7) {
                    throw new RuntimeException(l +
                            ": Expected size 7, got " + handler.messages);
                }
            }
            if (!stronger.contains(l)) {
                System.out.println(l + ": Logger.entering/exiting/throwing: " +
                        handler.messages);
            }
            handler.messages.clear();
        }

        // Cleanup so that we can start the next test with a clean plate...
        handler.messages.clear();
        logger.threadMap.clear();

        // Test that each logging method calls isLoggable()
        //
        for (LogTest testCase : LogTest.values()) {
            // Each test case is a combination of:
            //    1. A level to put in the threadMap.
            //    2. A log method to call
            final String method = testCase.method();

            // check our implementation of logger.isLoggable();
            // by default the logger level is Level.INFO, so our implementation
            // of isLoggable() should return true for all levels stronger or
            // equal to INFO and false for the others.
            // We check that here.
            for (Level level : LEVELS) {
                if (logger.isLoggable(level) != loggable.contains(level)) {
                    throw new RuntimeException(level +
                            ": unexpected result for isLoggable(): expected " +
                            (loggable.contains(level)));
                }
            }

            // Check that by default the log method will not log for level
            // weaker than Level.INFO.
            for (Level l : LEVELS.subList(LEVELS.indexOf(Level.INFO) + 1, LEVELS.size())) {
                final String test = method + l + ": ";
                testCase.log(l, logger, "blah");
                if (!handler.messages.isEmpty()) {
                    throw new RuntimeException(test +
                            "Expected empty, got " + handler.messages);
                }
            }

            // Let's put Level.OFF in the threadMap. Nothing should be logged,
            // whichever level is used...
            logger.threadMap.put(Thread.currentThread().getId(), Level.OFF);

            // Check that isLoggable() now always return false.
            for (Level level : LEVELS) {
                if (logger.isLoggable(level)) {
                    throw new RuntimeException(level +
                            ": unexpected result for isLoggable(): expected " +
                            false);
                }
            }

            // Check that the log method of the test case won't log, whatever
            // level we pass to it. This validates that level method calls
            // isLoggable() - because otherwise it would log for levels stronger
            // or equal to INFO.
            for (Level l : LEVELS) {
                final String test = "[threadMap=OFF] " + method + l + ": ";
                testCase.log(l, logger, "blah");
                if (!handler.messages.isEmpty()) {
                    throw new RuntimeException(test +
                            "Expected empty, got " + handler.messages);
                }
            }
            System.out.println("[threadMap=OFF] " + method + "logged " + handler.messages);

            // Now put the testcase's level in the threadMap.
            logger.threadMap.put(Thread.currentThread().getId(), testCase.threshold());

            // The levels for which logging should happen are those that are stronger
            // or equals to the testcase's  thresholds.
            final List<Level> loggableLevels =
                    LEVELS.subList(0, LEVELS.indexOf(testCase.threshold())+1);

            // Check that our implementation of isLoggable() is taking into account
            // what we put in the map.
            for (Level level : LEVELS) {
                if (logger.isLoggable(level) != loggableLevels.contains(level)) {
                    throw new RuntimeException(level +
                            ": unexpected result for isLoggable(): expected " +
                            (loggableLevels.contains(level)));
                }
            }

            // Now check that the log method is indeed calling our implementation
            // of isLoggable(). We do this by first verifying that it won't log
            // for levels weaker than what we put in the map.
            //
            for (Level l : testCase.weaker()) {
                final String test = method + l + ": ";
                testCase.log(l, logger, "blah");
                if (!handler.messages.isEmpty()) {
                    throw new RuntimeException(test +
                            "Expected empty, got " + handler.messages);
                }
            }

            // Then we check that it will log for the testcase threshold.
            final String test2 = method + testCase.threshold() + ": ";
            testCase.log(logger, testCase.threshold() + " blah");
            if (handler.messages.isEmpty()) {
                throw new RuntimeException(test2 +
                        "Expected 1 message, but list is empty");
            }
            if (!handler.messages.contains(testCase.threshold() + " blah")) {
                throw new RuntimeException(test2 + " blah not found: "
                        + handler.messages);
            }
            handler.messages.clear();

            // Now we check that it logs for all 'loggable' level (and doesn't
            // log for the others).
            for (Level l : LEVELS) {
                final String test = method + l + ": ";
                testCase.log(l, logger, l + ": blah");
                if (testCase.loggable().contains(l)) {
                    if (!handler.messages.contains(l + ": blah")) {
                        throw new RuntimeException(test + "blah not found: " +
                                handler.messages);
                    }
                } else {
                    if (handler.messages.contains(l + ": blah")) {
                        throw new RuntimeException(test + "blah found: " +
                                handler.messages);
                    }
                }
            }
            if (handler.messages.size() != testCase.loggable().size()) {
                throw new RuntimeException(method +
                        " Sizes don't match: expected " +
                        testCase.loggable().size() + " got " +
                        handler.messages);
            }

            // Some visual feedback on what happened.
            System.out.println(method + "logged " + handler.messages);

            // Cleanup for next step.
            // Since we're iterating over all possible levels we can be
            // sure that we haven't missed anything.
            // For instance - it could be argued that logger.severe() will
            // always log. But since we have 1 case where we put Level.OFF in
            // the map and we have verified that severe() didn't log in that
            // case, but that it logged in any other case, then we know
            // beyond doubt that it called our implementation of isLoggable().
            logger.threadMap.clear();
            handler.messages.clear();
        }

    }
}

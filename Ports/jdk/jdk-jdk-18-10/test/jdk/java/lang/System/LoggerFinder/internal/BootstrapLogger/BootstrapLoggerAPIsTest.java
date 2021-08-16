/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import jdk.internal.logger.BootstrapLogger;
import jdk.internal.logger.LazyLoggers;

/*
 * @test
 * @bug     8144460 8144214
 * @summary Cover the logXX and LogEvent.valueOf APIs of BootstrapLogger
 *          and logXX APIs of SimpleConsoleLogger.
 * @modules java.base/jdk.internal.logger:+open
 *          java.base/sun.util.logging
 * @build BootstrapLoggerUtils LogStream
 * @run main/othervm BootstrapLoggerAPIsTest
 */

public class BootstrapLoggerAPIsTest {

    private static final LogStream ERR = new LogStream();

    public static void main(String[] args) throws Exception {

        final ContentManager MGR = new ContentManager();

        // private reflection hook that allows us to simulate a non booted VM
        final AtomicBoolean VM_BOOTED = new AtomicBoolean(false);

        BootstrapLoggerUtils.setBootedHook(() -> VM_BOOTED.get());

        // We replace System.err to check the messages that have been logged
        // by the JUL ConsoleHandler and default SimpleConsoleLogger
        // implementaion
        System.setErr(new PrintStream(ERR));

        VM_BOOTED.getAndSet(false);
        if (BootstrapLogger.isBooted()) {
            throw new RuntimeException("VM should not be booted!");
        }

        final Logger LOGGER =
                LazyLoggers.getLogger("foo.bar", Thread.class.getModule());
        final sun.util.logging.PlatformLogger.Level PLATFORM_LEVEL =
                sun.util.logging.PlatformLogger.Level.SEVERE;
        final MyResources BUNDLE = new MyResources();

        /*
         * Test logXX APIs for interface java.lang.System.Logger. Log content
         * before VM is booted should be retained. Log content after VM was
         * booted should be flushed instantly. VM is not booted in first round
         * of loop, VM is booted in second round of loop.
         */
        for (int i = 0; i < 2; i++) {
            boolean booted = BootstrapLogger.isBooted();

            // make sure there is no [remaining] content in the LogStream.
            MGR.failLog("xyz", "throwable #", "MyClass_#", "MyMethod_#");

            /*
             * test logXX APIs for interface java.lang.System.Logger.
             */
            // void log(java.lang.System$Logger$Level,java.util.ResourceBundle,
            //          java.lang.String,java.lang.Throwable)
            LOGGER.log(Level.ERROR, BUNDLE, "abc #0", new RuntimeException("throwable #0"));
            MGR.checkLog(booted, "xyz #0", "throwable #0");

            // void log(java.lang.System$Logger$Level,java.util.ResourceBundle,
            //          java.lang.String,java.lang.Object[])
            LOGGER.log(Level.ERROR, BUNDLE, "abc #1");
            MGR.checkLog(booted, "xyz #1");

            // void log(java.lang.System$Logger$Level,java.lang.String,java.lang.Object[])
            LOGGER.log(Level.ERROR, BUNDLE, "abc {0}", "#2");
            MGR.checkLog(booted, "xyz #2");

            // void log(java.lang.System$Logger$Level,java.lang.String,java.lang.Throwable)
            LOGGER.log(Level.ERROR, "xyz #3", new RuntimeException("throwable #3"));
            MGR.checkLog(booted, "xyz #3", "throwable #3");

            // void log(java.lang.System$Logger$Level,java.util.function.Supplier)
            LOGGER.log(Level.ERROR, () -> "xyz #4");
            MGR.checkLog(booted, "xyz #4");

            // void log(java.lang.System$Logger$Level,java.lang.Object)
            LOGGER.log(Level.ERROR, new MyObject("xyz #5"));
            MGR.checkLog(booted, "xyz #5");

            // void log(java.lang.System$Logger$Level,java.util.function.Supplier,
            //          java.lang.Throwable)
            LOGGER.log(Level.ERROR, () -> "xyz #6", new RuntimeException("throwable #6"));
            MGR.checkLog(booted, "xyz #6", "throwable #6");


            /*
             * test logXX APIs for interface
             * sun.util.logging.PlatformLogger.Bridge.
             */
            sun.util.logging.PlatformLogger.Bridge bridge =
                    (sun.util.logging.PlatformLogger.Bridge) LOGGER;

            // void log(sun.util.logging.PlatformLogger$Level,java.lang.String)
            bridge.log(PLATFORM_LEVEL, "xyz #7");
            MGR.checkLog(booted, "xyz #7");

            // void log(sun.util.logging.PlatformLogger$Level,java.lang.String,java.lang.Throwable)
            bridge.log(PLATFORM_LEVEL, "xyz #8", new RuntimeException("throwable #8"));
            MGR.checkLog(booted, "xyz #8", "throwable #8");

            // void log(sun.util.logging.PlatformLogger$Level,java.lang.String,java.lang.Object[])
            bridge.log(PLATFORM_LEVEL, "xyz {0}", "#9");
            MGR.checkLog(booted, "xyz #9");

            // void log(sun.util.logging.PlatformLogger$Level,java.util.function.Supplier)
            bridge.log(PLATFORM_LEVEL, () -> "xyz #10");
            MGR.checkLog(booted, "xyz #10");

            // void log(sun.util.logging.PlatformLogger$Level,
            //        java.lang.Throwable,java.util.function.Supplier)
            bridge.log(PLATFORM_LEVEL, new RuntimeException("throwable #11"), () -> "xyz #11");
            MGR.checkLog(booted, "xyz #11", "throwable #11");

            // void logp(sun.util.logging.PlatformLogger$Level,java.lang.String,
            //          java.lang.String,java.lang.String)
            bridge.logp(PLATFORM_LEVEL, "MyClass_#12", "MyMethod_#12", "xyz #12");
            MGR.checkLog(booted, "xyz #12", "MyClass_#12", "MyMethod_#12");

            // void logp(sun.util.logging.PlatformLogger$Level,java.lang.String,
            //          java.lang.String,java.util.function.Supplier)
            bridge.logp(PLATFORM_LEVEL, "MyClass_#13", "MyMethod_#13", () -> "xyz #13");
            MGR.checkLog(booted, "xyz #13", "MyClass_#13", "MyMethod_#13");

            // void logp(sun.util.logging.PlatformLogger$Level,java.lang.String,
            //          java.lang.String,java.lang.String,java.lang.Object[])
            bridge.logp(PLATFORM_LEVEL, "MyClass_#14", "MyMethod_#14", "xyz {0}", "#14");
            MGR.checkLog(booted, "xyz #14", "MyClass_#14", "MyMethod_#14");

            // void logp(sun.util.logging.PlatformLogger$Level,java.lang.String,
            //          java.lang.String,java.lang.String,java.lang.Throwable)
            bridge.logp(PLATFORM_LEVEL, "MyClass_#15", "MyMethod_#15",
                    "xyz #15", new RuntimeException("throwable #15"));
            MGR.checkLog(booted, "xyz #15", "throwable #15", "MyClass_#15", "MyMethod_#15");

            // void logp(sun.util.logging.PlatformLogger$Level,java.lang.String,
            //          java.lang.String,java.lang.Throwable,java.util.function.Supplier)
            bridge.logp(PLATFORM_LEVEL, "MyClass_#16", "MyMethod_#16",
                    new RuntimeException("throwable #16"), () -> "xyz #16");
            MGR.checkLog(booted, "xyz #16", "throwable #16", "MyClass_#16", "MyMethod_#16");

            // void logrb(sun.util.logging.PlatformLogger$Level,java.lang.String,java.lang.String,
            //          java.util.ResourceBundle,java.lang.String,java.lang.Object[])
            bridge.logrb(PLATFORM_LEVEL, "MyClass_#17", "MyMethod_#17",
                    BUNDLE, "abc {0}", "#17");
            MGR.checkLog(booted, "xyz #17", "MyClass_#17", "MyMethod_#17");

            // void logrb(sun.util.logging.PlatformLogger$Level,java.lang.String,java.lang.String,
            //          java.util.ResourceBundle,java.lang.String,java.lang.Throwable)
            bridge.logrb(PLATFORM_LEVEL, "MyClass_#18", "MyMethod_#18",
                    BUNDLE, "abc #18", new RuntimeException("throwable #18"));
            MGR.checkLog(booted, "xyz #18", "throwable #18", "MyClass_#18", "MyMethod_#18");

            // void logrb(sun.util.logging.PlatformLogger$Level,java.util.ResourceBundle,
            //          java.lang.String,java.lang.Object[])
            bridge.logrb(PLATFORM_LEVEL, BUNDLE, "abc {0}", "#19");
            MGR.checkLog(booted, "xyz #19");

            // void logrb(sun.util.logging.PlatformLogger$Level,java.util.ResourceBundle,
            //          java.lang.String,java.lang.Throwable)
            bridge.logrb(PLATFORM_LEVEL, BUNDLE, "abc #20",
                        new RuntimeException("throwable #20"));
            MGR.checkLog(booted, "xyz #20", "throwable #20");

            /*
             * retained log content should be flushed after VM is booted.
             */
            if (!booted) {
                VM_BOOTED.getAndSet(true);
                // trigger the flush, make sure to call LOGGER.log(...)
                // after VM_BOOTED.getAndSet(true) and before MGR.assertCachedLog()
                LOGGER.log(Level.ERROR, "VM was just booted! This log should flush the cached logs.");
                MGR.assertCachedLog();
            }
        }
    }

    private static class ContentManager {
        final List<String[]> cached = new ArrayList<String[]>();
        String[] last;

        public void cache() {
            cached.add(last);
        }

        public ContentManager failLog(String... nonexistent) {
            last = nonexistent;
            for (String c : nonexistent) {
                if (ERR.drain().contains(c)) {
                    throw new RuntimeException("Content \"" + nonexistent
                            + "\" should not exist in the log!");
                }
            }
            return this;
        }

        public void assertLog(String... logs) {
            String log = ERR.drain();
            for (String str : logs) {
                if (!log.contains(str)) {
                    throw new RuntimeException("Content \"" + str + "\" does not exist in the log!");
                }
            }
        }

        public void checkLog(boolean booted, String... logs) {
            if (!booted) {
                failLog(logs).cache();
            } else {
                assertLog(logs);
            }
        }

        public void assertCachedLog() {
            String log = ERR.drain();
            for (String[] arr : cached) {
                for (String c : arr) {
                    if (!log.contains(c)) {
                        throw new RuntimeException("Content \"" + c + "\" does not exist in the log!");
                    }
                }
            }
        }
    }

    private static class MyObject {
        String str;

        public MyObject(String str) {
            this.str = str;
        }

        public String toString() {
            return str;
        }
    }

    private static class MyResources extends ResourceBundle {
        public Object handleGetObject(String key) {
            if (key.contains("abc #") || key.contains("abc {")) {
                return key.replaceAll("abc ", "xyz ");
            }
            return null;
        }

        public Enumeration<String> getKeys() {
            return null;
        }
    }
}

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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @test
 * @bug 7184195 8021003
 * @summary Test that the global logger can log with no configuration when accessed from multiple threads.
 * @build TestGetGlobalConcurrent testgetglobal.HandlerImpl testgetglobal.LogManagerImpl1 testgetglobal.LogManagerImpl2 testgetglobal.LogManagerImpl3 testgetglobal.BadLogManagerImpl testgetglobal.DummyLogManagerImpl
 * @run main/othervm TestGetGlobalConcurrent
 * @run main/othervm/policy=policy -Djava.security.manager TestGetGlobalConcurrent
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.LogManagerImpl1 TestGetGlobalConcurrent
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.LogManagerImpl1 TestGetGlobalConcurrent
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.LogManagerImpl2 TestGetGlobalConcurrent
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.LogManagerImpl2 TestGetGlobalConcurrent
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.LogManagerImpl3 TestGetGlobalConcurrent
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.LogManagerImpl3 TestGetGlobalConcurrent
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.BadLogManagerImpl TestGetGlobalConcurrent
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.BadLogManagerImpl TestGetGlobalConcurrent
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.DummyLogManagerImpl TestGetGlobalConcurrent
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.DummyLogManagerImpl TestGetGlobalConcurrent
 * @author danielfuchs
 */
public class TestGetGlobalConcurrent {

    static final String[] messages = {
        "1. This message should not appear on the console.",
        "2. This message should appear on the console.",
        "3. This message should now appear on the console too.",
        "4. This message should appear on the console.",
        "5. This message should now appear on the console too.",
        "6. This message should appear on the console.",
        "7. This message should now appear on the console too.",
        "8. This message should appear on the console.",
        "9. This message should now appear on the console too."
    };

    static {
        System.setProperty("java.util.logging.config.file",
            System.getProperty("test.src", ".") + java.io.File.separator + "logging.properties");
    }

    public static void test1() {
        final int nb = 1;
        final int i = 2*nb + 1;
        Logger.getGlobal().info(messages[i]); // calling getGlobal() will
             // initialize the LogManager - and thus this message should appear.
        Logger.global.info(messages[i+1]); // Now that the LogManager is
             // initialized, this message should appear too.
        final List<String> expected = Arrays.asList(Arrays.copyOfRange(messages, i, i+2));
        if (!testgetglobal.HandlerImpl.received.containsAll(expected)) {
            fail(new Error("Unexpected message list: "+testgetglobal.HandlerImpl.received+" vs "+ expected));
        }
    }
    public static void test2() {
        final int nb = 2;
        final int i = 2*nb + 1;
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(messages[i]); // calling getGlobal() will
             // initialize the LogManager - and thus this message should appear.
        Logger.global.info(messages[i+1]); // Now that the LogManager is
             // initialized, this message should appear too.
        final List<String> expected = Arrays.asList(Arrays.copyOfRange(messages, i, i+2));
        if (!testgetglobal.HandlerImpl.received.containsAll(expected)) {
            fail(new Error("Unexpected message list: "+testgetglobal.HandlerImpl.received+" vs "+ expected));
        }
    }
    public static void test3() {
        final int nb = 3;
        final int i = 2*nb + 1;
        java.util.logging.LogManager.getLogManager();
        Logger.getGlobal().info(messages[i]); // calling getGlobal() will
             // initialize the LogManager - and thus this message should appear.
        Logger.global.info(messages[i+1]); // Now that the LogManager is
             // initialized, this message should appear too.
        final List<String> expected = Arrays.asList(Arrays.copyOfRange(messages, i, i+2));
        if (!testgetglobal.HandlerImpl.received.containsAll(expected)) {
            fail(new Error("Unexpected message list: "+testgetglobal.HandlerImpl.received+" vs "+ expected));
        }
    }
    public static void test4() {
        log = new MyLogger("foo.bar");
        java.util.logging.LogManager.getLogManager().addLogger(log);
    }


    private static volatile Throwable failed = null;
    private static volatile Logger log = null;

    public static class MyLogger extends Logger {
        public MyLogger(String name) {
            super(name, null);
        }
    }

    public static void fail(Throwable failure) {
        failure.printStackTrace();
        if (failed == null) failed = failure;
    }

    public static class WaitAndRun implements Runnable {
          private final Runnable run;
          public WaitAndRun(Runnable run) {
              this.run = run;
          }
          public void run() {
              try {
                 Thread.sleep(10);
                 run.run();
              } catch (Exception | Error x) {
                 fail(x);
              }
          }
    }

    static final class Run1 implements Runnable {
        public void run() { test1(); }
    }
    static final class Run2 implements Runnable {
        public void run() { test2(); }
    }
    static final class Run3 implements Runnable {
        public void run() { test3(); }
    }
    static final class Run4 implements Runnable {
        public void run() { test4(); }
    }

    static String description = "Unknown";

    public static void main(String... args) throws Exception {

        final String manager = System.getProperty("java.util.logging.manager", null);

        description = "TestGetGlobalConcurrent"
            + (System.getSecurityManager() == null ? " " :
               " -Djava.security.manager ")
            + (manager == null ? "" : "-Djava.util.logging.manager=" + manager);

        final Thread t1 = new Thread(new WaitAndRun(new Run1()), "test1");
        final Thread t2 = new Thread(new WaitAndRun(new Run2()), "test2");
        final Thread t3 = new Thread(new WaitAndRun(new Run3()), "test3");
        final Thread t4 = new Thread(new WaitAndRun(new Run4()), "test4");

        t1.setDaemon(true); t2.setDaemon(true); t3.setDaemon(true); t4.setDaemon(true);
        t1.start(); t2.start(); t3.start(); t4.start();

        Thread.sleep(10);

        Logger.getGlobal().info(messages[1]); // calling getGlobal() will
             // initialize the LogManager - and thus this message should appear.
        Logger.global.info(messages[2]); // Now that the LogManager is
             // initialized, this message should appear too.

        final List<String> expected = Arrays.asList(Arrays.copyOfRange(messages, 1, 3));
        if (!testgetglobal.HandlerImpl.received.containsAll(expected)) {
            fail(new Error("Unexpected message list: "+testgetglobal.HandlerImpl.received+" vs "+ expected));
        }

        t1.join(); t2.join(); t3.join(); t4.join();

        if (failed != null) {
             throw new Error("Test failed: "+description, failed);
        }

        System.out.println("Test passed");
    }
}

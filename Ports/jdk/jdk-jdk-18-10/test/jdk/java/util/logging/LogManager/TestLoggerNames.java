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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * @test
 * @bug 7113878
 * @summary This is not a test that will check that 7113878 is fixed, but
 *          rather a test that will invoke the modified code & try to verify
 *          that fixing 7113878 has not introduced some big regression.
 *          This test should pass, whether 7113878 is there or not.
 * @run main/othervm TestLoggerNames
 * @author danielfuchs
 */
public class TestLoggerNames {

    static final class TestLogger extends java.util.logging.Logger {

        final Semaphore sem = new Semaphore(0);
        final Semaphore wait = new Semaphore(0);

        public TestLogger(String name, String resourceBundleName) {
            super(name, resourceBundleName);
        }

        @Override
        public Handler[] getHandlers() {
           boolean found = false;
           try {
                System.out.println("Entering "+getName()+" getHandlers()");
                for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                    if (LogManager.class.getName().equals(ste.getClassName())
                            && "reset".equals(ste.getMethodName())) {
                        found = true;
                        System.out.println(getName()+" getHandlers() called by " + ste);
                    }
                }
                sem.release();
                try {
                    System.out.println("TestLogger: Acquiring wait for "+getName());
                    wait.acquire();
                    try {
                        System.out.println("TestLogger: Acquired wait for "+getName());
                        return super.getHandlers();
                    } finally {
                        System.out.println("TestLogger: Releasing wait for "+getName());
                        wait.release();
                    }
                } finally {
                    System.out.println("Unblocking "+getName());
                    sem.acquire();
                    System.out.println("Unblocked "+getName());
                    if (found) {
                        System.out.println("Reset will proceed...");
                    }
                }
            } catch (InterruptedException x) {
                throw new IllegalStateException(x);
            }
        }
    }

    static volatile boolean stop;
    static volatile Throwable resetFailed;
    static volatile Throwable checkLoggerNamesFailed;
    static volatile Phaser phaser = new Phaser(2);


    static void checkLoggerNames(List<Logger> loggers) {
        Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
        if (names instanceof Iterator) {
            for (Iterator<?> it = Iterator.class.cast(names); it.hasNext(); ) {
                try {
                    it.remove();
                    throw new RuntimeException("Iterator supports remove!");
                } catch (UnsupportedOperationException x) {
                    System.out.println("OK: Iterator doesn't support remove.");
                }
            }
            // We're not supposed to come here, but if we do then we
            // need to rewind names...
            names = LogManager.getLogManager().getLoggerNames();
        }

        List<String> loggerNames = Collections.list(names);
        if (!loggerNames.contains("")) {
            throw new RuntimeException("\"\"" +
                    " not found in " + loggerNames);
        }
        if (!loggerNames.contains("global")) {
            throw new RuntimeException("global" +
                    " not found in " + loggerNames);
        }
        for (Logger l : loggers) {
            if (!loggerNames.contains(l.getName())) {
                throw new RuntimeException(l.getName() +
                        " not found in " + loggerNames);
            }
        }
        System.out.println("Got all expected logger names");
    }


    public static void main(String[] args) throws InterruptedException {
        TestLogger test = new TestLogger("com.foo.bar.zzz", null);
        LogManager.getLogManager().addLogger(test);
        try {
            Logger.getLogger(null);
            throw new RuntimeException("Logger.getLogger(null) didn't throw expected NPE");
        } catch (NullPointerException x) {
            System.out.println("Got expected NullPointerException for Logger.getLogger(null)");
        }
        List<Logger> loggers = new CopyOnWriteArrayList<>();
        loggers.add(Logger.getLogger("one.two.addMeAChild"));
        loggers.add(Logger.getLogger("aaa.bbb.replaceMe"));
        loggers.add(Logger.getLogger("bbb.aaa.addMeAChild"));
        if (test != Logger.getLogger("com.foo.bar.zzz")) {
            throw new AssertionError("wrong logger returned");
        }
        loggers.add(Logger.getLogger("ddd.aaa.addMeAChild"));

        checkLoggerNames(loggers);

        Thread loggerNamesThread = new Thread(() -> {
            try {
                while (!stop) {
                    // Make a defensive copy of the list of loggers that we pass
                    // to checkLoggerNames - in order to make sure that
                    // we won't see new loggers that might appear after
                    // checkLoggerNames has called LogManager.getLoggerNames().
                    // ('loggers' is a live list and the main thread adds and
                    //  and removes loggers from it concurrently to this thread).
                    checkLoggerNames(new ArrayList<>(loggers));
                    Thread.sleep(10);
                    if (!stop) {
                        phaser.arriveAndAwaitAdvance();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace(System.err);
                checkLoggerNamesFailed = t;
                // makes sure the main thread isn't going to wait
                // forever waiting for this dead thread to arrive at the
                // phaser.
                phaser.arrive();
            }
        }, "loggerNames");

        Thread resetThread = new Thread(() -> {
            try {
                System.out.println("Calling reset...");
                LogManager.getLogManager().reset();
                System.out.println("Reset done...");
                System.out.println("Reset again...");
                LogManager.getLogManager().reset();
                System.out.println("Reset done...");
            } catch(Throwable t) {
                resetFailed = t;
                System.err.println("Unexpected exception or error in reset Thread");
                t.printStackTrace(System.err);
            }
        }, "reset");

        resetThread.setDaemon(true);
        resetThread.start();

        System.out.println("Waiting for reset to get handlers");
        test.sem.acquire();
        try {
            loggerNamesThread.start();
            System.out.println("Reset has called getHandlers on " + test.getName());
            int i = 0;
            for (Enumeration<String> e = LogManager.getLogManager().getLoggerNames();
                e.hasMoreElements();) {
                String name = e.nextElement();
                if (name.isEmpty()) continue;
                if (name.endsWith(".addMeAChild")) {
                    Logger l =  Logger.getLogger(name+".child");
                    loggers.add(l);
                    System.out.println("*** Added " + l.getName());
                    i++;
                } else if (name.endsWith("replaceMe")) {
                    Logger l = Logger.getLogger(name);
                    loggers.remove(l);
                    l = Logger.getLogger(name.replace("replaceMe", "meReplaced"));
                    loggers.add(l);
                    // Give some chance to the loggerNames thread to arrive at the
                    // phaser first. We don't really care if the logger is actually
                    // replaced this turn - or the next.
                    Thread.sleep(100);
                    System.gc();
                    if (LogManager.getLogManager().getLogger(name) == null) {
                        // The original logger may not have been gc'ed yet, since
                        // it could still be referenced by the list being processed
                        // by the loggerNamesThread, if that thread hasn't arrived
                        // to the phaser yet...
                        System.out.println("*** "+ name
                                + " successfully replaced with " + l.getName());
                    }
                    i++;
                } else {
                    System.out.println("Nothing to do for logger: " + name);
                }
                if (checkLoggerNamesFailed != null) {
                    // The checkLoggerNames thread failed. No need to
                    // continue.
                    break;
                }
                // Waits for the checkLoggerNamesThread to arrive
                phaser.arriveAndAwaitAdvance();
                if (i >= 3 && i++ == 3) {
                    System.out.println("Loggers are now: " +
                            Collections.list(LogManager.getLogManager().getLoggerNames()));
                    test.wait.release();
                    test.sem.release();
                    System.out.println("Joining " + resetThread);
                    resetThread.join();
                }
            }
        } catch (RuntimeException | InterruptedException | Error x) {
            test.wait.release();
            test.sem.release();
            throw x;
        } finally {
            stop = true;
            phaser.arriveAndDeregister();
            loggerNamesThread.join();
            loggers.clear();
        }


        if (resetFailed != null || checkLoggerNamesFailed != null) {
            RuntimeException r = new RuntimeException("Some of the concurrent threads failed");
            if (resetFailed != null) r.addSuppressed(resetFailed);
            if (checkLoggerNamesFailed != null) r.addSuppressed(checkLoggerNamesFailed);
            throw r;
        }

    }

}

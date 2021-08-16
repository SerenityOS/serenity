/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * @test
 * @bug 8029281 8027670
 * @summary Synchronization issues in Logger and LogManager. This test
 *       focusses more particularly on potential deadlock in
 *       drainLoggerRefQueueBounded / readConfiguration
 * @modules java.logging
 *          java.management
 * @run main/othervm -Djava.security.manager=allow TestLogConfigurationDeadLock
 * @author danielfuchs
 */
// This test is a best effort to try & detect issues. The test itself will run
// for 8secs. This is usually unsufficient to detect issues.
// To get a greater confidence it is recommended to run this test in a loop:
// e.g. use something like:
// $ while jtreg -jdk:$JDK -verbose:all  \
//      test/java/util/logging/TestLogConfigurationDeadLock.java ; \
//      do echo Running test again ; done
// and let it run for a few hours...
//
public class TestLogConfigurationDeadLock {

    static volatile Exception thrown = null;
    static volatile boolean goOn = true;

    static final int READERS = 2;
    static final int LOGGERS = 2;
    static final long TIME = 4 * 1000; // 4 sec.
    static final long STEP = 1 * 1000;  // message every 1 sec.
    static final int  LCOUNT = 50; // 50 loggers created in a row...
    static final AtomicLong nextLogger = new AtomicLong(0);
    static final AtomicLong readCount = new AtomicLong(0);
    static final AtomicLong checkCount = new AtomicLong(0);

    /**
     * This test will run both with and without a security manager.
     *
     * The test starts a number of threads that will call
     *     LogManager.readConfiguration() concurrently (ReadConf), then starts
     *     a number of threads that will create new loggers concurrently
     *     (AddLogger), and then two additional threads: one (Stopper) that
     *     will stop the test after 4secs (TIME ms), and one DeadlockDetector
     *     that will attempt to detect deadlocks.
     * If after 4secs no deadlock was detected and no exception was thrown
     * then the test is considered a success and passes.
     *
     * This procedure is done twice: once without a security manager and once
     * again with a security manager - which means the test takes ~8secs to
     * run.
     *
     * Note that 8sec may not be enough to detect issues if there are some.
     * This is a best effort test.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        // test without security
        System.out.println("No security");
        test();

        // test with security
        System.out.println("\nWith security");
        Policy.setPolicy(new Policy() {
            @Override
            public boolean implies(ProtectionDomain domain, Permission permission) {
                if (super.implies(domain, permission)) return true;
                // System.out.println("Granting " + permission);
                return true; // all permissions
            }
        });
        System.setSecurityManager(new SecurityManager());
        test();
    }

    /**
     * Starts all threads, wait 4secs, then stops all threads.
     * @throws Exception if a deadlock was detected or an error occurred.
     */
    public static void test() throws Exception {
          goOn = true;
          thrown = null;
          long sNextLogger = nextLogger.get();
          long sReadCount  = readCount.get();
          long sCheckCount = checkCount.get();
          List<Thread> threads = new ArrayList<>();
          for (int i = 0; i<READERS; i++) {
              threads.add(new ReadConf());
          }
          for (int i = 0; i<LOGGERS; i++) {
              threads.add(new AddLogger());
          }
          threads.add(new DeadlockDetector());
          threads.add(0, new Stopper(TIME));
          for (Thread t : threads) {
              t.start();
          }
          for (Thread t : threads) {
              try {
                  t.join();
              } catch (Exception x) {
                  fail(x);
              }
          }
          if (thrown != null) {
              throw thrown;
          }
          System.out.println("Passed: " + (nextLogger.get() - sNextLogger)
                  + " loggers created by " + LOGGERS + " Thread(s),");
          System.out.println("\t LogManager.readConfiguration() called "
                  + (readCount.get() - sReadCount) + " times by " + READERS
                  + " Thread(s).");
          System.out.println("\t ThreadMXBean.findDeadlockedThreads called "
                  + (checkCount.get() -sCheckCount) + " times by 1 Thread.");

    }


    static final class ReadConf extends Thread {
        @Override
        public void run() {
            while (goOn) {
                try {
                    LogManager.getLogManager().readConfiguration();
                    readCount.incrementAndGet();
                    Thread.sleep(1);
                } catch (Exception x) {
                    fail(x);
                }
            }
        }
    }

    static final class AddLogger extends Thread {
        @Override
        public void run() {
            try {
                while (goOn) {
                    Logger l;
                    Logger foo = Logger.getLogger("foo");
                    Logger bar = Logger.getLogger("foo.bar");
                    for (int i=0; i < LCOUNT ; i++) {
                        l = Logger.getLogger("foo.bar.l"+nextLogger.incrementAndGet());
                        l.fine("I'm fine");
                        if (!goOn) break;
                        Thread.sleep(1);
                    }
                }
            } catch (InterruptedException | RuntimeException x ) {
                fail(x);
            }
        }
    }

    static final class DeadlockDetector extends Thread {

        @Override
        public void run() {
            while(goOn) {
                try {
                    long[] ids = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
                    checkCount.incrementAndGet();
                    ids = ids == null ? new long[0] : ids;
                    if (ids.length == 1) {
                        throw new RuntimeException("Found 1 deadlocked thread: "+ids[0]);
                    } else if (ids.length > 0) {
                        ThreadInfo[] infos = ManagementFactory.getThreadMXBean()
                            .getThreadInfo(ids, Integer.MAX_VALUE);
                        System.err.println("Found "+ids.length+" deadlocked threads: ");
                        for (ThreadInfo inf : infos) {
                            System.err.println(inf.toString());
                        }
                        throw new RuntimeException("Found "+ids.length+" deadlocked threads");
                    }
                    Thread.sleep(100);
                } catch(InterruptedException | RuntimeException x) {
                    fail(x);
                }
            }
        }

    }

    static final class Stopper extends Thread {
        long start;
        long time;

        Stopper(long time) {
            start = System.currentTimeMillis();
            this.time = time;
        }

        @Override
        public void run() {
            try {
                long rest, previous;
                previous = time;
                while (goOn && (rest = start - System.currentTimeMillis() + time) > 0) {
                    if (previous == time || previous - rest >= STEP) {
                        Logger.getLogger("remaining").info(String.valueOf(rest)+"ms remaining...");
                        previous = rest == time ? rest -1 : rest;
                        System.gc();
                    }
                    if (goOn == false) break;
                    Thread.sleep(Math.min(rest, 100));
                }
                System.out.println(System.currentTimeMillis() - start
                        + " ms elapsed ("+time+ " requested)");
                goOn = false;
            } catch(InterruptedException | RuntimeException x) {
                fail(x);
            }
        }

    }

    static void fail(Exception x) {
        x.printStackTrace();
        if (thrown == null) {
            thrown = x;
        }
        goOn = false;
    }
}

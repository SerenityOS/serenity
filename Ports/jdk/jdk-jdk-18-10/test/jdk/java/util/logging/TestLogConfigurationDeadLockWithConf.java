/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.File;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * @test
 * @bug 8027670 8029281
 * @summary Deadlock in drainLoggerRefQueueBounded / readConfiguration
 *          caused by synchronization issues in Logger and LogManager.
 * @modules java.logging
 *          java.management
 * @run main/othervm -Djava.security.manager=allow TestLogConfigurationDeadLockWithConf
 * @author danielfuchs
 * @key randomness
 */
// This test is a best effort to try & detect issues. The test itself will run
// for 8secs. This is usually sufficient to detect issues.
// However to get a greater confidence it is recommended to run this test in a loop:
// e.g. use something like:
// $ while jtreg -jdk:$JDK -verbose:all  \
//      test/java/util/logging/TestLogConfigurationDeadLockWithConf.java ; \
//      do echo Running test again ; done
// and let it run for a few hours...
//
public class TestLogConfigurationDeadLockWithConf {

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
     * @throws java.lang.Exception if the test fails.
     */
    public static void main(String[] args) throws Exception {
        File config =  new File(System.getProperty("test.src", "."),
                        "deadlockconf.properties");
        if (!config.canRead()) {
            System.err.println("Can't read config file: test cannot execute.");
            System.err.println("Please check your test environment: ");
            System.err.println("\t -Dtest.src=" + System.getProperty("test.src", "."));
            System.err.println("\t config file is: " + config.getAbsolutePath());
            throw new RuntimeException("Can't read config file: "
                + config.getAbsolutePath());
        }

        System.setProperty("java.util.logging.config.file",
               config.getAbsolutePath());

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

    static Random rand = new Random(System.currentTimeMillis());
    private static int getBarCount() {
        return rand.nextInt(10);
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
          DeadlockDetector detector = new DeadlockDetector();
          threads.add(detector);
          threads.add(0, new Stopper(TIME));
          for (Thread t : threads) {
              t.start();
          }

          // wait for the detector to finish.
          detector.join();

          final PrintStream out = thrown == null ? System.out : System.err;

          // Try to wait for all threads to finish.
          // This is a best effort: if some threads are in deadlock we can't
          //    obviously wait for them, and other threads may have joined in
          //    the deadlock since we last checked.
          //    However, all threads which are succeptible of deadlocking
          //    extend DeamonThread.
          for (Thread t : threads) {
              if (t == detector) {
                  continue;
              }
              if (detector.deadlocked.contains(t.getId())) {
                  out.println("Skipping deadlocked thread "
                          + t.getClass().getSimpleName() + ": " + t);
                  continue; // don't wait for deadlocked thread: they won't terminate
              }
              try {
                  if (detector.deadlocked.isEmpty()) {
                      t.join();
                  } else {
                      if (t instanceof DaemonThread) {
                          // Some other threads may have join the deadlock.
                          // don't wait forever.
                          t.join(100);
                      } else {
                          // Those threads that don't extend DaemonThread
                          // should be safe from deadlock.
                          out.println("Waiting for "
                                  + t.getClass().getSimpleName() + ": " + t);
                          t.join();
                      }
                  }
              } catch (Exception x) {
                  fail(x);
              }
          }
          out.println("All threads joined.");

          final String status = thrown == null ? "Passed" : "FAILED";

          out.println(status + ": " + (nextLogger.get() - sNextLogger)
                  + " loggers created by " + LOGGERS + " Thread(s),");
          out.println("\t LogManager.readConfiguration() called "
                  + (readCount.get() - sReadCount) + " times by " + READERS
                  + " Thread(s).");
          out.println("\t ThreadMXBean.findDeadlockedThreads called "
                  + (checkCount.get() -sCheckCount) + " times by 1 Thread.");

          if (thrown != null) {
              out.println("\t Error is: "+thrown.getMessage());
              throw thrown;
          }
    }

    static class DaemonThread extends Thread {
        public DaemonThread() {
            this.setDaemon(true);
        }
    }

    static final class ReadConf extends DaemonThread {
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

    static final class AddLogger extends DaemonThread {
        @Override
        public void run() {
            try {
                while (goOn) {
                    Logger l;
                    int barcount = getBarCount();
                    for (int i=0; i < LCOUNT ; i++) {
                        l = Logger.getLogger("foo.bar"+barcount+".l"+nextLogger.incrementAndGet());
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

        final Set<Long> deadlocked = Collections.synchronizedSet(new HashSet<Long>());

        static List<Long> asList(long... ids) {
            final List<Long> list = new ArrayList<>(ids.length);
            for (long id : ids) {
                list.add(id);
            }
            return list;
        }

        @Override
        public void run() {
            while(goOn) {
                try {
                    long[] ids = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
                    checkCount.incrementAndGet();
                    ids = ids == null ? new long[0] : ids;
                    if (ids.length > 0) {
                        deadlocked.addAll(asList(ids));
                    }
                    if (ids.length == 1) {
                        throw new RuntimeException("Found 1 deadlocked thread: "+ids[0]);
                    } else if (ids.length > 0) {
                        ThreadInfo[] infos = ManagementFactory.getThreadMXBean().getThreadInfo(ids, Integer.MAX_VALUE);
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

        static final Logger logger = Logger.getLogger("remaining");

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
                        logger.log(Level.INFO,
                                "{0}ms remaining...", String.valueOf(rest));
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

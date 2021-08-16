/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6467152 6716076 6829503 8132550
 * @summary deadlock occurs in LogManager initialization and JVM termination
 * @author  Serguei Spitsyn / Hitachi / Martin Buchholz
 *
 * @build    LoggingDeadlock2
 * @run  main LoggingDeadlock2
 * @key randomness
 */

/*
 *
 * There is a clear deadlock between LogManager.<clinit> and
 * Cleaner.run() methods.
 * T1 thread:
 *   The LogManager.<clinit> creates LogManager.manager object,
 *   sets shutdown hook with the Cleaner class and then waits
 *   to lock the LogManager.manager monitor.
 * T2 thread:
 *   It is started by the System.exit() as shutdown hook thread.
 *   It locks the LogManager.manager monitor and then calls the
 *   static methods of the LogManager class (in this particular
 *   case it is a trick of the inner classes implementation).
 *   It is waits when the LogManager.<clinit> is completed.
 *
 * This is a regression test for this bug.
 */

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.TimeUnit;

public class LoggingDeadlock2 {

    // ask child process to dumpstack after 60secs
    public static final long DUMP_STACK_FREQUENCY_MS = 60000;

    // A marker that allows to validate the subprocess output.
    public static final String MARKER = "$";

    public static void realMain(String arg[]) throws Throwable {
        try {
            System.out.println(javaChildArgs);
            ProcessBuilder pb = new ProcessBuilder(javaChildArgs);
            ProcessResults r = run(pb.start());
            equal(r.exitValue(), 99);

            // output of subprocess should end with "$"
            final String out = r.out();
            final String trailingOutput = out.indexOf(MARKER) > -1
                    ? out.substring(out.indexOf(MARKER)+MARKER.length())
                    : out;
            equal(trailingOutput, "");
            equal(r.err(), "");
            equal(out.startsWith("JavaChild started"), true);
            equal(out.endsWith("$"), true);
        } catch (Throwable t) { unexpected(t); }
    }

    public static class JavaChild {
        public static void main(String args[]) throws Throwable {
            System.out.println("JavaChild started");

            final CyclicBarrier startingGate = new CyclicBarrier(2);
            final Throwable[] thrown = new Throwable[1];

            // Some random variation, to help tickle races.
            final Random rnd = new Random();
            final long seed = rnd.nextLong();
            rnd.setSeed(seed);
            System.out.println("seed=" + seed);
            final boolean dojoin = rnd.nextBoolean();
            final int JITTER = 1024;
            final int iters1 = rnd.nextInt(JITTER);
            final int iters2 = JITTER - iters1;
            final AtomicInteger counter = new AtomicInteger(0);
            System.out.println("dojoin=" + dojoin);
            System.out.println("iters1=" + iters1);
            System.out.println("iters2=" + iters2);

            Thread exiter = new Thread() {
                public void run() {
                    try {
                        startingGate.await();
                        for (int i = 0; i < iters1; i++)
                            counter.getAndIncrement();
                        System.exit(99);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        System.exit(86);
                    }
                }};
            exiter.start();

            System.out.println("exiter started");

            // signal end of verbose output
            System.out.print(MARKER);
            System.out.flush();

            startingGate.await();
            for (int i = 0; i < iters2; i++)
                counter.getAndIncrement();
            // This may or may not result in a first call to
            // Runtime.addShutdownHook after shutdown has already
            // commenced.
            LogManager.getLogManager();

            if (dojoin) {
                exiter.join();
                if (thrown[0] != null)
                    throw new Error(thrown[0]);
                check(counter.get() == JITTER);
            }
        }
    }

    //----------------------------------------------------------------
    // The rest of this test is copied from ProcessBuilder/Basic.java
    //----------------------------------------------------------------
    private static final String javaExe =
        System.getProperty("java.home") +
        File.separator + "bin" + File.separator + "java";
    private static final String jstackExe =
        System.getProperty("java.home") +
        File.separator + "bin" + File.separator + "jstack";

    private static final String classpath =
        System.getProperty("java.class.path");

    private static final List<String> javaChildArgs =
        Arrays.asList(new String[]
            { javaExe, "-classpath", classpath,
              "LoggingDeadlock2$JavaChild"});

    private static class ProcessResults {
        private final String out;
        private final String err;
        private final int exitValue;
        private final Throwable throwable;

        public ProcessResults(String out,
                              String err,
                              int exitValue,
                              Throwable throwable) {
            this.out = out;
            this.err = err;
            this.exitValue = exitValue;
            this.throwable = throwable;
        }

        public String out()          { return out; }
        public String err()          { return err; }
        public int exitValue()       { return exitValue; }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<STDOUT>\n" + out() + "</STDOUT>\n")
                .append("<STDERR>\n" + err() + "</STDERR>\n")
                .append("exitValue = " + exitValue + "\n");
            if (throwable != null)
                sb.append(throwable.getStackTrace());
            return sb.toString();
        }
    }

    private static class StreamAccumulator extends Thread {
        private final InputStream is;
        private final StringBuilder sb = new StringBuilder();
        private Throwable throwable = null;

        public String result () throws Throwable {
            if (throwable != null)
                throw throwable;
            return sb.toString();
        }

        StreamAccumulator (InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                Reader r = new InputStreamReader(is);
                int n;
                while ((n = r.read()) > 0) {
                    sb.append((char)n);

                    // prints everything immediately to System.out so that we can
                    // see the traces even in the event of a test timeout
                    System.out.write((char)n);
                    System.out.flush();

                }
            } catch (Throwable t) {
                throwable = t;
            } finally {
                try { is.close(); }
                catch (Throwable t) { throwable = t; }
            }
        }
    }

    /**
     * If the child process deadlocks, then the parent may fail in timeout.
     * In that case, we won't have any interesting traces, unless we manage
     * to get a thread dump from the child.
     * It is unsure whether obtaining a thread dump from a deadlocked child
     * will work - but maybe we could see something if the timeout is a false
     * positive (the child has not deadlocked but hasn't managed to fully start
     * yet, for instance).
     * The idea here is to periodically try to obtain a thread dump from the
     * child, every 60sec - which should be always less than the jtreg timeout.
     */
    private static class TimeoutThread extends Thread {
        final long ms;
        final Process process;
        TimeoutThread(long ms, Process p) {
            super("TimeoutThread");
            setDaemon(true);
            this.ms = ms;
            this.process = p;
        }

        @Override
        public void run() {
            long start = System.nanoTime();
            try {
                while (true) {
                    sleep(ms);
                    System.err.println("Timeout reached: " + ms);
                    if (process.isAlive()) {
                        long pid = process.pid();
                        ProcessBuilder jstack = new ProcessBuilder(jstackExe, String.valueOf(pid));
                        System.err.println("Dumping subprocess stack: " + pid);
                        Process p = jstack.inheritIO().start();
                        p.waitFor(ms, TimeUnit.MILLISECONDS);
                    } else {
                        System.err.println("Process is not alive!");
                        break;
                    }
                }
            } catch (InterruptedException ex) {
                System.err.println("Interrupted: " + ex);
            } catch (IOException io) {
                System.err.println("Failed to get stack from subprocess");
                io.printStackTrace();
            }
        }


    }

    private static ProcessResults run(Process p) {
        Throwable throwable = null;
        int exitValue = -1;
        String out = "";
        String err = "";

        StreamAccumulator outAccumulator =
            new StreamAccumulator(p.getInputStream());
        StreamAccumulator errAccumulator =
            new StreamAccumulator(p.getErrorStream());

        try {
            System.out.println("Waiting for child process to exit");
            outAccumulator.start();
            errAccumulator.start();

            // ask subprocess to dump stack every 60 secs.
            new TimeoutThread(DUMP_STACK_FREQUENCY_MS, p).start();

            exitValue = p.waitFor();
            System.out.println("\nChild exited with status: " + exitValue);

            outAccumulator.join();
            errAccumulator.join();

            out = outAccumulator.result();
            err = errAccumulator.result();
        } catch (Throwable t) {
            throwable = t;
        }

        return new ProcessResults(out, err, exitValue, throwable);
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void check(boolean cond, String m) {if (cond) pass(); else fail(m);}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}

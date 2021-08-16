/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6460501 6236036 6500694 6490770
 * @summary Repeated failed timed waits shouldn't leak memory
 * @library /test/lib
 * @author Martin Buchholz
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.test.lib.Utils;

public class TimedAcquireLeak {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    static String javahome() {
        String jh = System.getProperty("java.home");
        return (jh.endsWith("jre")) ? jh.substring(0, jh.length() - 4) : jh;
    }

    static final File bin = new File(javahome(), "bin");

    static String javaProgramPath(String programName) {
        return new File(bin, programName).getPath();
    }

    static final String javaPath = javaProgramPath("java");
    static final String jmapPath = javaProgramPath("jmap");
    static final String jpsPath  = javaProgramPath("jps");

    static String outputOf(Reader r) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[1024];
        int n;
        while ((n = r.read(buf)) > 0)
            sb.append(buf, 0, n);
        return sb.toString();
    }

    static String outputOf(InputStream is) throws IOException {
        return outputOf(new InputStreamReader(is, "UTF-8"));
    }

    static final ExecutorService drainers = Executors.newFixedThreadPool(12);
    static Future<String> futureOutputOf(final InputStream is) {
        return drainers.submit(
            new Callable<String>() { public String call() throws IOException {
                    return outputOf(is); }});}

    static String outputOf(final Process p) {
        try {
            Future<String> outputFuture = futureOutputOf(p.getInputStream());
            Future<String> errorFuture = futureOutputOf(p.getErrorStream());
            final String output = outputFuture.get();
            final String error = errorFuture.get();
            // Check for successful process completion
            equal(error, "");
            equal(p.waitFor(), 0);
            equal(p.exitValue(), 0);
            return output;
        } catch (Throwable t) { unexpected(t); throw new Error(t); }
    }

    static String commandOutputOf(String... cmd) {
        try { return outputOf(new ProcessBuilder(cmd).start()); }
        catch (Throwable t) { unexpected(t); throw new Error(t); }
    }

    // To be called exactly twice by the parent process
    static <T> T rendezvousParent(Process p,
                                  Callable<T> callable) throws Throwable {
        p.getInputStream().read();
        T result = callable.call();
        sendByte(p.getOutputStream());
        return result;
    }

    /** No guarantees, but effective in practice. */
    private static void forceFullGc() {
        long timeoutMillis = 1000L;
        CountDownLatch finalized = new CountDownLatch(1);
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        WeakReference<Object> ref = new WeakReference<>(
            new Object() { protected void finalize() { finalized.countDown(); }},
            queue);
        try {
            for (int tries = 3; tries--> 0; ) {
                System.gc();
                if (finalized.await(timeoutMillis, MILLISECONDS)
                    && queue.remove(timeoutMillis) != null
                    && ref.get() == null) {
                    System.runFinalization(); // try to pick up stragglers
                    return;
                }
                timeoutMillis *= 4;
            }
        } catch (InterruptedException unexpected) {
            throw new AssertionError("unexpected InterruptedException");
        }
        throw new AssertionError("failed to do a \"full\" gc");
    }

    // To be called exactly twice by the child process
    public static void rendezvousChild() {
        try {
            forceFullGc();
            sendByte(System.out);
            System.in.read();
        } catch (Throwable t) { throw new Error(t); }
    }

    static String match(String s, String regex, int group) {
        Matcher matcher = Pattern.compile(regex).matcher(s);
        if (! matcher.find()) {
            String msg = String.format(
                "match failed: s=%s regex=%s", s, regex);
            throw new AssertionError(msg);
        }
        return matcher.group(group);
    }

    /** It's all about sending a message! */
    static void sendByte(OutputStream s) throws IOException {
        s.write('!');
        s.flush();
    }

    static int objectsInUse(final Process child,
                            final String childPid,
                            final String classNameRegex) {
        String regex =
            "(?m)^ *[0-9]+: +([0-9]+) +[0-9]+ +"+classNameRegex+"(?:$| )";
        Callable<Integer> objectsInUse = () -> {
            int i = Integer.parseInt(
                match(commandOutputOf(jmapPath, "-histo:live", childPid),
                      regex, 1));
            if (i > 100)
                System.out.print(
                    commandOutputOf(jmapPath,
                                    "-dump:file=dump,format=b",
                                    childPid));
            return i;
        };
        try { return rendezvousParent(child, objectsInUse); }
        catch (Throwable t) { unexpected(t); return -1; }
    }

    static void realMain(String[] args) throws Throwable {
        // jmap doesn't work on Windows
        if (System.getProperty("os.name").startsWith("Windows"))
            return;

        final String childClassName = Job.class.getName();
        final String classNameRegex = Job.classNameRegexToCheckForLeaks();
        final String uniqueID = String.valueOf(
            ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));

        final ArrayList<String> jobCmd = new ArrayList<>();
        Collections.addAll(
            jobCmd, javaPath, "-Xmx8m", "-XX:+UsePerfData",
            "-classpath", System.getProperty("test.class.path"));
        Collections.addAll(jobCmd, Utils.getTestJavaOpts());
        Collections.addAll(jobCmd, childClassName, uniqueID);
        final Process p = new ProcessBuilder(jobCmd).start();
        // Ensure subprocess jvm has started, so that jps can find it
        p.getInputStream().read();
        sendByte(p.getOutputStream());

        final String childPid =
            match(commandOutputOf(jpsPath, "-m"),
                  "(?m)^ *([0-9]+) +\\Q"+childClassName+"\\E *"+uniqueID+"$", 1);

        final int n0 = objectsInUse(p, childPid, classNameRegex);
        final int n1 = objectsInUse(p, childPid, classNameRegex);
        equal(p.waitFor(), 0);
        equal(p.exitValue(), 0);
        failed += p.exitValue();

        // Check that no objects were leaked.
        //
        // TODO: This test is very brittle, depending on current JDK
        // implementation, and needing occasional adjustment.
        System.out.printf("%d -> %d%n", n0, n1);
        // Almost always n0 == n1
        // Maximum jitter observed in practice is 7
        check(Math.abs(n1 - n0) < 10);
        check(n1 < 25);
        drainers.shutdown();
        if (!drainers.awaitTermination(LONG_DELAY_MS, MILLISECONDS)) {
            drainers.shutdownNow(); // last resort
            throw new AssertionError("thread pool did not terminate");
        }
    }

    //----------------------------------------------------------------
    // The main class of the child process.
    // Job's job is to:
    // - provide the name of a class to check for leaks.
    // - call rendezvousChild exactly twice, while quiescent.
    // - in between calls to rendezvousChild, run code that may leak.
    //----------------------------------------------------------------
    public static class Job {
        static String classNameRegexToCheckForLeaks() {
            return
                "\\Qjava.util.concurrent.locks.AbstractQueuedSynchronizer$\\E[A-Za-z]+";
        }

        public static void main(String[] args) throws Throwable {
            // Synchronize with parent process, so that jps can find us
            sendByte(System.out);
            System.in.read();

            final ReentrantLock lock = new ReentrantLock();
            lock.lock();

            final ReentrantReadWriteLock rwlock
                = new ReentrantReadWriteLock();
            final ReentrantReadWriteLock.ReadLock readLock
                = rwlock.readLock();
            final ReentrantReadWriteLock.WriteLock writeLock
                = rwlock.writeLock();
            rwlock.writeLock().lock();

            final BlockingQueue<Object> q = new LinkedBlockingQueue<>();
            final Semaphore fairSem = new Semaphore(0, true);
            final Semaphore unfairSem = new Semaphore(0, false);
            //final int threads =
            //rnd.nextInt(Runtime.getRuntime().availableProcessors() + 1) + 1;
            final int threads = 3;
            // On Linux, this test runs very slowly for some reason,
            // so use a smaller number of iterations.
            // Solaris can handle 1 << 18.
            // On the other hand, jmap is much slower on Solaris...
            final int iterations = 1 << 8;
            final CyclicBarrier cb = new CyclicBarrier(threads+1);

            for (int i = 0; i < threads; i++)
                new Thread() { public void run() {
                    try {
                        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
                        for (int j = 0; j < iterations; j++) {
                            if (j == iterations/10 || j == iterations - 1) {
                                cb.await(); // Quiesce
                                cb.await(); // Resume
                            }
                            //int t = rnd.nextInt(2000);
                            int t = rnd.nextInt(900);
                            check(! lock.tryLock(t, NANOSECONDS));
                            check(! readLock.tryLock(t, NANOSECONDS));
                            check(! writeLock.tryLock(t, NANOSECONDS));
                            equal(null, q.poll(t, NANOSECONDS));
                            check(! fairSem.tryAcquire(t, NANOSECONDS));
                            check(! unfairSem.tryAcquire(t, NANOSECONDS));
                        }
                    } catch (Throwable t) { unexpected(t); }
                }}.start();

            cb.await();         // Quiesce
            rendezvousChild();  // Measure
            cb.await();         // Resume

            cb.await();         // Quiesce
            rendezvousChild();  // Measure
            cb.await();         // Resume

            System.exit(failed);
        }

        // If something goes wrong, we might never see it, since IO
        // streams are connected to the parent.  So we need a special
        // purpose print method to debug Jobs.
        static void debugPrintf(String format, Object... args) {
            try {
                new PrintStream(new FileOutputStream("/dev/tty"))
                    .printf(format, args);
            } catch (Throwable t) { throw new Error(t); }
        }
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

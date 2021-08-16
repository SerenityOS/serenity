/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 *          lib/
 * @run testng/othervm LdapTimeoutTest
 * @bug 7094377 8000487 6176036 7056489 8151678
 * @summary Timeout tests for ldap
 */

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static jdk.test.lib.Utils.adjustTimeout;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class LdapTimeoutTest {

    // ------ configure test timeouts here ------

    /*
     * Practical representation of an infinite timeout.
     */
    private static final long INFINITY_MILLIS = adjustTimeout(20_000);
    /*
     * The acceptable variation in timeout measurements.
     */
    private static final long TOLERANCE       = adjustTimeout( 3_500);

    private static final long CONNECT_MILLIS  = adjustTimeout( 3_000);
    private static final long READ_MILLIS     = adjustTimeout(10_000);

    static {
        // a series of checks to make sure this timeouts configuration is
        // consistent and the timeouts do not overlap

        assert (TOLERANCE >= 0);
        // context creation
        assert (2 * CONNECT_MILLIS + TOLERANCE < READ_MILLIS);
        // context creation immediately followed by search
        assert (2 * CONNECT_MILLIS + READ_MILLIS + TOLERANCE < INFINITY_MILLIS);
    }

    @BeforeTest
    public void beforeTest() {
        startAuxiliaryDiagnosticOutput();
    }

    /*
     * These are timeout tests and they are run in parallel to reduce the total
     * amount of run time.
     *
     * Currently it doesn't seem possible to instruct JTREG to run TestNG test
     * methods in parallel. That said, this JTREG test is still
     * a "TestNG-flavored" test for the sake of having org.testng.Assert
     * capability.
     */
    @Test
    public void test() throws Exception {
        List<Future<?>> futures = new ArrayList<>();
        ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            futures.add(executorService.submit(() -> { test1(); return null; }));
            futures.add(executorService.submit(() -> { test2(); return null; }));
            futures.add(executorService.submit(() -> { test3(); return null; }));
            futures.add(executorService.submit(() -> { test4(); return null; }));
            futures.add(executorService.submit(() -> { test5(); return null; }));
            futures.add(executorService.submit(() -> { test6(); return null; }));
            futures.add(executorService.submit(() -> { test7(); return null; }));
        } finally {
            executorService.shutdown();
        }
        int failedCount = 0;
        for (var f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                failedCount++;
                e.getCause().printStackTrace(System.out);
            }
        }
        if (failedCount > 0)
            throw new RuntimeException(failedCount + " (sub)tests failed");
    }

    static void test1() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        // Here and in the other tests it's important to close the server as
        // calling `thread.interrupt` from assertion may not be enough
        // (depending on where the blocking call has stuck)
        try (TestServer server = new NotBindableServer()) {
            env.put(Context.PROVIDER_URL, urlTo(server));
            server.start();
            // Here and in the other tests joining done purely to reduce timing
            // jitter. Commenting out or removing that should not make the test
            // incorrect. (ServerSocket can accept connection as soon as it is
            // bound, not need to call `accept` before that.)
            server.starting().join();
            assertIncompletion(INFINITY_MILLIS, () -> new InitialDirContext(env));
        }
    }

    static void test2() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(CONNECT_MILLIS));
        try (TestServer server = new BindableButNotReadableServer()) {
            env.put(Context.PROVIDER_URL, urlTo(server));
            server.start();
            server.starting().join();
            InitialDirContext ctx = new InitialDirContext(env);
            SearchControls scl = new SearchControls();
            scl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            assertIncompletion(INFINITY_MILLIS,
                               () -> ctx.search("ou=People,o=JNDITutorial", "(objectClass=*)", scl));
        }
    }

    static void test3() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        try (TestServer server = new BindableButNotReadableServer()) {
            env.put(Context.PROVIDER_URL, urlTo(server));
            server.start();
            server.starting().join();
            InitialDirContext ctx = new InitialDirContext(env);
            SearchControls scl = new SearchControls();
            scl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            assertIncompletion(INFINITY_MILLIS,
                               () -> ctx.search("ou=People,o=JNDITutorial", "(objectClass=*)", scl));
        }
    }

    static void test4() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(CONNECT_MILLIS));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(READ_MILLIS));
        try (TestServer server = new NotBindableServer()) {
            env.put(Context.PROVIDER_URL, urlTo(server));
            server.start();
            server.starting().join();
            Assert.ThrowingRunnable completion =
                    () -> assertCompletion(CONNECT_MILLIS,
                                           2 * CONNECT_MILLIS + TOLERANCE,
                                           () -> new InitialDirContext(env));
            NamingException e = expectThrows(NamingException.class, completion);
            String msg = e.getMessage();
            assertTrue(msg != null && msg.contains("timeout")
                               && msg.contains(String.valueOf(CONNECT_MILLIS)),
                       msg);
        }
    }

    static void test5() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(CONNECT_MILLIS));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(READ_MILLIS));
        try (TestServer server = new BindableButNotReadableServer()) {
            env.put(Context.PROVIDER_URL, urlTo(server));
            server.start();
            server.starting().join();
            InitialDirContext ctx = new InitialDirContext(env);
            SearchControls scl = new SearchControls();
            scl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            Assert.ThrowingRunnable completion =
                    () -> assertCompletion(READ_MILLIS,
                                           READ_MILLIS + TOLERANCE,
                                           () -> ctx.search("ou=People,o=JNDITutorial", "(objectClass=*)", scl));
            NamingException e = expectThrows(NamingException.class, completion);
            String msg = e.getMessage();
            assertTrue(msg != null && msg.contains("timeout")
                               && msg.contains(String.valueOf(READ_MILLIS)),
                       msg);
        }
    }

    static void test6() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(CONNECT_MILLIS));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(READ_MILLIS));
        try (TestServer server = new NotBindableServer()) {
            env.put(Context.PROVIDER_URL, urlTo(server));
            server.start();
            server.starting().join();
            Assert.ThrowingRunnable completion =
                    () -> assertCompletion(CONNECT_MILLIS,
                                           2 * CONNECT_MILLIS + TOLERANCE,
                                           () -> new InitialDirContext(env));
            NamingException e = expectThrows(NamingException.class, completion);
            String msg = e.getMessage();
            assertTrue(msg != null && msg.contains("timeout")
                               && msg.contains(String.valueOf(CONNECT_MILLIS)),
                       msg);
        }
    }

    static void test7() throws Exception {
        // 8000487: Java JNDI connection library on ldap conn is
        // not honoring configured timeout
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(CONNECT_MILLIS));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(READ_MILLIS));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "user");
        env.put(Context.SECURITY_CREDENTIALS, "password");
        try (TestServer server = new NotBindableServer()) {
            env.put(Context.PROVIDER_URL, urlTo(server));
            server.start();
            server.starting().join();
            Assert.ThrowingRunnable completion =
                    () -> assertCompletion(CONNECT_MILLIS,
                                           2 * CONNECT_MILLIS + TOLERANCE,
                                           () -> new InitialDirContext(env));
            NamingException e = expectThrows(NamingException.class, completion);
            String msg = e.getMessage();
            assertTrue(msg != null && msg.contains("timeout")
                               && msg.contains(String.valueOf(CONNECT_MILLIS)),
                       msg);
        }
    }

    // ------ test stub servers ------

    static class TestServer extends BaseLdapServer {

        private final CompletableFuture<Void> starting = new CompletableFuture<>();

        TestServer() throws IOException { }

        @Override
        protected void beforeAcceptingConnections() {
            starting.completeAsync(() -> null);
        }

        public CompletableFuture<Void> starting() {
            return starting.copy();
        }
    }

    static class BindableButNotReadableServer extends TestServer {

        BindableButNotReadableServer() throws IOException { }

        private static final byte[] bindResponse = {
                0x30, 0x0C, 0x02, 0x01, 0x01, 0x61, 0x07, 0x0A,
                0x01, 0x00, 0x04, 0x00, 0x04, 0x00
        };

        @Override
        protected void handleRequest(Socket socket,
                                     LdapMessage msg,
                                     OutputStream out)
                throws IOException {
            switch (msg.getOperation()) {
                case BIND_REQUEST:
                    out.write(bindResponse);
                    out.flush();
                default:
                    break;
            }
        }
    }

    static class NotBindableServer extends TestServer {

        NotBindableServer() throws IOException { }

        @Override
        protected void beforeConnectionHandled(Socket socket) {
            try {
                TimeUnit.DAYS.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------ timeouts check utilities ------

    /*
     * Asserts that the specified executable yields a result or an exception
     * within the specified time frame. Interrupts the executable
     * unconditionally.
     *
     * If the executable yields a result or an exception within the specified
     * time frame, the result will be returned and the exception will be
     * rethrown respectively in a transparent fashion as if the executable was
     * executed directly.
     */
    public static <T> T assertCompletion(long loMillis,
                                         long hiMillis,
                                         Callable<T> code)
            throws Throwable {
        if (loMillis < 0 || hiMillis < 0 || loMillis > hiMillis) {
            throw new IllegalArgumentException("loMillis=" + loMillis +
                                                       ", hiMillis=" + hiMillis);
        }
        Objects.requireNonNull(code);

        // this queue acts both as an exchange point and a barrier
        SynchronousQueue<Long> startTime = new SynchronousQueue<>();

        Callable<T> wrappedTask = () -> {
            // by the time this value reaches the "stopwatch" thread it might be
            // well outdated and that's okay, we will adjust the wait time
            startTime.put(System.nanoTime());
            return code.call();
        };

        FutureTask<T> task = new FutureTask<>(wrappedTask);
        Thread t = new Thread(task);
        t.start();

        final long startNanos;
        try {
            startNanos = startTime.take(); // (1) wait for the initial time mark
        } catch (Throwable e) {
            t.interrupt();
            throw e;
        }

        final long waitTime = hiMillis -
                NANOSECONDS.toMillis(System.nanoTime() - startNanos); // (2) adjust wait time

        try {
            T r = task.get(waitTime, MILLISECONDS); // (3) wait for the task to complete
            long elapsed = NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            if (elapsed < loMillis || elapsed > hiMillis) {
                throw new RuntimeException(format(
                        "After %s ms. (waitTime %s ms.) returned result '%s'", elapsed, waitTime, r));
            }
            return r;
        } catch (ExecutionException e) {
            long elapsed = NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            if (elapsed < loMillis || elapsed > hiMillis) {
                throw new RuntimeException(format(
                        "After %s ms. (waitTime %s ms.) thrown exception", elapsed, waitTime), e);
            }
            throw e.getCause();
        } catch (TimeoutException e) {
            // We trust timed get not to throw TimeoutException prematurely
            // (i.e. before the wait time elapses)
            long elapsed = NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            throw new RuntimeException(format(
                    "After %s ms. (waitTime %s ms.) is incomplete", elapsed, waitTime));
        } finally {
            t.interrupt();
        }
    }

    /*
     * Asserts that the specified executable yields no result and no exception
     * for at least the specified amount of time. Interrupts the executable
     * unconditionally.
     */
    public static void assertIncompletion(long millis, Callable<?> code)
            throws Exception
    {
        if (millis < 0) {
            throw new IllegalArgumentException("millis=" + millis);
        }
        Objects.requireNonNull(code);

        // this queue acts both as an exchange point and a barrier
        SynchronousQueue<Long> startTime = new SynchronousQueue<>();

        Callable<?> wrappedTask = () -> {
            // by the time this value reaches the "stopwatch" thread it might be
            // well outdated and that's okay, we will adjust the wait time
            startTime.put(System.nanoTime());
            return code.call();
        };

        FutureTask<?> task = new FutureTask<>(wrappedTask);
        Thread t = new Thread(task);
        t.start();

        final long startNanos;
        try {
            startNanos = startTime.take(); // (1) wait for the initial time mark
        } catch (Throwable e) {
            t.interrupt();
            throw e;
        }

        final long waitTime = millis -
                NANOSECONDS.toMillis(System.nanoTime() - startNanos); // (2) adjust wait time

        try {
            Object r = task.get(waitTime, MILLISECONDS); // (3) wait for the task to complete
            long elapsed = NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            if (elapsed < waitTime) {
                throw new RuntimeException(format(
                        "After %s ms. (waitTime %s ms.) returned result '%s'", elapsed, waitTime, r));
            }
        } catch (ExecutionException e) {
            long elapsed = NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            if (elapsed < waitTime) {
                throw new RuntimeException(format(
                        "After %s ms. (waitTime %s ms.) thrown exception", elapsed, waitTime), e);
            }
        } catch (TimeoutException expected) {
        } finally {
            t.interrupt();
        }
    }

    // ------ miscellaneous utilities ------

    private static String urlTo(TestServer server) {
        String hostAddress = server.getInetAddress().getHostAddress();
        String addr;
        if (hostAddress.contains(":")) { // IPv6
            addr = '[' + hostAddress + ']';
        } else {                         // IPv4
            addr = hostAddress;
        }
        return "ldap://" + addr + ":" + server.getPort();
    }

    /*
     * A diagnostic aid that might help with debugging timeout issues. The idea
     * is to continuously measure accuracy and responsiveness of the system that
     * runs this test. If the system is overwhelmed (with something else), it
     * might affect the test run. At the very least we will have traces of that
     * in the logs.
     *
     * This utility does not automatically scale up test timeouts, it simply
     * gathers information.
     */
    private static void startAuxiliaryDiagnosticOutput() {
        System.out.printf("Starting diagnostic output (probe)%n");
        Thread t = new Thread(() -> {
            for (int i = 0; ; i = ((i % 20) + 1)) {
                // 500, 1_000, 1_500, ..., 9_500, 10_000, 500, 1_000, ...
                long expected = i * 500;
                long start = System.nanoTime();
                try {
                    MILLISECONDS.sleep(expected);
                } catch (InterruptedException e) {
                    return;
                }
                long stop = System.nanoTime();
                long actual = NANOSECONDS.toMillis(stop - start);
                System.out.printf("(probe) expected [ms.]: %s, actual [ms.]: %s%n",
                                  expected, actual);

            }
        }, "probe");
        t.setDaemon(true);
        t.start();
    }
}

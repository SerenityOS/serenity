/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import jdk.internal.access.JavaAWTAccess;
import jdk.internal.access.SharedSecrets;

/**
 * @test
 * @bug 8065991
 * @summary check that when LogManager is initialized, a deadlock similar
 *          to that described in 8065709 will not occur.
 * @modules java.base/jdk.internal.access
 *          java.logging
 *          java.management
 * @run main/othervm LogManagerAppContextDeadlock UNSECURE
 * @run main/othervm LogManagerAppContextDeadlock SECURE
 *
 * @author danielfuchs
 */
public class LogManagerAppContextDeadlock {

    public static final Semaphore sem = new Semaphore(0);
    public static final Semaphore sem2 = new Semaphore(0);
    public static final Semaphore sem3 = new Semaphore(-2);
    public static volatile boolean goOn = true;
    public static volatile Exception thrown;

    // Emulate EventQueue
    static class FakeEventQueue {
        static final Logger logger = Logger.getLogger("foo");
    }

    // Emulate AppContext
    static class FakeAppContext {

        static final AtomicInteger numAppContexts = new AtomicInteger(0);
        static final class FakeAppContextLock {}
        static final FakeAppContextLock lock = new FakeAppContextLock();
        static volatile FakeAppContext appContext;

        final FakeEventQueue queue;
        FakeAppContext() {
            appContext = this;
            numAppContexts.incrementAndGet();
            // release sem2 to let Thread t2 call Logger.getLogger().
            sem2.release();
            try {
                // Wait until we JavaAWTAccess is called by LogManager.
                // Thread 2 will call Logger.getLogger() which will
                // trigger a call to JavaAWTAccess - which will release
                // sem, thus ensuring that Thread #2 is where we want it.
                sem.acquire();
                System.out.println("Sem acquired: Thread #2 has called JavaAWTAccess");
            } catch(InterruptedException x) {
                Thread.interrupted();
            }
            queue = new FakeEventQueue();
        }

        static FakeAppContext getAppContext() {
            synchronized (lock) {
                if (numAppContexts.get() == 0) {
                    return new FakeAppContext();
                }
                return appContext;
            }
        }

        static {
            SharedSecrets.setJavaAWTAccess(new JavaAWTAccess() {
                @Override
                public Object getAppletContext() {
                    if (numAppContexts.get() == 0) return null;
                    // We are in JavaAWTAccess, we can release sem and let
                    // FakeAppContext constructor proceeed.
                    System.out.println("Releasing Sem");
                    sem.release();
                    return getAppContext();
                }

            });
        }

    }


    // Test with or without a security manager
    public static enum TestCase {
        UNSECURE, SECURE;
        public void run() throws Exception {
            System.out.println("Running test case: " + name());
            Configure.setUp(this);
            test(this);
        }
    }

    public static void test(TestCase test) throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                sem3.release();
                System.out.println("FakeAppContext.getAppContext()");
                FakeAppContext.getAppContext();
                System.out.println("Done: FakeAppContext.getAppContext()");
            }
        };
        t1.setDaemon(true);
        t1.start();
        Thread t2 = new Thread() {
            public Object logger;
            public void run() {
                sem3.release();
                try {
                    // Wait until Thread1 is in FakeAppContext constructor
                    sem2.acquire();
                    System.out.println("Sem2 acquired: Thread #1 will be waiting to acquire Sem");
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                }
                System.out.println("Logger.getLogger(name).info(name)");
                // stick the logger in an instance variable to prevent it
                // from being garbage collected before the main thread
                // calls LogManager.getLogger() below.
                logger = Logger.getLogger(test.name());//.info(name);
                System.out.println("Done: Logger.getLogger(name).info(name)");
            }
        };
        t2.setDaemon(true);
        t2.start();
        System.out.println("Should exit now...");
        Thread detector = new DeadlockDetector();
        detector.start();

        // Wait for the 3 threads to start
        sem3.acquire();

        // Now wait for t1 & t2 to finish, or for a deadlock to be detected.
        while (goOn && (t1.isAlive() || t2.isAlive())) {
            if (t2.isAlive()) t2.join(1000);
            if (test == TestCase.UNSECURE && System.getSecurityManager() == null) {
                // if there's no security manager, AppContext.getAppContext() is
                // not called -  so Thread t2 will not end up calling
                // sem.release(). In that case we must release the semaphore here
                // so that t1 can proceed.
                if (LogManager.getLogManager().getLogger(TestCase.UNSECURE.name()) != null) {
                    // means Thread t2 has created the logger
                    sem.release();
                }
            }
            if (t1.isAlive()) t1.join(1000);
        }
        if (thrown != null) {
            throw thrown;
        }
    }

    // Thrown by the deadlock detector
    static final class DeadlockException extends RuntimeException {
        public DeadlockException(String message) {
            super(message);
        }
        @Override
        public void printStackTrace() {
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            args = new String[] { "SECURE" };
        }

        // If we don't initialize LogManager here, there will be
        // a deadlock.
        // See <https://bugs.openjdk.java.net/browse/JDK-8065709?focusedCommentId=13582038&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13582038>
        // for more details.
        Logger.getLogger("main").info("starting...");
        try {
            TestCase.valueOf(args[0]).run();
            System.out.println("Test "+args[0]+" Passed");
        } catch(Throwable t) {
            System.err.println("Test " + args[0] +" failed: " + t);
            t.printStackTrace();
        }
    }

    // Called by the deadlock detector when a deadlock is found.
    static void fail(Exception x) {
        x.printStackTrace();
        if (thrown == null) {
            thrown = x;
        }
        goOn = false;
    }

    // A thread that detect deadlocks.
    static final class DeadlockDetector extends Thread {

        public DeadlockDetector() {
            this.setDaemon(true);
        }

        @Override
        public void run() {
            sem3.release();
            Configure.doPrivileged(this::loop);
        }
        public void loop() {
            while(goOn) {
                try {
                    long[] ids = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
                    ids = ids == null ? new long[0] : ids;
                    if (ids.length == 1) {
                        throw new RuntimeException("Found 1 deadlocked thread: "+ids[0]);
                    } else if (ids.length > 0) {
                        ThreadInfo[] infos = ManagementFactory.getThreadMXBean().getThreadInfo(ids, Integer.MAX_VALUE);
                        System.err.println("Found "+ids.length+" deadlocked threads: ");
                        for (ThreadInfo inf : infos) {
                            System.err.println(inf);
                        }
                        throw new DeadlockException("Found "+ids.length+" deadlocked threads");
                    }
                    Thread.sleep(100);
                } catch(InterruptedException | RuntimeException x) {
                    fail(x);
                }
            }
        }

    }

    // A helper class to configure the security manager for the test,
    // and bypass it when needed.
    static class Configure {
        static Policy policy = null;
        static final ThreadLocal<AtomicBoolean> allowAll = new ThreadLocal<AtomicBoolean>() {
            @Override
            protected AtomicBoolean initialValue() {
                return  new AtomicBoolean(false);
            }
        };
        static void setUp(TestCase test) {
            switch (test) {
                case SECURE:
                    if (policy == null && System.getSecurityManager() != null) {
                        throw new IllegalStateException("SecurityManager already set");
                    } else if (policy == null) {
                        policy = new SimplePolicy(TestCase.SECURE, allowAll);
                        Policy.setPolicy(policy);
                        System.setSecurityManager(new SecurityManager());
                    }
                    if (System.getSecurityManager() == null) {
                        throw new IllegalStateException("No SecurityManager.");
                    }
                    if (policy == null) {
                        throw new IllegalStateException("policy not configured");
                    }
                    break;
                case UNSECURE:
                    if (System.getSecurityManager() != null) {
                        throw new IllegalStateException("SecurityManager already set");
                    }
                    break;
                default:
                    new InternalError("No such testcase: " + test);
            }
        }
        static void doPrivileged(Runnable run) {
            allowAll.get().set(true);
            try {
                run.run();
            } finally {
                allowAll.get().set(false);
            }
        }
    }

    // A Helper class to build a set of permissions.
    static final class PermissionsBuilder {
        final Permissions perms;
        public PermissionsBuilder() {
            this(new Permissions());
        }
        public PermissionsBuilder(Permissions perms) {
            this.perms = perms;
        }
        public PermissionsBuilder add(Permission p) {
            perms.add(p);
            return this;
        }
        public PermissionsBuilder addAll(PermissionCollection col) {
            if (col != null) {
                for (Enumeration<Permission> e = col.elements(); e.hasMoreElements(); ) {
                    perms.add(e.nextElement());
                }
            }
            return this;
        }
        public Permissions toPermissions() {
            final PermissionsBuilder builder = new PermissionsBuilder();
            builder.addAll(perms);
            return builder.perms;
        }
    }

    // Policy for the test...
    public static class SimplePolicy extends Policy {

        static final Policy DEFAULT_POLICY = Policy.getPolicy();

        final Permissions permissions;
        final Permissions allPermissions;
        final ThreadLocal<AtomicBoolean> allowAll; // actually: this should be in a thread locale
        public SimplePolicy(TestCase test, ThreadLocal<AtomicBoolean> allowAll) {
            this.allowAll = allowAll;
            // we don't actually need any permission to create our
            // FileHandlers because we're passing invalid parameters
            // which will make the creation fail...
            permissions = new Permissions();
            permissions.add(new RuntimePermission("accessClassInPackage.jdk.internal.access"));

            // these are used for configuring the test itself...
            allPermissions = new Permissions();
            allPermissions.add(new java.security.AllPermission());

        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (allowAll.get().get()) return allPermissions.implies(permission);
            return permissions.implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return new PermissionsBuilder().addAll(allowAll.get().get()
                    ? allPermissions : permissions).toPermissions();
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return new PermissionsBuilder().addAll(allowAll.get().get()
                    ? allPermissions : permissions).toPermissions();
        }
    }

}

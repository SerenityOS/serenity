/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.internal.logger.BootstrapLogger;
import jdk.internal.logger.LazyLoggers;

/*
 * @test
 * @bug     8140364 8189291
 * @author  danielfuchs
 * @summary JDK implementation specific unit test for JDK internal artifacts.
            Tests the behavior of bootstrap loggers (and SimpleConsoleLoggers
 *          too).
 * @modules java.base/jdk.internal.logger:+open
 *          java.logging
 * @build BootstrapLoggerUtils LogStream
 * @run main/othervm BootstrapLoggerTest NO_SECURITY
 * @run main/othervm -Djava.security.manager=allow BootstrapLoggerTest SECURE
 * @run main/othervm/timeout=120 -Djava.security.manager=allow BootstrapLoggerTest SECURE_AND_WAIT
 */
public class BootstrapLoggerTest {

    static final Policy DEFAULT_POLICY = Policy.getPolicy();
    static final Method isAlive;
    static final Field logManagerInitialized;
    static {
        try {
            // private reflection hook that allows us to test whether
            // the BootstrapExecutor is alive.
            isAlive = BootstrapLogger.class
                    .getDeclaredMethod("isAlive");
            isAlive.setAccessible(true);
            // private reflection hook that allows us to test whether the LogManager
            // has initialized and registered with the BootstrapLogger class
            logManagerInitialized = BootstrapLogger.class
                    .getDeclaredField("logManagerConfigured");
            logManagerInitialized.setAccessible(true);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static enum TestCase {
        NO_SECURITY, SECURE, SECURE_AND_WAIT
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            args = new String[] { TestCase.SECURE_AND_WAIT.name() };
        }
        if (args.length > 1) throw new RuntimeException("Only one argument allowed");
        TestCase test = TestCase.valueOf(args[0]);
        System.err.println("Testing: " + test);


        // private reflection hook that allows us to simulate a non booted VM
        final AtomicBoolean vmBooted = new AtomicBoolean(false);
        BootstrapLoggerUtils.setBootedHook(() -> vmBooted.get());

        // We  replace System.err to check the messages that have been logged
        // by the JUL ConsoleHandler and default SimpleConsoleLogger
        // implementaion
        final LogStream err = new LogStream();
        System.setErr(new PrintStream(err));

        if (BootstrapLogger.isBooted()) {
            throw new RuntimeException("VM should not be booted!");
        }
        Logger logger = LazyLoggers.getLogger("foo.bar", Thread.class.getModule());

        if (test != TestCase.NO_SECURITY) {
            LogStream.err.println("Setting security manager");
            Policy.setPolicy(new SimplePolicy());
            System.setSecurityManager(new SecurityManager());
        }

        Level[] levels = {Level.INFO, Level.WARNING, Level.INFO};
        int index = 0;
        logger.log(levels[index], "Early message #" + (index+1)); index++;
        logger.log(levels[index], "Early message #" + (index+1)); index++;
        LogStream.err.println("VM Booted: " + vmBooted.get());
        LogStream.err.println("LogManager initialized: " + logManagerInitialized.get(null));
        logger.log(levels[index], "Early message #" + (index+1)); index++;
        if (err.drain().contains("Early message")) {
            // We're expecting that logger will be a LazyLogger wrapping a
            // BootstrapLogger. The Bootstrap logger will stack the log messages
            // it receives until the VM is booted.
            // Since our private hook pretend that the VM is not booted yet,
            // the logged messages shouldn't have reached System.err yet.
            throw new RuntimeException("Early message logged while VM is not booted!");
        }

        // Now pretend that the VM is booted. Nothing should happen yet, until
        // we try to log a new message.
        vmBooted.getAndSet(true);
        LogStream.err.println("VM Booted: " + vmBooted.get());
        LogStream.err.println("LogManager initialized: " + logManagerInitialized.get(null));
        if (!BootstrapLogger.isBooted()) {
            throw new RuntimeException("VM should now be booted!");
        }
        if (((Boolean)logManagerInitialized.get(null)).booleanValue()) {
            throw new RuntimeException("LogManager shouldn't be initialized yet!");
        }

        // Logging a message should cause the BootstrapLogger to replace itself
        // by a 'real' logger in the LazyLogger. But since the LogManager isn't
        // initialized yet, this should be a SimpleConsoleLogger...
        logger.log(Level.INFO, "LOG#4: VM now booted: {0}", vmBooted.get());
        logger.log(Level.DEBUG, "LOG#5: hi!");
        SimplePolicy.allowAll.set(Boolean.TRUE);
        WeakReference<Thread> threadRef = null;
        ReferenceQueue<Thread> queue = new ReferenceQueue<>();
        try {
            Set<Thread> set = Thread.getAllStackTraces().keySet().stream()
                    .filter((t) -> t.getName().startsWith("BootstrapMessageLoggerTask-"))
                    .collect(Collectors.toSet());
            set.stream().forEach(t -> LogStream.err.println("Found: " + t));
            if (set.size() > 1) {
                throw new RuntimeException("Too many bootstrap threads found");
            }
            Optional<Thread> t = set.stream().findFirst();
            if (t.isPresent()) {
                threadRef = new WeakReference<>(t.get(), queue);
            }
        } finally{
            SimplePolicy.allowAll.set(Boolean.FALSE);
        }
        if (!BootstrapLogger.isBooted()) {
            throw new RuntimeException("VM should still be booted!");
        }
        if (((Boolean)logManagerInitialized.get(null)).booleanValue()) {
            throw new RuntimeException("LogManager shouldn't be initialized yet!");
        }

        // Now check that the early messages we had printed before the VM was
        // booted have appeared on System.err...
        String afterBoot = err.drain();
        for (int i=0; i<levels.length; i++) {
            String m = levels[i].getName()+": Early message #"+(i+1);
            if (!afterBoot.contains(m)) {
                throw new RuntimeException("System.err does not contain: "+m);
            }
        }
        // check that the message logged *after* the VM was booted also printed.
        if (!afterBoot.contains("INFO: LOG#4")) {
            throw new RuntimeException("System.err does not contain: "
                    + "INFO: LOG#4");
        }
        // check that the debug message was not printed.
        if (afterBoot.contains("LOG#5")) {
            throw new RuntimeException("System.err contain: " + "LOG#5");
        }
        LogStream.err.println("VM Booted: " + vmBooted.get());
        LogStream.err.println("LogManager initialized: " + logManagerInitialized.get(null));
        if (!BootstrapLogger.isBooted()) {
            throw new RuntimeException("VM should still be booted!");
        }
        if (((Boolean)logManagerInitialized.get(null)).booleanValue()) {
            throw new RuntimeException("LogManager shouldn't be initialized yet!");
        }

        // Now we're going to use reflection to access JUL, and change
        // the level of the "foo" logger.
        // We're using reflection so that the test can also run in
        // configurations where java.util.logging is not present.
        boolean hasJUL = false;
        SimplePolicy.allowAll.set(Boolean.TRUE);
        try {
            Class<?> loggerClass = Class.forName("java.util.logging.Logger");
            Class<?> levelClass  = Class.forName("java.util.logging.Level");
            Class<?> handlerClass  = Class.forName("java.util.logging.Handler");

            // java.util.logging.Logger.getLogger("foo")
            //        .setLevel(java.util.logging.Level.FINEST);
            Object fooLogger = loggerClass.getMethod("getLogger", String.class)
                    .invoke(null, "foo");
            loggerClass.getMethod("setLevel", levelClass)
                    .invoke(fooLogger, levelClass.getField("FINEST").get(null));

            // java.util.logging.Logger.getLogger("").getHandlers()[0]
            //        .setLevel(java.util.logging.Level.ALL);
            Object rootLogger = loggerClass.getMethod("getLogger", String.class)
                    .invoke(null, "");
            Object handlers = loggerClass.getMethod("getHandlers").
                    invoke(rootLogger);
            handlerClass.getMethod("setLevel", levelClass)
                    .invoke(Array.get(handlers, 0), levelClass.getField("ALL")
                            .get(null));

            hasJUL = true;
        } catch (ClassNotFoundException x) {
            LogStream.err.println("JUL is not present: class " + x.getMessage()
                    + " not found");
            hasJUL = false;
        } finally {
            SimplePolicy.allowAll.set(Boolean.FALSE);
        }

        logger.log(Level.DEBUG, "hi now!");
        String debug = err.drain();
        if (hasJUL) {
            if (!((Boolean)logManagerInitialized.get(null)).booleanValue()) {
                throw new RuntimeException("LogManager should be initialized now!");
            }
            if (!debug.contains("FINE: hi now!")) {
                throw new RuntimeException("System.err does not contain: "
                        + "FINE: hi now!");
            }
        } else {
            if (debug.contains("hi now!")) {
                throw new RuntimeException("System.err contains: " + "hi now!");
            }
            if (((Boolean)logManagerInitialized.get(null)).booleanValue()) {
                throw new RuntimeException("LogManager shouldn't be initialized yet!");
            }
            Logger baz = System.getLogger("foo.bar.baz");
            if (((Boolean)logManagerInitialized.get(null)).booleanValue()) {
                throw new RuntimeException("LogManager shouldn't be initialized yet!");
            }
        }
        Logger bazbaz = null;
        SimplePolicy.allowAll.set(Boolean.TRUE);
        try {
            bazbaz = java.lang.System.LoggerFinder
                    .getLoggerFinder().getLogger("foo.bar.baz.baz", BootstrapLoggerTest.class.getModule());
        } finally {
            SimplePolicy.allowAll.set(Boolean.FALSE);
        }
        if (!((Boolean)logManagerInitialized.get(null)).booleanValue()) {
            throw new RuntimeException("LogManager should be initialized now!");
        }
        Logger bazbaz2 = System.getLogger("foo.bar.baz.baz");
        if (bazbaz2.getClass() != bazbaz.getClass()) {
            throw new RuntimeException("bazbaz2.class != bazbaz.class ["
                    + bazbaz2.getClass() + " != "
                    + bazbaz.getClass() + "]");
        }
        if (hasJUL != bazbaz2.getClass().getName()
                .equals("sun.util.logging.internal.LoggingProviderImpl$JULWrapper")) {
            throw new RuntimeException("Unexpected class for bazbaz: "
                    + bazbaz.getClass().getName()
                    + "\n\t expected: "
                    + "sun.util.logging.internal.LoggingProviderImpl$JULWrapper");
        }

        // Now we're going to check that the thread of the BootstrapLogger
        // executor terminates, and that the Executor is GC'ed after that.
        // This will involve a bit of waiting, hence the timeout=120 in
        // the @run line.
        // If this test fails in timeout - we could envisage skipping this part,
        // or adding some System property to configure the keep alive delay
        // of the executor.
        SimplePolicy.allowAll.set(Boolean.TRUE);
        try {
            // Though unlikely, it is not impossible that the bootstrap logger
            // executor may have released its first thread and spawned a new one.
            // If that happened then the executor itself might have been GC'ed
            // as well and a new one might have been created.
            // The code below will lookup the executor threads again and
            // join them.
            // Only one may be active at a given time, but that might not
            // be the one referenced by threadRef.
            // We're just making sure all of them have stopped running
            // before verifying that the executor is eventually GC'ed.
            final WeakReference<Thread> previous = threadRef;
            Stream<WeakReference<Thread>> stream = Thread.getAllStackTraces().keySet().stream()
               .filter((t) -> t.getName().startsWith("BootstrapMessageLoggerTask-"))
               .filter((t) -> previous == null ? true : t != previous.get())
               .map((t) -> new WeakReference<>(t, queue));
            List<WeakReference<Thread>> threads = stream.collect(Collectors.toList());
            if (previous != null) threads.add(previous);
            threads.forEach(t -> LogStream.err.println(t.get()));
            stream = null;

            if (test == TestCase.SECURE_AND_WAIT) {
                // First wait for all executor threads to terminate
                for (var ref : threads) {
                    Thread t = ref.get();
                    if (t != null) {
                        if (!(Boolean)isAlive.invoke(null) && t.isAlive()) {
                            throw new RuntimeException("Executor already terminated");
                        } else {
                            LogStream.err.println("Executor still alive as expected: " + t.getName());
                        }
                        LogStream.err.println("Waiting for " + t.getName() + " to terminate (join)");
                        t.join(60_000);
                        t = null;
                    } else {
                        LogStream.err.println("WeakReference<Thread> is already cleared.");
                        long count = Thread.getAllStackTraces().keySet().stream()
                            .filter((tr) -> tr.getName().startsWith("BootstrapMessageLoggerTask-"))
                            .count();
                        if (count != 0) {
                            LogStream.err.println("There are " + count + " threads still lingering.");
                        }
                    }
                }
                // Then wait until all the executor threads are GC'ed
                while (!threads.isEmpty()) {
                    LogStream.err.println("Calling System.gc()");
                    System.gc();
                    LogStream.err.println("Waiting for BootstrapMessageLoggerTask to be gc'ed");
                    Reference<?> tref;
                    while ((tref = queue.remove(1000)) == null) {
                        LogStream.err.println("Calling System.gc()");
                        System.gc();
                    }

                    threads.remove(tref);
                    LogStream.err.println("BootstrapMessageLoggerTask has been gc'ed: "
                                          + threads.size() + " remaining...");
                }
                // Then wait for the executor to be gc'ed...
                LogStream.err.println("Waiting for the executor to be gc'ed: Calling System.gc()");
                System.gc();
                for (int i=0; i<10; i++) {
                    if (!(Boolean)isAlive.invoke(null)) break;
                    // It would be unexpected that we reach here...
                    Thread.sleep(1000);
                    LogStream.err.println("Calling System.gc()");
                    System.gc();
                }

                if ((Boolean)isAlive.invoke(null)) {
                    throw new RuntimeException("Executor still alive");
                } else {
                    LogStream.err.println("Executor terminated as expected.");
                }
            } else {
                LogStream.err.println("Not checking executor termination for " + test);
            }
        } finally {
            SimplePolicy.allowAll.set(Boolean.FALSE);
        }
        LogStream.err.println(test.name() + ": PASSED");
    }

    final static class SimplePolicy extends Policy {
        static final ThreadLocal<Boolean> allowAll = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };

        Permissions getPermissions() {
            Permissions perms = new Permissions();
            if (allowAll.get()) {
                perms.add(new AllPermission());
            }
            return perms;
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return getPermissions(domain).implies(permission) ||
                   DEFAULT_POLICY.implies(domain, permission);
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return getPermissions();
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return getPermissions();
        }

    }
}

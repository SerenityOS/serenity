/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.logger;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jdk.internal.misc.InnocuousThread;
import jdk.internal.misc.VM;
import sun.util.logging.PlatformLogger;
import jdk.internal.logger.LazyLoggers.LazyLoggerAccessor;

/**
 * The BootstrapLogger class handles all the logic needed by Lazy Loggers
 * to delay the creation of System.Logger instances until the VM is booted.
 * By extension - it also contains the logic that will delay the creation
 * of JUL Loggers until the LogManager is initialized by the application, in
 * the common case where JUL is the default and there is no custom JUL
 * configuration.
 *
 * A BootstrapLogger instance is both a Logger and a
 * PlatformLogger.Bridge instance, which will put all Log messages in a queue
 * until the VM is booted.
 * Once the VM is booted, it obtain the real System.Logger instance from the
 * LoggerFinder and flushes the message to the queue.
 *
 * There are a few caveat:
 *  - the queue may not be flush until the next message is logged after
 *    the VM is booted
 *  - while the BootstrapLogger is active, the default implementation
 *    for all convenience methods is used
 *  - PlatformLogger.setLevel calls are ignored
 *
 *
 */
public final class BootstrapLogger implements Logger, PlatformLogger.Bridge,
        PlatformLogger.ConfigurableBridge {

    // We use the BootstrapExecutors class to submit delayed messages
    // to an independent InnocuousThread which will ensure that
    // delayed log events will be clearly identified as messages that have
    // been delayed during the boot sequence.
    private static class BootstrapExecutors implements ThreadFactory {

        // Maybe that should be made configurable with system properties.
        static final long KEEP_EXECUTOR_ALIVE_SECONDS = 30;

        // The BootstrapMessageLoggerTask is a Runnable which keeps
        // a hard ref to the ExecutorService that owns it.
        // This ensure that the ExecutorService is not gc'ed until the thread
        // has stopped running.
        private static class BootstrapMessageLoggerTask implements Runnable {
            ExecutorService owner;
            Runnable run;
            public BootstrapMessageLoggerTask(ExecutorService owner, Runnable r) {
                this.owner = owner;
                this.run = r;
            }
            @Override
            public void run() {
                try {
                    run.run();
                } finally {
                    owner = null; // allow the ExecutorService to be gced.
                }
            }
        }

        private static volatile WeakReference<ExecutorService> executorRef;
        private static ExecutorService getExecutor() {
            WeakReference<ExecutorService> ref = executorRef;
            ExecutorService executor = ref == null ? null : ref.get();
            if (executor != null) return executor;
            synchronized (BootstrapExecutors.class) {
                ref = executorRef;
                executor = ref == null ? null : ref.get();
                if (executor == null) {
                    executor = new ThreadPoolExecutor(0, 1,
                            KEEP_EXECUTOR_ALIVE_SECONDS, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(), new BootstrapExecutors());
                }
                // The executor service will be elligible for gc
                // KEEP_EXECUTOR_ALIVE_SECONDS seconds (30s)
                // after the execution of its last pending task.
                executorRef = new WeakReference<>(executor);
                return executorRef.get();
            }
        }

        @Override
        public Thread newThread(Runnable r) {
            ExecutorService owner = getExecutor();
            @SuppressWarnings("removal")
            Thread thread = AccessController.doPrivileged(new PrivilegedAction<Thread>() {
                @Override
                public Thread run() {
                    Thread t = InnocuousThread.newThread(new BootstrapMessageLoggerTask(owner, r));
                    t.setName("BootstrapMessageLoggerTask-"+t.getName());
                    return t;
                }
            }, null, new RuntimePermission("enableContextClassLoaderOverride"));
            thread.setDaemon(true);
            return thread;
        }

        static void submit(Runnable r) {
            getExecutor().execute(r);
        }

        // This is used by tests.
        static void join(Runnable r) {
            try {
                getExecutor().submit(r).get();
            } catch (InterruptedException | ExecutionException ex) {
                // should not happen
                throw new RuntimeException(ex);
            }
        }

        // This is used by tests.
        static void awaitPendingTasks() {
            WeakReference<ExecutorService> ref = executorRef;
            ExecutorService executor = ref == null ? null : ref.get();
            if (ref == null) {
                synchronized(BootstrapExecutors.class) {
                    ref = executorRef;
                    executor = ref == null ? null : ref.get();
                }
            }
            if (executor != null) {
                // since our executor uses a FIFO and has a single thread
                // then awaiting the execution of its pending tasks can be done
                // simply by registering a new task and waiting until it
                // completes. This of course would not work if we were using
                // several threads, but we don't.
                join(()->{});
            }
        }

        // This is used by tests.
        static boolean isAlive() {
            WeakReference<ExecutorService> ref = executorRef;
            if (ref != null && !ref.refersTo(null)) return true;
            synchronized (BootstrapExecutors.class) {
                ref = executorRef;
                return ref != null && !ref.refersTo(null);
            }
        }

        // The pending log event queue. The first event is the head, and
        // new events are added at the tail
        static LogEvent head, tail;

        static void enqueue(LogEvent event) {
            if (event.next != null) return;
            synchronized (BootstrapExecutors.class) {
                if (event.next != null) return;
                event.next = event;
                if (tail == null) {
                    head = tail = event;
                } else {
                    tail.next = event;
                    tail = event;
                }
            }
        }

        static void flush() {
            LogEvent event;
            // drain the whole queue
            synchronized(BootstrapExecutors.class) {
                event = head;
                head = tail = null;
            }
            while(event != null) {
                LogEvent.log(event);
                synchronized(BootstrapExecutors.class) {
                    LogEvent prev = event;
                    event = (event.next == event ? null : event.next);
                    prev.next = null;
                }
            }
        }
    }

    // The accessor in which this logger is temporarily set.
    final LazyLoggerAccessor holder;

    BootstrapLogger(LazyLoggerAccessor holder) {
        this.holder = holder;
    }

    // Temporary data object storing log events
    // It would be nice to use a Consumer<Logger> instead of a LogEvent.
    // This way we could simply do things like:
    //    push((logger) -> logger.log(level, msg));
    // Unfortunately, if we come to here it means we are in the bootsraping
    // phase where using lambdas is not safe yet - so we have to use
    // a data object instead...
    //
    static final class LogEvent {
        // only one of these two levels should be non null
        final Level level;
        final PlatformLogger.Level platformLevel;
        final BootstrapLogger bootstrap;

        final ResourceBundle bundle;
        final String msg;
        final Throwable thrown;
        final Object[] params;
        final Supplier<String> msgSupplier;
        final String sourceClass;
        final String sourceMethod;
        final long timeMillis;
        final long nanoAdjustment;

        // because logging a message may entail calling toString() on
        // the parameters etc... we need to store the context of the
        // caller who logged the message - so that we can reuse it when
        // we finally log the message.
        @SuppressWarnings("removal")
        final AccessControlContext acc;

        // The next event in the queue
        LogEvent next;

        @SuppressWarnings("removal")
        private LogEvent(BootstrapLogger bootstrap, Level level,
                ResourceBundle bundle, String msg,
                Throwable thrown, Object[] params) {
            this.acc = AccessController.getContext();
            this.timeMillis = System.currentTimeMillis();
            this.nanoAdjustment = VM.getNanoTimeAdjustment(timeMillis);
            this.level = level;
            this.platformLevel = null;
            this.bundle = bundle;
            this.msg = msg;
            this.msgSupplier = null;
            this.thrown = thrown;
            this.params = params;
            this.sourceClass = null;
            this.sourceMethod = null;
            this.bootstrap = bootstrap;
        }

        @SuppressWarnings("removal")
        private LogEvent(BootstrapLogger bootstrap, Level level,
                Supplier<String> msgSupplier,
                Throwable thrown, Object[] params) {
            this.acc = AccessController.getContext();
            this.timeMillis = System.currentTimeMillis();
            this.nanoAdjustment = VM.getNanoTimeAdjustment(timeMillis);
            this.level = level;
            this.platformLevel = null;
            this.bundle = null;
            this.msg = null;
            this.msgSupplier = msgSupplier;
            this.thrown = thrown;
            this.params = params;
            this.sourceClass = null;
            this.sourceMethod = null;
            this.bootstrap = bootstrap;
        }

        @SuppressWarnings("removal")
        private LogEvent(BootstrapLogger bootstrap,
                PlatformLogger.Level platformLevel,
                String sourceClass, String sourceMethod,
                ResourceBundle bundle, String msg,
                Throwable thrown, Object[] params) {
            this.acc = AccessController.getContext();
            this.timeMillis = System.currentTimeMillis();
            this.nanoAdjustment = VM.getNanoTimeAdjustment(timeMillis);
            this.level = null;
            this.platformLevel = platformLevel;
            this.bundle = bundle;
            this.msg = msg;
            this.msgSupplier = null;
            this.thrown = thrown;
            this.params = params;
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.bootstrap = bootstrap;
        }

        @SuppressWarnings("removal")
        private LogEvent(BootstrapLogger bootstrap,
                PlatformLogger.Level platformLevel,
                String sourceClass, String sourceMethod,
                Supplier<String> msgSupplier,
                Throwable thrown, Object[] params) {
            this.acc = AccessController.getContext();
            this.timeMillis = System.currentTimeMillis();
            this.nanoAdjustment = VM.getNanoTimeAdjustment(timeMillis);
            this.level = null;
            this.platformLevel = platformLevel;
            this.bundle = null;
            this.msg = null;
            this.msgSupplier = msgSupplier;
            this.thrown = thrown;
            this.params = params;
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.bootstrap = bootstrap;
        }

        // Log this message in the given logger. Do not call directly.
        // Use LogEvent.log(LogEvent, logger) instead.
        private void log(Logger logger) {
            assert platformLevel == null && level != null;
            //new Exception("logging delayed message").printStackTrace();
            if (msgSupplier != null) {
                if (thrown != null) {
                    logger.log(level, msgSupplier, thrown);
                } else {
                    logger.log(level, msgSupplier);
                }
            } else {
                // BootstrapLoggers are never localized so we can safely
                // use the method that takes a ResourceBundle parameter
                // even when that resource bundle is null.
                if (thrown != null) {
                    logger.log(level, bundle, msg, thrown);
                } else {
                    logger.log(level, bundle, msg, params);
                }
            }
        }

        // Log this message in the given logger.  Do not call directly.
        // Use LogEvent.doLog(LogEvent, logger) instead.
        private void log(PlatformLogger.Bridge logger) {
            assert platformLevel != null && level == null;
            if (sourceClass == null) {
                if (msgSupplier != null) {
                    if (thrown != null) {
                        logger.log(platformLevel, thrown, msgSupplier);
                    } else {
                        logger.log(platformLevel, msgSupplier);
                    }
                } else {
                    // BootstrapLoggers are never localized so we can safely
                    // use the method that takes a ResourceBundle parameter
                    // even when that resource bundle is null.
                    if (thrown != null) {
                        logger.logrb(platformLevel, bundle, msg, thrown);
                    } else {
                        logger.logrb(platformLevel, bundle, msg, params);
                    }
                }
            } else {
                if (msgSupplier != null) {
                    if (thrown != null) {
                        logger.logp(platformLevel, sourceClass, sourceMethod, thrown, msgSupplier);
                    } else {
                        logger.logp(platformLevel, sourceClass, sourceMethod, msgSupplier);
                    }
                } else {
                    // BootstrapLoggers are never localized so we can safely
                    // use the method that takes a ResourceBundle parameter
                    // even when that resource bundle is null.
                    if (thrown != null) {
                        logger.logrb(platformLevel, sourceClass, sourceMethod, bundle, msg, thrown);
                    } else {
                        logger.logrb(platformLevel, sourceClass, sourceMethod, bundle, msg, params);
                    }
                }
            }
        }

        // non default methods from Logger interface
        static LogEvent valueOf(BootstrapLogger bootstrap, Level level,
                ResourceBundle bundle, String key, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), bundle, key,
                                thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, Level level,
                ResourceBundle bundle, String format, Object[] params) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), bundle, format,
                                null, params);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, Level level,
                                Supplier<String> msgSupplier, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                    Objects.requireNonNull(level),
                    Objects.requireNonNull(msgSupplier), thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, Level level,
                                Supplier<String> msgSupplier) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level),
                                Objects.requireNonNull(msgSupplier), null, null);
        }
        @SuppressWarnings("removal")
        static void log(LogEvent log, Logger logger) {
            final SecurityManager sm = System.getSecurityManager();
            // not sure we can actually use lambda here. We may need to create
            // an anonymous class. Although if we reach here, then it means
            // the VM is booted.
            if (sm == null || log.acc == null) {
                BootstrapExecutors.submit(() -> log.log(logger));
            } else {
                BootstrapExecutors.submit(() ->
                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                        log.log(logger); return null;
                    }, log.acc));
            }
        }

        // non default methods from PlatformLogger.Bridge interface
        static LogEvent valueOf(BootstrapLogger bootstrap,
                                PlatformLogger.Level level, String msg) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), null, null, null,
                                msg, null, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String msg, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                    Objects.requireNonNull(level), null, null, null, msg, thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String msg, Object[] params) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                    Objects.requireNonNull(level), null, null, null, msg, null, params);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                Supplier<String> msgSupplier) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                    Objects.requireNonNull(level), null, null, msgSupplier, null, null);
        }
        static LogEvent vaueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                               Supplier<String> msgSupplier,
                               Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), null, null,
                                msgSupplier, thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String sourceClass, String sourceMethod,
                                ResourceBundle bundle, String msg, Object[] params) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), sourceClass,
                                sourceMethod, bundle, msg, null, params);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String sourceClass, String sourceMethod,
                                ResourceBundle bundle, String msg, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), sourceClass,
                                sourceMethod, bundle, msg, thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String sourceClass, String sourceMethod,
                                Supplier<String> msgSupplier, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), sourceClass,
                                sourceMethod, msgSupplier, thrown, null);
        }
        @SuppressWarnings("removal")
        static void log(LogEvent log, PlatformLogger.Bridge logger) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm == null || log.acc == null) {
                log.log(logger);
            } else {
                // not sure we can actually use lambda here. We may need to create
                // an anonymous class. Although if we reach here, then it means
                // the VM is booted.
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    log.log(logger); return null;
                }, log.acc);
            }
        }

        static void log(LogEvent event) {
            event.bootstrap.flush(event);
        }

    }

    // Push a log event at the end of the pending LogEvent queue.
    void push(LogEvent log) {
        BootstrapExecutors.enqueue(log);
        // if the queue has been flushed just before we entered
        // the synchronized block we need to flush it again.
        checkBootstrapping();
    }

    // Flushes the queue of pending LogEvents to the logger.
    void flush(LogEvent event) {
        assert event.bootstrap == this;
        if (event.platformLevel != null) {
            PlatformLogger.Bridge concrete = holder.getConcretePlatformLogger(this);
            LogEvent.log(event, concrete);
        } else {
            Logger concrete = holder.getConcreteLogger(this);
            LogEvent.log(event, concrete);
        }
    }

    /**
     * The name of this logger. This is the name of the actual logger for which
     * this logger acts as a temporary proxy.
     * @return The logger name.
     */
    @Override
    public String getName() {
        return holder.name;
    }

    /**
     * Check whether the VM is still bootstrapping, and if not, arranges
     * for this logger's holder to create the real logger and flush the
     * pending event queue.
     * @return true if the VM is still bootstrapping.
     */
    boolean checkBootstrapping() {
        if (isBooted()) {
            BootstrapExecutors.flush();
            return false;
        }
        return true;
    }

    // ----------------------------------
    // Methods from Logger
    // ----------------------------------

    @Override
    public boolean isLoggable(Level level) {
        if (checkBootstrapping()) {
            return level.getSeverity() >= Level.INFO.getSeverity();
        } else {
            final Logger spi = holder.wrapped();
            return spi.isLoggable(level);
        }
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String key, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, bundle, key, thrown));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, bundle, key, thrown);
        }
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, bundle, format, params));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, bundle, format, params);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, msg, thrown));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, msg, thrown);
        }
    }

    @Override
    public void log(Level level, String format, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, format, params));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, format, params);
        }
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msgSupplier));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, msgSupplier);
        }
    }

    @Override
    public void log(Level level, Object obj) {
        if (checkBootstrapping()) {
            Logger.super.log(level, obj);
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, obj);
        }
    }

    @Override
    public void log(Level level, String msg) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, msg, (Object[])null));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, msg);
        }
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msgSupplier, thrown));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, msgSupplier, thrown);
        }
    }

    // ----------------------------------
    // Methods from PlatformLogger.Bridge
    // ----------------------------------

    @Override
    public boolean isLoggable(PlatformLogger.Level level) {
        if (checkBootstrapping()) {
            return level.intValue() >= PlatformLogger.Level.INFO.intValue();
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            return spi.isLoggable(level);
        }
    }

    @Override
    public boolean isEnabled() {
        if (checkBootstrapping()) {
            return true;
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            return spi.isEnabled();
        }
    }

    @Override
    public void log(PlatformLogger.Level level, String msg) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msg));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, msg);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msg, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, msg, thrown);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, String msg, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msg, params));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, msg, params);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msgSupplier));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, msgSupplier);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, Throwable thrown,
            Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.vaueOf(this, level, msgSupplier, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, thrown, msgSupplier);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, String msg) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, null,
                    msg, (Object[])null));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, msg);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, msgSupplier, null));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, msgSupplier);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, String msg, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, null, msg, params));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, msg, params);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, null, msg, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, msg, thrown);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, Throwable thrown, Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, msgSupplier, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, thrown, msgSupplier);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, ResourceBundle bundle, String msg, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, bundle, msg, params));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logrb(level, sourceClass, sourceMethod, bundle, msg, params);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, ResourceBundle bundle, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, bundle, msg, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logrb(level, sourceClass, sourceMethod, bundle, msg, thrown);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, ResourceBundle bundle,
            String msg, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, null, bundle, msg, params));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logrb(level, bundle, msg, params);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, null, bundle, msg, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logrb(level, bundle, msg, thrown);
        }
    }

    @Override
    public LoggerConfiguration getLoggerConfiguration() {
        if (checkBootstrapping()) {
            // This practically means that PlatformLogger.setLevel()
            // calls will be ignored if the VM is still bootstrapping. We could
            // attempt to fix that but is it worth it?
            return PlatformLogger.ConfigurableBridge.super.getLoggerConfiguration();
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            return PlatformLogger.ConfigurableBridge.getLoggerConfiguration(spi);
        }
    }

    // This BooleanSupplier is a hook for tests - so that we can simulate
    // what would happen before the VM is booted.
    private static volatile BooleanSupplier isBooted;
    public static boolean isBooted() {
        if (isBooted != null) return isBooted.getAsBoolean();
        else return VM.isBooted();
    }

    // A bit of magic. We try to find out the nature of the logging
    // backend without actually loading it.
    private static enum LoggingBackend {
        // There is no LoggerFinder and JUL is not present
        NONE(true),

        // There is no LoggerFinder, but we have found a
        // JdkLoggerFinder installed (which means JUL is present),
        // and we haven't found any custom configuration for JUL.
        // Until LogManager is initialized we can use a simple console
        // logger.
        JUL_DEFAULT(false),

        // Same as above, except that we have found a custom configuration
        // for JUL. We cannot use the simple console logger in this case.
        JUL_WITH_CONFIG(true),

        // We have found a custom LoggerFinder.
        CUSTOM(true);

        final boolean useLoggerFinder;
        private LoggingBackend(boolean useLoggerFinder) {
            this.useLoggerFinder = useLoggerFinder;
        }
    };

    // The purpose of this class is to delay the initialization of
    // the detectedBackend field until it is actually read.
    // We do not want this field to get initialized if VM.isBooted() is false.
    @SuppressWarnings("removal")
    private static final class DetectBackend {
        static final LoggingBackend detectedBackend;
        static {
            detectedBackend = AccessController.doPrivileged(new PrivilegedAction<LoggingBackend>() {
                    @Override
                    public LoggingBackend run() {
                        final Iterator<LoggerFinder> iterator =
                            ServiceLoader.load(LoggerFinder.class, ClassLoader.getSystemClassLoader())
                            .iterator();
                        if (iterator.hasNext()) {
                            return LoggingBackend.CUSTOM; // Custom Logger Provider is registered
                        }
                        // No custom logger provider: we will be using the default
                        // backend.
                        final Iterator<DefaultLoggerFinder> iterator2 =
                            ServiceLoader.loadInstalled(DefaultLoggerFinder.class)
                            .iterator();
                        if (iterator2.hasNext()) {
                            // LoggingProviderImpl is registered. The default
                            // implementation is java.util.logging
                            String cname = System.getProperty("java.util.logging.config.class");
                            String fname = System.getProperty("java.util.logging.config.file");
                            return (cname != null || fname != null)
                                ? LoggingBackend.JUL_WITH_CONFIG
                                : LoggingBackend.JUL_DEFAULT;
                        } else {
                            // SimpleConsoleLogger is used
                            return LoggingBackend.NONE;
                        }
                    }
                });

        }
    }

    // We will use a temporary SurrogateLogger if
    // the logging backend is JUL, there is no custom config,
    // and the LogManager has not been initialized yet.
    private static  boolean useSurrogateLoggers() {
        // being paranoid: this should already have been checked
        if (!isBooted()) return true;
        return DetectBackend.detectedBackend == LoggingBackend.JUL_DEFAULT
                && !logManagerConfigured;
    }

    // We will use lazy loggers if:
    //    - the VM is not yet booted
    //    - the logging backend is a custom backend
    //    - the logging backend is JUL, there is no custom config,
    //      and the LogManager has not been initialized yet.
    public static synchronized boolean useLazyLoggers() {
        return !BootstrapLogger.isBooted()
                || DetectBackend.detectedBackend == LoggingBackend.CUSTOM
                || useSurrogateLoggers();
    }

    // Called by LazyLoggerAccessor. This method will determine whether
    // to create a BootstrapLogger (if the VM is not yet booted),
    // a SurrogateLogger (if JUL is the default backend and there
    // is no custom JUL configuration and LogManager is not yet initialized),
    // or a logger returned by the loaded LoggerFinder (all other cases).
    static Logger getLogger(LazyLoggerAccessor accessor) {
        if (!BootstrapLogger.isBooted()) {
            return new BootstrapLogger(accessor);
        } else {
            if (useSurrogateLoggers()) {
                // JUL is the default backend, there is no custom configuration,
                // LogManager has not been used.
                synchronized(BootstrapLogger.class) {
                    if (useSurrogateLoggers()) {
                        return createSurrogateLogger(accessor);
                    }
                }
            }
            // Already booted. Return the real logger.
            return accessor.createLogger();
        }
    }


    // If the backend is JUL, and there is no custom configuration, and
    // nobody has attempted to call LogManager.getLogManager() yet, then
    // we can temporarily substitute JUL Logger with SurrogateLoggers,
    // which avoids the cost of actually loading up the LogManager...
    // The RedirectedLoggers class has the logic to create such surrogate
    // loggers, and to possibly replace them with real JUL loggers if
    // someone calls LogManager.getLogManager().
    static final class RedirectedLoggers implements
            Function<LazyLoggerAccessor, SurrogateLogger> {

        // all accesses must be synchronized on the outer BootstrapLogger.class
        final Map<LazyLoggerAccessor, SurrogateLogger> redirectedLoggers =
                new HashMap<>();

        // all accesses must be synchronized on the outer BootstrapLogger.class
        // The redirectLoggers map will be cleared when LogManager is initialized.
        boolean cleared;

        @Override
        // all accesses must be synchronized on the outer BootstrapLogger.class
        public SurrogateLogger apply(LazyLoggerAccessor t) {
            if (cleared) throw new IllegalStateException("LoggerFinder already initialized");
            return SurrogateLogger.makeSurrogateLogger(t.getLoggerName());
        }

        // all accesses must be synchronized on the outer BootstrapLogger.class
        SurrogateLogger get(LazyLoggerAccessor a) {
            if (cleared) throw new IllegalStateException("LoggerFinder already initialized");
            return redirectedLoggers.computeIfAbsent(a, this);
        }

        // all accesses must be synchronized on the outer BootstrapLogger.class
        Map<LazyLoggerAccessor, SurrogateLogger> drainLoggersMap() {
            if (redirectedLoggers.isEmpty()) return null;
            if (cleared) throw new IllegalStateException("LoggerFinder already initialized");
            final Map<LazyLoggerAccessor, SurrogateLogger> accessors = new HashMap<>(redirectedLoggers);
            redirectedLoggers.clear();
            cleared = true;
            return accessors;
        }

        static void replaceSurrogateLoggers(Map<LazyLoggerAccessor, SurrogateLogger> accessors) {
            // When the backend is JUL we want to force the creation of
            // JUL loggers here: some tests are expecting that the
            // PlatformLogger will create JUL loggers as soon as the
            // LogManager is initialized.
            //
            // If the backend is not JUL then we can delay the re-creation
            // of the wrapped logger until they are next accessed.
            //
            final LoggingBackend detectedBackend = DetectBackend.detectedBackend;
            final boolean lazy = detectedBackend != LoggingBackend.JUL_DEFAULT
                    && detectedBackend != LoggingBackend.JUL_WITH_CONFIG;
            for (Map.Entry<LazyLoggerAccessor, SurrogateLogger> a : accessors.entrySet()) {
                a.getKey().release(a.getValue(), !lazy);
            }
        }

        // all accesses must be synchronized on the outer BootstrapLogger.class
        static final RedirectedLoggers INSTANCE = new RedirectedLoggers();
    }

    static synchronized Logger createSurrogateLogger(LazyLoggerAccessor a) {
        // accesses to RedirectedLoggers is synchronized on BootstrapLogger.class
        return RedirectedLoggers.INSTANCE.get(a);
    }

    private static volatile boolean logManagerConfigured;

    private static synchronized Map<LazyLoggerAccessor, SurrogateLogger>
         releaseSurrogateLoggers() {
        // first check whether there's a chance that we have used
        // surrogate loggers; Will be false if logManagerConfigured is already
        // true.
        final boolean releaseSurrogateLoggers = useSurrogateLoggers();

        // then sets the flag that tells that the log manager is configured
        logManagerConfigured = true;

        // finally retrieves all surrogate loggers that should be replaced
        // by real JUL loggers, and return them in the form of a redirected
        // loggers map.
        if (releaseSurrogateLoggers) {
            // accesses to RedirectedLoggers is synchronized on BootstrapLogger.class
            return RedirectedLoggers.INSTANCE.drainLoggersMap();
        } else {
            return null;
        }
    }

    public static void redirectTemporaryLoggers() {
        // This call is synchronized on BootstrapLogger.class.
        final Map<LazyLoggerAccessor, SurrogateLogger> accessors =
                releaseSurrogateLoggers();

        // We will now reset the logger accessors, triggering the
        // (possibly lazy) replacement of any temporary surrogate logger by the
        // real logger returned from the loaded LoggerFinder.
        if (accessors != null) {
            RedirectedLoggers.replaceSurrogateLoggers(accessors);
        }

        BootstrapExecutors.flush();
    }

    // Hook for tests which need to wait until pending messages
    // are processed.
    static void awaitPendingTasks() {
        BootstrapExecutors.awaitPendingTasks();
    }
    static boolean isAlive() {
        return BootstrapExecutors.isAlive();
    }

}

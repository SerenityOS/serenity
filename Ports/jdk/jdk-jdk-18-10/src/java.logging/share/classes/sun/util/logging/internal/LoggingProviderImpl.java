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


package sun.util.logging.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.util.Objects;
import java.util.logging.LogManager;
import jdk.internal.logger.DefaultLoggerFinder;
import java.util.logging.LoggingPermission;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.ConfigurableBridge.LoggerConfiguration;

/**
 * This {@code LoggingProviderImpl} is the JDK internal implementation of the
 * {@link jdk.internal.logger.DefaultLoggerFinder} which is used by
 * the default implementation of the {@link Logger}
 * when no {@link LoggerFinder} is found
 * and {@code java.util.logging} is present.
 * When {@code java.util.logging} is present, the {@code LoggingProviderImpl}
 * is {@linkplain java.util.ServiceLoader#loadInstalled(Class) installed} as
 * an internal service provider, making it possible to use {@code java.util.logging}
 * as the backend for loggers returned by the default LoggerFinder implementation.
 * <p>
 * This implementation of {@link DefaultLoggerFinder} returns instances of
 * {@link java.lang.System.Logger} which
 * delegate to a wrapped instance of {@link java.util.logging.Logger
 * java.util.logging.Logger}.
 * <br>
 * Loggers returned by this class can therefore be configured by accessing
 * their wrapped implementation through the regular {@code java.util.logging}
 * APIs - such as {@link java.util.logging.LogManager java.util.logging.LogManager}
 * and {@link java.util.logging.Logger java.util.logging.Logger}.
 *
 * @apiNote Programmers are not expected to call this class directly.
 * Instead they should rely on the static methods defined by
 * {@link java.lang.System java.lang.System}.
 * <p>
 * To replace this default
 * {@code java.util.logging} backend, an application is expected to install
 * its own {@link java.lang.System.LoggerFinder}.
 *
 * @see java.lang.System.Logger
 * @see java.lang.System.LoggerFinder
 * @see sun.util.logging.PlatformLogger.Bridge
 * @see java.lang.System
 * @see jdk.internal.logger
 * @see jdk.internal.logger
 *
 */
public final class LoggingProviderImpl extends DefaultLoggerFinder {
    static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");
    private static final LoggingPermission LOGGING_CONTROL_PERMISSION =
            new LoggingPermission("control", null);

    /**
     * Creates a new instance of LoggingProviderImpl.
     * @throws SecurityException if the calling code does not have the
     * {@code RuntimePermission("loggerFinder")}.
     */
    public LoggingProviderImpl() {
    }

    /**
     * A logger that delegates to a java.util.logging.Logger delegate.
     */
    static final class JULWrapper extends LoggerConfiguration
            implements System.Logger, PlatformLogger.Bridge,
                       PlatformLogger.ConfigurableBridge {


        private static final java.util.logging.Level[] spi2JulLevelMapping = {
                java.util.logging.Level.ALL,     // mapped from ALL
                java.util.logging.Level.FINER,   // mapped from TRACE
                java.util.logging.Level.FINE,    // mapped from DEBUG
                java.util.logging.Level.INFO,    // mapped from INFO
                java.util.logging.Level.WARNING, // mapped from WARNING
                java.util.logging.Level.SEVERE,  // mapped from ERROR
                java.util.logging.Level.OFF      // mapped from OFF
        };

        private static final java.util.logging.Level[] platform2JulLevelMapping = {
                java.util.logging.Level.ALL,     // mapped from ALL
                java.util.logging.Level.FINEST,  // mapped from FINEST
                java.util.logging.Level.FINER,   // mapped from FINER
                java.util.logging.Level.FINE,    // mapped from FINE
                java.util.logging.Level.CONFIG,  // mapped from CONFIG
                java.util.logging.Level.INFO,    // mapped from INFO
                java.util.logging.Level.WARNING, // mapped from WARNING
                java.util.logging.Level.SEVERE,  // mapped from SEVERE
                java.util.logging.Level.OFF      // mapped from OFF
        };

        private final java.util.logging.Logger julLogger;


        private JULWrapper(java.util.logging.Logger logger) {
            this.julLogger = logger;
        }

        @Override
        public String getName() {
            return julLogger.getName();
        }
        @Override
        public void log(sun.util.logging.PlatformLogger.Level level, String msg, Throwable throwable) {
            julLogger.log(toJUL(level), msg, throwable);
        }

        @Override
        public void log(sun.util.logging.PlatformLogger.Level level, String format, Object... params) {
            julLogger.log(toJUL(level), format, params);
        }

        @Override
        public void log(sun.util.logging.PlatformLogger.Level level, String msg) {
            julLogger.log(toJUL(level), msg);
        }

        @Override
        public void log(sun.util.logging.PlatformLogger.Level level, Supplier<String> msgSuppier) {
            julLogger.log(toJUL(level), msgSuppier);
        }

        @Override
        public void log(sun.util.logging.PlatformLogger.Level level, Throwable thrown, Supplier<String> msgSuppier) {
            julLogger.log(toJUL(level), thrown, msgSuppier);
        }

        @Override
        public void logrb(sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle, String key, Throwable throwable) {
            julLogger.logrb(toJUL(level), bundle, key, throwable);
        }

        @Override
        public void logrb(sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle, String key, Object... params) {
            julLogger.logrb(toJUL(level), bundle, key, params);
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass, String sourceMethod, String msg) {
            julLogger.logp(toJUL(level), sourceClass, sourceMethod, msg);
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass, String sourceMethod,
                Supplier<String> msgSupplier) {
            julLogger.logp(toJUL(level), sourceClass, sourceMethod, msgSupplier);
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass, String sourceMethod,
                String msg, Object... params) {
            julLogger.logp(toJUL(level), sourceClass, sourceMethod, msg, params);
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass, String sourceMethod,
                String msg, Throwable thrown) {
            julLogger.logp(toJUL(level), sourceClass, sourceMethod, msg, thrown);
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass, String sourceMethod,
                Throwable thrown, Supplier<String> msgSupplier) {
            julLogger.logp(toJUL(level), sourceClass, sourceMethod,
                    thrown, msgSupplier);
        }

        @Override
        public void logrb(sun.util.logging.PlatformLogger.Level level, String sourceClass, String sourceMethod,
                ResourceBundle bundle, String key, Object... params) {
            julLogger.logrb(toJUL(level), sourceClass, sourceMethod,
                    bundle, key, params);
        }

        @Override
        public void logrb(sun.util.logging.PlatformLogger.Level level, String sourceClass, String sourceMethod,
                ResourceBundle bundle, String key, Throwable thrown) {
            julLogger.logrb(toJUL(level), sourceClass, sourceMethod,
                    bundle, key, thrown);
        }

        @Override
        public  boolean isLoggable(sun.util.logging.PlatformLogger.Level level) {
            return julLogger.isLoggable(toJUL(level));
        }

        // -----------------------------------------------------------------
        // Generic methods taking a Level as parameter
        // -----------------------------------------------------------------


        @Override
        public boolean isLoggable(Level level) {
            return julLogger.isLoggable(toJUL(level));
        }

        @Override
        public void log(Level level, String msg) {
            julLogger.log(toJUL(level), msg);
        }

        @Override
        public void log(Level level,
                        Supplier<String> msgSupplier) {
            // We need to check for null here to satisfy the contract
            // of System.Logger - because the underlying implementation
            // of julLogger will check for it only if the level is
            // loggable
            Objects.requireNonNull(msgSupplier);
            julLogger.log(toJUL(level), msgSupplier);
        }

        @Override
        public void log(Level level, Object obj) {
            // We need to check for null here to satisfy the contract
            // of System.Logger - because the underlying implementation
            // of julLogger will check for it only if the level is
            // loggable
            Objects.requireNonNull(obj);
            julLogger.log(toJUL(level), () -> obj.toString());
        }

        @Override
        public void log(Level level,
                        String msg, Throwable thrown) {
            julLogger.log(toJUL(level), msg, thrown);
        }

        @Override
        public void log(Level level, Supplier<String> msgSupplier,
                        Throwable thrown) {
            // We need to check for null here to satisfy the contract
            // of System.Logger - because the underlying implementation
            // of julLogger will check for it only if the level is
            // loggable
            Objects.requireNonNull(msgSupplier);
            julLogger.log(toJUL(level), thrown, msgSupplier);
        }

        @Override
        public void log(Level level,
                        String format, Object... params) {
            julLogger.log(toJUL(level), format, params);
        }

        @Override
        public void log(Level level, ResourceBundle bundle,
                        String key, Throwable thrown) {
            julLogger.logrb(toJUL(level), bundle, key, thrown);
        }

        @Override
        public void log(Level level, ResourceBundle bundle,
                        String format, Object... params) {
            julLogger.logrb(toJUL(level), bundle, format, params);
        }

        static java.util.logging.Level toJUL(Level level) {
            if (level == null) return null;
            assert level.ordinal() < spi2JulLevelMapping.length;
            return spi2JulLevelMapping[level.ordinal()];
        }

        // ---------------------------------------------------------
        // Methods from PlatformLogger.Bridge
        // ---------------------------------------------------------

        @Override
        public boolean isEnabled() {
            return julLogger.getLevel() != java.util.logging.Level.OFF;
        }

        @Override
        public PlatformLogger.Level getPlatformLevel() {
            final java.util.logging.Level javaLevel = julLogger.getLevel();
            if (javaLevel == null) return null;
            try {
                return PlatformLogger.Level.valueOf(javaLevel.getName());
            } catch (IllegalArgumentException e) {
                return PlatformLogger.Level.valueOf(javaLevel.intValue());
            }
        }

        @Override
        public void setPlatformLevel(PlatformLogger.Level level) {
            // null is allowed here
            julLogger.setLevel(toJUL(level));
        }

        @Override
        public LoggerConfiguration getLoggerConfiguration() {
            return this;
        }

        static java.util.logging.Level toJUL(PlatformLogger.Level level) {
            // The caller will throw if null is invalid in its context.
            // There's at least one case where a null level is valid.
            if (level == null) return null;
            assert level.ordinal() < platform2JulLevelMapping.length;
            return platform2JulLevelMapping[level.ordinal()];
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof JULWrapper)
                    && obj.getClass() == this.getClass()
                    && ((JULWrapper)obj).julLogger == this.julLogger;
        }

        @Override
        public int hashCode() {
            return julLogger.hashCode();
        }

        // A JULWrapper is just a stateless thin shell over a JUL logger - so
        // for a given JUL logger, we could always return the same wrapper.
        //
        // This is an optimization which may - or may not - be worth the
        // trouble: if many classes use the same logger, and if each class
        // keeps a reference to that logger, then caching the wrapper will
        // be worthwhile. Otherwise, if each logger is only referred once,
        // then the cache will eat up more memory than would be necessary...
        //
        // Here is an example of how we could implement JULWrapper.of(...)
        // if we wanted to create at most one wrapper instance for each logger
        // instance:
        //
        //        static final WeakHashMap<JULWrapper, WeakReference<JULWrapper>>
        //                wrappers = new WeakHashMap<>();
        //
        //        static JULWrapper of(java.util.logging.Logger logger) {
        //
        //            // First access without synchronizing
        //            final JULWrapper candidate = new JULWrapper(logger);
        //            WeakReference<JULWrapper> ref = wrappers.get(candidate);
        //            JULWrapper found = ref.get();
        //
        //            // OK - we found it - lets return it.
        //            if (found != null) return found;
        //
        //            // Not found. Need to synchronize.
        //            synchronized (wrappers) {
        //                ref = wrappers.get(candidate);
        //                found = ref.get();
        //                if (found == null) {
        //                    wrappers.put(candidate, new WeakReference<>(candidate));
        //                    found = candidate;
        //                }
        //            }
        //            assert found != null;
        //            return found;
        //        }
        //
        // But given that it may end up eating more memory in the nominal case
        // (where each class that does logging has its own logger with the
        //  class name as logger name and stashes that logger away in a static
        //  field, thus making the cache redundant - as only one wrapper will
        //  ever be created anyway) - then we will simply return a new wrapper
        // for each invocation of JULWrapper.of(...) - which may
        // still prove more efficient in terms of memory consumption...
        //
        static JULWrapper of(java.util.logging.Logger logger) {
            return new JULWrapper(logger);
        }


    }

    /**
     * Creates a java.util.logging.Logger for the given module.
     * @param name the logger name.
     * @param module the module for which the logger should be created.
     * @return a Logger suitable for use in the given module.
     */
    @SuppressWarnings("removal")
    private static java.util.logging.Logger demandJULLoggerFor(final String name,
                                                               Module module) {
        final LogManager manager = LogManager.getLogManager();
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return logManagerAccess.demandLoggerFor(manager, name, module);
        } else {
            final PrivilegedAction<java.util.logging.Logger> pa =
                    () -> logManagerAccess.demandLoggerFor(manager, name, module);
            return AccessController.doPrivileged(pa, null, LOGGING_CONTROL_PERMISSION);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote The logger returned by this method can be configured through
     * its {@linkplain java.util.logging.LogManager#getLogger(String)
     * corresponding java.util.logging.Logger backend}.
     *
     * @return {@inheritDoc}
     * @throws SecurityException if the calling code doesn't have the
     * {@code RuntimePermission("loggerFinder")}.
     */
    @Override
    protected Logger demandLoggerFor(String name, Module module) {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(LOGGERFINDER_PERMISSION);
        }
        return JULWrapper.of(demandJULLoggerFor(name,module));
    }

    public static interface LogManagerAccess {
        java.util.logging.Logger demandLoggerFor(LogManager manager,
                String name, Module module);
    }

    // Hook for tests
    public static LogManagerAccess getLogManagerAccess() {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(LOGGING_CONTROL_PERMISSION);
        }
        // Triggers initialization of accessJulLogger if not set.
        if (logManagerAccess == null) LogManager.getLogManager();
        return logManagerAccess;
    }


    private static volatile LogManagerAccess logManagerAccess;
    public static void setLogManagerAccess(LogManagerAccess accesLoggers) {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(LOGGING_CONTROL_PERMISSION);
        }
        logManagerAccess = accesLoggers;
    }

}

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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.BiFunction;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.ref.WeakReference;
import java.util.Objects;
import jdk.internal.misc.VM;
import sun.util.logging.PlatformLogger;

/**
 * This class is a factory for Lazy Loggers; only system loggers can be
 * Lazy Loggers.
 */
public final class LazyLoggers {

    static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");

    private LazyLoggers() {
        throw new InternalError();
    }

    /**
     * This class is used to hold the factories that a Lazy Logger will use
     * to create (or map) its wrapped logger.
     * @param <L> {@link Logger} or a subclass of {@link Logger}.
     */
    private static final class LazyLoggerFactories<L extends Logger> {

        /**
         * A factory method to create an SPI logger.
         * Usually, this will be something like LazyLoggers::getSystemLogger.
         */
        final BiFunction<String, Module, L> loggerSupplier;


        public LazyLoggerFactories(BiFunction<String, Module, L> loggerSupplier) {
            this(Objects.requireNonNull(loggerSupplier),
                 (Void)null);
        }

        private LazyLoggerFactories(BiFunction<String, Module, L> loggerSupplier,
                          Void unused) {
            this.loggerSupplier = loggerSupplier;
        }

    }

    static interface LoggerAccessor {
        /**
         * The logger name.
         * @return The name of the logger that is / will be lazily created.
         */
        public String getLoggerName();

        /**
         * Returns the wrapped logger object.
         * @return the wrapped logger object.
         */
        public Logger wrapped();

        /**
         * A PlatformLogger.Bridge view of the wrapped logger object.
         * @return A PlatformLogger.Bridge view of the wrapped logger object.
         */
        public PlatformLogger.Bridge platform();
    }

    /**
     * The LazyLoggerAccessor class holds all the logic that delays the creation
     * of the SPI logger until such a time that the VM is booted and the logger
     * is actually used for logging.
     *
     * This class uses the services of the BootstrapLogger class to instantiate
     * temporary loggers if appropriate.
     */
    static final class LazyLoggerAccessor implements LoggerAccessor {

        // The factories that will be used to create the logger lazyly
        final LazyLoggerFactories<? extends Logger> factories;

        // We need to pass the actual caller module when creating the logger.
        private final WeakReference<Module> moduleRef;

        // The name of the logger that will be created lazyly
        final String name;
        // The plain logger SPI object - null until it is accessed for the
        // first time.
        private volatile Logger w;
        // A PlatformLogger.Bridge view of w.
        private volatile PlatformLogger.Bridge p;


        private LazyLoggerAccessor(String name,
                                   LazyLoggerFactories<? extends Logger> factories,
                                   Module module) {
            this(Objects.requireNonNull(name), Objects.requireNonNull(factories),
                    Objects.requireNonNull(module), null);
        }

        private LazyLoggerAccessor(String name,
                                   LazyLoggerFactories<? extends Logger> factories,
                                   Module module, Void unused) {
            this.name = name;
            this.factories = factories;
            this.moduleRef = new WeakReference<>(module);
        }

        /**
         * The logger name.
         * @return The name of the logger that is / will be lazily created.
         */
        @Override
        public String getLoggerName() {
            return name;
        }

        // must be called in synchronized block
        // set wrapped logger if not set
        private void setWrappedIfNotSet(Logger wrapped) {
            if (w == null) {
                w = wrapped;
            }
        }

        /**
         * Returns the logger SPI object, creating it if 'w' is still null.
         * @return the logger SPI object.
         */
        public Logger wrapped() {
            Logger wrapped = w;
            if (wrapped != null) return wrapped;
            // Wrapped logger not created yet: create it.
            // BootstrapLogger has the logic to decide whether to invoke the
            // SPI or use a temporary (BootstrapLogger or SimpleConsoleLogger)
            // logger.
            wrapped = BootstrapLogger.getLogger(this);
            synchronized(this) {
                // if w has already been in between, simply drop 'wrapped'.
                setWrappedIfNotSet(wrapped);
                return w;
            }
        }

        /**
         * A PlatformLogger.Bridge view of the wrapped logger.
         * @return A PlatformLogger.Bridge view of the wrapped logger.
         */
        public PlatformLogger.Bridge platform() {
            // We can afford to return the platform view of the previous
            // logger - if that view is not null.
            // Because that view will either be the BootstrapLogger, which
            // will redirect to the new wrapper properly, or the temporary
            // logger - which in effect is equivalent to logging something
            // just before the application initialized LogManager.
            PlatformLogger.Bridge platform = p;
            if (platform != null) return platform;
            synchronized (this) {
                if (w != null) {
                    if (p == null) p = PlatformLogger.Bridge.convert(w);
                    return p;
                }
            }
            // If we reach here it means that the wrapped logger may not
            // have been created yet: attempt to create it.
            // BootstrapLogger has the logic to decide whether to invoke the
            // SPI or use a temporary (BootstrapLogger or SimpleConsoleLogger)
            // logger.
            final Logger wrapped = BootstrapLogger.getLogger(this);
            synchronized(this) {
                // if w has already been set, simply drop 'wrapped'.
                setWrappedIfNotSet(wrapped);
                if (p == null) p = PlatformLogger.Bridge.convert(w);
                return p;
            }
        }

        /**
         * Makes this accessor release a temporary logger.
         * This method is called
         * by BootstrapLogger when JUL is the default backend and LogManager
         * is initialized, in order to replace temporary SimpleConsoleLoggers by
         * real JUL loggers. See BootstrapLogger for more details.
         * If {@code replace} is {@code true}, then this method will force
         * the accessor to eagerly recreate its wrapped logger.
         * Note: passing {@code replace=false} is no guarantee that the
         * method will not actually replace the released logger.
         * @param temporary The temporary logger too be released.
         * @param replace   Whether the released logger should be eagerly
         *                  replaced.
         */
        void release(SimpleConsoleLogger temporary, boolean replace) {
            PlatformLogger.ConfigurableBridge.LoggerConfiguration conf =
                PlatformLogger.ConfigurableBridge.getLoggerConfiguration(temporary);
            PlatformLogger.Level level = conf != null
                    ? conf.getPlatformLevel()
                    : null;
            synchronized (this) {
                if (this.w == temporary) {
                    this.w = null; this.p = null;
                }
            }
            PlatformLogger.Bridge platform =  replace || level != null
                    ? this.platform() : null;

            if (level != null) {
                conf = (platform != null && platform != temporary)
                        ? PlatformLogger.ConfigurableBridge.getLoggerConfiguration(platform)
                        : null;
                if (conf != null) conf.setPlatformLevel(level);
            }
        }

        /**
         * Replace 'w' by the real SPI logger and flush the log messages pending
         * in the temporary 'bootstrap' Logger. Called by BootstrapLogger when
         * this accessor's bootstrap logger is accessed and BootstrapLogger
         * notices that the VM is no longer booting.
         * @param bootstrap This accessor's bootstrap logger (usually this is 'w').
         */
        Logger getConcreteLogger(BootstrapLogger bootstrap) {
            assert VM.isBooted();
            synchronized(this) {
                // another thread may have already invoked flush()
                if (this.w == bootstrap) {
                    this.w = null; this.p = null;
                }
            }
            return this.wrapped();
        }

        PlatformLogger.Bridge getConcretePlatformLogger(BootstrapLogger bootstrap) {
            assert VM.isBooted();
            synchronized(this) {
                // another thread may have already invoked flush()
                if (this.w == bootstrap) {
                    this.w = null; this.p = null;
                }
            }
            return this.platform();
        }

        // Creates the wrapped logger by invoking the SPI.
        Logger createLogger() {
            final Module module = moduleRef.get();
            if (module == null) {
                throw new IllegalStateException("The module for which this logger"
                        + " was created has been garbage collected");
            }
            return this.factories.loggerSupplier.apply(name, module);
        }

        /**
         * Creates a new lazy logger accessor for the named logger. The given
         * factories will be use when it becomes necessary to actually create
         * the logger.
         * @param <T> An interface that extends {@link Logger}.
         * @param name The logger name.
         * @param factories The factories that should be used to create the
         *                  wrapped logger.
         * @return A new LazyLoggerAccessor.
         */
        public static LazyLoggerAccessor makeAccessor(String name,
                LazyLoggerFactories<? extends Logger> factories, Module module) {
                return new LazyLoggerAccessor(name, factories, module);
        }

    }

    /**
     * An implementation of {@link Logger} that redirects all calls to a wrapped
     * instance of {@code Logger}.
     */
    private static class LazyLoggerWrapper
        extends AbstractLoggerWrapper<Logger> {

        final LoggerAccessor loggerAccessor;

        public LazyLoggerWrapper(LazyLoggerAccessor loggerSinkSupplier) {
            this(Objects.requireNonNull(loggerSinkSupplier), (Void)null);
        }

        private LazyLoggerWrapper(LazyLoggerAccessor loggerSinkSupplier,
                Void unused) {
            this.loggerAccessor = loggerSinkSupplier;
        }

        @Override
        final Logger wrapped() {
            return loggerAccessor.wrapped();
        }

        @Override
        PlatformLogger.Bridge platformProxy() {
            return loggerAccessor.platform();
        }

    }

    // Do not expose this outside of this package.
    private static volatile LoggerFinder provider;
    @SuppressWarnings("removal")
    private static LoggerFinder accessLoggerFinder() {
        LoggerFinder prov = provider;
        if (prov == null) {
            // no need to lock: it doesn't matter if we call
            // getLoggerFinder() twice - since LoggerFinder already caches
            // the result.
            // This is just an optimization to avoid the cost of calling
            // doPrivileged every time.
            final SecurityManager sm = System.getSecurityManager();
            prov = sm == null ? LoggerFinder.getLoggerFinder() :
                AccessController.doPrivileged(
                        (PrivilegedAction<LoggerFinder>)LoggerFinder::getLoggerFinder);
            provider = prov;
        }
        return prov;
    }

    // Avoid using lambda here as lazy loggers could be created early
    // in the bootstrap sequence...
    private static final BiFunction<String, Module, Logger> loggerSupplier =
           new BiFunction<>() {
        @Override
        public Logger apply(String name, Module module) {
            return LazyLoggers.getLoggerFromFinder(name, module);
        }
    };

    private static final LazyLoggerFactories<Logger> factories =
           new LazyLoggerFactories<>(loggerSupplier);



    // A concrete implementation of Logger that delegates to a  System.Logger,
    // but only creates the System.Logger instance lazily when it's used for
    // the first time.
    // The JdkLazyLogger uses a LazyLoggerAccessor objects, which relies
    // on the logic embedded in BootstrapLogger to avoid loading the concrete
    // logger provider until the VM has finished booting.
    //
    private static final class JdkLazyLogger extends LazyLoggerWrapper {
        JdkLazyLogger(String name, Module module) {
            this(LazyLoggerAccessor.makeAccessor(name, factories, module),
                 (Void)null);
        }
        private JdkLazyLogger(LazyLoggerAccessor holder, Void unused) {
            super(holder);
        }
    }

    /**
     * Gets a logger from the LoggerFinder. Creates the actual concrete
     * logger.
     * @param name    name of the logger
     * @param module  module on behalf of which the logger is created
     * @return  The logger returned by the LoggerFinder.
     */
    @SuppressWarnings("removal")
    static Logger getLoggerFromFinder(String name, Module module) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return accessLoggerFinder().getLogger(name, module);
        } else {
            return AccessController.doPrivileged((PrivilegedAction<Logger>)
                    () -> {return accessLoggerFinder().getLogger(name, module);},
                    null, LOGGERFINDER_PERMISSION);
        }
    }

    /**
     * Returns a (possibly lazy) Logger for the caller.
     *
     * @param name the logger name
     * @param module The module on behalf of which the logger is created.
     *               If the module is not loaded from the Boot ClassLoader,
     *               the LoggerFinder is accessed and the logger returned
     *               by {@link LoggerFinder#getLogger(java.lang.String, java.lang.Module)}
     *               is returned to the caller directly.
     *               Otherwise, the logger returned by
     *               {@link #getLazyLogger(java.lang.String, java.lang.Module)}
     *               is returned to the caller.
     *
     * @return  a (possibly lazy) Logger instance.
     */
    public static final Logger getLogger(String name, Module module) {
        if (DefaultLoggerFinder.isSystem(module)) {
            return getLazyLogger(name, module);
        } else {
            return getLoggerFromFinder(name, module);
        }
    }

    /**
     * Returns a (possibly lazy) Logger suitable for system classes.
     * Whether the returned logger is lazy or not depend on the result
     * returned by {@link BootstrapLogger#useLazyLoggers()}.
     *
     * @param name the logger name
     * @param module the module on behalf of which the logger is created.
     * @return  a (possibly lazy) Logger instance.
     */
    public static final Logger getLazyLogger(String name, Module module) {

        // BootstrapLogger has the logic to determine whether a LazyLogger
        // should be used. Usually, it is worth it only if:
        //   - the VM is not yet booted
        //   - or, the backend is JUL and there is no configuration
        //   - or, the backend is a custom backend, as we don't know what
        //     that is going to load...
        // So if for instance the VM is booted and we use JUL with a custom
        // configuration, we're not going to delay the creation of loggers...
        final boolean useLazyLogger = BootstrapLogger.useLazyLoggers();
        if (useLazyLogger) {
            return new JdkLazyLogger(name, module);
        } else {
            // Directly invoke the LoggerFinder.
            return getLoggerFromFinder(name, module);
        }
    }

}

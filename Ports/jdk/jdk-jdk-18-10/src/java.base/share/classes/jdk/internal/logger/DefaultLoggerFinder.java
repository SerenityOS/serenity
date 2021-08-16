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

import jdk.internal.misc.VM;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.Objects;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * Internal Service Provider Interface (SPI) that makes it possible to use
 * {@code java.util.logging} as backend when the {@link
 * sun.util.logging.internal.LoggingProviderImpl
 * sun.util.logging.internal.LoggingProviderImpl} is present.
 * <p>
 * The JDK default implementation of the {@link LoggerFinder} will
 * attempt to locate and load an {@linkplain
 * java.util.ServiceLoader#loadInstalled(java.lang.Class) installed}
 * implementation of the {@code DefaultLoggerFinder}. If {@code java.util.logging}
 * is present, this will usually resolve to an instance of {@link
 * sun.util.logging.internal.LoggingProviderImpl sun.util.logging.internal.LoggingProviderImpl}.
 * Otherwise, if no concrete service provider is declared for
 * {@code DefaultLoggerFinder}, the default implementation provided by this class
 * will be used.
 * <p>
 * When the {@link sun.util.logging.internal.LoggingProviderImpl
 * sun.util.logging.internal.LoggingProviderImpl} is not present then the
 * default implementation provided by this class is to use a simple logger
 * that will log messages whose level is INFO and above to the console.
 * These simple loggers are not configurable.
 * <p>
 * When configuration is needed, an application should either link with
 * {@code java.util.logging} - and use the {@code java.util.logging} for
 * configuration, or link with {@link LoggerFinder another implementation}
 * of the {@link LoggerFinder}
 * that provides the necessary configuration.
 *
 * @apiNote Programmers are not expected to call this class directly.
 * Instead they should rely on the static methods defined by {@link
 * java.lang.System java.lang.System} or {@link sun.util.logging.PlatformLogger
 * sun.util.logging.PlatformLogger}.
 *
 * @see java.lang.System.LoggerFinder
 * @see jdk.internal.logger
 * @see sun.util.logging.internal
 *
 */
public class DefaultLoggerFinder extends LoggerFinder {

    static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");

    /**
     * Creates a new instance of DefaultLoggerFinder.
     * @throws SecurityException if the calling code does not have the
     * {@code RuntimePermission("loggerFinder")}
     */
    protected DefaultLoggerFinder() {
        this(checkPermission());
    }

    private DefaultLoggerFinder(Void unused) {
        // nothing to do.
    }

    private static Void checkPermission() {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(LOGGERFINDER_PERMISSION);
        }
        return null;
    }

    // SharedLoggers is a default cache of loggers used when JUL is not
    // present - in that case we use instances of SimpleConsoleLogger which
    // cannot be directly configure through public APIs.
    //
    // We can therefore afford to simply maintain two domains - one for the
    // system, and one for the application.
    //
    static final class SharedLoggers {
        private final Map<String, Reference<Logger>> loggers =
                new HashMap<>();
        private final ReferenceQueue<Logger> queue = new ReferenceQueue<>();

        synchronized Logger get(Function<String, Logger> loggerSupplier, final String name) {
            Reference<? extends Logger> ref = loggers.get(name);
            Logger w = ref == null ? null :  ref.get();
            if (w == null) {
                w = loggerSupplier.apply(name);
                loggers.put(name, new WeakReference<>(w, queue));
            }

            // Remove stale mapping...
            Collection<Reference<Logger>> values = null;
            while ((ref = queue.poll()) != null) {
                if (values == null) values = loggers.values();
                values.remove(ref);
            }
            return w;
        }

        static final SharedLoggers system = new SharedLoggers();
        static final SharedLoggers application = new SharedLoggers();
    }

    @SuppressWarnings("removal")
    public static boolean isSystem(Module m) {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Boolean run() {
                // returns true if moduleCL is the platform class loader
                // or one of its ancestors.
                return VM.isSystemDomainLoader(m.getClassLoader());
            }
        });
    }

    @Override
    public final Logger getLogger(String name,  Module module) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(module, "module");
        checkPermission();
        return demandLoggerFor(name, module);
    }

    @Override
    public final Logger getLocalizedLogger(String name, ResourceBundle bundle,
                                           Module module) {
        return super.getLocalizedLogger(name, bundle, module);
    }

    /**
     * Returns a {@link Logger logger} suitable for use within the
     * given {@code module}.
     *
     * @implSpec The default implementation for this method is to return a
     *    simple logger that will print all messages of INFO level and above
     *    to the console. That simple logger is not configurable.
     *
     * @param name The name of the logger.
     * @param module The module on behalf of which the logger is created.
     * @return A {@link Logger logger} suitable for the application usage.
     * @throws SecurityException if the calling code does not have the
     * {@code RuntimePermission("loggerFinder")}.
     */
    protected Logger demandLoggerFor(String name, Module module) {
        checkPermission();
        if (isSystem(module)) {
            return SharedLoggers.system.get(SimpleConsoleLogger::makeSimpleLogger, name);
        } else {
            return SharedLoggers.application.get(SimpleConsoleLogger::makeSimpleLogger, name);
        }
    }

}

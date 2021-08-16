/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     8140364
 * @author  danielfuchs
 * @summary  JDK implementation specific unit test for JDK internal artifacts.
 *           This test tests all the public API methods defined in the {@link
 *           java.lang.System.Logger} interface, as well as all the JDK
 *           internal methods defined in the
 *           {@link sun.util.logging.PlatformLogger.Bridge}
 *           interface, with loggers returned by  {@link
 *           java.lang.System.LoggerFinder#getLogger(java.lang.String, java.lang.Class)}
 *           and {@link java.lang.System.LoggerFinder#getLocalizedLogger(java.lang.String,
 *           java.util.ResourceBundle, java.lang.Class)}
 *           (using both a null resource bundle and a non null resource bundle).
 *           It calls both the {@link java.lang.System} factory methods and
 *           {@link jdk.internal.logger.LazyLoggers} to obtains those loggers,
 *           and configure them with all possible known levels.
 * @modules java.base/java.lang:open
 *          java.base/sun.util.logging
 *          java.base/jdk.internal.logger
 *          java.logging/sun.util.logging.internal
 * @build LoggerFinderBackendTest SystemClassLoader
 * @run  main/othervm -Djava.system.class.loader=SystemClassLoader -Dtest.logger.hidesProvider=true LoggerFinderBackendTest
 * @run  main/othervm -Djava.system.class.loader=SystemClassLoader -Dtest.logger.hidesProvider=false LoggerFinderBackendTest
 */


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.lang.System.LoggerFinder;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import sun.util.logging.PlatformLogger.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import sun.util.logging.internal.LoggingProviderImpl;

/**
 * @author danielfuchs
 */
public class LoggerFinderBackendTest {

    // whether the implementation of Logger try to do a best
    // effort for logp... If the provider is not hidden, then
    // the logp() implementation comes from LoggerWrapper - which does a
    // best effort. Otherwise, it comes from the default provider
    // which does support logp.
    static final boolean BEST_EFFORT_FOR_LOGP =
            !Boolean.getBoolean("test.logger.hidesProvider");
    static final boolean VERBOSE = false;

    static final Class<java.lang.System.Logger> spiLoggerClass =
            java.lang.System.Logger.class;
    static final Class<java.lang.System.Logger> jdkLoggerClass =
            java.lang.System.Logger.class;
    static final Class<sun.util.logging.PlatformLogger.Bridge> bridgeLoggerClass =
            sun.util.logging.PlatformLogger.Bridge.class;

    /** Use to retrieve the log records that were produced by the JUL backend */
    static class LoggerTesterHandler extends Handler {
        public final List<LogRecord> records =
                Collections.synchronizedList(new ArrayList<>());

        @Override
        public void publish(LogRecord record) {
            record.getSourceClassName(); record.getSourceMethodName();
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
            records.clear();
        }

        public void reset() {
            records.clear();
        }
    }

    /** The {@link LoggerTesterHandler} handler is added to the root logger. */
    static final LoggerTesterHandler handler = new LoggerTesterHandler();
    static {
        for (Handler h : Logger.getLogger("").getHandlers()) {
            if (h instanceof ConsoleHandler) {
                Logger.getLogger("").removeHandler(h);
            }
        }
        Logger.getLogger("").addHandler(handler);
    }

    /**
     * A resource handler parameter that will be used when calling out the
     * logrb-like methods - as well as when calling the level-specific
     * methods that take a ResourceBundle parameter.
     */
    public static class ResourceBundeParam extends ResourceBundle {
        Map<String, String> map = Collections.synchronizedMap(new LinkedHashMap<>());
        @Override
        protected Object handleGetObject(String key) {
            map.putIfAbsent(key, "${"+key+"}");
            return map.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(new LinkedHashSet<>(map.keySet()));
        }

    }

    final static ResourceBundle bundleParam =
            ResourceBundle.getBundle(ResourceBundeParam.class.getName());

    /**
     * A resource handler parameter that will be used when creating localized
     * loggers by calling {@link
     * LoggerFinder#getLocalizedLogger(java.lang.String, java.util.ResourceBundle, java.lang.Class)}.
     */
    public static class ResourceBundeLocalized extends ResourceBundle {
        Map<String, String> map = Collections.synchronizedMap(new LinkedHashMap<>());
        @Override
        protected Object handleGetObject(String key) {
            map.putIfAbsent(key, "Localized:${"+key+"}");
            return map.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(new LinkedHashSet<>(map.keySet()));
        }

    }

    /**
     * The Levels enum is used to call all the level-specific methods on
     * a logger instance. To minimize the amount of code it uses reflection
     * to do so.
     */
    static Lookup lookup = MethodHandles.lookup();
    public enum Levels {
        /** Used to call all forms of Logger.log?(SEVERE, ...) */
        SEVERE("severe", bridgeLoggerClass, Level.SEVERE, null, "error", false),
        /** Used to call all forms of Logger.log?(WARNING,...) */
        WARNING("warning", bridgeLoggerClass, Level.WARNING, "warning", "warning", false),
        /** Used to call all forms of Logger.log?(INFO,...) */
        INFO("info", bridgeLoggerClass, Level.INFO, "info", "info", false),
        /** Used to call all forms of Logger.log?(CONFIG,...) */
        CONFIG("config", bridgeLoggerClass, Level.CONFIG, null, "debug", false),
        /** Used to call all forms of Logger.log?(FINE,...) */
        FINE("fine", bridgeLoggerClass, Level.FINE, null, "debug", false),
        /** Used to call all forms of Logger.log?(FINER,...) */
        FINER("finer", bridgeLoggerClass, Level.FINER, null, "trace", false),
        /** Used to call all forms of Logger.log?(FINEST,...) */
        FINEST("finest", bridgeLoggerClass, Level.FINEST, null, "trace", false),
        ;
        public final String method;  // The name of the level-specific method to call
        public final Class<?> definingClass; // which interface j.u.logger.Logger or j.u.logging.spi.Logger defines it
        public final Level platformLevel; // The platform Level it will be mapped to in Jul when Jul is the backend
        public final String jdkExtensionToJUL; // The name of the method called on the JUL logger when JUL is the backend
        public final String julToJdkExtension; // The name of the method called in the jdk extension by the default impl in jdk.internal.logging.Logger
        public final String enableMethod; // The name of the isXxxxEnabled method
        public final boolean hasSpecificIsEnabled;
        Levels(String method, Class<?> definingClass, Level defaultMapping,
                String jdkExtensionToJUL, String julToJdkExtension,
                boolean hasSpecificIsEnabled) {
            this.method = method;
            this.definingClass = definingClass;
            this.platformLevel = defaultMapping;
            this.jdkExtensionToJUL = jdkExtensionToJUL;
            this.julToJdkExtension = julToJdkExtension;
            this.hasSpecificIsEnabled = hasSpecificIsEnabled;
            if (hasSpecificIsEnabled) {
                this.enableMethod = "is" + method.substring(0,1).toUpperCase()
                    + method.substring(1) + "Enabled";
            } else {
                this.enableMethod = "isLoggable";
            }
        }

        /*
         * calls this level specific method - e.g. if this==INFO: logger.info(msg);
         */
        public void level(Object logger, String msg) {
            MethodType mt = MethodType.methodType(void.class, Level.class, String.class);
            invoke("log", logger, mt, platformLevel, msg);
        }

        /*
         * calls this level specific method - e.g. if this==INFO: logger.info(msgSupplier);
         */
        public void level(Object logger, Supplier<String> msgSupplier) {
            MethodType mt = MethodType.methodType(void.class,  Level.class, Supplier.class);
            invoke("log", logger, mt, platformLevel, msgSupplier);
        }

        /*
         * calls this level specific method - e.g. if this==INFO: logger.info(msg, params);
         */
        public void level(Object logger, String msg, Object... params) {
            MethodType mt = MethodType.methodType(void.class,  Level.class, String.class,
                    Object[].class);
            invoke("log", logger, mt, platformLevel, msg, params);
        }

        /*
         * calls this level specific method - e.g. if this==INFO: logger.info(msg, thrown);
         */
        public void level(Object logger, String msg, Throwable thrown) {
            MethodType mt = MethodType.methodType(void.class,  Level.class, String.class,
                    Throwable.class);
            invoke("log", logger, mt, platformLevel, msg, thrown);
        }

        /*
         * calls this level specific method - e.g. if this==INFO: logger.info(msgSupplier, thrown);
         */
        public void level(Object logger, Supplier<String> msgSupplier, Throwable thrown) {
            MethodType mt = MethodType.methodType(void.class,  Level.class,
                     Throwable.class, Supplier.class);
            invoke("log", logger, mt, platformLevel, thrown, msgSupplier);
        }

        /*
         * calls this level specific method - e.g. if this==INFO: logger.info(bundle, msg);
         */
        public void level(Object logger, String msg, ResourceBundle bundle) {
            MethodType mt = MethodType.methodType(void.class, Level.class,
                    ResourceBundle.class, String.class, Object[].class);
            invoke("logrb", logger, mt, platformLevel, bundle, msg, null);
        }

        public void level(Object logger, String msg, ResourceBundle bundle,
                Object... params) {
            MethodType mt = MethodType.methodType(void.class, Level.class,
                    ResourceBundle.class, String.class, Object[].class);
            invoke("logrb", logger, mt, platformLevel, bundle, msg, params);
        }

        public void level(Object logger, String msg, ResourceBundle bundle,
                Throwable thrown) {
            MethodType mt = MethodType.methodType(void.class, Level.class,
                    ResourceBundle.class, String.class, Throwable.class);
            invoke("logrb", logger, mt, platformLevel, bundle, msg, thrown);
        }

        public boolean isEnabled(Object logger) {
            try {
                if (hasSpecificIsEnabled) {
                    MethodType mt = MethodType.methodType(boolean.class);
                    final MethodHandle handle = lookup.findVirtual(definingClass,
                        enableMethod, mt).bindTo(logger);
                    return Boolean.class.cast(handle.invoke());
                } else {
                    MethodType mt = MethodType.methodType(boolean.class,
                        Level.class);
                    final MethodHandle handle = lookup.findVirtual(definingClass,
                        enableMethod, mt).bindTo(logger);
                    return Boolean.class.cast(handle.invoke(platformLevel));
                }
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        private void invoke(String method, Object logger, MethodType mt, Object... args) {
            try {
                final int last = mt.parameterCount()-1;
                boolean isVarargs = mt.parameterType(last).isArray();
                final MethodHandle handle = lookup.findVirtual(definingClass,
                        method, mt).bindTo(logger);

                final StringBuilder builder = new StringBuilder();
                builder.append(logger.getClass().getSimpleName()).append('.')
                        .append(method).append('(');
                String sep = "";
                int offset = 0;
                Object[] params = args;
                for (int i=0; (i-offset) < params.length; i++) {
                    if (isVarargs && i == last) {
                        offset = last;
                        params = (Object[])args[i];
                        if (params == null) break;
                    }
                    Object p = params[i - offset];
                    String quote = (p instanceof String) ? "\"" : "";
                    builder.append(sep).append(quote).append(p).append(quote);
                    sep = ", ";
                }
                builder.append(')');
                if (verbose) {
                    System.out.println(builder);
                }
                handle.invokeWithArguments(args);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

    };

    static interface Checker<LogResult, L> extends BiFunction<LogResult, L, Void> {}
    static interface JdkLogTester
            extends BiFunction<sun.util.logging.PlatformLogger.Bridge, Level, Void> {}
    static interface SpiLogTester
            extends BiFunction<java.lang.System.Logger, java.lang.System.Logger.Level, Void> {}

    static interface MethodInvoker<LOGGER, LEVEL> {
        public void logX(LOGGER logger, LEVEL level, Object... args);
    }

    public enum JdkLogMethodInvoker
           implements MethodInvoker<sun.util.logging.PlatformLogger.Bridge, Level> {
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, String, Object...)};
         **/
        LOG_STRING_PARAMS("log", MethodType.methodType(void.class,
                Level.class, String.class, Object[].class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, String, Throwable)};
         **/
        LOG_STRING_THROWN("log", MethodType.methodType(void.class,
                Level.class, String.class, Throwable.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, Supplier<String>)};
         **/
        LOG_SUPPLIER("log", MethodType.methodType(void.class,
                Level.class, Supplier.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, Throwable, Supplier<String>)};
         **/
        LOG_SUPPLIER_THROWN("log", MethodType.methodType(void.class,
                Level.class, Throwable.class, Supplier.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logp(Level, String, String, String)};
         **/
        LOGP_STRING("logp", MethodType.methodType(void.class,
                Level.class, String.class, String.class, String.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logp(Level, String, String, String, Object...)};
         **/
        LOGP_STRING_PARAMS("logp", MethodType.methodType(void.class,
                Level.class, String.class, String.class, String.class, Object[].class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logp(Level, String, String, String, Throwable)};
         **/
        LOGP_STRING_THROWN("logp", MethodType.methodType(void.class,
                Level.class, String.class, String.class, String.class, Throwable.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logp(Level, String, String, Supplier<String>)};
         **/
        LOGP_SUPPLIER("logp", MethodType.methodType(void.class,
                Level.class, String.class, String.class, Supplier.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logp(Level, String, String, Throwable, Supplier<String>)};
         **/
        LOGP_SUPPLIER_THROWN("logp", MethodType.methodType(void.class,
                Level.class, String.class, String.class,
                Throwable.class, Supplier.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logrb(Level, ResourceBundle, String, Object...)};
         **/
        LOGRB_STRING_PARAMS("logrb", MethodType.methodType(void.class,
                Level.class, ResourceBundle.class, String.class, Object[].class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logrb(Level, ResourceBundle, String, Throwable)};
         **/
        LOGRB_STRING_THROWN("logrb", MethodType.methodType(void.class,
                Level.class, ResourceBundle.class, String.class, Throwable.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logrb(Level, String, String, ResourceBundle, String, Object...)};
         **/
        LOGRBP_STRING_PARAMS("logrb", MethodType.methodType(void.class,
                Level.class, String.class, String.class, ResourceBundle.class,
                String.class, Object[].class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logrb(Level, String, String, ResourceBundle, String, Throwable)};
         **/
        LOGRBP_STRING_THROWN("logrb", MethodType.methodType(void.class,
                Level.class, String.class, String.class, ResourceBundle.class,
                String.class, Throwable.class)),
        ;
        final MethodType mt;
        final String method;
        JdkLogMethodInvoker(String method, MethodType mt) {
            this.mt = mt;
            this.method = method;
        }
        Object[] makeArgs(Level level, Object... rest) {
            List<Object> list = new ArrayList<>(rest == null ? 1 : rest.length + 1);
            list.add(level);
            if (rest != null) {
                list.addAll(Arrays.asList(rest));
            }
            return list.toArray(new Object[list.size()]);
        }

        @Override
        public void logX(sun.util.logging.PlatformLogger.Bridge logger, Level level, Object... args) {
            try {
                MethodHandle handle = lookup.findVirtual(bridgeLoggerClass,
                        method, mt).bindTo(logger);
                final int last = mt.parameterCount()-1;
                boolean isVarargs = mt.parameterType(last).isArray();

                args = makeArgs(level, args);

                final StringBuilder builder = new StringBuilder();
                builder.append(logger.getClass().getSimpleName()).append('.')
                        .append(this.method).append('(');
                String sep = "";
                int offset = 0;
                Object[] params = args;
                for (int i=0; (i-offset) < params.length; i++) {
                    if (isVarargs && i == last) {
                        offset = last;
                        params = (Object[])args[i];
                        if (params == null) break;
                    }
                    Object p = params[i - offset];
                    String quote = (p instanceof String) ? "\"" : "";
                    p = p instanceof Level ? "Level."+p : p;
                    builder.append(sep).append(quote).append(p).append(quote);
                    sep = ", ";
                }
                builder.append(')');
                if (verbose) System.out.println(builder);
                handle.invokeWithArguments(args);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public enum SpiLogMethodInvoker implements MethodInvoker<java.lang.System.Logger,
            java.lang.System.Logger.Level> {
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, String, Object...)};
         **/
        LOG_STRING_PARAMS("log", MethodType.methodType(void.class,
                java.lang.System.Logger.Level.class, String.class, Object[].class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, String, Throwable)};
         **/
        LOG_STRING_THROWN("log", MethodType.methodType(void.class,
                java.lang.System.Logger.Level.class, String.class, Throwable.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, Supplier<String>)};
         **/
        LOG_SUPPLIER("log", MethodType.methodType(void.class,
                java.lang.System.Logger.Level.class, Supplier.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, Throwable, Supplier<String>)};
         **/
        LOG_SUPPLIER_THROWN("log", MethodType.methodType(void.class,
                java.lang.System.Logger.Level.class, Supplier.class, Throwable.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#log(Level, Supplier<String>)};
         **/
        LOG_OBJECT("log", MethodType.methodType(void.class,
                java.lang.System.Logger.Level.class, Object.class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logrb(Level, ResourceBundle, String, Object...)};
         **/
        LOGRB_STRING_PARAMS("log", MethodType.methodType(void.class,
                java.lang.System.Logger.Level.class, ResourceBundle.class,
                String.class, Object[].class)),
        /**
         * Tests {@link
         * jdk.internal.logging.Logger#logrb(Level, ResourceBundle, String, Throwable)};
         **/
        LOGRB_STRING_THROWN("log", MethodType.methodType(void.class,
                java.lang.System.Logger.Level.class, ResourceBundle.class,
                String.class, Throwable.class)),
        ;
        final MethodType mt;
        final String method;
        SpiLogMethodInvoker(String method, MethodType mt) {
            this.mt = mt;
            this.method = method;
        }
        Object[] makeArgs(java.lang.System.Logger.Level level, Object... rest) {
            List<Object> list = new ArrayList<>(rest == null ? 1 : rest.length + 1);
            list.add(level);
            if (rest != null) {
                list.addAll(Arrays.asList(rest));
            }
            return list.toArray(new Object[list.size()]);
        }

        @Override
        public void logX(java.lang.System.Logger logger,
                java.lang.System.Logger.Level level, Object... args) {
            try {
                MethodHandle handle = lookup.findVirtual(spiLoggerClass,
                        method, mt).bindTo(logger);
                final int last = mt.parameterCount()-1;
                boolean isVarargs = mt.parameterType(last).isArray();

                args = makeArgs(level, args);

                final StringBuilder builder = new StringBuilder();
                builder.append(logger.getClass().getSimpleName()).append('.')
                        .append(this.method).append('(');
                String sep = "";
                int offset = 0;
                Object[] params = args;
                for (int i=0; (i-offset) < params.length; i++) {
                    if (isVarargs && i == last) {
                        offset = last;
                        params = (Object[])args[i];
                        if (params == null) break;
                    }
                    Object p = params[i - offset];
                    String quote = (p instanceof String) ? "\"" : "";
                    p = p instanceof Level ? "Level."+p : p;
                    builder.append(sep).append(quote).append(p).append(quote);
                    sep = ", ";
                }
                builder.append(')');
                if (verbose) System.out.println(builder);
                handle.invokeWithArguments(args);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public abstract static class BackendTester<BackendRecord> {
        static final Level[] levelMap = {Level.ALL, Level.FINER, Level.FINE,
            Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF};

        abstract class BackendAdaptor {
            public abstract String getLoggerName(BackendRecord res);
            public abstract Object getLevel(BackendRecord res);
            public abstract String getMessage(BackendRecord res);
            public abstract String getSourceClassName(BackendRecord res);
            public abstract String getSourceMethodName(BackendRecord res);
            public abstract Throwable getThrown(BackendRecord res);
            public abstract ResourceBundle getResourceBundle(BackendRecord res);
            public abstract void setLevel(java.lang.System.Logger logger,
                    Level level);
            public abstract void setLevel(java.lang.System.Logger logger,
                    java.lang.System.Logger.Level level);
            public abstract List<BackendRecord> getBackendRecords();
            public abstract void resetBackendRecords();
            public boolean shouldBeLoggable(Levels level, Level loggerLevel) {
                final Level logLevel = level.platformLevel;
                return shouldBeLoggable(logLevel, loggerLevel);
            }
            public boolean shouldBeLoggable(Level logLevel, Level loggerLevel) {
                return loggerLevel.intValue() != Level.OFF.intValue()
                        && logLevel.intValue() >= loggerLevel.intValue();
            }
            public boolean shouldBeLoggable(java.lang.System.Logger.Level logLevel,
                    java.lang.System.Logger.Level loggerLevel) {
                return loggerLevel != java.lang.System.Logger.Level.OFF
                        && logLevel.ordinal() >= loggerLevel.ordinal();
            }
            public boolean isLoggable(java.lang.System.Logger logger, Level l) {
                return bridgeLoggerClass.cast(logger).isLoggable(l);
            }
            public String getCallerClassName(Levels level, String clazz) {
                return clazz != null ? clazz : Levels.class.getName();
            }
            public String getCallerClassName(MethodInvoker<?,?> logMethod,
                   String clazz) {
                return clazz != null ? clazz : logMethod.getClass().getName();
            }
            public String getCallerMethodName(Levels level, String method) {
                return method != null ? method : "invoke";
            }
            public String getCallerMethodName(MethodInvoker<?,?> logMethod,
                    String method) {
                return method != null ? method : "logX";
            }
            public Object getMappedLevel(Object level) {
                return level;
            }

            public Level toJUL(java.lang.System.Logger.Level level) {
                return levelMap[level.ordinal()];
            }
        }

        public final boolean isSystem;
        public final Class<? extends java.lang.System.Logger> restrictedTo;
        public final ResourceBundle localized;
        public BackendTester(boolean isSystem) {
            this(isSystem,null,null);
        }
        public BackendTester(boolean isSystem, ResourceBundle localized) {
            this(isSystem,null,localized);
        }
        public BackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo) {
            this(isSystem, restrictedTo, null);
        }
        public BackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo,
                ResourceBundle localized) {
            this.isSystem = isSystem;
            this.restrictedTo = restrictedTo;
            this.localized = localized;
        }

        public java.lang.System.Logger convert(java.lang.System.Logger logger) {
            return logger;
        }

        public static Level[] LEVELS = {
            Level.OFF,
            Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG,
            Level.FINE, Level.FINER, Level.FINEST,
            Level.ALL
        };

        abstract BackendAdaptor adaptor();

        protected void checkRecord(Levels test, BackendRecord res, String loggerName,
                Level level, String msg, String className, String methodName,
                Throwable thrown, ResourceBundle bundle, Object... params) {
            checkRecord(test, res, loggerName, level, ()->msg, className,
                    methodName, thrown, bundle, params);

        }
        protected void checkRecord(Levels test, BackendRecord res, String loggerName,
                Level level, Supplier<String> msg, String className, String methodName,
                Throwable thrown, ResourceBundle bundle, Object... params) {
            checkRecord(test.method, res, loggerName, level, msg,
                    className, methodName, thrown, bundle, params);
        }
        protected <L> void checkRecord(String logMethod, BackendRecord res, String loggerName,
                L level, Supplier<String> msg, String className, String methodName,
                Throwable thrown, ResourceBundle bundle, Object... params) {
            final BackendAdaptor analyzer = adaptor();
            if (! Objects.equals(analyzer.getLoggerName(res), loggerName)) {
                throw new RuntimeException(logMethod+": expected logger name "
                        + loggerName + " got " + analyzer.getLoggerName(res));
            }
            if (!Objects.equals(analyzer.getLevel(res), analyzer.getMappedLevel(level))) {
                throw new RuntimeException(logMethod+": expected level "
                        + analyzer.getMappedLevel(level) + " got " + analyzer.getLevel(res));
            }
            if (!Objects.equals(analyzer.getMessage(res), msg.get())) {
                throw new RuntimeException(logMethod+": expected message \""
                        + msg.get() + "\" got \"" + analyzer.getMessage(res) +"\"");
            }
            if (!Objects.equals(analyzer.getSourceClassName(res), className)) {
                throw new RuntimeException(logMethod
                        + ": expected class name \"" + className
                        + "\" got \"" + analyzer.getSourceClassName(res) +"\"");
            }
            if (!Objects.equals(analyzer.getSourceMethodName(res), methodName)) {
                throw new RuntimeException(logMethod
                        + ": expected method name \"" + methodName
                        + "\" got \"" + analyzer.getSourceMethodName(res) +"\"");
            }
            final Throwable thrownRes = analyzer.getThrown(res);
            if (!Objects.equals(thrownRes, thrown)) {
                throw new RuntimeException(logMethod
                        + ": expected throwable \"" + thrown
                        + "\" got \"" + thrownRes + "\"");
            }
            if (!Objects.equals(analyzer.getResourceBundle(res), bundle)) {
                throw new RuntimeException(logMethod
                        + ": expected bundle \"" + bundle
                        + "\" got \"" + analyzer.getResourceBundle(res) +"\"");
            }
        }

        public void testLevel(Levels level, java.lang.System.Logger logger,
                String msg) {
            Runnable test = () -> level.level(logger, msg);
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord(level, res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(level, Levels.class.getName()),
                            adaptor().getCallerMethodName(level, "invoke"),
                            null, localized);
                return null;
            };
            test("msg", level, logger, test, check);
        }

        public void testLevel(Levels level, java.lang.System.Logger logger,
                String msg, Object... params) {
            Runnable test = () -> level.level(logger, msg, (Object[])params);
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord(level, res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(level, Levels.class.getName()),
                            adaptor().getCallerMethodName(level, "invoke"),
                            null, localized, (Object[])params);
                return null;
            };
            test("msg, params", level, logger, test, check);
        }

        public void testLevel(Levels level, java.lang.System.Logger logger,
                String msg, Throwable thrown) {
            Runnable test = () -> level.level(logger, msg, thrown);
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord(level, res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(level, Levels.class.getName()),
                            adaptor().getCallerMethodName(level, "invoke"),
                            thrown, localized);
                return null;
            };
            test("msg, thrown", level, logger, test, check);
        }

        public void testLevel(Levels level, java.lang.System.Logger logger,
                Supplier<String> msg) {
            Runnable test = () -> level.level(logger, msg);
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord(level, res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(level, Levels.class.getName()),
                            adaptor().getCallerMethodName(level, "invoke"),
                            null, null);
                return null;
            };
            test("msgSupplier", level, logger, test, check);
        }

        public void testLevel(Levels level, java.lang.System.Logger logger,
                Supplier<String> msg, Throwable thrown) {
            Runnable test = () -> level.level(logger, msg, thrown);
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord(level, res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(level, Levels.class.getName()),
                            adaptor().getCallerMethodName(level, "invoke"),
                            thrown, null);
                return null;
            };
            test("throw, msgSupplier", level, logger, test, check);
        }

        public void testLevel(Levels level, java.lang.System.Logger logger,
                String msg, ResourceBundle bundle) {
            Runnable test = () -> level.level(logger, msg, bundle);
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord(level, res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(level, Levels.class.getName()),
                            adaptor().getCallerMethodName(level, "invoke"),
                            null, bundle);
                return null;
            };
            test("bundle, msg", level, logger, test, check);
        }

        public void testLevel(Levels level, java.lang.System.Logger logger,
                String msg, ResourceBundle bundle, Object... params) {
            Runnable test = () -> level.level(logger, msg, bundle, (Object[])params);
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord(level, res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(level, Levels.class.getName()),
                            adaptor().getCallerMethodName(level, "invoke"),
                            null, bundle, (Object[])params);
                return null;
            };
            test("bundle, msg, params", level, logger, test, check);
        }

        public void testLevel(Levels level, java.lang.System.Logger logger,
                String msg, ResourceBundle bundle, Throwable thrown) {
            Runnable test = () -> level.level(logger, msg, bundle, thrown);
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord(level, res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(level, Levels.class.getName()),
                            adaptor().getCallerMethodName(level, "invoke"),
                            thrown, bundle);
                return null;
            };
            test("bundle, msg, throwable", level, logger, test, check);
        }

        // System.Logger
        public void testSpiLog(java.lang.System.Logger logger, String msg) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOG_STRING_PARAMS,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOG_STRING_PARAMS,
                                    "logX"), null, localized);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOG_STRING_PARAMS.logX(x, level, msg, (Object[])null);
                return null;
            };
            Function<String, String> nameProducer = (l) -> "log(Level." + l + ", \"" + msg + "\")";
            testSpiLog(logger, tester, check, nameProducer);
        }

        public void testSpiLog(java.lang.System.Logger logger,
                ResourceBundle bundle, String msg) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOGRB_STRING_PARAMS,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOGRB_STRING_PARAMS,
                                    "logX"), null, bundle);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOGRB_STRING_PARAMS.logX(x, level, bundle, msg, (Object[])null);
                return null;
            };
            Function<String, String> nameProducer = (l) -> "log(Level." + l
                    + ", bundle, \"" + msg + "\")";
            testSpiLog(logger, tester, check, nameProducer);
        }

        public void testSpiLog(java.lang.System.Logger logger, String msg, Object... params) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOG_STRING_PARAMS,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOG_STRING_PARAMS,
                                    "logX"), null, localized, params);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOG_STRING_PARAMS.logX(x, level, msg, params);
                return null;
            };
            Function<String, String> nameProducer = (l) -> "log(Level." + l + ", \"" + msg + "\", params...)";
            testSpiLog(logger, tester, check, nameProducer);
        }

        public void testSpiLog(java.lang.System.Logger logger,
                ResourceBundle bundle, String msg, Object... params) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOGRB_STRING_PARAMS,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOGRB_STRING_PARAMS,
                                    "logX"), null, bundle, params);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOGRB_STRING_PARAMS.logX(x, level, bundle, msg, params);
                return null;
            };
            Function<String, String> nameProducer = (l) -> "log(Level." + l
                    + ", bundle, \"" + msg + "\", params...)";
            testSpiLog(logger, tester, check, nameProducer);
        }

        public void testSpiLog(java.lang.System.Logger logger, String msg, Throwable thrown) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                           adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOG_STRING_THROWN,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOG_STRING_THROWN,
                                    "logX"), thrown, localized);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOG_STRING_THROWN.logX(x, level, msg, thrown);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", \"" + msg + "\", thrown)";
            testSpiLog(logger, tester, check, nameProducer);
        }

        public void testSpiLog(java.lang.System.Logger logger,
                ResourceBundle bundle, String msg, Throwable thrown) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOGRB_STRING_THROWN,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOGRB_STRING_THROWN,
                                    "logX"), thrown, bundle);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOGRB_STRING_THROWN.logX(x, level, bundle, msg, thrown);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", bundle, \"" + msg + "\", thrown)";
            testSpiLog(logger, tester, check, nameProducer);
        }

        public void testSpiLog(java.lang.System.Logger logger, Supplier<String> msg) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOG_SUPPLIER,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOG_SUPPLIER,
                                    "logX"), null, null);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOG_SUPPLIER.logX(x, level, msg);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", () -> \"" + msg.get() + "\")";
            testSpiLog(logger, tester, check, nameProducer);
        }

        public void testSpiLog(java.lang.System.Logger logger, Object obj) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> obj.toString(),
                            adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOG_OBJECT,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOG_OBJECT,
                                    "logX"), null, null);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOG_OBJECT.logX(x, level, obj);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", new "+obj.getClass().getSimpleName()+"(\""
                            + obj.toString() + "\"))";
            testSpiLog(logger, tester, check, nameProducer);
        }

        public void testSpiLog(java.lang.System.Logger logger, Throwable thrown, Supplier<String> msg) {
            Checker<BackendRecord, java.lang.System.Logger.Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(
                                    SpiLogMethodInvoker.LOG_SUPPLIER_THROWN,
                                    SpiLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    SpiLogMethodInvoker.LOG_SUPPLIER_THROWN,
                                    "logX"), thrown, null);
                return null;
            };
            SpiLogTester tester = (x, level) -> {
                SpiLogMethodInvoker.LOG_SUPPLIER_THROWN.logX(x, level, msg, thrown);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", () -> \"" + msg.get() + "\", thrown)";
            testSpiLog(logger, tester, check, nameProducer);
        }


        // JDK

        public void testLog(java.lang.System.Logger logger, String msg) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOG_STRING_PARAMS,
                                    JdkLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    JdkLogMethodInvoker.LOG_STRING_PARAMS,
                                    "logX"), null, localized);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOG_STRING_PARAMS.logX(x, level, msg, (Object[])null);
                return null;
            };
            Function<String, String> nameProducer = (l) -> "log(Level." + l + ", \"" + msg + "\")";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogrb(java.lang.System.Logger logger,
                ResourceBundle bundle, String msg) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRB_STRING_PARAMS,
                                    JdkLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    JdkLogMethodInvoker.LOGRB_STRING_PARAMS,
                                    "logX"), null, bundle);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGRB_STRING_PARAMS.logX(x, level, bundle, msg, (Object[])null);
                return null;
            };
            Function<String, String> nameProducer = (l) -> "logrb(Level." + l
                    + ", bundle, \"" + msg + "\")";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLog(java.lang.System.Logger logger, String msg, Object... params) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOG_STRING_PARAMS,
                                    JdkLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    JdkLogMethodInvoker.LOG_STRING_PARAMS,
                                    "logX"), null, localized, params);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOG_STRING_PARAMS.logX(x, level, msg, params);
                return null;
            };
            Function<String, String> nameProducer = (l) -> "log(Level." + l + ", \"" + msg + "\", params...)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogrb(java.lang.System.Logger logger,
                ResourceBundle bundle, String msg, Object... params) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRB_STRING_PARAMS,
                                    JdkLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    JdkLogMethodInvoker.LOGRB_STRING_PARAMS,
                                    "logX"), null, bundle, params);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGRB_STRING_PARAMS.logX(x, level, bundle, msg, params);
                return null;
            };
            Function<String, String> nameProducer = (l) -> "log(Level." + l
                    + ", bundle, \"" + msg + "\", params...)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLog(java.lang.System.Logger logger, String msg, Throwable thrown) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                           adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOG_STRING_THROWN,
                                    JdkLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    JdkLogMethodInvoker.LOG_STRING_THROWN,
                                    "logX"), thrown, localized);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOG_STRING_THROWN.logX(x, level, msg, thrown);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", \"" + msg + "\", thrown)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogrb(java.lang.System.Logger logger,
                ResourceBundle bundle, String msg, Throwable thrown) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRB_STRING_THROWN,
                                    JdkLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    JdkLogMethodInvoker.LOGRB_STRING_THROWN,
                                    "logX"), thrown, bundle);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGRB_STRING_THROWN.logX(x, level, bundle, msg, thrown);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", bundle, \"" + msg + "\", thrown)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLog(java.lang.System.Logger logger, Supplier<String> msg) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOG_SUPPLIER,
                                    JdkLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    JdkLogMethodInvoker.LOG_SUPPLIER,
                                    "logX"), null, null);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOG_SUPPLIER.logX(x, level, msg);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", () -> \"" + msg.get() + "\")";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLog(java.lang.System.Logger logger, Throwable thrown, Supplier<String> msg) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOG_SUPPLIER_THROWN,
                                    JdkLogMethodInvoker.class.getName()),
                            adaptor().getCallerMethodName(
                                    JdkLogMethodInvoker.LOG_SUPPLIER_THROWN,
                                    "logX"), thrown, null);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOG_SUPPLIER_THROWN.logX(x, level, thrown, msg);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", () -> \"" + msg.get() + "\", thrown)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        static Supplier<String> logpMessage(ResourceBundle bundle,
                String className, String methodName, Supplier<String> msg) {
            if (BEST_EFFORT_FOR_LOGP && bundle == null
                    && (className != null || methodName != null)) {
                final String cName = className == null ? "" :  className;
                final String mName = methodName == null ? "" : methodName;
                return () -> {
                    String m = msg.get();
                    return String.format("[%s %s] %s", cName, mName, m == null ? "" : m);
                };
            } else {
                return msg;
            }
        }

        public void testLogp(java.lang.System.Logger logger, String className,
                String methodName, String msg) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("logp", res, logger.getName(), l,
                            logpMessage(localized, className, methodName, () -> msg),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_STRING, className),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_STRING, methodName),
                            null, localized);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGP_STRING.logX(x, level,
                        className, methodName, msg);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "logp(Level." + l + ", class, method, \"" + msg + "\")";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogrb(java.lang.System.Logger logger, String className,
                String methodName, ResourceBundle bundle, String msg) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("logp", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRBP_STRING_PARAMS, className),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRBP_STRING_PARAMS, methodName),
                            null, bundle);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGRBP_STRING_PARAMS.logX(x, level,
                        className, methodName, bundle, msg, (Object[])null);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "logp(Level." + l + ", class, method, bundle, \"" + msg + "\")";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogp(java.lang.System.Logger logger, String className,
                String methodName, String msg, Object... params) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("logp", res, logger.getName(), l,
                            logpMessage(localized, className, methodName, () -> msg),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_STRING_PARAMS, className),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_STRING_PARAMS, methodName),
                            null, localized, params);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGP_STRING_PARAMS.logX(x, level,
                        className, methodName, msg, params);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", class, method, \"" + msg + "\", params...)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogrb(java.lang.System.Logger logger, String className,
                String methodName, ResourceBundle bundle, String msg,
                Object... params) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("logp", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRBP_STRING_PARAMS, className),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRBP_STRING_PARAMS, methodName),
                            null, bundle, params);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGRBP_STRING_PARAMS.logX(x, level,
                        className, methodName, bundle, msg, params);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", class, method, bundle, \""
                            + msg + "\", params...)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogp(java.lang.System.Logger logger, String className,
                String methodName, String msg, Throwable thrown) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l,
                            logpMessage(localized, className, methodName, () -> msg),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_STRING_THROWN, className),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_STRING_THROWN, methodName),
                            thrown, localized);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGP_STRING_THROWN.logX(x, level,
                        className, methodName, msg, thrown);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", class, method, \"" + msg + "\", thrown)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogrb(java.lang.System.Logger logger, String className,
                String methodName, ResourceBundle bundle,
                String msg, Throwable thrown) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l, () -> msg,
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRBP_STRING_THROWN, className),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGRBP_STRING_THROWN, methodName),
                            thrown, bundle);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGRBP_STRING_THROWN.logX(x, level,
                        className, methodName, bundle, msg, thrown);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", class, method, bundle, \"" + msg + "\", thrown)";
            testJdkLog(logger, tester, check, nameProducer);

        }

        public void testLogp(java.lang.System.Logger logger, String className,
                String methodName, Supplier<String> msg) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l,
                            logpMessage(null, className, methodName, msg),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_SUPPLIER, className),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_SUPPLIER, methodName),
                            null, null);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGP_SUPPLIER.logX(x, level,
                        className, methodName, msg);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", class, method, () -> \"" + msg.get() + "\")";
            testJdkLog(logger, tester, check, nameProducer);
        }

        public void testLogp(java.lang.System.Logger logger, String className,
                String methodName, Throwable thrown, Supplier<String> msg) {
            Checker<BackendRecord, Level> check = (res, l) -> {
                checkRecord("log", res, logger.getName(), l,
                            logpMessage(null, className, methodName, msg),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_SUPPLIER_THROWN, className),
                            adaptor().getCallerClassName(
                                    JdkLogMethodInvoker.LOGP_SUPPLIER_THROWN, methodName),
                            thrown, null);
                return null;
            };
            JdkLogTester tester = (x, level) -> {
                JdkLogMethodInvoker.LOGP_SUPPLIER_THROWN.logX(x, level,
                        className, methodName, thrown, msg);
                return null;
            };
            Function<String, String> nameProducer = (l) ->
                    "log(Level." + l + ", class, method, () -> \"" + msg.get() + "\", thrown)";
            testJdkLog(logger, tester, check, nameProducer);
        }

        private void testJdkLog(java.lang.System.Logger logger,
                JdkLogTester log, Checker<BackendRecord,Level> check,
                Function<String, String> nameProducer) {
            if (restrictedTo != null) {
                if (!bridgeLoggerClass.isAssignableFrom(restrictedTo)) {
                    if (VERBOSE) {
                        System.out.println("Skipping method from "
                            + bridgeLoggerClass);
                    }
                    return;
                }
            }
            System.out.println("Testing Logger." + nameProducer.apply("*")
                     + " on " + logger);
            final BackendAdaptor adaptor = adaptor();
            for (Level loggerLevel : LEVELS) {
                adaptor.setLevel(logger, loggerLevel);
                for (Level l : LEVELS) {
                    check(logger, () -> log.apply(bridgeLoggerClass.cast(logger), l),
                          check, () -> adaptor.isLoggable(logger, l),
                          () -> adaptor.shouldBeLoggable(l, loggerLevel),
                          l, loggerLevel, nameProducer.apply(l.toString()));
                }
            }
        }

        private void testSpiLog(java.lang.System.Logger logger,
                SpiLogTester log, Checker<BackendRecord, java.lang.System.Logger.Level> check,
                Function<String, String> nameProducer) {
            System.out.println("Testing System.Logger." + nameProducer.apply("*")
                     + " on " + logger);
            final BackendAdaptor adaptor = adaptor();
            for (java.lang.System.Logger.Level loggerLevel : java.lang.System.Logger.Level.values()) {

                adaptor.setLevel(logger, loggerLevel);
                for (java.lang.System.Logger.Level l : java.lang.System.Logger.Level.values()) {
                    check(logger, () -> log.apply(logger, l),
                          check, () -> logger.isLoggable(l),
                          () -> adaptor.shouldBeLoggable(l, loggerLevel),
                          l, loggerLevel, nameProducer.apply(l.toString()));
                }
            }
        }

        private void test(String args, Levels level, java.lang.System.Logger logger,
                Runnable test, Checker<BackendRecord, Level> check) {
            if (restrictedTo != null) {
                if (!level.definingClass.isAssignableFrom(restrictedTo)) {
                    if (VERBOSE) {
                        System.out.println("Skipping method from "
                            + level.definingClass);
                    }
                    return;
                }
            }
            String method = args.contains("bundle") ? "logrb" : "log";
            System.out.println("Testing Logger."
                    + method + "(Level." + level.platformLevel
                    + ", "+ args + ")" + " on " + logger);
            final BackendAdaptor adaptor = adaptor();
            for (Level loggerLevel : LEVELS) {
                adaptor.setLevel(logger, loggerLevel);
                check(logger, test, check,
                        () -> level.isEnabled(logger),
                        () -> adaptor.shouldBeLoggable(level, loggerLevel),
                        level.platformLevel, loggerLevel, level.method);
            }
        }

        private <L> void check(java.lang.System.Logger logger,
                Runnable test, Checker<BackendRecord,L> check,
                BooleanSupplier checkLevelEnabled,
                BooleanSupplier shouldBeLoggable,
                L logLevel, L loggerLevel, String logMethod) {
            final BackendAdaptor adaptor = adaptor();
            adaptor.resetBackendRecords();
            test.run();
            final List<BackendRecord> records = adaptor.getBackendRecords();
            if (shouldBeLoggable.getAsBoolean()) {
                if (!checkLevelEnabled.getAsBoolean()) {
                    throw new RuntimeException("Logger is not enabled for "
                            + logMethod
                            + " although logger level is " + loggerLevel);
                }
                if (records.size() != 1) {
                    throw new RuntimeException(loggerLevel + " [" +
                            logLevel + "] : Unexpected record sizes: "
                        + records.toString());
                }
                BackendRecord res = records.get(0);
                check.apply(res, logLevel);
            } else {
                if (checkLevelEnabled.getAsBoolean()) {
                    throw new RuntimeException("Logger is enabled for "
                            + logMethod
                            + " although logger level is " + loggerLevel);
                }
                if (!records.isEmpty()) {
                    throw new RuntimeException(loggerLevel + " [" +
                            logLevel + "] : Unexpected record sizes: "
                        + records.toString());
                }
            }
        }
    }

    public static class JULBackendTester extends BackendTester<LogRecord>{

        public JULBackendTester(boolean isSystem) {
            this(isSystem,null,null);
        }
        public JULBackendTester(boolean isSystem, ResourceBundle localized) {
            this(isSystem,null,localized);
        }
        public JULBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo) {
            this(isSystem, restrictedTo, null);
        }
        public JULBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo,
                ResourceBundle localized) {
            super(isSystem, restrictedTo, localized);
        }

        Logger getBackendLogger(String name) {
            if (isSystem) {
                return LoggingProviderImpl.getLogManagerAccess().demandLoggerFor(
                        LogManager.getLogManager(), name, Thread.class.getModule());
            } else {
                return Logger.getLogger(name);
            }
        }

        class JULBackendAdaptor extends BackendAdaptor {
            @Override
            public String getLoggerName(LogRecord res) {
                return res.getLoggerName();
            }
            @Override
            public Level getLevel(LogRecord res) {
                return Level.valueOf(res.getLevel().getName());
            }
            @Override
            public String getMessage(LogRecord res) {
                return res.getMessage();
            }
            @Override
            public String getSourceClassName(LogRecord res) {
                return res.getSourceClassName();
            }
            @Override
            public String getSourceMethodName(LogRecord res) {
                return res.getSourceMethodName();
            }
            @Override
            public Throwable getThrown(LogRecord res) {
                return res.getThrown();
            }
            @Override
            public ResourceBundle getResourceBundle(LogRecord res) {
                return res.getResourceBundle();
            }
            @Override
            public void setLevel(java.lang.System.Logger logger, Level level) {
                Logger backend = getBackendLogger(logger.getName());
                backend.setLevel(java.util.logging.Level.parse(level.name()));
            }
            @Override
            public void setLevel(java.lang.System.Logger logger, java.lang.System.Logger.Level level) {
                setLevel(logger, toJUL(level));
            }
            @Override
            public List<LogRecord> getBackendRecords() {
                return handler.records;
            }
            @Override
            public void resetBackendRecords() {
                handler.reset();
            }
            @Override
            public Level getMappedLevel(Object level) {
                if (level instanceof java.lang.System.Logger.Level) {
                    return toJUL((java.lang.System.Logger.Level)level);
                }
                return (Level)level;
            }
        }

        final JULBackendAdaptor julAdaptor = new JULBackendAdaptor();

        @Override
        BackendAdaptor adaptor() {
            return julAdaptor;
        }

    }

    public abstract static class BackendTesterFactory {
        public abstract BackendTester createBackendTester(boolean isSystem);
        public abstract  BackendTester createBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo);
        public abstract  BackendTester createBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo,
                ResourceBundle bundle);
        public abstract  BackendTester createBackendTester(boolean isSystem,
                ResourceBundle bundle);
    }

    public static class JULBackendTesterFactory extends BackendTesterFactory {

        @Override
        public BackendTester createBackendTester(boolean isSystem) {
            return new JULBackendTester(isSystem);
        }

        @Override
        public BackendTester createBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo) {
            return new JULBackendTester(isSystem, restrictedTo);
        }

        @Override
        public BackendTester createBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo,
                ResourceBundle bundle) {
            return new JULBackendTester(isSystem, restrictedTo, bundle);
        }

        @Override
        public BackendTester createBackendTester(boolean isSystem,
                ResourceBundle bundle) {
            return new JULBackendTester(isSystem, bundle);
        }
    }

    public static class CustomLoggerFinder extends LoggerFinder {

        static enum CustomLevel { OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL };
        static CustomLevel[] customLevelMap = { CustomLevel.ALL,
            CustomLevel.TRACE, CustomLevel.DEBUG, CustomLevel.INFO,
            CustomLevel.WARN, CustomLevel.ERROR, CustomLevel.OFF
        };
        static class CustomLogRecord {
            public final CustomLevel logLevel;
            public final java.lang.System.Logger logger;
            public final String msg;
            public final Object[] params;
            public final Throwable thrown;
            public final ResourceBundle bundle;

            CustomLogRecord(java.lang.System.Logger producer,
                    CustomLevel level, String msg) {
                this(producer, level, msg, (ResourceBundle)null, (Throwable)null, (Object[])null);
            }

            CustomLogRecord(java.lang.System.Logger producer,
                    CustomLevel level, String msg, ResourceBundle bundle,
                    Throwable thrown, Object... params) {
                this.logger   = producer;
                this.logLevel = level;
                this.msg = msg;
                this.params = params;
                this.thrown = thrown;
                this.bundle = bundle;
            }
        }

        static final List<CustomLogRecord>  records =
                Collections.synchronizedList(new ArrayList<>());

        static class CustomLogger implements java.lang.System.Logger {

            final String name;
            volatile CustomLevel level;
            CustomLogger(String name) {
                this.name = name;
                this.level = CustomLevel.INFO;
            }

            @Override
            public String getName() {
                return name;
            }

            public void setLevel(CustomLevel level) {
                this.level = level;
            }


            @Override
            public boolean isLoggable(java.lang.System.Logger.Level level) {

                return this.level != CustomLevel.OFF && this.level.ordinal()
                        >= customLevelMap[level.ordinal()].ordinal();
            }

            @Override
            public void log(java.lang.System.Logger.Level level, ResourceBundle bundle, String key, Throwable thrown) {
                if (isLoggable(level)) {
                    records.add(new CustomLogRecord(this, customLevelMap[level.ordinal()],
                              key, bundle, thrown));
                }
            }

            @Override
            public void log(java.lang.System.Logger.Level level, ResourceBundle bundle, String format, Object... params) {
                if (isLoggable(level)) {
                    records.add(new CustomLogRecord(this, customLevelMap[level.ordinal()],
                              format, bundle, null, params));
                }
            }

        }

        final Map<String, java.lang.System.Logger> applicationLoggers =
                Collections.synchronizedMap(new HashMap<>());
        final Map<String, java.lang.System.Logger> systemLoggers =
                Collections.synchronizedMap(new HashMap<>());

        @Override
        public java.lang.System.Logger getLogger(String name, Module caller) {
            ClassLoader callerLoader = caller.getClassLoader();
            if (callerLoader == null) {
                systemLoggers.putIfAbsent(name, new CustomLogger(name));
                return systemLoggers.get(name);
            } else {
                applicationLoggers.putIfAbsent(name, new CustomLogger(name));
                return applicationLoggers.get(name);
            }
        }

        CustomLevel fromJul(Level level) {
            if (level.intValue() == Level.OFF.intValue()) {
                return CustomLevel.OFF;
            } else if (level.intValue() > Level.SEVERE.intValue()) {
                return CustomLevel.ERROR;
            } else if (level.intValue() > Level.WARNING.intValue()) {
                return CustomLevel.ERROR;
            } else if (level.intValue() > Level.INFO.intValue()) {
                return CustomLevel.WARN;
            } else if (level.intValue() > Level.CONFIG.intValue()) {
                return CustomLevel.INFO;
            } else if (level.intValue() > Level.FINER.intValue()) {
                return CustomLevel.DEBUG;
            } else if (level.intValue() > Level.FINEST.intValue()) {
                return CustomLevel.TRACE;
            } else if (level.intValue() == Level.ALL.intValue()) {
                return CustomLevel.ALL;
            } else {
                return CustomLevel.TRACE;
            }
        }

        Level toJul(CustomLevel level) {
            switch(level) {
                case OFF: return Level.OFF;
                case FATAL: return Level.SEVERE;
                case ERROR: return Level.SEVERE;
                case WARN: return Level.WARNING;
                case INFO: return Level.INFO;
                case DEBUG: return Level.FINE;
                case TRACE: return Level.FINER;
                case ALL: return Level.ALL;
                default: throw new InternalError("No such level: "+level);
            }
        }

    }

    public static class CustomBackendTester extends
            BackendTester<CustomLoggerFinder.CustomLogRecord> {

        public final CustomLoggerFinder provider;

        public CustomBackendTester(boolean isSystem) {
            this(isSystem, null, null);
        }

        public CustomBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo) {
            this(isSystem, restrictedTo, null);
        }

        public CustomBackendTester(boolean isSystem,
                ResourceBundle localized) {
            this(isSystem, null, localized);
        }

        public CustomBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo,
                ResourceBundle localized) {
            super(isSystem, restrictedTo, localized);
            provider = (CustomLoggerFinder)java.lang.System.LoggerFinder.getLoggerFinder();
        }

        @Override
        public java.lang.System.Logger convert(java.lang.System.Logger logger) {
            if (restrictedTo != null && restrictedTo.isInstance(logger)) {
                return logger;
            } else if (restrictedTo == jdkLoggerClass) {
                return logger;
            } else {
                return java.lang.System.Logger.class.cast(
                        sun.util.logging.PlatformLogger.Bridge.convert(logger));
            }
        }

        class CustomBackendAdaptor extends BackendAdaptor {

            @Override
            public String getLoggerName(CustomLoggerFinder.CustomLogRecord res) {
                return res.logger.getName();
            }

            @Override
            public CustomLoggerFinder.CustomLevel getLevel(CustomLoggerFinder.CustomLogRecord res) {
                return res.logLevel;
            }

            @Override
            public String getMessage(CustomLoggerFinder.CustomLogRecord res) {
                return res.msg;
            }

            @Override // we don't support source class name in our custom provider implementation
            public String getSourceClassName(CustomLoggerFinder.CustomLogRecord res) {
                return null;
            }

            @Override // we don't support source method name in our custom provider implementation
            public String getSourceMethodName(CustomLoggerFinder.CustomLogRecord res) {
                return null;
            }

            @Override
            public Throwable getThrown(CustomLoggerFinder.CustomLogRecord res) {
                return res.thrown;
            }

            @Override
            public ResourceBundle getResourceBundle(CustomLoggerFinder.CustomLogRecord res) {
                return res.bundle;
            }

            @Override
            public void setLevel(java.lang.System.Logger logger, Level level) {
                final CustomLoggerFinder.CustomLogger l =
                        (CustomLoggerFinder.CustomLogger)
                        (isSystem ? provider.getLogger(logger.getName(), Thread.class.getModule()) :
                        provider.getLogger(logger.getName(), LoggerFinderBackendTest.class.getModule()));
                l.setLevel(provider.fromJul(level));
            }
            @Override
            public void setLevel(java.lang.System.Logger logger,
                    java.lang.System.Logger.Level level) {
                setLevel(logger, toJUL(level));
            }

            CustomLoggerFinder.CustomLevel getLevel(java.lang.System.Logger logger) {
                final CustomLoggerFinder.CustomLogger l =
                        (CustomLoggerFinder.CustomLogger)
                        (isSystem ? provider.getLogger(logger.getName(), Thread.class.getModule()) :
                        provider.getLogger(logger.getName(), LoggerFinderBackendTest.class.getModule()));
                return l.level;
            }

            @Override
            public List<CustomLoggerFinder.CustomLogRecord> getBackendRecords() {
                return CustomLoggerFinder.records;
            }

            @Override
            public void resetBackendRecords() {
                CustomLoggerFinder.records.clear();
            }

            @Override
            public boolean shouldBeLoggable(Levels level, Level loggerLevel) {
                return loggerLevel != Level.OFF &&
                       fromLevels(level).ordinal() <= provider.fromJul(loggerLevel).ordinal();
            }

            @Override
            public boolean isLoggable(java.lang.System.Logger logger, Level l) {
                return super.isLoggable(logger, l);
            }

            @Override
            public boolean shouldBeLoggable(Level logLevel, Level loggerLevel) {
                return loggerLevel != Level.OFF &&
                        provider.fromJul(logLevel).ordinal() <= provider.fromJul(loggerLevel).ordinal();
            }

            @Override // we don't support source class name in our custom provider implementation
            public String getCallerClassName(Levels level, String clazz) {
                return null;
            }

            @Override // we don't support source method name in our custom provider implementation
            public String getCallerMethodName(Levels level, String method) {
                return null;
            }

            @Override // we don't support source class name in our custom provider implementation
            public String getCallerClassName(MethodInvoker<?,?> logMethod, String clazz) {
                return null;
            }

            @Override // we don't support source method name in our custom provider implementation
            public String getCallerMethodName(MethodInvoker<?,?> logMethod, String method) {
                return null;
            }

            @Override
            public CustomLoggerFinder.CustomLevel getMappedLevel(Object level) {
                if (level instanceof java.lang.System.Logger.Level) {
                    final int index = ((java.lang.System.Logger.Level)level).ordinal();
                    return CustomLoggerFinder.customLevelMap[index];
                } else if (level instanceof Level) {
                    return provider.fromJul((Level)level);
                }
                return (CustomLoggerFinder.CustomLevel) level;
            }

            CustomLoggerFinder.CustomLevel fromLevels(Levels level) {
                switch(level) {
                    case SEVERE:
                        return CustomLoggerFinder.CustomLevel.ERROR;
                    case WARNING:
                        return CustomLoggerFinder.CustomLevel.WARN;
                    case INFO:
                        return CustomLoggerFinder.CustomLevel.INFO;
                    case CONFIG: case FINE:
                        return CustomLoggerFinder.CustomLevel.DEBUG;
                    case FINER:  case FINEST:
                        return CustomLoggerFinder.CustomLevel.TRACE;
                }
                throw new InternalError("No such level "+level);
            }

        }

        @Override
        BackendAdaptor adaptor() {
            return new CustomBackendAdaptor();
        }

    }

    public static class CustomBackendTesterFactory extends BackendTesterFactory {

        @Override
        public BackendTester createBackendTester(boolean isSystem) {
            return new CustomBackendTester(isSystem);
        }

        @Override
        public BackendTester createBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo) {
            return new CustomBackendTester(isSystem, restrictedTo);
        }

        @Override
        public BackendTester createBackendTester(boolean isSystem,
                Class<? extends java.lang.System.Logger> restrictedTo,
                ResourceBundle bundle) {
            return new CustomBackendTester(isSystem, restrictedTo, bundle);
        }

        @Override
        public BackendTester createBackendTester(boolean isSystem,
                ResourceBundle bundle) {
            return new CustomBackendTester(isSystem, bundle);
        }
    }

    static final Method getLazyLogger;
    static final Method accessLoggerFinder;
    static {
        // jdk.internal.logger.LazyLoggers.getLazyLogger(name, caller);
        try {
            Class<?> lazyLoggers = jdk.internal.logger.LazyLoggers.class;
            getLazyLogger = lazyLoggers.getMethod("getLazyLogger",
                    String.class, Module.class);
            getLazyLogger.setAccessible(true);
            Class<?> loggerFinderLoader =
                    Class.forName("java.lang.System$LoggerFinder");
            accessLoggerFinder = loggerFinderLoader.getDeclaredMethod("accessProvider");
            accessLoggerFinder.setAccessible(true);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static java.lang.System.Logger getSystemLogger(String name, Module caller) throws Exception {
        try {
            return java.lang.System.Logger.class.cast(getLazyLogger.invoke(null, name, caller));
        } catch (InvocationTargetException x) {
            Throwable t = x.getTargetException();
            if (t instanceof Exception) {
                throw (Exception)t;
            } else {
                throw (Error)t;
            }
        }
    }
    static java.lang.System.Logger getSystemLogger(String name,
            ResourceBundle bundle, Module caller) throws Exception {
        try {
            LoggerFinder provider = LoggerFinder.class.cast(accessLoggerFinder.invoke(null));
            return provider.getLocalizedLogger(name, bundle, caller);
        } catch (InvocationTargetException x) {
            Throwable t = x.getTargetException();
            if (t instanceof Exception) {
                throw (Exception)t;
            } else {
                throw (Error)t;
            }
        }
    }

    // Change this to 'true' to get more traces...
    public static boolean verbose = false;

    public static void main(String[] argv) throws Exception {

        final AtomicInteger nb = new AtomicInteger(0);
        final boolean hidesProvider = Boolean.getBoolean("test.logger.hidesProvider");
        System.out.println(ClassLoader.getSystemClassLoader());
        final BackendTesterFactory factory;
        if (java.lang.System.LoggerFinder.getLoggerFinder() instanceof CustomLoggerFinder) {
            if (hidesProvider) {
                System.err.println("Custom backend "
                        + java.lang.System.LoggerFinder.getLoggerFinder()
                        + " should have been hidden!");
                throw new RuntimeException(
                        "Custom backend should have been hidden: "
                        + "check value of java.system.class.loader property");
            }
            System.out.println("Using custom backend");
            factory = new CustomBackendTesterFactory();
        } else {
            if (!hidesProvider) {
                System.err.println("Default JUL backend "
                        + java.lang.System.LoggerFinder.getLoggerFinder()
                        + " should have been hidden!");
                throw new RuntimeException(
                        "Default JUL backend should have been hidden: "
                        + "check value of java.system.class.loader property");
            }
            System.out.println("Using JUL backend");
            factory = new JULBackendTesterFactory();
        }

        testBackend(nb, factory);
    }

    public static void testBackend(AtomicInteger nb, BackendTesterFactory factory) throws Exception {

        // Tests all level specifics methods with loggers configured with
        // all possible levels and loggers obtained with all possible
        // entry points from LoggerFactory and JdkLoggerFactory, with
        // JUL as backend.

        // Test a simple application logger with JUL backend
        final BackendTester tester = factory.createBackendTester(false);
        final java.lang.System.Logger logger =
                java.lang.System.LoggerFinder.getLoggerFinder()
                        .getLogger("foo", LoggerFinderBackendTest.class.getModule());

        testLogger(tester, logger, nb);

        // Test a simple system logger with JUL backend
        final java.lang.System.Logger system =
                java.lang.System.LoggerFinder.getLoggerFinder()
                        .getLogger("bar", Thread.class.getModule());
        final BackendTester systemTester = factory.createBackendTester(true);
        testLogger(systemTester, system, nb);

        // Test a localized application logger with null resource bundle and
        // JUL backend
        final java.lang.System.Logger noBundleLogger =
                java.lang.System.LoggerFinder.getLoggerFinder()
                        .getLocalizedLogger("baz", null, LoggerFinderBackendTest.class.getModule());
        final BackendTester noBundleTester =
                factory.createBackendTester(false, spiLoggerClass);
        testLogger(noBundleTester, noBundleLogger, nb);

        // Test a localized system logger with null resource bundle and JUL
        // backend
        final java.lang.System.Logger noBundleSysLogger =
                java.lang.System.LoggerFinder.getLoggerFinder()
                        .getLocalizedLogger("oof", null, Thread.class.getModule());
        final BackendTester noBundleSysTester =
                factory.createBackendTester(true, spiLoggerClass);
        testLogger(noBundleSysTester, noBundleSysLogger, nb);

        // Test a localized application logger with null resource bundle and
        // JUL backend
        try {
            System.getLogger("baz", null);
            throw new RuntimeException("Expected NullPointerException not thrown");
        } catch (NullPointerException x) {
            System.out.println("System.Loggers.getLogger(\"baz\", null): got expected " + x);
        }
        final java.lang.System.Logger noBundleExtensionLogger =
                getSystemLogger("baz", null, LoggerFinderBackendTest.class.getModule());
        final BackendTester noBundleExtensionTester =
                factory.createBackendTester(false, jdkLoggerClass);
        testLogger(noBundleExtensionTester, noBundleExtensionLogger, nb);

        // Test a simple system logger with JUL backend
        final java.lang.System.Logger sysExtensionLogger =
                getSystemLogger("oof", Thread.class.getModule());
        final BackendTester sysExtensionTester =
                factory.createBackendTester(true, jdkLoggerClass);
        testLogger(sysExtensionTester, sysExtensionLogger, nb);

        // Test a localized system logger with null resource bundle and JUL
        // backend
        final java.lang.System.Logger noBundleSysExtensionLogger =
                getSystemLogger("oof", null, Thread.class.getModule());
        final BackendTester noBundleSysExtensionTester =
                factory.createBackendTester(true, jdkLoggerClass);
        testLogger(noBundleSysExtensionTester, noBundleSysExtensionLogger, nb);

        // Test a localized application logger converted to JDK with null
        // resource bundle and JUL backend
        final java.lang.System.Logger noBundleConvertedLogger =
                (java.lang.System.Logger)
                sun.util.logging.PlatformLogger.Bridge.convert(noBundleLogger);
        final BackendTester noBundleJdkTester = factory.createBackendTester(false);
        testLogger(noBundleJdkTester, noBundleConvertedLogger, nb);

        // Test a localized system logger converted to JDK with null resource
        // bundle and JUL backend
        final java.lang.System.Logger noBundleConvertedSysLogger =
                (java.lang.System.Logger)
                sun.util.logging.PlatformLogger.Bridge.convert(noBundleSysLogger);
        final BackendTester noBundleJdkSysTester = factory.createBackendTester(true);
        testLogger(noBundleJdkSysTester, noBundleConvertedSysLogger, nb);

        // Test a localized application logger with resource bundle and JUL
        // backend
        final ResourceBundle bundle =
                ResourceBundle.getBundle(ResourceBundeLocalized.class.getName());
        final java.lang.System.Logger bundleLogger =
                java.lang.System.LoggerFinder.getLoggerFinder()
                        .getLocalizedLogger("toto", bundle, LoggerFinderBackendTest.class.getModule());
        final BackendTester bundleTester =
                factory.createBackendTester(false, spiLoggerClass, bundle);
        testLogger(bundleTester, bundleLogger, nb);

        // Test a localized system logger with resource bundle and JUL backend
        final java.lang.System.Logger bundleSysLogger =
                java.lang.System.LoggerFinder.getLoggerFinder()
                        .getLocalizedLogger("titi", bundle, Thread.class.getModule());
        final BackendTester bundleSysTester =
                factory.createBackendTester(true, spiLoggerClass, bundle);
        testLogger(bundleSysTester, bundleSysLogger, nb);

        // Test a localized Jdk application logger with resource bundle and JUL
        // backend
        final java.lang.System.Logger bundleExtensionLogger =
                System.getLogger("tita", bundle);
        final BackendTester bundleExtensionTester =
                factory.createBackendTester(false, jdkLoggerClass, bundle);
        testLogger(bundleExtensionTester, bundleExtensionLogger, nb);

        // Test a localized Jdk system logger with resource bundle and JUL
        // backend
        final java.lang.System.Logger bundleExtensionSysLogger =
                getSystemLogger("titu", bundle, Thread.class.getModule());
        final BackendTester bundleExtensionSysTester =
                factory.createBackendTester(true, jdkLoggerClass, bundle);
        testLogger(bundleExtensionSysTester, bundleExtensionSysLogger, nb);

        // Test a localized application logger converted to JDK with resource
        // bundle and JUL backend
        final BackendTester bundleJdkTester =
                factory.createBackendTester(false, bundle);
        final java.lang.System.Logger bundleConvertedLogger =
                (java.lang.System.Logger)
                sun.util.logging.PlatformLogger.Bridge.convert(bundleLogger);
        testLogger(bundleJdkTester, bundleConvertedLogger, nb);

        // Test a localized Jdk system logger converted to JDK with resource
        // bundle and JUL backend
        final BackendTester bundleJdkSysTester =
                factory.createBackendTester(true, bundle);
        final java.lang.System.Logger bundleConvertedSysLogger =
                (java.lang.System.Logger)
                sun.util.logging.PlatformLogger.Bridge.convert(bundleSysLogger);
        testLogger(bundleJdkSysTester, bundleConvertedSysLogger, nb);

        // Now need to add tests for all the log/logp/logrb methods...

    }

    private static class FooObj {
        final String s;
        FooObj(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return super.toString() +": "+s;
        }

    }

    public static void testLogger(BackendTester tester,
            java.lang.System.Logger spiLogger, AtomicInteger nb) {

        // Test all level-specific method forms:
        // fatal(...) error(...) severe(...) etc...
        java.lang.System.Logger jdkLogger = tester.convert(spiLogger);
        for (Levels l : Levels.values()) {
            java.lang.System.Logger logger =
                    l.definingClass.equals(spiLoggerClass) ? spiLogger : jdkLogger;
            tester.testLevel(l, logger, l.method + "[" + logger.getName()+ "]-"
                    + nb.incrementAndGet());
            tester.testLevel(l, logger, l.method + "[" + logger.getName()+ "]-"
                    + nb.incrementAndGet(),
                    bundleParam);
            final int nbb = nb.incrementAndGet();
            tester.testLevel(l, logger, () -> l.method + "[" + logger.getName()
                    + "]-" + nbb);
        }
        for (Levels l : Levels.values()) {
            java.lang.System.Logger logger =
                    l.definingClass.equals(spiLoggerClass) ? spiLogger : jdkLogger;
            tester.testLevel(l, logger,
                    l.method + "[" + logger.getName()+ "]({0},{1})-"
                    + nb.incrementAndGet(),
                    "One", "Two");
            tester.testLevel(l, logger,
                    l.method + "[" + logger.getName()+ "]({0},{1})-"
                    + nb.incrementAndGet(),
                    bundleParam, "One", "Two");
        }
        final Throwable thrown = new RuntimeException("Test");
        for (Levels l : Levels.values()) {
            java.lang.System.Logger logger =
                    l.definingClass.equals(spiLoggerClass) ? spiLogger : jdkLogger;
            tester.testLevel(l, logger, l.method + "[" + logger.getName()+ "]-"
                    + nb.incrementAndGet(),
                    thrown);
            tester.testLevel(l, logger, l.method + "[" + logger.getName()+ "]-"
                    + nb.incrementAndGet(),
                    bundleParam, thrown);
            final int nbb = nb.incrementAndGet();
            tester.testLevel(l, logger, ()->l.method + "[" + logger.getName()+ "]-"
                    + nbb, thrown);
        }

        java.lang.System.Logger logger = jdkLogger;

         // test System.Logger methods
       tester.testSpiLog(logger, "[" + logger.getName()+ "]-"
                + nb.incrementAndGet());
        tester.testSpiLog(logger, bundleParam, "[" + logger.getName()+ "]-"
                + nb.incrementAndGet());
        tester.testSpiLog(logger, "[" + logger.getName()+ "]-({0},{1})"
                + nb.incrementAndGet(), "One", "Two");
        tester.testSpiLog(logger, bundleParam, "[" + logger.getName()+ "]-({0},{1})"
                + nb.incrementAndGet(), "One", "Two");
        tester.testSpiLog(logger, "[" + logger.getName()+ "]-"
                + nb.incrementAndGet(), thrown);
        tester.testSpiLog(logger, bundleParam, "[" + logger.getName()+ "]-"
                + nb.incrementAndGet(), thrown);
        final int nbb01 = nb.incrementAndGet();
        tester.testSpiLog(logger, () -> "[" + logger.getName()+ "]-" + nbb01);
        final int nbb02 = nb.incrementAndGet();
        tester.testSpiLog(logger, thrown, () -> "[" + logger.getName()+ "]-" + nbb02);
        final int nbb03 = nb.incrementAndGet();
        tester.testSpiLog(logger, new FooObj("[" + logger.getName()+ "]-" + nbb03));

        // Test all log method forms:
        // jdk.internal.logging.Logger.log(...)
        tester.testLog(logger, "[" + logger.getName()+ "]-"
                + nb.incrementAndGet());
        tester.testLogrb(logger, bundleParam, "[" + logger.getName()+ "]-"
                + nb.incrementAndGet());
        tester.testLog(logger, "[" + logger.getName()+ "]-({0},{1})"
                + nb.incrementAndGet(), "One", "Two");
        tester.testLogrb(logger, bundleParam, "[" + logger.getName()+ "]-({0},{1})"
                + nb.incrementAndGet(), "One", "Two");
        tester.testLog(logger, "[" + logger.getName()+ "]-"
                + nb.incrementAndGet(), thrown);
        tester.testLogrb(logger, bundleParam, "[" + logger.getName()+ "]-"
                + nb.incrementAndGet(), thrown);
        final int nbb1 = nb.incrementAndGet();
        tester.testLog(logger, () -> "[" + logger.getName()+ "]-" + nbb1);
        final int nbb2 = nb.incrementAndGet();
        tester.testLog(logger, thrown, () -> "[" + logger.getName()+ "]-" + nbb2);

        // Test all logp method forms
        // jdk.internal.logging.Logger.logp(...)
        tester.testLogp(logger, "clazz" + nb.incrementAndGet(),
                "method" + nb.incrementAndGet(),
                "[" + logger.getName()+ "]-"
                + nb.incrementAndGet());
        tester.testLogrb(logger, "clazz" + nb.incrementAndGet(),
                "method" + nb.incrementAndGet(), bundleParam,
                "[" + logger.getName()+ "]-"
                + nb.incrementAndGet());
        tester.testLogp(logger, "clazz" + nb.incrementAndGet(),
                "method" + nb.incrementAndGet(),
                "[" + logger.getName()+ "]-({0},{1})"
                + nb.incrementAndGet(), "One", "Two");
        tester.testLogrb(logger, "clazz" + nb.incrementAndGet(),
                "method" + nb.incrementAndGet(), bundleParam,
                "[" + logger.getName()+ "]-({0},{1})"
                + nb.incrementAndGet(), "One", "Two");
        tester.testLogp(logger, "clazz" + nb.incrementAndGet(),
                "method" + nb.incrementAndGet(),
                "[" + logger.getName()+ "]-"
                + nb.incrementAndGet(), thrown);
        tester.testLogrb(logger, "clazz" + nb.incrementAndGet(),
                "method" + nb.incrementAndGet(), bundleParam,
                "[" + logger.getName()+ "]-"
                + nb.incrementAndGet(), thrown);
        final int nbb3 = nb.incrementAndGet();
        tester.testLogp(logger, "clazz" + nb.incrementAndGet(),
                "method" + nb.incrementAndGet(),
                () -> "[" + logger.getName()+ "]-" + nbb3);
        final int nbb4 = nb.incrementAndGet();
        tester.testLogp(logger, "clazz" + nb.incrementAndGet(),
                "method" + nb.incrementAndGet(),
                thrown, () -> "[" + logger.getName()+ "]-" + nbb4);
    }

}

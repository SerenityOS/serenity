/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.logging;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import jdk.internal.access.JavaUtilResourceBundleAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import static jdk.internal.logger.DefaultLoggerFinder.isSystem;

/**
 * A Logger object is used to log messages for a specific
 * system or application component.  Loggers are normally named,
 * using a hierarchical dot-separated namespace.  Logger names
 * can be arbitrary strings, but they should normally be based on
 * the package name or class name of the logged component, such
 * as java.net or javax.swing.  In addition it is possible to create
 * "anonymous" Loggers that are not stored in the Logger namespace.
 * <p>
 * Logger objects may be obtained by calls on one of the getLogger
 * factory methods.  These will either create a new Logger or
 * return a suitable existing Logger. It is important to note that
 * the Logger returned by one of the {@code getLogger} factory methods
 * may be garbage collected at any time if a strong reference to the
 * Logger is not kept.
 * <p>
 * Logging messages will be forwarded to registered Handler
 * objects, which can forward the messages to a variety of
 * destinations, including consoles, files, OS logs, etc.
 * <p>
 * Each Logger keeps track of a "parent" Logger, which is its
 * nearest existing ancestor in the Logger namespace.
 * <p>
 * Each Logger has a "Level" associated with it.  This reflects
 * a minimum Level that this logger cares about.  If a Logger's
 * level is set to {@code null}, then its effective level is inherited
 * from its parent, which may in turn obtain it recursively from its
 * parent, and so on up the tree.
 * <p>
 * The log level can be configured based on the properties from the
 * logging configuration file, as described in the description
 * of the LogManager class.  However it may also be dynamically changed
 * by calls on the Logger.setLevel method.  If a logger's level is
 * changed the change may also affect child loggers, since any child
 * logger that has {@code null} as its level will inherit its
 * effective level from its parent.
 * <p>
 * On each logging call the Logger initially performs a cheap
 * check of the request level (e.g., SEVERE or FINE) against the
 * effective log level of the logger.  If the request level is
 * lower than the log level, the logging call returns immediately.
 * <p>
 * After passing this initial (cheap) test, the Logger will allocate
 * a LogRecord to describe the logging message.  It will then call a
 * Filter (if present) to do a more detailed check on whether the
 * record should be published.  If that passes it will then publish
 * the LogRecord to its output Handlers.  By default, loggers also
 * publish to their parent's Handlers, recursively up the tree.
 * <p>
 * Each Logger may have a {@code ResourceBundle} associated with it.
 * The {@code ResourceBundle} may be specified by name, using the
 * {@link #getLogger(java.lang.String, java.lang.String)} factory
 * method, or by value - using the {@link
 * #setResourceBundle(java.util.ResourceBundle) setResourceBundle} method.
 * This bundle will be used for localizing logging messages.
 * If a Logger does not have its own {@code ResourceBundle} or resource bundle
 * name, then it will inherit the {@code ResourceBundle} or resource bundle name
 * from its parent, recursively up the tree.
 * <p>
 * Most of the logger output methods take a "msg" argument.  This
 * msg argument may be either a raw value or a localization key.
 * During formatting, if the logger has (or inherits) a localization
 * {@code ResourceBundle} and if the {@code ResourceBundle} has a mapping for
 * the msg string, then the msg string is replaced by the localized value.
 * Otherwise the original msg string is used.  Typically, formatters use
 * java.text.MessageFormat style formatting to format parameters, so
 * for example a format string "{0} {1}" would format two parameters
 * as strings.
 * <p>
 * A set of methods alternatively take a "msgSupplier" instead of a "msg"
 * argument.  These methods take a {@link Supplier}{@code <String>} function
 * which is invoked to construct the desired log message only when the message
 * actually is to be logged based on the effective log level thus eliminating
 * unnecessary message construction. For example, if the developer wants to
 * log system health status for diagnosis, with the String-accepting version,
 * the code would look like:
 * <pre>{@code
 *
 *  class DiagnosisMessages {
 *    static String systemHealthStatus() {
 *      // collect system health information
 *      ...
 *    }
 *  }
 *  ...
 *  logger.log(Level.FINER, DiagnosisMessages.systemHealthStatus());
 * }</pre>
 * With the above code, the health status is collected unnecessarily even when
 * the log level FINER is disabled. With the Supplier-accepting version as
 * below, the status will only be collected when the log level FINER is
 * enabled.
 * <pre>{@code
 *
 *  logger.log(Level.FINER, DiagnosisMessages::systemHealthStatus);
 * }</pre>
 * <p>
 * When looking for a {@code ResourceBundle}, the logger will first look at
 * whether a bundle was specified using {@link
 * #setResourceBundle(java.util.ResourceBundle) setResourceBundle}, and then
 * only whether a resource bundle name was specified through the {@link
 * #getLogger(java.lang.String, java.lang.String) getLogger} factory method.
 * If no {@code ResourceBundle} or no resource bundle name is found,
 * then it will use the nearest {@code ResourceBundle} or resource bundle
 * name inherited from its parent tree.<br>
 * When a {@code ResourceBundle} was inherited or specified through the
 * {@link
 * #setResourceBundle(java.util.ResourceBundle) setResourceBundle} method, then
 * that {@code ResourceBundle} will be used. Otherwise if the logger only
 * has or inherited a resource bundle name, then that resource bundle name
 * will be mapped to a {@code ResourceBundle} object, using the default Locale
 * at the time of logging.
 * <br id="ResourceBundleMapping">When mapping resource bundle names to
 * {@code ResourceBundle} objects, the logger will first try to use the
 * Thread's {@linkplain java.lang.Thread#getContextClassLoader() context class
 * loader} to map the given resource bundle name to a {@code ResourceBundle}.
 * If the thread context class loader is {@code null}, it will try the
 * {@linkplain java.lang.ClassLoader#getSystemClassLoader() system class loader}
 * instead.  If the {@code ResourceBundle} is still not found, it will use the
 * class loader of the first caller of the {@link
 * #getLogger(java.lang.String, java.lang.String) getLogger} factory method.
 * <p>
 * Formatting (including localization) is the responsibility of
 * the output Handler, which will typically call a Formatter.
 * <p>
 * Note that formatting need not occur synchronously.  It may be delayed
 * until a LogRecord is actually written to an external sink.
 * <p>
 * The logging methods are grouped in five main categories:
 * <ul>
 * <li><p>
 *     There are a set of "log" methods that take a log level, a message
 *     string, and optionally some parameters to the message string.
 * <li><p>
 *     There are a set of "logp" methods (for "log precise") that are
 *     like the "log" methods, but also take an explicit source class name
 *     and method name.
 * <li><p>
 *     There are a set of "logrb" method (for "log with resource bundle")
 *     that are like the "logp" method, but also take an explicit resource
 *     bundle object for use in localizing the log message.
 * <li><p>
 *     There are convenience methods for tracing method entries (the
 *     "entering" methods), method returns (the "exiting" methods) and
 *     throwing exceptions (the "throwing" methods).
 * <li><p>
 *     Finally, there are a set of convenience methods for use in the
 *     very simplest cases, when a developer simply wants to log a
 *     simple string at a given log level.  These methods are named
 *     after the standard Level names ("severe", "warning", "info", etc.)
 *     and take a single argument, a message string.
 * </ul>
 * <p>
 * For the methods that do not take an explicit source name and
 * method name, the Logging framework will make a "best effort"
 * to determine which class and method called into the logging method.
 * However, it is important to realize that this automatically inferred
 * information may only be approximate (or may even be quite wrong!).
 * Virtual machines are allowed to do extensive optimizations when
 * JITing and may entirely remove stack frames, making it impossible
 * to reliably locate the calling class and method.
 * <P>
 * All methods on Logger are multi-thread safe.
 * <p>
 * <b>Subclassing Information:</b> Note that a LogManager class may
 * provide its own implementation of named Loggers for any point in
 * the namespace.  Therefore, any subclasses of Logger (unless they
 * are implemented in conjunction with a new LogManager class) should
 * take care to obtain a Logger instance from the LogManager class and
 * should delegate operations such as "isLoggable" and "log(LogRecord)"
 * to that instance.  Note that in order to intercept all logging
 * output, subclasses need only override the log(LogRecord) method.
 * All the other logging methods are implemented as calls on this
 * log(LogRecord) method.
 *
 * @since 1.4
 */
public class Logger {
    private static final Handler emptyHandlers[] = new Handler[0];
    private static final int offValue = Level.OFF.intValue();

    static final String SYSTEM_LOGGER_RB_NAME = "sun.util.logging.resources.logging";

    // This class is immutable and it is important that it remains so.
    private static final class LoggerBundle {
        final String resourceBundleName; // Base name of the bundle.
        final ResourceBundle userBundle; // Bundle set through setResourceBundle.
        private LoggerBundle(String resourceBundleName, ResourceBundle bundle) {
            this.resourceBundleName = resourceBundleName;
            this.userBundle = bundle;
        }
        boolean isSystemBundle() {
            return SYSTEM_LOGGER_RB_NAME.equals(resourceBundleName);
        }
        static LoggerBundle get(String name, ResourceBundle bundle) {
            if (name == null && bundle == null) {
                return NO_RESOURCE_BUNDLE;
            } else if (SYSTEM_LOGGER_RB_NAME.equals(name) && bundle == null) {
                return SYSTEM_BUNDLE;
            } else {
                return new LoggerBundle(name, bundle);
            }
        }
    }

    // This instance will be shared by all loggers created by the system
    // code
    private static final LoggerBundle SYSTEM_BUNDLE =
            new LoggerBundle(SYSTEM_LOGGER_RB_NAME, null);

    // This instance indicates that no resource bundle has been specified yet,
    // and it will be shared by all loggers which have no resource bundle.
    private static final LoggerBundle NO_RESOURCE_BUNDLE =
            new LoggerBundle(null, null);

    // Calling SharedSecrets.getJavaUtilResourceBundleAccess()
    // forces the initialization of ResourceBundle.class, which
    // can be too early if the VM has not finished booting yet.
    private static final class RbAccess {
        static final JavaUtilResourceBundleAccess RB_ACCESS =
            SharedSecrets.getJavaUtilResourceBundleAccess();
    }

    // A value class that holds the logger configuration data.
    // This configuration can be shared between an application logger
    // and a system logger of the same name.
    private static final class ConfigurationData {

        // The delegate field is used to avoid races while
        // merging configuration. This will ensure that any pending
        // configuration action on an application logger will either
        // be finished before the merge happens, or will be forwarded
        // to the system logger configuration after the merge is completed.
        // By default delegate=this.
        private volatile ConfigurationData delegate;

        volatile boolean useParentHandlers;
        volatile Filter filter;
        volatile Level levelObject;
        volatile int levelValue;  // current effective level value
        final CopyOnWriteArrayList<Handler> handlers =
            new CopyOnWriteArrayList<>();

        ConfigurationData() {
            delegate = this;
            useParentHandlers = true;
            levelValue = Level.INFO.intValue();
        }

        void setUseParentHandlers(boolean flag) {
            useParentHandlers = flag;
            if (delegate != this) {
                // merge in progress - propagate value to system peer.
                final ConfigurationData system = delegate;
                synchronized (system) {
                    system.useParentHandlers = useParentHandlers;
                }
            }
        }

        void setFilter(Filter f) {
            filter = f;
            if (delegate != this) {
                // merge in progress - propagate value to system peer.
                final ConfigurationData system = delegate;
                synchronized (system) {
                    system.filter = filter;
                }
            }
        }

        void setLevelObject(Level l) {
            levelObject = l;
            if (delegate != this) {
                // merge in progress - propagate value to system peer.
                final ConfigurationData system = delegate;
                synchronized (system) {
                    system.levelObject = levelObject;
                }
            }
        }

        void setLevelValue(int v) {
            levelValue = v;
            if (delegate != this) {
                // merge in progress - propagate value to system peer.
                final ConfigurationData system = delegate;
                synchronized (system) {
                    system.levelValue = levelValue;
                }
            }
        }

        void addHandler(Handler h) {
            if (handlers.add(h)) {
                if (delegate != this) {
                    // merge in progress - propagate value to system peer.
                    final ConfigurationData system = delegate;
                    synchronized (system) {
                        system.handlers.addIfAbsent(h);
                    }
                }
            }
        }

        void removeHandler(Handler h) {
            if (handlers.remove(h)) {
                if (delegate != this) {
                    // merge in progress - propagate value to system peer.
                    final ConfigurationData system = delegate;
                    synchronized (system) {
                        system.handlers.remove(h);
                    }
                }
            }
        }

        ConfigurationData merge(Logger systemPeer) {
            if (!systemPeer.isSystemLogger) {
                // should never come here
                throw new InternalError("not a system logger");
            }

            ConfigurationData system = systemPeer.config;

            if (system == this) {
                // nothing to do
                return system;
            }

            synchronized (system) {
                // synchronize before checking on delegate to counter
                // race conditions where two threads might attempt to
                // merge concurrently
                if (delegate == system) {
                    // merge already performed;
                    return system;
                }

                // publish system as the temporary delegate configuration.
                // This should take care of potential race conditions where
                // an other thread might attempt to call e.g. setlevel on
                // the application logger while merge is in progress.
                // (see implementation of ConfigurationData::setLevel)
                delegate = system;

                // merge this config object data into the system config
                system.useParentHandlers = useParentHandlers;
                system.filter = filter;
                system.levelObject = levelObject;
                system.levelValue = levelValue;

                // Prevent race condition in case two threads attempt to merge
                // configuration and add handlers at the same time. We don't want
                // to add the same handlers twice.
                //
                // Handlers are created and loaded by LogManager.addLogger. If we
                // reach here, then it means that the application logger has
                // been created first and added with LogManager.addLogger, and the
                // system logger was created after - and no handler has been added
                // to it by LogManager.addLogger. Therefore, system.handlers
                // should be empty.
                //
                // A non empty cfg.handlers list indicates a race condition
                // where two threads might attempt to merge the configuration
                // or add handlers concurrently. Though of no consequence for
                // the other data (level etc...) this would be an issue if we
                // added the same handlers twice.
                //
                for (Handler h : handlers) {
                    if (!system.handlers.contains(h)) {
                        systemPeer.addHandler(h);
                    }
                }
                system.handlers.retainAll(handlers);
                system.handlers.addAllAbsent(handlers);
            }

            // sanity: update effective level after merging
            synchronized(treeLock) {
                systemPeer.updateEffectiveLevel();
            }

            return system;
        }

    }

    // The logger configuration data. Ideally, this should be final
    // for system loggers, and replace-once for application loggers.
    // When an application requests a logger by name, we do not know a-priori
    // whether that corresponds to a system logger name or not.
    // So if no system logger by that name already exists, we simply return an
    // application logger.
    // If a system class later requests a system logger of the same name, then
    // the application logger and system logger configurations will be merged
    // in a single instance of ConfigurationData that both loggers will share.
    private volatile ConfigurationData config;

    private volatile LogManager manager;
    private String name;
    private volatile LoggerBundle loggerBundle = NO_RESOURCE_BUNDLE;
    private boolean anonymous;

    // Cache to speed up behavior of findResourceBundle:
    private WeakReference<ResourceBundle> catalogRef;  // Cached resource bundle
    private String catalogName;         // name associated with catalog
    private Locale catalogLocale;       // locale associated with catalog

    // The fields relating to parent-child relationships and levels
    // are managed under a separate lock, the treeLock.
    private static final Object treeLock = new Object();
    // We keep weak references from parents to children, but strong
    // references from children to parents.
    private volatile Logger parent;    // our nearest parent.
    private ArrayList<LogManager.LoggerWeakRef> kids;   // WeakReferences to loggers that have us as parent
    private WeakReference<Module> callerModuleRef;
    private final boolean isSystemLogger;

    /**
     * GLOBAL_LOGGER_NAME is a name for the global logger.
     *
     * @since 1.6
     */
    public static final String GLOBAL_LOGGER_NAME = "global";

    /**
     * Return global logger object with the name Logger.GLOBAL_LOGGER_NAME.
     *
     * @return global logger object
     * @since 1.7
     */
    public static final Logger getGlobal() {
        // In order to break a cyclic dependence between the LogManager
        // and Logger static initializers causing deadlocks, the global
        // logger is created with a special constructor that does not
        // initialize its log manager.
        //
        // If an application calls Logger.getGlobal() before any logger
        // has been initialized, it is therefore possible that the
        // LogManager class has not been initialized yet, and therefore
        // Logger.global.manager will be null.
        //
        // In order to finish the initialization of the global logger, we
        // will therefore call LogManager.getLogManager() here.
        //
        // To prevent race conditions we also need to call
        // LogManager.getLogManager() unconditionally here.
        // Indeed we cannot rely on the observed value of global.manager,
        // because global.manager will become not null somewhere during
        // the initialization of LogManager.
        // If two threads are calling getGlobal() concurrently, one thread
        // will see global.manager null and call LogManager.getLogManager(),
        // but the other thread could come in at a time when global.manager
        // is already set although ensureLogManagerInitialized is not finished
        // yet...
        // Calling LogManager.getLogManager() unconditionally will fix that.

        LogManager.getLogManager();

        // Now the global LogManager should be initialized,
        // and the global logger should have been added to
        // it, unless we were called within the constructor of a LogManager
        // subclass installed as LogManager, in which case global.manager
        // would still be null, and global will be lazily initialized later on.

        return global;
    }

    /**
     * The "global" Logger object is provided as a convenience to developers
     * who are making casual use of the Logging package.  Developers
     * who are making serious use of the logging package (for example
     * in products) should create and use their own Logger objects,
     * with appropriate names, so that logging can be controlled on a
     * suitable per-Logger granularity. Developers also need to keep a
     * strong reference to their Logger objects to prevent them from
     * being garbage collected.
     *
     * @deprecated Initialization of this field is prone to deadlocks.
     * The field must be initialized by the Logger class initialization
     * which may cause deadlocks with the LogManager class initialization.
     * In such cases two class initialization wait for each other to complete.
     * The preferred way to get the global logger object is via the call
     * {@code Logger.getGlobal()}.
     * For compatibility with old JDK versions where the
     * {@code Logger.getGlobal()} is not available use the call
     * {@code Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)}
     * or {@code Logger.getLogger("global")}.
     */
    @Deprecated
    public static final Logger global = new Logger(GLOBAL_LOGGER_NAME);

    /**
     * Protected method to construct a logger for a named subsystem.
     * <p>
     * The logger will be initially configured with a null Level
     * and with useParentHandlers set to true.
     *
     * @param   name    A name for the logger.  This should
     *                          be a dot-separated name and should normally
     *                          be based on the package name or class name
     *                          of the subsystem, such as java.net
     *                          or javax.swing.  It may be null for anonymous Loggers.
     * @param   resourceBundleName  name of ResourceBundle to be used for localizing
     *                          messages for this logger.  May be null if none
     *                          of the messages require localization.
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *             no corresponding resource can be found.
     */
    protected Logger(String name, String resourceBundleName) {
        this(name, resourceBundleName, null, LogManager.getLogManager(), false);
    }

    Logger(String name, String resourceBundleName, Module caller,
           LogManager manager, boolean isSystemLogger) {
        this.manager = manager;
        this.isSystemLogger = isSystemLogger;
        this.config = new ConfigurationData();
        this.name = name;
        setupResourceInfo(resourceBundleName, caller);
    }

    // Called by LogManager when a system logger is created
    // after a user logger of the same name.
    // Ensure that both loggers will share the same
    // configuration.
    final void mergeWithSystemLogger(Logger system) {
        // sanity checks
        if (!system.isSystemLogger
                || anonymous
                || name == null
                || !name.equals(system.name)) {
            // should never come here
            throw new InternalError("invalid logger merge");
        }
        checkPermission();
        final ConfigurationData cfg = config;
        if (cfg != system.config) {
            config = cfg.merge(system);
        }
    }

    private void setCallerModuleRef(Module callerModule) {
        if (callerModule != null) {
            this.callerModuleRef = new WeakReference<>(callerModule);
        }
    }

    private Module getCallerModule() {
        return (callerModuleRef != null)
                ? callerModuleRef.get()
                : null;
    }

    // This constructor is used only to create the global Logger.
    // It is needed to break a cyclic dependence between the LogManager
    // and Logger static initializers causing deadlocks.
    private Logger(String name) {
        // The manager field is not initialized here.
        this.name = name;
        this.isSystemLogger = true;
        config = new ConfigurationData();
    }

    // It is called from LoggerContext.addLocalLogger() when the logger
    // is actually added to a LogManager.
    void setLogManager(LogManager manager) {
        this.manager = manager;
    }

    private void checkPermission() throws SecurityException {
        if (!anonymous) {
            if (manager == null) {
                // Complete initialization of the global Logger.
                manager = LogManager.getLogManager();
            }
            manager.checkPermission();
        }
    }

    // Until all JDK code converted to call sun.util.logging.PlatformLogger
    // (see 7054233), we need to determine if Logger.getLogger is to add
    // a system logger or user logger.
    //
    // As an interim solution, if the immediate caller whose caller loader is
    // null, we assume it's a system logger and add it to the system context.
    // These system loggers only set the resource bundle to the given
    // resource bundle name (rather than the default system resource bundle).
    private static class SystemLoggerHelper {
        static boolean disableCallerCheck = getBooleanProperty("sun.util.logging.disableCallerCheck");
        private static boolean getBooleanProperty(final String key) {
            @SuppressWarnings("removal")
            String s = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(key);
                }
            });
            return Boolean.parseBoolean(s);
        }
    }

    private static Logger demandLogger(String name, String resourceBundleName, Class<?> caller) {
        LogManager manager = LogManager.getLogManager();
        if (!SystemLoggerHelper.disableCallerCheck) {
            if (isSystem(caller.getModule())) {
                return manager.demandSystemLogger(name, resourceBundleName, caller);
            }
        }
        return manager.demandLogger(name, resourceBundleName, caller);
        // ends up calling new Logger(name, resourceBundleName, caller)
        // iff the logger doesn't exist already
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager configuration and it will be configured
     * to also send logging output to its parent's Handlers.  It will
     * be registered in the LogManager global namespace.
     * <p>
     * Note: The LogManager may only retain a weak reference to the newly
     * created Logger. It is important to understand that a previously
     * created Logger with the given name may be garbage collected at any
     * time if there is no strong reference to the Logger. In particular,
     * this means that two back-to-back calls like
     * {@code getLogger("MyLogger").log(...)} may use different Logger
     * objects named "MyLogger" if there is no strong reference to the
     * Logger named "MyLogger" elsewhere in the program.
     *
     * @param   name            A name for the logger.  This should
     *                          be a dot-separated name and should normally
     *                          be based on the package name or class name
     *                          of the subsystem, such as java.net
     *                          or javax.swing
     * @return a suitable Logger
     * @throws NullPointerException if the name is null.
     */

    // Synchronization is not required here. All synchronization for
    // adding a new Logger object is handled by LogManager.addLogger().
    @CallerSensitive
    public static Logger getLogger(String name) {
        // This method is intentionally not a wrapper around a call
        // to getLogger(name, resourceBundleName). If it were then
        // this sequence:
        //
        //     getLogger("Foo", "resourceBundleForFoo");
        //     getLogger("Foo");
        //
        // would throw an IllegalArgumentException in the second call
        // because the wrapper would result in an attempt to replace
        // the existing "resourceBundleForFoo" with null.
        return Logger.getLogger(name, Reflection.getCallerClass());
    }

    /**
     * Find or create a logger for a named subsystem on behalf
     * of the given caller.
     *
     * This method is called by {@link #getLogger(java.lang.String)} after
     * it has obtained a reference to its caller's class.
     *
     * @param   name            A name for the logger.
     * @param   callerClass     The class that called {@link
     *                          #getLogger(java.lang.String)}.
     * @return a suitable Logger for {@code callerClass}.
     */
    private static Logger getLogger(String name, Class<?> callerClass) {
        return demandLogger(name, null, callerClass);
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     *
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager and it will be configured to also send logging
     * output to its parent's Handlers.  It will be registered in
     * the LogManager global namespace.
     * <p>
     * Note: The LogManager may only retain a weak reference to the newly
     * created Logger. It is important to understand that a previously
     * created Logger with the given name may be garbage collected at any
     * time if there is no strong reference to the Logger. In particular,
     * this means that two back-to-back calls like
     * {@code getLogger("MyLogger", ...).log(...)} may use different Logger
     * objects named "MyLogger" if there is no strong reference to the
     * Logger named "MyLogger" elsewhere in the program.
     * <p>
     * If the named Logger already exists and does not yet have a
     * localization resource bundle then the given resource bundle
     * name is used. If the named Logger already exists and has
     * a different resource bundle name then an IllegalArgumentException
     * is thrown.
     *
     * @param   name    A name for the logger.  This should
     *                          be a dot-separated name and should normally
     *                          be based on the package name or class name
     *                          of the subsystem, such as java.net
     *                          or javax.swing
     * @param   resourceBundleName  name of ResourceBundle to be used for localizing
     *                          messages for this logger. May be {@code null}
     *                          if none of the messages require localization.
     * @return a suitable Logger
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *             no corresponding resource can be found.
     * @throws IllegalArgumentException if the Logger already exists and uses
     *             a different resource bundle name; or if
     *             {@code resourceBundleName} is {@code null} but the named
     *             logger has a resource bundle set.
     * @throws NullPointerException if the name is null.
     */

    // Synchronization is not required here. All synchronization for
    // adding a new Logger object is handled by LogManager.addLogger().
    @CallerSensitive
    public static Logger getLogger(String name, String resourceBundleName) {
        return Logger.getLogger(name, resourceBundleName, Reflection.getCallerClass());
    }

    /**
     * Find or create a logger for a named subsystem on behalf
     * of the given caller.
     *
     * This method is called by {@link
     * #getLogger(java.lang.String, java.lang.String)} after
     * it has obtained a reference to its caller's class.
     *
     * @param   name            A name for the logger.
     * @param   resourceBundleName  name of ResourceBundle to be used for localizing
     *                          messages for this logger. May be {@code null}
     *                          if none of the messages require localization.
     * @param   callerClass     The class that called {@link
     *                          #getLogger(java.lang.String, java.lang.String)}.
     *                          This class will also be used for locating the
     *                          resource bundle if {@code resourceBundleName} is
     *                          not {@code null}.
     * @return a suitable Logger for {@code callerClass}.
     */
    private static Logger getLogger(String name, String resourceBundleName,
                                    Class<?> callerClass) {
        Logger result = demandLogger(name, resourceBundleName, callerClass);

        // MissingResourceException or IllegalArgumentException can be
        // thrown by setupResourceInfo().
        // We have to set the callers ClassLoader here in case demandLogger
        // above found a previously created Logger.  This can happen, for
        // example, if Logger.getLogger(name) is called and subsequently
        // Logger.getLogger(name, resourceBundleName) is called.  In this case
        // we won't necessarily have the correct classloader saved away, so
        // we need to set it here, too.

        result.setupResourceInfo(resourceBundleName, callerClass);
        return result;
    }

    // package-private
    // Add a platform logger to the system context.
    // i.e. caller of sun.util.logging.PlatformLogger.getLogger
    static Logger getPlatformLogger(String name) {
        LogManager manager = LogManager.getLogManager();

        // all loggers in the system context will default to
        // the system logger's resource bundle - therefore the caller won't
        // be needed and can be null.
        Logger result = manager.demandSystemLogger(name, SYSTEM_LOGGER_RB_NAME, (Module)null);
        return result;
    }

    /**
     * Create an anonymous Logger.  The newly created Logger is not
     * registered in the LogManager namespace.  There will be no
     * access checks on updates to the logger.
     * <p>
     * This factory method is primarily intended for use from applets.
     * Because the resulting Logger is anonymous it can be kept private
     * by the creating class.  This removes the need for normal security
     * checks, which in turn allows untrusted applet code to update
     * the control state of the Logger.  For example an applet can do
     * a setLevel or an addHandler on an anonymous Logger.
     * <p>
     * Even although the new logger is anonymous, it is configured
     * to have the root logger ("") as its parent.  This means that
     * by default it inherits its effective level and handlers
     * from the root logger. Changing its parent via the
     * {@link #setParent(java.util.logging.Logger) setParent} method
     * will still require the security permission specified by that method.
     *
     * @return a newly created private Logger
     */
    public static Logger getAnonymousLogger() {
        return getAnonymousLogger(null);
    }

    /**
     * Create an anonymous Logger.  The newly created Logger is not
     * registered in the LogManager namespace.  There will be no
     * access checks on updates to the logger.
     * <p>
     * This factory method is primarily intended for use from applets.
     * Because the resulting Logger is anonymous it can be kept private
     * by the creating class.  This removes the need for normal security
     * checks, which in turn allows untrusted applet code to update
     * the control state of the Logger.  For example an applet can do
     * a setLevel or an addHandler on an anonymous Logger.
     * <p>
     * Even although the new logger is anonymous, it is configured
     * to have the root logger ("") as its parent.  This means that
     * by default it inherits its effective level and handlers
     * from the root logger.  Changing its parent via the
     * {@link #setParent(java.util.logging.Logger) setParent} method
     * will still require the security permission specified by that method.
     *
     * @param   resourceBundleName  name of ResourceBundle to be used for localizing
     *                          messages for this logger.
     *          May be null if none of the messages require localization.
     * @return a newly created private Logger
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *             no corresponding resource can be found.
     */

    // Synchronization is not required here. All synchronization for
    // adding a new anonymous Logger object is handled by doSetParent().
    @CallerSensitive
    public static Logger getAnonymousLogger(String resourceBundleName) {
        LogManager manager = LogManager.getLogManager();
        // cleanup some Loggers that have been GC'ed
        manager.drainLoggerRefQueueBounded();
        final Class<?> callerClass = Reflection.getCallerClass();
        final Module module = callerClass.getModule();
        Logger result = new Logger(null, resourceBundleName,
                                   module, manager, false);
        result.anonymous = true;
        Logger root = manager.getLogger("");
        result.doSetParent(root);
        return result;
    }

    /**
     * Retrieve the localization resource bundle for this
     * logger.
     * This method will return a {@code ResourceBundle} that was either
     * set by the {@link
     * #setResourceBundle(java.util.ResourceBundle) setResourceBundle} method or
     * <a href="#ResourceBundleMapping">mapped from the
     * the resource bundle name</a> set via the {@link
     * Logger#getLogger(java.lang.String, java.lang.String) getLogger} factory
     * method for the current default locale.
     * <br>Note that if the result is {@code null}, then the Logger will use a resource
     * bundle or resource bundle name inherited from its parent.
     *
     * @return localization bundle (may be {@code null})
     */
    public ResourceBundle getResourceBundle() {
        return findResourceBundle(getResourceBundleName(), true);
    }

    /**
     * Retrieve the localization resource bundle name for this
     * logger.
     * This is either the name specified through the {@link
     * #getLogger(java.lang.String, java.lang.String) getLogger} factory method,
     * or the {@linkplain ResourceBundle#getBaseBundleName() base name} of the
     * ResourceBundle set through {@link
     * #setResourceBundle(java.util.ResourceBundle) setResourceBundle} method.
     * <br>Note that if the result is {@code null}, then the Logger will use a resource
     * bundle or resource bundle name inherited from its parent.
     *
     * @return localization bundle name (may be {@code null})
     */
    public String getResourceBundleName() {
        return loggerBundle.resourceBundleName;
    }

    /**
     * Set a filter to control output on this Logger.
     * <P>
     * After passing the initial "level" check, the Logger will
     * call this Filter to check if a log record should really
     * be published.
     *
     * @param   newFilter  a filter object (may be null)
     * @throws  SecurityException if a security manager exists,
     *          this logger is not anonymous, and the caller
     *          does not have LoggingPermission("control").
     */
    public void setFilter(Filter newFilter) throws SecurityException {
        checkPermission();
        config.setFilter(newFilter);
    }

    /**
     * Get the current filter for this Logger.
     *
     * @return  a filter object (may be null)
     */
    public Filter getFilter() {
        return config.filter;
    }

    /**
     * Log a LogRecord.
     * <p>
     * All the other logging methods in this class call through
     * this method to actually perform any logging.  Subclasses can
     * override this single method to capture all log activity.
     *
     * @param record the LogRecord to be published
     */
    public void log(LogRecord record) {
        if (!isLoggable(record.getLevel())) {
            return;
        }
        Filter theFilter = config.filter;
        if (theFilter != null && !theFilter.isLoggable(record)) {
            return;
        }

        // Post the LogRecord to all our Handlers, and then to
        // our parents' handlers, all the way up the tree.

        Logger logger = this;
        while (logger != null) {
            final Handler[] loggerHandlers = isSystemLogger
                ? logger.accessCheckedHandlers()
                : logger.getHandlers();

            for (Handler handler : loggerHandlers) {
                handler.publish(record);
            }

            final boolean useParentHdls = isSystemLogger
                ? logger.config.useParentHandlers
                : logger.getUseParentHandlers();

            if (!useParentHdls) {
                break;
            }

            logger = isSystemLogger ? logger.parent : logger.getParent();
        }
    }

    // private support method for logging.
    // We fill in the logger name, resource bundle name, and
    // resource bundle and then call "void log(LogRecord)".
    private void doLog(LogRecord lr) {
        lr.setLoggerName(name);
        final LoggerBundle lb = getEffectiveLoggerBundle();
        final ResourceBundle  bundle = lb.userBundle;
        final String ebname = lb.resourceBundleName;
        if (ebname != null && bundle != null) {
            lr.setResourceBundleName(ebname);
            lr.setResourceBundle(bundle);
        }
        log(lr);
    }


    //================================================================
    // Start of convenience methods WITHOUT className and methodName
    //================================================================

    /**
     * Log a message, with no arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void log(Level level, String msg) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        doLog(lr);
    }

    /**
     * Log a message, which is only to be constructed if the logging level
     * is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since 1.8
     */
    public void log(Level level, Supplier<String> msgSupplier) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        doLog(lr);
    }

    /**
     * Log a message, with one object parameter.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   param1  parameter to the message
     */
    public void log(Level level, String msg, Object param1) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        Object params[] = { param1 };
        lr.setParameters(params);
        doLog(lr);
    }

    /**
     * Log a message, with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  array of parameters to the message
     */
    public void log(Level level, String msg, Object params[]) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setParameters(params);
        doLog(lr);
    }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    public void log(Level level, String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setThrown(thrown);
        doLog(lr);
    }

    /**
     * Log a lazily constructed message, with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message level then the
     * message is constructed by invoking the provided supplier function. The
     * message and the given {@link Throwable} are then stored in a {@link
     * LogRecord} which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   thrown  Throwable associated with log message.
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        lr.setThrown(thrown);
        doLog(lr);
    }

    //================================================================
    // Start of convenience methods WITH className and methodName
    //================================================================

    /**
     * Log a message, specifying source class and method,
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        doLog(lr);
    }

    /**
     * Log a lazily constructed message, specifying source class and method,
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void logp(Level level, String sourceClass, String sourceMethod,
                     Supplier<String> msgSupplier) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        doLog(lr);
    }

    /**
     * Log a message, specifying source class and method,
     * with a single object parameter to the log message.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg      The string message (or a key in the message catalog)
     * @param   param1    Parameter to the log message.
     */
    public void logp(Level level, String sourceClass, String sourceMethod,
                                                String msg, Object param1) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        Object params[] = { param1 };
        lr.setParameters(params);
        doLog(lr);
    }

    /**
     * Log a message, specifying source class and method,
     * with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  Array of parameters to the message
     */
    public void logp(Level level, String sourceClass, String sourceMethod,
                                                String msg, Object params[]) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setParameters(params);
        doLog(lr);
    }

    /**
     * Log a message, specifying source class and method,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    public void logp(Level level, String sourceClass, String sourceMethod,
                     String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr);
    }

    /**
     * Log a lazily constructed message, specifying source class and method,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message level then the
     * message is constructed by invoking the provided supplier function. The
     * message and the given {@link Throwable} are then stored in a {@link
     * LogRecord} which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   thrown  Throwable associated with log message.
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void logp(Level level, String sourceClass, String sourceMethod,
                     Throwable thrown, Supplier<String> msgSupplier) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr);
    }


    //=========================================================================
    // Start of convenience methods WITH className, methodName and bundle name.
    //=========================================================================

    // Private support method for logging for "logrb" methods.
    // We fill in the logger name, resource bundle name, and
    // resource bundle and then call "void log(LogRecord)".
    private void doLog(LogRecord lr, String rbname) {
        lr.setLoggerName(name);
        if (rbname != null) {
            lr.setResourceBundleName(rbname);
            lr.setResourceBundle(findResourceBundle(rbname, false));
        }
        log(lr);
    }

    // Private support method for logging for "logrb" methods.
    private void doLog(LogRecord lr, ResourceBundle rb) {
        lr.setLoggerName(name);
        if (rb != null) {
            lr.setResourceBundleName(rb.getBaseBundleName());
            lr.setResourceBundle(rb);
        }
        log(lr);
    }

    /**
     * Log a message, specifying source class, method, and resource bundle name
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null
     * @param   msg     The string message (or a key in the message catalog)
     * @deprecated Use {@link #logrb(java.util.logging.Level, java.lang.String,
     * java.lang.String, java.util.ResourceBundle, java.lang.String,
     * java.lang.Object...)} instead.
     */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                                String bundleName, String msg) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        doLog(lr, bundleName);
    }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with a single object parameter to the log message.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null
     * @param   msg      The string message (or a key in the message catalog)
     * @param   param1    Parameter to the log message.
     * @deprecated Use {@link #logrb(java.util.logging.Level, java.lang.String,
     *   java.lang.String, java.util.ResourceBundle, java.lang.String,
     *   java.lang.Object...)} instead
     */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                                String bundleName, String msg, Object param1) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        Object params[] = { param1 };
        lr.setParameters(params);
        doLog(lr, bundleName);
    }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null.
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  Array of parameters to the message
     * @deprecated Use {@link #logrb(java.util.logging.Level, java.lang.String,
     *      java.lang.String, java.util.ResourceBundle, java.lang.String,
     *      java.lang.Object...)} instead.
     */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                                String bundleName, String msg, Object params[]) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setParameters(params);
        doLog(lr, bundleName);
    }

    /**
     * Log a message, specifying source class, method, and resource bundle,
     * with an optional list of message parameters.
     * <p>
     * If the logger is currently enabled for the given message
     * {@code level} then a corresponding {@code LogRecord} is created and
     * forwarded to all the registered output {@code Handler} objects.
     * <p>
     * The {@code msg} string is localized using the given resource bundle.
     * If the resource bundle is {@code null}, then the {@code msg} string is not
     * localized.
     *
     * @param   level   One of the message level identifiers, e.g., {@code SEVERE}
     * @param   sourceClass    Name of the class that issued the logging request
     * @param   sourceMethod   Name of the method that issued the logging request
     * @param   bundle         Resource bundle to localize {@code msg},
     *                         can be {@code null}.
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  Parameters to the message (optional, may be none).
     * @since 1.8
     */
    public void logrb(Level level, String sourceClass, String sourceMethod,
                      ResourceBundle bundle, String msg, Object... params) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        if (params != null && params.length != 0) {
            lr.setParameters(params);
        }
        doLog(lr, bundle);
    }

    /**
     * Log a message, specifying source class, method, and resource bundle,
     * with an optional list of message parameters.
     * <p>
     * If the logger is currently enabled for the given message
     * {@code level} then a corresponding {@code LogRecord} is created
     * and forwarded to all the registered output {@code Handler} objects.
     * <p>
     * The {@code msg} string is localized using the given resource bundle.
     * If the resource bundle is {@code null}, then the {@code msg} string is not
     * localized.
     *
     * @param   level   One of the message level identifiers, e.g., {@code SEVERE}
     * @param   bundle  Resource bundle to localize {@code msg};
     *                  can be {@code null}.
     * @param   msg     The string message (or a key in the message catalog)
     * @param   params  Parameters to the message (optional, may be none).
     * @since 9
     */
    public void logrb(Level level, ResourceBundle bundle, String msg, Object... params) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        if (params != null && params.length != 0) {
            lr.setParameters(params);
        }
        doLog(lr, bundle);
    }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, or an empty String or invalid
     * then the msg string is not localized.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     *
     * @param   level   One of the message level identifiers, e.g., SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg,
     *                         can be null
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     * @deprecated Use {@link #logrb(java.util.logging.Level, java.lang.String,
     *     java.lang.String, java.util.ResourceBundle, java.lang.String,
     *     java.lang.Throwable)} instead.
     */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                                        String bundleName, String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr, bundleName);
    }

    /**
     * Log a message, specifying source class, method, and resource bundle,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message
     * {@code level} then the given arguments are stored in a {@code LogRecord}
     * which is forwarded to all registered output handlers.
     * <p>
     * The {@code msg} string is localized using the given resource bundle.
     * If the resource bundle is {@code null}, then the {@code msg} string is not
     * localized.
     * <p>
     * Note that the {@code thrown} argument is stored in the {@code LogRecord}
     * {@code thrown} property, rather than the {@code LogRecord}
     * {@code parameters} property.  Thus it is
     * processed specially by output {@code Formatter} objects and is not treated
     * as a formatting parameter to the {@code LogRecord} {@code message} property.
     *
     * @param   level   One of the message level identifiers, e.g., {@code SEVERE}
     * @param   sourceClass    Name of the class that issued the logging request
     * @param   sourceMethod   Name of the method that issued the logging request
     * @param   bundle         Resource bundle to localize {@code msg},
     *                         can be {@code null}
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with the log message.
     * @since 1.8
     */
    public void logrb(Level level, String sourceClass, String sourceMethod,
                      ResourceBundle bundle, String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr, bundle);
    }

    /**
     * Log a message, specifying source class, method, and resource bundle,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message
     * {@code level} then the given arguments are stored in a {@code LogRecord}
     * which is forwarded to all registered output handlers.
     * <p>
     * The {@code msg} string is localized using the given resource bundle.
     * If the resource bundle is {@code null}, then the {@code msg} string is not
     * localized.
     * <p>
     * Note that the {@code thrown} argument is stored in the {@code LogRecord}
     * {@code thrown} property, rather than the {@code LogRecord}
     * {@code parameters} property.  Thus it is
     * processed specially by output {@code Formatter} objects and is not treated
     * as a formatting parameter to the {@code LogRecord} {@code message}
     * property.
     *
     * @param   level   One of the message level identifiers, e.g., {@code SEVERE}
     * @param   bundle  Resource bundle to localize {@code msg};
     *                  can be {@code null}.
     * @param   msg     The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with the log message.
     * @since 9
     */
    public void logrb(Level level, ResourceBundle bundle, String msg,
            Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setThrown(thrown);
        doLog(lr, bundle);
    }

    //======================================================================
    // Start of convenience methods for logging method entries and returns.
    //======================================================================

    /**
     * Log a method entry.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     *
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     */
    public void entering(String sourceClass, String sourceMethod) {
        logp(Level.FINER, sourceClass, sourceMethod, "ENTRY");
    }

    /**
     * Log a method entry, with one parameter.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY {0}", log level
     * FINER, and the given sourceMethod, sourceClass, and parameter
     * is logged.
     *
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   param1         parameter to the method being entered
     */
    public void entering(String sourceClass, String sourceMethod, Object param1) {
        logp(Level.FINER, sourceClass, sourceMethod, "ENTRY {0}", param1);
    }

    /**
     * Log a method entry, with an array of parameters.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY" (followed by a
     * format {N} indicator for each entry in the parameter array),
     * log level FINER, and the given sourceMethod, sourceClass, and
     * parameters is logged.
     *
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   params         array of parameters to the method being entered
     */
    public void entering(String sourceClass, String sourceMethod, Object params[]) {
        String msg = "ENTRY";
        if (params == null ) {
           logp(Level.FINER, sourceClass, sourceMethod, msg);
           return;
        }
        if (!isLoggable(Level.FINER)) return;
        if (params.length > 0) {
            final StringBuilder b = new StringBuilder(msg);
            for (int i = 0; i < params.length; i++) {
                b.append(' ').append('{').append(i).append('}');
            }
            msg = b.toString();
        }
        logp(Level.FINER, sourceClass, sourceMethod, msg, params);
    }

    /**
     * Log a method return.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     *
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method
     */
    public void exiting(String sourceClass, String sourceMethod) {
        logp(Level.FINER, sourceClass, sourceMethod, "RETURN");
    }


    /**
     * Log a method return, with result object.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN {0}", log level
     * FINER, and the gives sourceMethod, sourceClass, and result
     * object is logged.
     *
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method
     * @param   result  Object that is being returned
     */
    public void exiting(String sourceClass, String sourceMethod, Object result) {
        logp(Level.FINER, sourceClass, sourceMethod, "RETURN {0}", result);
    }

    /**
     * Log throwing an exception.
     * <p>
     * This is a convenience method to log that a method is
     * terminating by throwing an exception.  The logging is done
     * using the FINER level.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.  The
     * LogRecord's message is set to "THROW".
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     *
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod  name of the method.
     * @param   thrown  The Throwable that is being thrown.
     */
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (!isLoggable(Level.FINER)) {
            return;
        }
        LogRecord lr = new LogRecord(Level.FINER, "THROW");
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr);
    }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    //=======================================================================

    /**
     * Log a SEVERE message.
     * <p>
     * If the logger is currently enabled for the SEVERE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     * Log a WARNING message.
     * <p>
     * If the logger is currently enabled for the WARNING message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void warning(String msg) {
        log(Level.WARNING, msg);
    }

    /**
     * Log an INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    /**
     * Log a CONFIG message.
     * <p>
     * If the logger is currently enabled for the CONFIG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void config(String msg) {
        log(Level.CONFIG, msg);
    }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void fine(String msg) {
        log(Level.FINE, msg);
    }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void finer(String msg) {
        log(Level.FINER, msg);
    }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void finest(String msg) {
        log(Level.FINEST, msg);
    }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    // and use Supplier<String>
    //=======================================================================

    /**
     * Log a SEVERE message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the SEVERE message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void severe(Supplier<String> msgSupplier) {
        log(Level.SEVERE, msgSupplier);
    }

    /**
     * Log a WARNING message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the WARNING message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void warning(Supplier<String> msgSupplier) {
        log(Level.WARNING, msgSupplier);
    }

    /**
     * Log a INFO message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void info(Supplier<String> msgSupplier) {
        log(Level.INFO, msgSupplier);
    }

    /**
     * Log a CONFIG message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the CONFIG message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void config(Supplier<String> msgSupplier) {
        log(Level.CONFIG, msgSupplier);
    }

    /**
     * Log a FINE message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void fine(Supplier<String> msgSupplier) {
        log(Level.FINE, msgSupplier);
    }

    /**
     * Log a FINER message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void finer(Supplier<String> msgSupplier) {
        log(Level.FINER, msgSupplier);
    }

    /**
     * Log a FINEST message, which is only to be constructed if the logging
     * level is such that the message will actually be logged.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the message is constructed by invoking the provided
     * supplier function and forwarded to all the registered output
     * Handler objects.
     *
     * @param   msgSupplier   A function, which when called, produces the
     *                        desired log message
     * @since   1.8
     */
    public void finest(Supplier<String> msgSupplier) {
        log(Level.FINEST, msgSupplier);
    }

    //================================================================
    // End of convenience methods
    //================================================================

    /**
     * Set the log level specifying which message levels will be
     * logged by this logger.  Message levels lower than this
     * value will be discarded.  The level value Level.OFF
     * can be used to turn off logging.
     * <p>
     * If the new level is null, it means that this node should
     * inherit its level from its nearest ancestor with a specific
     * (non-null) level value.
     *
     * @param newLevel   the new value for the log level (may be null)
     * @throws  SecurityException if a security manager exists,
     *          this logger is not anonymous, and the caller
     *          does not have LoggingPermission("control").
     */
    public void setLevel(Level newLevel) throws SecurityException {
        checkPermission();
        synchronized (treeLock) {
            config.setLevelObject(newLevel);
            updateEffectiveLevel();
        }
    }

    final boolean isLevelInitialized() {
        return config.levelObject != null;
    }

    /**
     * Get the log Level that has been specified for this Logger.
     * The result may be null, which means that this logger's
     * effective level will be inherited from its parent.
     *
     * @return  this Logger's level
     */
    public Level getLevel() {
        return config.levelObject;
    }

    /**
     * Check if a message of the given level would actually be logged
     * by this logger.  This check is based on the Loggers effective level,
     * which may be inherited from its parent.
     *
     * @param   level   a message logging level
     * @return  true if the given message level is currently being logged.
     */
    public boolean isLoggable(Level level) {
        int levelValue = config.levelValue;
        if (level.intValue() < levelValue || levelValue == offValue) {
            return false;
        }
        return true;
    }

    /**
     * Get the name for this logger.
     * @return logger name.  Will be null for anonymous Loggers.
     */
    public String getName() {
        return name;
    }

    /**
     * Add a log Handler to receive logging messages.
     * <p>
     * By default, Loggers also send their output to their parent logger.
     * Typically the root Logger is configured with a set of Handlers
     * that essentially act as default handlers for all loggers.
     *
     * @param   handler a logging Handler
     * @throws  SecurityException if a security manager exists,
     *          this logger is not anonymous, and the caller
     *          does not have LoggingPermission("control").
     */
    public void addHandler(Handler handler) throws SecurityException {
        Objects.requireNonNull(handler);
        checkPermission();
        config.addHandler(handler);
    }

    /**
     * Remove a log Handler.
     * <P>
     * Returns silently if the given Handler is not found or is null
     *
     * @param   handler a logging Handler
     * @throws  SecurityException if a security manager exists,
     *          this logger is not anonymous, and the caller
     *          does not have LoggingPermission("control").
     */
    public void removeHandler(Handler handler) throws SecurityException {
        checkPermission();
        if (handler == null) {
            return;
        }
        config.removeHandler(handler);
    }

    /**
     * Get the Handlers associated with this logger.
     *
     * @return  an array of all registered Handlers
     */
    public Handler[] getHandlers() {
        return accessCheckedHandlers();
    }

    // This method should ideally be marked final - but unfortunately
    // it needs to be overridden by LogManager.RootLogger
    Handler[] accessCheckedHandlers() {
        return config.handlers.toArray(emptyHandlers);
    }

    /**
     * Specify whether or not this logger should send its output
     * to its parent Logger.  This means that any LogRecords will
     * also be written to the parent's Handlers, and potentially
     * to its parent, recursively up the namespace.
     *
     * @param useParentHandlers   true if output is to be sent to the
     *          logger's parent.
     * @throws  SecurityException if a security manager exists,
     *          this logger is not anonymous, and the caller
     *          does not have LoggingPermission("control").
     */
    public void setUseParentHandlers(boolean useParentHandlers) {
        checkPermission();
        config.setUseParentHandlers(useParentHandlers);
    }

    /**
     * Discover whether or not this logger is sending its output
     * to its parent logger.
     *
     * @return  true if output is to be sent to the logger's parent
     */
    public boolean getUseParentHandlers() {
        return config.useParentHandlers;
    }

    private ResourceBundle catalog() {
        WeakReference<ResourceBundle> ref = catalogRef;
        return ref == null ? null : ref.get();
    }

    /**
     * Private utility method to map a resource bundle name to an
     * actual resource bundle, using a simple one-entry cache.
     * Returns null for a null name.
     * May also return null if we can't find the resource bundle and
     * there is no suitable previous cached value.
     *
     * @param name the ResourceBundle to locate
     * @param useCallersModule if true search using the caller's module.
     * @return ResourceBundle specified by name or null if not found
     */
    private synchronized ResourceBundle findResourceBundle(String name,
                                                           boolean useCallersModule) {
        // When this method is called from logrb, useCallersModule==false, and
        // the resource bundle 'name' is the argument provided to logrb.
        // It may, or may not be, equal to lb.resourceBundleName.
        // Otherwise, useCallersModule==true, and name is the resource bundle
        // name that is set (or will be set) in this logger.
        //
        // When useCallersModule is false, or when the caller's module is
        // null, or when the caller's module is an unnamed module, we look
        // first in the TCCL (or the System ClassLoader if the TCCL is null)
        // to locate the resource bundle.
        //
        // Otherwise, if useCallersModule is true, and the caller's module is not
        // null, and the caller's module is named, we look in the caller's module
        // to locate the resource bundle.
        //
        // Finally, if the caller's module is not null and is unnamed, and
        // useCallersModule is true, we look in the caller's module class loader
        // (unless we already looked there in step 1).

        // Return a null bundle for a null name.
        if (name == null) {
            return null;
        }

        Locale currentLocale = Locale.getDefault();
        final LoggerBundle lb = loggerBundle;
        ResourceBundle catalog = catalog();

        // Normally we should hit on our simple one entry cache.
        if (lb.userBundle != null &&
                name.equals(lb.resourceBundleName)) {
            return lb.userBundle;
        } else if (catalog != null && currentLocale.equals(catalogLocale)
                    && name.equals(catalogName)) {
            return catalog;
        }

        // Use the thread's context ClassLoader.  If there isn't one, use the
        // {@linkplain java.lang.ClassLoader#getSystemClassLoader() system ClassLoader}.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }

        final Module callerModule = getCallerModule();

        // If useCallersModule is false, we are called by logrb, with a name
        // that is provided by the user. In that case we will look in the TCCL.
        // We also look in the TCCL if callerModule is null or unnamed.
        if (!useCallersModule || callerModule == null || !callerModule.isNamed()) {
            try {
                Module mod = cl.getUnnamedModule();
                catalog = RbAccess.RB_ACCESS.getBundle(name, currentLocale, mod);
                catalogRef = new WeakReference<>(catalog);
                catalogName = name;
                catalogLocale = currentLocale;
                return catalog;
            } catch (MissingResourceException ex) {
                // We can't find the ResourceBundle in the default
                // ClassLoader.  Drop through.
                if (useCallersModule && callerModule != null) {
                    try {
                        // We are called by an unnamed module: try with the
                        // unnamed module class loader:
                        PrivilegedAction<ClassLoader> getModuleClassLoader =
                                () -> callerModule.getClassLoader();
                        @SuppressWarnings("removal")
                        ClassLoader moduleCL =
                                AccessController.doPrivileged(getModuleClassLoader);
                        // moduleCL can be null if the logger is created by a class
                        // appended to the bootclasspath.
                        // If moduleCL is null we would use cl, but we already tried
                        // that above (we first looked in the TCCL for unnamed
                        // caller modules) - so there no point in trying again: we
                        // won't find anything more this second time.
                        // In this case just return null.
                        if (moduleCL == cl || moduleCL == null) return null;

                        // we already tried the TCCL and found nothing - so try
                        // with the module's loader this time.
                        catalog = ResourceBundle.getBundle(name, currentLocale,
                                                           moduleCL);
                        catalogRef = new WeakReference<>(catalog);
                        catalogName = name;
                        catalogLocale = currentLocale;
                        return catalog;
                    } catch (MissingResourceException x) {
                        return null; // no luck
                    }
                } else {
                    return null;
                }
            }
        } else {
            // we should have:
            //  useCallersModule && callerModule != null && callerModule.isNamed();
            // Try with the caller's module
            try {
                // Use the caller's module
                catalog = RbAccess.RB_ACCESS.getBundle(name, currentLocale, callerModule);
                catalogRef = new WeakReference<>(catalog);
                catalogName = name;
                catalogLocale = currentLocale;
                return catalog;
            } catch (MissingResourceException ex) {
                return null; // no luck
            }
        }
    }

    private void setupResourceInfo(String name, Class<?> caller) {
        final Module module = caller == null ? null : caller.getModule();
        setupResourceInfo(name, module);
    }

    // Private utility method to initialize our one entry
    // resource bundle name cache and the callers Module
    // Note: for consistency reasons, we are careful to check
    // that a suitable ResourceBundle exists before setting the
    // resourceBundleName field.
    // Synchronized to prevent races in setting the fields.
    private synchronized void setupResourceInfo(String name,
                                                Module callerModule) {
        final LoggerBundle lb = loggerBundle;
        if (lb.resourceBundleName != null) {
            // this Logger already has a ResourceBundle

            if (lb.resourceBundleName.equals(name)) {
                // the names match so there is nothing more to do
                return;
            }

            // cannot change ResourceBundles once they are set
            throw new IllegalArgumentException(
                lb.resourceBundleName + " != " + name);
        }

        if (name == null) {
            return;
        }

        setCallerModuleRef(callerModule);

        if (isSystemLogger && (callerModule != null && !isSystem(callerModule))) {
            checkPermission();
        }

        if (name.equals(SYSTEM_LOGGER_RB_NAME)) {
            loggerBundle = SYSTEM_BUNDLE;
        } else {
            ResourceBundle bundle = findResourceBundle(name, true);
            if (bundle == null) {
                // We've failed to find an expected ResourceBundle.
                // unset the caller's module since we were unable to find the
                // the bundle using it
                this.callerModuleRef = null;
                throw new MissingResourceException("Can't find " + name + " bundle from ",
                        name, "");
            }

            loggerBundle = LoggerBundle.get(name, null);
        }
    }

    /**
     * Sets a resource bundle on this logger.
     * All messages will be logged using the given resource bundle for its
     * specific {@linkplain ResourceBundle#getLocale locale}.
     * @param bundle The resource bundle that this logger shall use.
     * @throws NullPointerException if the given bundle is {@code null}.
     * @throws IllegalArgumentException if the given bundle doesn't have a
     *         {@linkplain ResourceBundle#getBaseBundleName base name},
     *         or if this logger already has a resource bundle set but
     *         the given bundle has a different base name.
     * @throws SecurityException if a security manager exists,
     *         this logger is not anonymous, and the caller
     *         does not have LoggingPermission("control").
     * @since 1.8
     */
    public void setResourceBundle(ResourceBundle bundle) {
        checkPermission();

        // Will throw NPE if bundle is null.
        final String baseName = bundle.getBaseBundleName();

        // bundle must have a name
        if (baseName == null || baseName.isEmpty()) {
            throw new IllegalArgumentException("resource bundle must have a name");
        }

        synchronized (this) {
            LoggerBundle lb = loggerBundle;
            final boolean canReplaceResourceBundle = lb.resourceBundleName == null
                    || lb.resourceBundleName.equals(baseName);

            if (!canReplaceResourceBundle) {
                throw new IllegalArgumentException("can't replace resource bundle");
            }


            loggerBundle = LoggerBundle.get(baseName, bundle);
        }
    }

    /**
     * Return the parent for this Logger.
     * <p>
     * This method returns the nearest extant parent in the namespace.
     * Thus if a Logger is called "a.b.c.d", and a Logger called "a.b"
     * has been created but no logger "a.b.c" exists, then a call of
     * getParent on the Logger "a.b.c.d" will return the Logger "a.b".
     * <p>
     * The result will be null if it is called on the root Logger
     * in the namespace.
     *
     * @return nearest existing parent Logger
     */
    public Logger getParent() {
        // Note: this used to be synchronized on treeLock.  However, this only
        // provided memory semantics, as there was no guarantee that the caller
        // would synchronize on treeLock (in fact, there is no way for external
        // callers to so synchronize).  Therefore, we have made parent volatile
        // instead.
        return parent;
    }

    /**
     * Set the parent for this Logger.  This method is used by
     * the LogManager to update a Logger when the namespace changes.
     * <p>
     * It should not be called from application code.
     *
     * @param  parent   the new parent logger
     * @throws  SecurityException  if a security manager exists and if
     *          the caller does not have LoggingPermission("control").
     */
    public void setParent(Logger parent) {
        if (parent == null) {
            throw new NullPointerException();
        }

        // check permission for all loggers, including anonymous loggers
        if (manager == null) {
            manager = LogManager.getLogManager();
        }
        manager.checkPermission();

        doSetParent(parent);
    }

    // Private method to do the work for parenting a child
    // Logger onto a parent logger.
    private void doSetParent(Logger newParent) {

        // System.err.println("doSetParent \"" + getName() + "\" \""
        //                              + newParent.getName() + "\"");

        synchronized (treeLock) {

            // Remove ourself from any previous parent.
            LogManager.LoggerWeakRef ref = null;
            if (parent != null) {
                // assert parent.kids != null;
                for (Iterator<LogManager.LoggerWeakRef> iter = parent.kids.iterator(); iter.hasNext(); ) {
                    ref = iter.next();
                    if (ref.refersTo(this)) {
                        // ref is used down below to complete the reparenting
                        iter.remove();
                        break;
                    } else {
                        ref = null;
                    }
                }
                // We have now removed ourself from our parents' kids.
            }

            // Set our new parent.
            parent = newParent;
            if (parent.kids == null) {
                parent.kids = new ArrayList<>(2);
            }
            if (ref == null) {
                // we didn't have a previous parent
                ref = manager.new LoggerWeakRef(this);
            }
            ref.setParentRef(new WeakReference<>(parent));
            parent.kids.add(ref);

            // As a result of the reparenting, the effective level
            // may have changed for us and our children.
            updateEffectiveLevel();

        }
    }

    // Package-level method.
    // Remove the weak reference for the specified child Logger from the
    // kid list. We should only be called from LoggerWeakRef.dispose().
    final void removeChildLogger(LogManager.LoggerWeakRef child) {
        synchronized (treeLock) {
            for (Iterator<LogManager.LoggerWeakRef> iter = kids.iterator(); iter.hasNext(); ) {
                LogManager.LoggerWeakRef ref = iter.next();
                if (ref == child) {
                    iter.remove();
                    return;
                }
            }
        }
    }

    // Recalculate the effective level for this node and
    // recursively for our children.

    private void updateEffectiveLevel() {
        // assert Thread.holdsLock(treeLock);

        // Figure out our current effective level.
        int newLevelValue;
        final ConfigurationData cfg = config;
        final Level levelObject = cfg.levelObject;
        if (levelObject != null) {
            newLevelValue = levelObject.intValue();
        } else {
            if (parent != null) {
                newLevelValue = parent.config.levelValue;
            } else {
                // This may happen during initialization.
                newLevelValue = Level.INFO.intValue();
            }
        }

        // If our effective value hasn't changed, we're done.
        if (cfg.levelValue == newLevelValue) {
            return;
        }

        cfg.setLevelValue(newLevelValue);

        // System.err.println("effective level: \"" + getName() + "\" := " + level);

        // Recursively update the level on each of our kids.
        if (kids != null) {
            for (LogManager.LoggerWeakRef ref : kids) {
                Logger kid = ref.get();
                if (kid != null) {
                    kid.updateEffectiveLevel();
                }
            }
        }
    }


    // Private method to get the potentially inherited
    // resource bundle and resource bundle name for this Logger.
    // This method never returns null.
    private LoggerBundle getEffectiveLoggerBundle() {
        final LoggerBundle lb = loggerBundle;
        if (lb.isSystemBundle()) {
            return SYSTEM_BUNDLE;
        }

        // first take care of this logger
        final ResourceBundle b = getResourceBundle();
        if (b != null && b == lb.userBundle) {
            return lb;
        } else if (b != null) {
            // either lb.userBundle is null or getResourceBundle() is
            // overriden
            final String rbName = getResourceBundleName();
            return LoggerBundle.get(rbName, b);
        }

        // no resource bundle was specified on this logger, look up the
        // parent stack.
        Logger target = this.parent;
        while (target != null) {
            final LoggerBundle trb = target.loggerBundle;
            if (trb.isSystemBundle()) {
                return SYSTEM_BUNDLE;
            }
            if (trb.userBundle != null) {
                return trb;
            }
            final String rbName = isSystemLogger
                // ancestor of a system logger is expected to be a system logger.
                // ignore resource bundle name if it's not.
                ? (target.isSystemLogger ? trb.resourceBundleName : null)
                : target.getResourceBundleName();
            if (rbName != null) {
                return LoggerBundle.get(rbName,
                        findResourceBundle(rbName, true));
            }
            target = isSystemLogger ? target.parent : target.getParent();
        }
        return NO_RESOURCE_BUNDLE;
    }

}

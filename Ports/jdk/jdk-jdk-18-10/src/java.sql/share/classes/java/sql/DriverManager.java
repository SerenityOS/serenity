/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;


/**
 * The basic service for managing a set of JDBC drivers.
 * <p>
 * <strong>NOTE:</strong> The {@link javax.sql.DataSource} interface, provides
 * another way to connect to a data source.
 * The use of a {@code DataSource} object is the preferred means of
 * connecting to a data source.
 * <P>
 * As part of its initialization, the {@code DriverManager} class will
 * attempt to load available JDBC drivers by using:
 * <ul>
 * <li>The {@code jdbc.drivers} system property which contains a
 * colon separated list of fully qualified class names of JDBC drivers. Each
 * driver is loaded using the {@linkplain ClassLoader#getSystemClassLoader
 * system class loader}:
 * <ul>
 * <li>{@code jdbc.drivers=foo.bah.Driver:wombat.sql.Driver:bad.taste.ourDriver}
 * </ul>
 *
 * <li>Service providers of the {@code java.sql.Driver} class, that are loaded
 * via the {@linkplain ServiceLoader#load service-provider loading} mechanism.
 *</ul>
 *
 * @implNote
 * {@code DriverManager} initialization is done lazily and looks up service
 * providers using the thread context class loader.  The drivers loaded and
 * available to an application will depend on the thread context class loader of
 * the thread that triggers driver initialization by {@code DriverManager}.
 *
 * <P>When the method {@code getConnection} is called,
 * the {@code DriverManager} will attempt to
 * locate a suitable driver from amongst those loaded at
 * initialization and those loaded explicitly using the same class loader
 * as the current application.
 *
 * @see Driver
 * @see Connection
 * @since 1.1
 */
public class DriverManager {


    // List of registered JDBC drivers
    private static final CopyOnWriteArrayList<DriverInfo> registeredDrivers = new CopyOnWriteArrayList<>();
    private static volatile int loginTimeout = 0;
    private static volatile java.io.PrintWriter logWriter = null;
    private static volatile java.io.PrintStream logStream = null;
    // Used in println() to synchronize logWriter
    private static final Object logSync = new Object();
    // Used in ensureDriversInitialized() to synchronize driversInitialized
    private static final Object lockForInitDrivers = new Object();
    private static volatile boolean driversInitialized;
    private static final String JDBC_DRIVERS_PROPERTY = "jdbc.drivers";

    /* Prevent the DriverManager class from being instantiated. */
    private DriverManager(){}

    /**
     * The {@code SQLPermission} constant that allows the
     * setting of the logging stream.
     * @since 1.3
     */
    static final SQLPermission SET_LOG_PERMISSION =
        new SQLPermission("setLog");

    /**
     * The {@code SQLPermission} constant that allows the
     * un-register a registered JDBC driver.
     * @since 1.8
     */
    static final SQLPermission DEREGISTER_DRIVER_PERMISSION =
        new SQLPermission("deregisterDriver");

    //--------------------------JDBC 2.0-----------------------------

    /**
     * Retrieves the log writer.
     *
     * The {@code getLogWriter} and {@code setLogWriter}
     * methods should be used instead
     * of the {@code get/setlogStream} methods, which are deprecated.
     * @return a {@code java.io.PrintWriter} object
     * @see #setLogWriter
     * @since 1.2
     */
    public static java.io.PrintWriter getLogWriter() {
            return logWriter;
    }

    /**
     * Sets the logging/tracing {@code PrintWriter} object
     * that is used by the {@code DriverManager} and all drivers.
     *<P>
     * If a security manager exists, its {@code checkPermission}
     * method is first called with a {@code SQLPermission("setLog")}
     * permission to check that the caller is allowed to call {@code setLogWriter}.
     *
     * @param out the new logging/tracing {@code PrintStream} object;
     *      {@code null} to disable logging and tracing
     * @throws SecurityException if a security manager exists and its
     * {@code checkPermission} method denies permission to set the log writer.
     * @see SecurityManager#checkPermission
     * @see #getLogWriter
     * @since 1.2
     */
    public static void setLogWriter(java.io.PrintWriter out) {

        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_LOG_PERMISSION);
        }
            logStream = null;
            logWriter = out;
    }


    //---------------------------------------------------------------

    /**
     * Attempts to establish a connection to the given database URL.
     * The {@code DriverManager} attempts to select an appropriate driver from
     * the set of registered JDBC drivers.
     *<p>
     * <B>Note:</B> If a property is specified as part of the {@code url} and
     * is also specified in the {@code Properties} object, it is
     * implementation-defined as to which value will take precedence.
     * For maximum portability, an application should only specify a
     * property once.
     *
     * @param url a database url of the form
     * <code> jdbc:<em>subprotocol</em>:<em>subname</em></code>
     * @param info a list of arbitrary string tag/value pairs as
     * connection arguments; normally at least a "user" and
     * "password" property should be included
     * @return a Connection to the URL
     * @throws SQLException if a database access error occurs or the url is
     * {@code null}
     * @throws SQLTimeoutException  when the driver has determined that the
     * timeout value specified by the {@code setLoginTimeout} method
     * has been exceeded and has at least tried to cancel the
     * current database connection attempt
     */
    @CallerSensitive
    public static Connection getConnection(String url,
        java.util.Properties info) throws SQLException {

        return (getConnection(url, info, Reflection.getCallerClass()));
    }

    /**
     * Attempts to establish a connection to the given database URL.
     * The {@code DriverManager} attempts to select an appropriate driver from
     * the set of registered JDBC drivers.
     *<p>
     * <B>Note:</B> If the {@code user} or {@code password} property are
     * also specified as part of the {@code url}, it is
     * implementation-defined as to which value will take precedence.
     * For maximum portability, an application should only specify a
     * property once.
     *
     * @param url a database url of the form
     * <code>jdbc:<em>subprotocol</em>:<em>subname</em></code>
     * @param user the database user on whose behalf the connection is being
     *   made
     * @param password the user's password
     * @return a connection to the URL
     * @throws SQLException if a database access error occurs or the url is
     * {@code null}
     * @throws SQLTimeoutException  when the driver has determined that the
     * timeout value specified by the {@code setLoginTimeout} method
     * has been exceeded and has at least tried to cancel the
     * current database connection attempt
     */
    @CallerSensitive
    public static Connection getConnection(String url,
        String user, String password) throws SQLException {
        java.util.Properties info = new java.util.Properties();

        if (user != null) {
            info.put("user", user);
        }
        if (password != null) {
            info.put("password", password);
        }

        return (getConnection(url, info, Reflection.getCallerClass()));
    }

    /**
     * Attempts to establish a connection to the given database URL.
     * The {@code DriverManager} attempts to select an appropriate driver from
     * the set of registered JDBC drivers.
     *
     * @param url a database url of the form
     *  <code> jdbc:<em>subprotocol</em>:<em>subname</em></code>
     * @return a connection to the URL
     * @throws SQLException if a database access error occurs or the url is
     * {@code null}
     * @throws SQLTimeoutException  when the driver has determined that the
     * timeout value specified by the {@code setLoginTimeout} method
     * has been exceeded and has at least tried to cancel the
     * current database connection attempt
     */
    @CallerSensitive
    public static Connection getConnection(String url)
        throws SQLException {

        java.util.Properties info = new java.util.Properties();
        return (getConnection(url, info, Reflection.getCallerClass()));
    }

    /**
     * Attempts to locate a driver that understands the given URL.
     * The {@code DriverManager} attempts to select an appropriate driver from
     * the set of registered JDBC drivers.
     *
     * @param url a database URL of the form
     *     <code>jdbc:<em>subprotocol</em>:<em>subname</em></code>
     * @return a {@code Driver} object representing a driver
     * that can connect to the given URL
     * @throws SQLException if a database access error occurs
     */
    @CallerSensitive
    public static Driver getDriver(String url)
        throws SQLException {

        println("DriverManager.getDriver(\"" + url + "\")");

        ensureDriversInitialized();

        Class<?> callerClass = Reflection.getCallerClass();

        // Walk through the loaded registeredDrivers attempting to locate someone
        // who understands the given URL.
        for (DriverInfo aDriver : registeredDrivers) {
            // If the caller does not have permission to load the driver then
            // skip it.
            if (isDriverAllowed(aDriver.driver, callerClass)) {
                try {
                    if (aDriver.driver.acceptsURL(url)) {
                        // Success!
                        println("getDriver returning " + aDriver.driver.getClass().getName());
                    return (aDriver.driver);
                    }

                } catch(SQLException sqe) {
                    // Drop through and try the next driver.
                }
            } else {
                println("    skipping: " + aDriver.driver.getClass().getName());
            }

        }

        println("getDriver: no suitable driver");
        throw new SQLException("No suitable driver", "08001");
    }


    /**
     * Registers the given driver with the {@code DriverManager}.
     * A newly-loaded driver class should call
     * the method {@code registerDriver} to make itself
     * known to the {@code DriverManager}. If the driver is currently
     * registered, no action is taken.
     *
     * @param driver the new JDBC Driver that is to be registered with the
     *               {@code DriverManager}
     * @throws SQLException if a database access error occurs
     * @throws NullPointerException if {@code driver} is null
     */
    public static void registerDriver(java.sql.Driver driver)
        throws SQLException {

        registerDriver(driver, null);
    }

    /**
     * Registers the given driver with the {@code DriverManager}.
     * A newly-loaded driver class should call
     * the method {@code registerDriver} to make itself
     * known to the {@code DriverManager}. If the driver is currently
     * registered, no action is taken.
     *
     * @param driver the new JDBC Driver that is to be registered with the
     *               {@code DriverManager}
     * @param da     the {@code DriverAction} implementation to be used when
     *               {@code DriverManager#deregisterDriver} is called
     * @throws SQLException if a database access error occurs
     * @throws NullPointerException if {@code driver} is null
     * @since 1.8
     */
    public static void registerDriver(java.sql.Driver driver,
            DriverAction da)
        throws SQLException {

        /* Register the driver if it has not already been added to our list */
        if (driver != null) {
            registeredDrivers.addIfAbsent(new DriverInfo(driver, da));
        } else {
            // This is for compatibility with the original DriverManager
            throw new NullPointerException();
        }

        println("registerDriver: " + driver);

    }

    /**
     * Removes the specified driver from the {@code DriverManager}'s list of
     * registered drivers.
     * <p>
     * If a {@code null} value is specified for the driver to be removed, then no
     * action is taken.
     * <p>
     * If a security manager exists, its {@code checkPermission}
     * method is first called with a {@code SQLPermission("deregisterDriver")}
     * permission to check that the caller is allowed to deregister a JDBC Driver.
     * <p>
     * If the specified driver is not found in the list of registered drivers,
     * then no action is taken.  If the driver was found, it will be removed
     * from the list of registered drivers.
     * <p>
     * If a {@code DriverAction} instance was specified when the JDBC driver was
     * registered, its deregister method will be called
     * prior to the driver being removed from the list of registered drivers.
     *
     * @param driver the JDBC Driver to remove
     * @throws SQLException if a database access error occurs
     * @throws SecurityException if a security manager exists and its
     * {@code checkPermission} method denies permission to deregister a driver.
     *
     * @see SecurityManager#checkPermission
     */
    @CallerSensitive
    public static void deregisterDriver(Driver driver) throws SQLException {
        if (driver == null) {
            return;
        }

        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(DEREGISTER_DRIVER_PERMISSION);
        }

        println("DriverManager.deregisterDriver: " + driver);

        DriverInfo aDriver = new DriverInfo(driver, null);
        synchronized (lockForInitDrivers) {
            if (registeredDrivers.contains(aDriver)) {
                if (isDriverAllowed(driver, Reflection.getCallerClass())) {
                    DriverInfo di = registeredDrivers.get(registeredDrivers.indexOf(aDriver));
                     // If a DriverAction was specified, Call it to notify the
                     // driver that it has been deregistered
                     if (di.action() != null) {
                         di.action().deregister();
                     }
                     registeredDrivers.remove(aDriver);
                } else {
                    // If the caller does not have permission to load the driver then
                    // throw a SecurityException.
                    throw new SecurityException();
                }
            } else {
                println("    couldn't find driver to unload");
            }
        }
    }

    /**
     * Retrieves an Enumeration with all of the currently loaded JDBC drivers
     * to which the current caller has access.
     *
     * <P><B>Note:</B> The classname of a driver can be found using
     * {@code d.getClass().getName()}
     *
     * @return the list of JDBC Drivers loaded by the caller's class loader
     * @see #drivers()
     */
    @CallerSensitive
    public static Enumeration<Driver> getDrivers() {
        ensureDriversInitialized();

        return Collections.enumeration(getDrivers(Reflection.getCallerClass()));
    }

    /**
     * Retrieves a Stream with all of the currently loaded JDBC drivers
     * to which the current caller has access.
     *
     * @return the stream of JDBC Drivers loaded by the caller's class loader
     * @since 9
     */
    @CallerSensitive
    public static Stream<Driver> drivers() {
        ensureDriversInitialized();

        return getDrivers(Reflection.getCallerClass()).stream();
    }

    private static List<Driver> getDrivers(Class<?> callerClass) {
        List<Driver> result = new ArrayList<>();
        // Walk through the loaded registeredDrivers.
        for (DriverInfo aDriver : registeredDrivers) {
            // If the caller does not have permission to load the driver then
            // skip it.
            if (isDriverAllowed(aDriver.driver, callerClass)) {
                result.add(aDriver.driver);
            } else {
                println("    skipping: " + aDriver.driver.getClass().getName());
            }
        }
        return result;
    }

    /**
     * Sets the maximum time in seconds that a driver will wait
     * while attempting to connect to a database once the driver has
     * been identified.
     *
     * @param seconds the login time limit in seconds; zero means there is no limit
     * @see #getLoginTimeout
     */
    public static void setLoginTimeout(int seconds) {
        loginTimeout = seconds;
    }

    /**
     * Gets the maximum time in seconds that a driver can wait
     * when attempting to log in to a database.
     *
     * @return the driver login time limit in seconds
     * @see #setLoginTimeout
     */
    public static int getLoginTimeout() {
        return (loginTimeout);
    }

    /**
     * Sets the logging/tracing PrintStream that is used
     * by the {@code DriverManager}
     * and all drivers.
     *<P>
     * If a security manager exists, its {@code checkPermission}
     * method is first called with a {@code SQLPermission("setLog")}
     * permission to check that the caller is allowed to call {@code setLogStream}.
     *
     * @param out the new logging/tracing PrintStream; to disable, set to {@code null}
     * @deprecated Use {@code setLogWriter}
     * @throws SecurityException if a security manager exists and its
     * {@code checkPermission} method denies permission to set the log stream.
     * @see SecurityManager#checkPermission
     * @see #getLogStream
     */
    @Deprecated(since="1.2")
    public static void setLogStream(java.io.PrintStream out) {

        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_LOG_PERMISSION);
        }

        logStream = out;
        if ( out != null )
            logWriter = new java.io.PrintWriter(out);
        else
            logWriter = null;
    }

    /**
     * Retrieves the logging/tracing PrintStream that is used by the {@code DriverManager}
     * and all drivers.
     *
     * @return the logging/tracing PrintStream; if disabled, is {@code null}
     * @deprecated  Use {@code getLogWriter}
     * @see #setLogStream
     */
    @Deprecated(since="1.2")
    public static java.io.PrintStream getLogStream() {
        return logStream;
    }

    /**
     * Prints a message to the current JDBC log stream.
     *
     * @param message a log or tracing message
     */
    public static void println(String message) {
        synchronized (logSync) {
            if (logWriter != null) {
                logWriter.println(message);

                // automatic flushing is never enabled, so we must do it ourselves
                logWriter.flush();
            }
        }
    }

    //------------------------------------------------------------------------

    // Indicates whether the class object that would be created if the code calling
    // DriverManager is accessible.
    private static boolean isDriverAllowed(Driver driver, Class<?> caller) {
        ClassLoader callerCL = caller != null ? caller.getClassLoader() : null;
        return isDriverAllowed(driver, callerCL);
    }

    private static boolean isDriverAllowed(Driver driver, ClassLoader classLoader) {
        boolean result = false;
        if (driver != null) {
            Class<?> aClass = null;
            try {
                aClass =  Class.forName(driver.getClass().getName(), true, classLoader);
            } catch (Exception ex) {
                result = false;
            }

             result = ( aClass == driver.getClass() ) ? true : false;
        }

        return result;
    }

    /*
     * Load the initial JDBC drivers by checking the System property
     * jdbc.drivers and then use the {@code ServiceLoader} mechanism
     */
    @SuppressWarnings("removal")
    private static void ensureDriversInitialized() {
        if (driversInitialized) {
            return;
        }

        synchronized (lockForInitDrivers) {
            if (driversInitialized) {
                return;
            }
            String drivers;
            try {
                drivers = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty(JDBC_DRIVERS_PROPERTY);
                    }
                });
            } catch (Exception ex) {
                drivers = null;
            }
            // If the driver is packaged as a Service Provider, load it.
            // Get all the drivers through the classloader
            // exposed as a java.sql.Driver.class service.
            // ServiceLoader.load() replaces the sun.misc.Providers()

            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {

                    ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
                    Iterator<Driver> driversIterator = loadedDrivers.iterator();

                    /* Load these drivers, so that they can be instantiated.
                     * It may be the case that the driver class may not be there
                     * i.e. there may be a packaged driver with the service class
                     * as implementation of java.sql.Driver but the actual class
                     * may be missing. In that case a java.util.ServiceConfigurationError
                     * will be thrown at runtime by the VM trying to locate
                     * and load the service.
                     *
                     * Adding a try catch block to catch those runtime errors
                     * if driver not available in classpath but it's
                     * packaged as service and that service is there in classpath.
                     */
                    try {
                        while (driversIterator.hasNext()) {
                            driversIterator.next();
                        }
                    } catch (Throwable t) {
                        // Do nothing
                    }
                    return null;
                }
            });

            println("DriverManager.initialize: jdbc.drivers = " + drivers);

            if (drivers != null && !drivers.isEmpty()) {
                String[] driversList = drivers.split(":");
                println("number of Drivers:" + driversList.length);
                for (String aDriver : driversList) {
                    try {
                        println("DriverManager.Initialize: loading " + aDriver);
                        Class.forName(aDriver, true,
                                ClassLoader.getSystemClassLoader());
                    } catch (Exception ex) {
                        println("DriverManager.Initialize: load failed: " + ex);
                    }
                }
            }

            driversInitialized = true;
            println("JDBC DriverManager initialized");
        }
    }


    //  Worker method called by the public getConnection() methods.
    private static Connection getConnection(
        String url, java.util.Properties info, Class<?> caller) throws SQLException {
        /*
         * When callerCl is null, we should check the application's
         * (which is invoking this class indirectly)
         * classloader, so that the JDBC driver class outside rt.jar
         * can be loaded from here.
         */
        ClassLoader callerCL = caller != null ? caller.getClassLoader() : null;
        if (callerCL == null || callerCL == ClassLoader.getPlatformClassLoader()) {
            callerCL = Thread.currentThread().getContextClassLoader();
        }

        if (url == null) {
            throw new SQLException("The url cannot be null", "08001");
        }

        println("DriverManager.getConnection(\"" + url + "\")");

        ensureDriversInitialized();

        // Walk through the loaded registeredDrivers attempting to make a connection.
        // Remember the first exception that gets raised so we can reraise it.
        SQLException reason = null;

        for (DriverInfo aDriver : registeredDrivers) {
            // If the caller does not have permission to load the driver then
            // skip it.
            if (isDriverAllowed(aDriver.driver, callerCL)) {
                try {
                    println("    trying " + aDriver.driver.getClass().getName());
                    Connection con = aDriver.driver.connect(url, info);
                    if (con != null) {
                        // Success!
                        println("getConnection returning " + aDriver.driver.getClass().getName());
                        return (con);
                    }
                } catch (SQLException ex) {
                    if (reason == null) {
                        reason = ex;
                    }
                }

            } else {
                println("    skipping: " + aDriver.driver.getClass().getName());
            }

        }

        // if we got here nobody could connect.
        if (reason != null)    {
            println("getConnection failed: " + reason);
            throw reason;
        }

        println("getConnection: no suitable driver found for "+ url);
        throw new SQLException("No suitable driver found for "+ url, "08001");
    }


}

/*
 * Wrapper class for registered Drivers in order to not expose Driver.equals()
 * to avoid the capture of the Driver it being compared to as it might not
 * normally have access.
 */
class DriverInfo {

    final Driver driver;
    DriverAction da;
    DriverInfo(Driver driver, DriverAction action) {
        this.driver = driver;
        da = action;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof DriverInfo)
                && this.driver == ((DriverInfo) other).driver;
    }

    @Override
    public int hashCode() {
        return driver.hashCode();
    }

    @Override
    public String toString() {
        return ("driver[className="  + driver + "]");
    }

    DriverAction action() {
        return da;
    }
}

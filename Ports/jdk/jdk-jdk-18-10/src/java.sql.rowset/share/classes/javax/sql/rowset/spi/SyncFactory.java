/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset.spi;

import java.util.logging.*;
import java.util.*;

import java.sql.*;
import javax.sql.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.naming.*;
import sun.reflect.misc.ReflectUtil;

/**
 * The Service Provider Interface (SPI) mechanism that generates <code>SyncProvider</code>
 * instances to be used by disconnected <code>RowSet</code> objects.
 * The <code>SyncProvider</code> instances in turn provide the
 * <code>javax.sql.RowSetReader</code> object the <code>RowSet</code> object
 * needs to populate itself with data and the
 * <code>javax.sql.RowSetWriter</code> object it needs to
 * propagate changes to its
 * data back to the underlying data source.
 * <P>
 * Because the methods in the <code>SyncFactory</code> class are all static,
 * there is only one <code>SyncFactory</code> object
 * per Java VM at any one time. This ensures that there is a single source from which a
 * <code>RowSet</code> implementation can obtain its <code>SyncProvider</code>
 * implementation.
 *
 * <h2>1.0 Overview</h2>
 * The <code>SyncFactory</code> class provides an internal registry of available
 * synchronization provider implementations (<code>SyncProvider</code> objects).
 * This registry may be queried to determine which
 * synchronization providers are available.
 * The following line of code gets an enumeration of the providers currently registered.
 * <PRE>
 *     java.util.Enumeration e = SyncFactory.getRegisteredProviders();
 * </PRE>
 * All standard <code>RowSet</code> implementations must provide at least two providers:
 * <UL>
 *  <LI>an optimistic provider for use with a <code>CachedRowSet</code> implementation
 *     or an implementation derived from it
 *  <LI>an XML provider, which is used for reading and writing XML, such as with
 *       <code>WebRowSet</code> objects
 * </UL>
 * Note that the JDBC RowSet Implementations include the <code>SyncProvider</code>
 * implementations <code>RIOptimisticProvider</code> and <code>RIXmlProvider</code>,
 * which satisfy this requirement.
 * <P>
 * The <code>SyncFactory</code> class provides accessor methods to assist
 * applications in determining which synchronization providers are currently
 * registered with the <code>SyncFactory</code>.
 * <p>
 * Other methods let <code>RowSet</code> persistence providers be
 * registered or de-registered with the factory mechanism. This
 * allows additional synchronization provider implementations to be made
 * available to <code>RowSet</code> objects at run time.
 * <p>
 * Applications can apply a degree of filtering to determine the level of
 * synchronization that a <code>SyncProvider</code> implementation offers.
 * The following criteria determine whether a provider is
 * made available to a <code>RowSet</code> object:
 * <ol>
 * <li>If a particular provider is specified by a <code>RowSet</code> object, and
 * the <code>SyncFactory</code> does not contain a reference to this provider,
 * a <code>SyncFactoryException</code> is thrown stating that the synchronization
 * provider could not be found.
 *
 * <li>If a <code>RowSet</code> implementation is instantiated with a specified
 * provider and the specified provider has been properly registered, the
 * requested provider is supplied. Otherwise a <code>SyncFactoryException</code>
 * is thrown.
 *
 * <li>If a <code>RowSet</code> object does not specify a
 * <code>SyncProvider</code> implementation and no additional
 * <code>SyncProvider</code> implementations are available, the reference
 * implementation providers are supplied.
 * </ol>
 * <h2>2.0 Registering <code>SyncProvider</code> Implementations</h2>
 * <p>
 * Both vendors and developers can register <code>SyncProvider</code>
 * implementations using one of the following mechanisms.
 * <ul>
 * <LI><B>Using the command line</B><BR>
 * The name of the provider is supplied on the command line, which will add
 * the provider to the system properties.
 * For example:
 * <PRE>
 *    -Drowset.provider.classname=com.fred.providers.HighAvailabilityProvider
 * </PRE>
 * <li><b>Using the Standard Properties File</b><BR>
 * The reference implementation is targeted
 * to ship with J2SE 1.5, which will include an additional resource file
 * that may be edited by hand. Here is an example of the properties file
 * included in the reference implementation:
 * <PRE>
 *   #Default JDBC RowSet sync providers listing
 *   #
 *
 *   # Optimistic synchronization provider
 *   rowset.provider.classname.0=com.sun.rowset.providers.RIOptimisticProvider
 *   rowset.provider.vendor.0=Oracle Corporation
 *   rowset.provider.version.0=1.0
 *
 *   # XML Provider using standard XML schema
 *   rowset.provider.classname.1=com.sun.rowset.providers.RIXMLProvider
 *   rowset.provider.vendor.1=Oracle Corporation
 *   rowset.provider.version.1=1.0
 * </PRE>
 * The <code>SyncFactory</code> checks this file and registers the
 * <code>SyncProvider</code> implementations that it contains. A
 * developer or vendor can add other implementations to this file.
 * For example, here is a possible addition:
 * <PRE>
 *     rowset.provider.classname.2=com.fred.providers.HighAvailabilityProvider
 *     rowset.provider.vendor.2=Fred, Inc.
 *     rowset.provider.version.2=1.0
 * </PRE>
 *
 * <li><b>Using a JNDI Context</b><BR>
 * Available providers can be registered on a JNDI
 * context, and the <code>SyncFactory</code> will attempt to load
 * <code>SyncProvider</code> implementations from that JNDI context.
 * For example, the following code fragment registers a provider implementation
 * on a JNDI context.  This is something a deployer would normally do. In this
 * example, <code>MyProvider</code> is being registered on a CosNaming
 * namespace, which is the namespace used by J2EE resources.
 * <PRE>
 *    import javax.naming.*;
 *
 *    Hashtable svrEnv = new  Hashtable();
 *    srvEnv.put(Context.INITIAL_CONTEXT_FACTORY, "CosNaming");
 *
 *    Context ctx = new InitialContext(svrEnv);
 *    com.fred.providers.MyProvider = new MyProvider();
 *    ctx.rebind("providers/MyProvider", syncProvider);
 * </PRE>
 * </ul>
 * Next, an application will register the JNDI context with the
 * <code>SyncFactory</code> instance.  This allows the <code>SyncFactory</code>
 * to browse within the JNDI context looking for <code>SyncProvider</code>
 * implementations.
 * <PRE>
 *    Hashtable appEnv = new Hashtable();
 *    appEnv.put(Context.INITIAL_CONTEXT_FACTORY, "CosNaming");
 *    appEnv.put(Context.PROVIDER_URL, "iiop://hostname/providers");
 *    Context ctx = new InitialContext(appEnv);
 *
 *    SyncFactory.registerJNDIContext(ctx);
 * </PRE>
 * If a <code>RowSet</code> object attempts to obtain a <code>MyProvider</code>
 * object, the <code>SyncFactory</code> will try to locate it. First it searches
 * for it in the system properties, then it looks in the resource files, and
 * finally it checks the JNDI context that has been set. The <code>SyncFactory</code>
 * instance verifies that the requested provider is a valid extension of the
 * <code>SyncProvider</code> abstract class and then gives it to the
 * <code>RowSet</code> object. In the following code fragment, a new
 * <code>CachedRowSet</code> object is created and initialized with
 * <i>env</i>, which contains the binding to <code>MyProvider</code>.
 * <PRE>
 *    Hashtable env = new Hashtable();
 *    env.put(SyncFactory.ROWSET_SYNC_PROVIDER, "com.fred.providers.MyProvider");
 *    CachedRowSet crs = new com.sun.rowset.CachedRowSetImpl(env);
 * </PRE>
 * Further details on these mechanisms are available in the
 * <code>javax.sql.rowset.spi</code> package specification.
 *
 * @author  Jonathan Bruce
 * @see javax.sql.rowset.spi.SyncProvider
 * @see javax.sql.rowset.spi.SyncFactoryException
 * @since 1.5
 */
public class SyncFactory {

    /**
     * Creates a new <code>SyncFactory</code> object, which is the singleton
     * instance.
     * Having a private constructor guarantees that no more than
     * one <code>SyncProvider</code> object can exist at a time.
     */
    private SyncFactory() {
    }

    /**
     * The standard property-id for a synchronization provider implementation
     * name.
     */
    public static final String ROWSET_SYNC_PROVIDER =
            "rowset.provider.classname";
    /**
     * The standard property-id for a synchronization provider implementation
     * vendor name.
     */
    public static final String ROWSET_SYNC_VENDOR =
            "rowset.provider.vendor";
    /**
     * The standard property-id for a synchronization provider implementation
     * version tag.
     */
    public static final String ROWSET_SYNC_PROVIDER_VERSION =
            "rowset.provider.version";
    /**
     * The standard resource file name.
     */
    private static String ROWSET_PROPERTIES = "rowset.properties";

    /**
     *  Permission required to invoke setJNDIContext and setLogger
     */
    private static final SQLPermission SET_SYNCFACTORY_PERMISSION =
            new SQLPermission("setSyncFactory");
    /**
     * The initial JNDI context where <code>SyncProvider</code> implementations can
     * be stored and from which they can be invoked.
     */
    private static Context ic;
    /**
     * The <code>Logger</code> object to be used by the <code>SyncFactory</code>.
     */
    private static volatile Logger rsLogger;

    /**
     * The registry of available <code>SyncProvider</code> implementations.
     * See section 2.0 of the class comment for <code>SyncFactory</code> for an
     * explanation of how a provider can be added to this registry.
     */
    private static Hashtable<String, SyncProvider> implementations;

    /**
     * Adds the given synchronization provider to the factory register. Guidelines
     * are provided in the <code>SyncProvider</code> specification for the
     * required naming conventions for <code>SyncProvider</code>
     * implementations.
     * <p>
     * Synchronization providers bound to a JNDI context can be
     * registered by binding a SyncProvider instance to a JNDI namespace.
     *
     * <pre>
     * {@code
     * SyncProvider p = new MySyncProvider();
     * InitialContext ic = new InitialContext();
     * ic.bind ("jdbc/rowset/MySyncProvider", p);
     * } </pre>
     *
     * Furthermore, an initial JNDI context should be set with the
     * <code>SyncFactory</code> using the <code>setJNDIContext</code> method.
     * The <code>SyncFactory</code> leverages this context to search for
     * available <code>SyncProvider</code> objects bound to the JNDI
     * context and its child nodes.
     *
     * @param providerID A <code>String</code> object with the unique ID of the
     *             synchronization provider being registered
     * @throws SyncFactoryException if an attempt is made to supply an empty
     *         or null provider name
     * @see #setJNDIContext
     */
    public static synchronized void registerProvider(String providerID)
            throws SyncFactoryException {

        ProviderImpl impl = new ProviderImpl();
        impl.setClassname(providerID);
        initMapIfNecessary();
        implementations.put(providerID, impl);

    }

    /**
     * Returns the <code>SyncFactory</code> singleton.
     *
     * @return the <code>SyncFactory</code> instance
     */
    public static SyncFactory getSyncFactory() {
        /*
         * Using Initialization on Demand Holder idiom as
         * Effective Java 2nd Edition,ITEM 71, indicates it is more performant
         * than the Double-Check Locking idiom.
         */
        return SyncFactoryHolder.factory;
    }

    /**
     * Removes the designated currently registered synchronization provider from the
     * Factory SPI register.
     *
     * @param providerID The unique-id of the synchronization provider
     * @throws SyncFactoryException If an attempt is made to
     * unregister a SyncProvider implementation that was not registered.
     */
    public static synchronized void unregisterProvider(String providerID)
            throws SyncFactoryException {
        initMapIfNecessary();
        if (implementations.containsKey(providerID)) {
            implementations.remove(providerID);
        }
    }
    private static String colon = ":";
    private static String strFileSep = "/";

    @SuppressWarnings("removal")
    private static synchronized void initMapIfNecessary() throws SyncFactoryException {

        // Local implementation class names and keys from Properties
        // file, translate names into Class objects using Class.forName
        // and store mappings
        final Properties properties = new Properties();

        if (implementations == null) {
            implementations = new Hashtable<>();

            try {

                // check if user is supplying his Synchronisation Provider
                // Implementation if not using Oracle's implementation.
                // properties.load(new FileInputStream(ROWSET_PROPERTIES));

                // The rowset.properties needs to be in jdk/jre/lib when
                // integrated with jdk.
                // else it should be picked from -D option from command line.

                // -Drowset.properties will add to standard properties. Similar
                // keys will over-write

                /*
                 * Dependent on application
                 */
                String strRowsetProperties;
                try {
                    strRowsetProperties = AccessController.doPrivileged(new PrivilegedAction<String>() {
                        public String run() {
                            return System.getProperty("rowset.properties");
                        }
                    }, null, new PropertyPermission("rowset.properties", "read"));
                } catch (Exception ex) {
                    System.out.println("errorget rowset.properties: " + ex);
                    strRowsetProperties = null;
                };

                if (strRowsetProperties != null) {
                    // Load user's implementation of SyncProvider
                    // here. -Drowset.properties=/abc/def/pqr.txt
                    ROWSET_PROPERTIES = strRowsetProperties;
                    try (FileInputStream fis = new FileInputStream(ROWSET_PROPERTIES)) {
                        properties.load(fis);
                    }
                    parseProperties(properties);
                }

                /*
                 * Always available
                 */
                ROWSET_PROPERTIES = "javax" + strFileSep + "sql" +
                        strFileSep + "rowset" + strFileSep +
                        "rowset.properties";

                try {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                        InputStream in = SyncFactory.class.getModule().getResourceAsStream(ROWSET_PROPERTIES);
                        if (in == null) {
                            throw new SyncFactoryException("Resource " + ROWSET_PROPERTIES + " not found");
                        }
                        try (in) {
                            properties.load(in);
                        }
                        return null;
                    });
                } catch (PrivilegedActionException ex) {
                    Throwable e = ex.getException();
                    if (e instanceof SyncFactoryException) {
                      throw (SyncFactoryException) e;
                    } else {
                        SyncFactoryException sfe = new SyncFactoryException();
                        sfe.initCause(ex.getException());
                        throw sfe;
                    }
                }

                parseProperties(properties);

            // removed else, has properties should sum together

            } catch (FileNotFoundException e) {
                throw new SyncFactoryException("Cannot locate properties file: " + e);
            } catch (IOException e) {
                throw new SyncFactoryException("IOException: " + e);
            }

            /*
             * Now deal with -Drowset.provider.classname
             * load additional properties from -D command line
             */
            properties.clear();
            String providerImpls;
            try {
                providerImpls = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty(ROWSET_SYNC_PROVIDER);
                    }
                }, null, new PropertyPermission(ROWSET_SYNC_PROVIDER, "read"));
            } catch (Exception ex) {
                providerImpls = null;
            }

            if (providerImpls != null) {
                int i = 0;
                if (providerImpls.indexOf(colon) > 0) {
                    StringTokenizer tokenizer = new StringTokenizer(providerImpls, colon);
                    while (tokenizer.hasMoreElements()) {
                        properties.put(ROWSET_SYNC_PROVIDER + "." + i, tokenizer.nextToken());
                        i++;
                    }
                } else {
                    properties.put(ROWSET_SYNC_PROVIDER, providerImpls);
                }
                parseProperties(properties);
            }
        }
    }

    /**
     * The internal debug switch.
     */
    private static boolean debug = false;
    /**
     * Internal registry count for the number of providers contained in the
     * registry.
     */
    private static int providerImplIndex = 0;

    /**
     * Internal handler for all standard property parsing. Parses standard
     * ROWSET properties and stores lazy references into the internal registry.
     */
    private static void parseProperties(Properties p) {

        ProviderImpl impl = null;
        String key = null;
        String[] propertyNames = null;

        for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {

            String str = (String) e.nextElement();

            int w = str.length();

            if (str.startsWith(SyncFactory.ROWSET_SYNC_PROVIDER)) {

                impl = new ProviderImpl();
                impl.setIndex(providerImplIndex++);

                if (w == (SyncFactory.ROWSET_SYNC_PROVIDER).length()) {
                    // no property index has been set.
                    propertyNames = getPropertyNames(false);
                } else {
                    // property index has been set.
                    propertyNames = getPropertyNames(true, str.substring(w - 1));
                }

                key = p.getProperty(propertyNames[0]);
                impl.setClassname(key);
                impl.setVendor(p.getProperty(propertyNames[1]));
                impl.setVersion(p.getProperty(propertyNames[2]));
                implementations.put(key, impl);
            }
        }
    }

    /**
     * Used by the parseProperties methods to disassemble each property tuple.
     */
    private static String[] getPropertyNames(boolean append) {
        return getPropertyNames(append, null);
    }

    /**
     * Disassembles each property and its associated value. Also handles
     * overloaded property names that contain indexes.
     */
    private static String[] getPropertyNames(boolean append,
            String propertyIndex) {
        String dot = ".";
        String[] propertyNames =
                new String[]{SyncFactory.ROWSET_SYNC_PROVIDER,
            SyncFactory.ROWSET_SYNC_VENDOR,
            SyncFactory.ROWSET_SYNC_PROVIDER_VERSION};
        if (append) {
            for (int i = 0; i < propertyNames.length; i++) {
                propertyNames[i] = propertyNames[i] +
                        dot +
                        propertyIndex;
            }
            return propertyNames;
        } else {
            return propertyNames;
        }
    }

    /**
     * Internal debug method that outputs the registry contents.
     */
    private static void showImpl(ProviderImpl impl) {
        System.out.println("Provider implementation:");
        System.out.println("Classname: " + impl.getClassname());
        System.out.println("Vendor: " + impl.getVendor());
        System.out.println("Version: " + impl.getVersion());
        System.out.println("Impl index: " + impl.getIndex());
    }

    /**
     * Returns the <code>SyncProvider</code> instance identified by <i>providerID</i>.
     *
     * @param providerID the unique identifier of the provider
     * @return a <code>SyncProvider</code> implementation
     * @throws SyncFactoryException If the SyncProvider cannot be found,
     * the providerID is {@code null}, or
     * some error was encountered when trying to invoke this provider.
     */
    public static SyncProvider getInstance(String providerID)
            throws SyncFactoryException {

        if(providerID == null) {
            throw new SyncFactoryException("The providerID cannot be null");
        }

        initMapIfNecessary(); // populate HashTable
        initJNDIContext();    // check JNDI context for any additional bindings

        ProviderImpl impl = (ProviderImpl) implementations.get(providerID);

        if (impl == null) {
            // Requested SyncProvider is unavailable. Return default provider.
            return new com.sun.rowset.providers.RIOptimisticProvider();
        }

        try {
            ReflectUtil.checkPackageAccess(providerID);
        } catch (@SuppressWarnings("removal") java.security.AccessControlException e) {
            SyncFactoryException sfe = new SyncFactoryException();
            sfe.initCause(e);
            throw sfe;
        }

        // Attempt to invoke classname from registered SyncProvider list
        Class<?> c = null;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            /**
             * The SyncProvider implementation of the user will be in
             * the classpath. We need to find the ClassLoader which loads
             * this SyncFactory and try to load the SyncProvider class from
             * there.
             **/
            c = Class.forName(providerID, true, cl);
            @SuppressWarnings("deprecation")
            Object result =  c.newInstance();
            return (SyncProvider)result;

        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new SyncFactoryException("IllegalAccessException: " + e.getMessage());
        }
    }

    /**
     * Returns an Enumeration of currently registered synchronization
     * providers.  A <code>RowSet</code> implementation may use any provider in
     * the enumeration as its <code>SyncProvider</code> object.
     * <p>
     * At a minimum, the reference synchronization provider allowing
     * RowSet content data to be stored using a JDBC driver should be
     * possible.
     *
     * @return Enumeration  A enumeration of available synchronization
     * providers that are registered with this Factory
     * @throws SyncFactoryException If an error occurs obtaining the registered
     * providers
     */
    public static Enumeration<SyncProvider> getRegisteredProviders()
            throws SyncFactoryException {
        initMapIfNecessary();
        // return a collection of classnames
        // of type SyncProvider
        return implementations.elements();
    }

    /**
     * Sets the logging object to be used by the <code>SyncProvider</code>
     * implementation provided by the <code>SyncFactory</code>. All
     * <code>SyncProvider</code> implementations can log their events to
     * this object and the application can retrieve a handle to this
     * object using the <code>getLogger</code> method.
     * <p>
     * This method checks to see that there is an {@code SQLPermission}
     * object  which grants the permission {@code setSyncFactory}
     * before allowing the method to succeed.  If a
     * {@code SecurityManager} exists and its
     * {@code checkPermission} method denies calling {@code setLogger},
     * this method throws a
     * {@code java.lang.SecurityException}.
     *
     * @param logger A Logger object instance
     * @throws java.lang.SecurityException if a security manager exists and its
     *   {@code checkPermission} method denies calling {@code setLogger}
     * @throws NullPointerException if the logger is null
     * @see SecurityManager#checkPermission
     */
    public static void setLogger(Logger logger) {

        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_SYNCFACTORY_PERMISSION);
        }

        if(logger == null){
            throw new NullPointerException("You must provide a Logger");
        }
        rsLogger = logger;
    }

    /**
     * Sets the logging object that is used by <code>SyncProvider</code>
     * implementations provided by the <code>SyncFactory</code> SPI. All
     * <code>SyncProvider</code> implementations can log their events
     * to this object and the application can retrieve a handle to this
     * object using the <code>getLogger</code> method.
     * <p>
     * This method checks to see that there is an {@code SQLPermission}
     * object  which grants the permission {@code setSyncFactory}
     * before allowing the method to succeed.  If a
     * {@code SecurityManager} exists and its
     * {@code checkPermission} method denies calling {@code setLogger},
     * this method throws a
     * {@code java.lang.SecurityException}.
     *
     * @param logger a Logger object instance
     * @param level a Level object instance indicating the degree of logging
     * required
     * @throws java.lang.SecurityException if a security manager exists and its
     *   {@code checkPermission} method denies calling {@code setLogger}
     * @throws NullPointerException if the logger is null
     * @see SecurityManager#checkPermission
     * @see LoggingPermission
     */
    public static void setLogger(Logger logger, Level level) {
        // singleton
        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_SYNCFACTORY_PERMISSION);
        }

        if(logger == null){
            throw new NullPointerException("You must provide a Logger");
        }
        logger.setLevel(level);
        rsLogger = logger;
    }

    /**
     * Returns the logging object for applications to retrieve
     * synchronization events posted by SyncProvider implementations.
     * @return The {@code Logger} that has been specified for use by
     * {@code SyncProvider} implementations
     * @throws SyncFactoryException if no logging object has been set.
     */
    public static Logger getLogger() throws SyncFactoryException {

        Logger result = rsLogger;
        // only one logger per session
        if (result == null) {
            throw new SyncFactoryException("(SyncFactory) : No logger has been set");
        }

        return result;
    }

    /**
     * Sets the initial JNDI context from which SyncProvider implementations
     * can be retrieved from a JNDI namespace
     * <p>
     *  This method checks to see that there is an {@code SQLPermission}
     * object  which grants the permission {@code setSyncFactory}
     * before allowing the method to succeed.  If a
     * {@code SecurityManager} exists and its
     * {@code checkPermission} method denies calling {@code setJNDIContext},
     * this method throws a
     * {@code java.lang.SecurityException}.
     *
     * @param ctx a valid JNDI context
     * @throws SyncFactoryException if the supplied JNDI context is null
     * @throws java.lang.SecurityException if a security manager exists and its
     *  {@code checkPermission} method denies calling {@code setJNDIContext}
     * @see SecurityManager#checkPermission
     */
    public static synchronized void setJNDIContext(javax.naming.Context ctx)
            throws SyncFactoryException {
        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_SYNCFACTORY_PERMISSION);
        }
        if (ctx == null) {
            throw new SyncFactoryException("Invalid JNDI context supplied");
        }
        ic = ctx;
    }

    /**
     * Controls JNDI context initialization.
     *
     * @throws SyncFactoryException if an error occurs parsing the JNDI context
     */
    private static synchronized void initJNDIContext() throws SyncFactoryException {

        if ((ic != null) && (lazyJNDICtxRefresh == false)) {
            try {
                parseProperties(parseJNDIContext());
                lazyJNDICtxRefresh = true; // touch JNDI namespace once.
            } catch (NamingException e) {
                e.printStackTrace();
                throw new SyncFactoryException("SPI: NamingException: " + e.getExplanation());
            } catch (Exception e) {
                e.printStackTrace();
                throw new SyncFactoryException("SPI: Exception: " + e.getMessage());
            }
        }
    }
    /**
     * Internal switch indicating whether the JNDI namespace should be re-read.
     */
    private static boolean lazyJNDICtxRefresh = false;

    /**
     * Parses the set JNDI Context and passes bindings to the enumerateBindings
     * method when complete.
     */
    private static Properties parseJNDIContext() throws NamingException {

        NamingEnumeration<?> bindings = ic.listBindings("");
        Properties properties = new Properties();

        // Hunt one level below context for available SyncProvider objects
        enumerateBindings(bindings, properties);

        return properties;
    }

    /**
     * Scans each binding on JNDI context and determines if any binding is an
     * instance of SyncProvider, if so, add this to the registry and continue to
     * scan the current context using a re-entrant call to this method until all
     * bindings have been enumerated.
     */
    private static void enumerateBindings(NamingEnumeration<?> bindings,
            Properties properties) throws NamingException {

        boolean syncProviderObj = false; // move to parameters ?

        try {
            Binding bd = null;
            Object elementObj = null;
            String element = null;
            while (bindings.hasMore()) {
                bd = (Binding) bindings.next();
                element = bd.getName();
                elementObj = bd.getObject();

                if (!(ic.lookup(element) instanceof Context)) {
                    // skip directories/sub-contexts
                    if (ic.lookup(element) instanceof SyncProvider) {
                        syncProviderObj = true;
                    }
                }

                if (syncProviderObj) {
                    SyncProvider sync = (SyncProvider) elementObj;
                    properties.put(SyncFactory.ROWSET_SYNC_PROVIDER,
                            sync.getProviderID());
                    syncProviderObj = false; // reset
                }

            }
        } catch (javax.naming.NotContextException e) {
            bindings.next();
            // Re-entrant call into method
            enumerateBindings(bindings, properties);
        }
    }

    /**
     * Lazy initialization Holder class used by {@code getSyncFactory}
     */
    private static class SyncFactoryHolder {
        static final SyncFactory factory = new SyncFactory();
    }
}

/**
 * Internal class that defines the lazy reference construct for each registered
 * SyncProvider implementation.
 */
class ProviderImpl extends SyncProvider {

    private String className = null;
    private String vendorName = null;
    private String ver = null;
    private int index;

    public void setClassname(String classname) {
        className = classname;
    }

    public String getClassname() {
        return className;
    }

    public void setVendor(String vendor) {
        vendorName = vendor;
    }

    public String getVendor() {
        return vendorName;
    }

    public void setVersion(String providerVer) {
        ver = providerVer;
    }

    public String getVersion() {
        return ver;
    }

    public void setIndex(int i) {
        index = i;
    }

    public int getIndex() {
        return index;
    }

    public int getDataSourceLock() throws SyncProviderException {

        int dsLock = 0;
        try {
            dsLock = SyncFactory.getInstance(className).getDataSourceLock();
        } catch (SyncFactoryException sfEx) {

            throw new SyncProviderException(sfEx.getMessage());
        }

        return dsLock;
    }

    public int getProviderGrade() {

        int grade = 0;

        try {
            grade = SyncFactory.getInstance(className).getProviderGrade();
        } catch (SyncFactoryException sfEx) {
            //
        }

        return grade;
    }

    public String getProviderID() {
        return className;
    }

    /*
    public javax.sql.RowSetInternal getRowSetInternal() {
    try
    {
    return SyncFactory.getInstance(className).getRowSetInternal();
    } catch(SyncFactoryException sfEx) {
    //
    }
    }
     */
    public javax.sql.RowSetReader getRowSetReader() {

        RowSetReader rsReader = null;

        try {
            rsReader = SyncFactory.getInstance(className).getRowSetReader();
        } catch (SyncFactoryException sfEx) {
            //
        }

        return rsReader;

    }

    public javax.sql.RowSetWriter getRowSetWriter() {

        RowSetWriter rsWriter = null;
        try {
            rsWriter = SyncFactory.getInstance(className).getRowSetWriter();
        } catch (SyncFactoryException sfEx) {
            //
        }

        return rsWriter;
    }

    public void setDataSourceLock(int param)
            throws SyncProviderException {

        try {
            SyncFactory.getInstance(className).setDataSourceLock(param);
        } catch (SyncFactoryException sfEx) {

            throw new SyncProviderException(sfEx.getMessage());
        }
    }

    public int supportsUpdatableView() {

        int view = 0;

        try {
            view = SyncFactory.getInstance(className).supportsUpdatableView();
        } catch (SyncFactoryException sfEx) {
            //
        }

        return view;
    }
}

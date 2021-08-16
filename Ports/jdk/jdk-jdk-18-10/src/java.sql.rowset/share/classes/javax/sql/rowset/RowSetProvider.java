/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.SQLException;
import java.util.PropertyPermission;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import sun.reflect.misc.ReflectUtil;

/**
 * A factory API that enables applications to obtain a
 * {@code RowSetFactory} implementation  that can be used to create different
 * types of {@code RowSet} implementations.
 * <p>
 * Example:
 * </p>
 * <pre>
 * RowSetFactory aFactory = RowSetProvider.newFactory();
 * CachedRowSet crs = aFactory.createCachedRowSet();
 * ...
 * RowSetFactory rsf = RowSetProvider.newFactory("com.sun.rowset.RowSetFactoryImpl", null);
 * WebRowSet wrs = rsf.createWebRowSet();
 * </pre>
 *<p>
 * Tracing of this class may be enabled by setting the System property
 * {@code javax.sql.rowset.RowSetFactory.debug} to any value but {@code false}.
 * </p>
 *
 * @author Lance Andersen
 * @since 1.7
 */
public class RowSetProvider {

    private static final String ROWSET_DEBUG_PROPERTY = "javax.sql.rowset.RowSetProvider.debug";
    private static final String ROWSET_FACTORY_IMPL = "com.sun.rowset.RowSetFactoryImpl";
    private static final String ROWSET_FACTORY_NAME = "javax.sql.rowset.RowSetFactory";
    /**
     * Internal debug flag.
     */
    private static boolean debug = true;


    static {
        // Check to see if the debug property is set
        String val = getSystemProperty(ROWSET_DEBUG_PROPERTY);
        // Allow simply setting the prop to turn on debug
        debug = val != null && !"false".equals(val);
    }

    /**
     * RowSetProvider constructor
     */
    protected RowSetProvider () {
    }

    /**
     * <p>Creates a new instance of a <code>RowSetFactory</code>
     * implementation.  This method uses the following
     * look up order to determine
     * the <code>RowSetFactory</code> implementation class to load:</p>
     * <ul>
     * <li>
     * The System property {@code javax.sql.rowset.RowSetFactory}.  For example:
     * <ul>
     * <li>
     * -Djavax.sql.rowset.RowSetFactory=com.sun.rowset.RowSetFactoryImpl
     * </li>
     * </ul>
     * <li>
     * The {@link ServiceLoader} API. The {@code ServiceLoader} API will look
     * for a class name in the file
     * {@code META-INF/services/javax.sql.rowset.RowSetFactory}
     * in jars available to the runtime. For example, to have the RowSetFactory
     * implementation {@code com.sun.rowset.RowSetFactoryImpl } loaded, the
     * entry in {@code META-INF/services/javax.sql.rowset.RowSetFactory} would be:
     *  <ul>
     * <li>
     * {@code com.sun.rowset.RowSetFactoryImpl }
     * </li>
     * </ul>
     * </li>
     * <li>
     * Platform default <code>RowSetFactory</code> instance.
     * </li>
     * </ul>
     *
     * <p>Once an application has obtained a reference to a {@code RowSetFactory},
     * it can use the factory to obtain RowSet instances.</p>
     *
     * @return New instance of a <code>RowSetFactory</code>
     *
     * @throws SQLException if the default factory class cannot be loaded,
     * instantiated. The cause will be set to actual Exception
     *
     * @see ServiceLoader
     * @since 1.7
     */
    public static RowSetFactory newFactory()
            throws SQLException {
        // Use the system property first
        RowSetFactory factory = null;
        String factoryClassName = null;
        try {
            trace("Checking for Rowset System Property...");
            factoryClassName = getSystemProperty(ROWSET_FACTORY_NAME);
            if (factoryClassName != null) {
                trace("Found system property, value=" + factoryClassName);
                if (factoryClassName.equals(ROWSET_FACTORY_IMPL)) {
                    return defaultRowSetFactory();
                }
                // getFactoryClass takes care of adding the read edge if
                // necessary
                @SuppressWarnings("deprecation")
                Object o = getFactoryClass(factoryClassName, null, false).newInstance();
                factory = (RowSetFactory) o;
            }
        } catch (Exception e) {
            throw new SQLException( "RowSetFactory: " + factoryClassName +
                    " could not be instantiated: ", e);
        }

        // Check to see if we found the RowSetFactory via a System property
        if (factory == null) {
            // If the RowSetFactory is not found via a System Property, now
            // look it up via the ServiceLoader API and if not found, use the
            // Java SE default.
            factory = loadViaServiceLoader();
        }
        return  factory == null ? defaultRowSetFactory() : factory;
    }

    private static RowSetFactory defaultRowSetFactory() {
        return new com.sun.rowset.RowSetFactoryImpl();
    }

    /**
     * <p>Creates  a new instance of a <code>RowSetFactory</code> from the
     * specified factory class name.
     * This function is useful when there are multiple providers in the classpath.
     * It gives more control to the application as it can specify which provider
     * should be loaded.</p>
     *
     * <p>Once an application has obtained a reference to a <code>RowSetFactory</code>
     * it can use the factory to obtain RowSet instances.</p>
     *
     * @param factoryClassName fully qualified factory class name that
     * provides  an implementation of <code>javax.sql.rowset.RowSetFactory</code>.
     *
     * @param cl <code>ClassLoader</code> used to load the factory
     * class. If <code>null</code> current <code>Thread</code>'s context
     * classLoader is used to load the factory class.
     *
     * @return New instance of a <code>RowSetFactory</code>
     *
     * @throws SQLException if <code>factoryClassName</code> is
     * <code>null</code>, or the factory class cannot be loaded, instantiated.
     *
     * @see #newFactory()
     *
     * @since 1.7
     */
    public static RowSetFactory newFactory(String factoryClassName, ClassLoader cl)
            throws SQLException {

        trace("***In newInstance()");

        if(factoryClassName == null) {
            throw new SQLException("Error: factoryClassName cannot be null");
        }
        try {
            ReflectUtil.checkPackageAccess(factoryClassName);
        } catch (@SuppressWarnings("removal") java.security.AccessControlException e) {
            throw new SQLException("Access Exception",e);
        }

        try {
            // getFactoryClass takes care of adding the read edge if
            // necessary
            Class<?> providerClass = getFactoryClass(factoryClassName, cl, false);
            @SuppressWarnings("deprecation")
            RowSetFactory instance = (RowSetFactory) providerClass.newInstance();
            if (debug) {
                trace("Created new instance of " + providerClass +
                        " using ClassLoader: " + cl);
            }
            return instance;
        } catch (ClassNotFoundException x) {
            throw new SQLException(
                    "Provider " + factoryClassName + " not found", x);
        } catch (Exception x) {
            throw new SQLException(
                    "Provider " + factoryClassName + " could not be instantiated: " + x,
                    x);
        }
    }

    /*
     * Returns the class loader to be used.
     * @return The ClassLoader to use.
     *
     */
    @SuppressWarnings("removal")
    static private ClassLoader getContextClassLoader() throws SecurityException {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            public ClassLoader run() {
                ClassLoader cl = null;

                cl = Thread.currentThread().getContextClassLoader();

                if (cl == null) {
                    cl = ClassLoader.getSystemClassLoader();
                }

                return cl;
            }
        });
    }

    /**
     * Attempt to load a class using the class loader supplied. If that fails
     * and fall back is enabled, the current (i.e. bootstrap) class loader is
     * tried.
     *
     * If the class loader supplied is <code>null</code>, first try using the
     * context class loader followed by the current class loader.
     *  @return The class which was loaded
     */
    static private Class<?> getFactoryClass(String factoryClassName, ClassLoader cl,
            boolean doFallback) throws ClassNotFoundException {
        Class<?> factoryClass = null;

        try {
            if (cl == null) {
                cl = getContextClassLoader();
                if (cl == null) {
                    throw new ClassNotFoundException();
                } else {
                    factoryClass = cl.loadClass(factoryClassName);
                }
            } else {
                factoryClass = cl.loadClass(factoryClassName);
            }
        } catch (ClassNotFoundException e) {
            if (doFallback) {
                // Use current class loader
                factoryClass = Class.forName(factoryClassName, true, RowSetFactory.class.getClassLoader());
            } else {
                throw e;
            }
        }

        ReflectUtil.checkPackageAccess(factoryClass);
        return factoryClass;
    }

    /**
     * Use the ServiceLoader mechanism to load  the default RowSetFactory
     * @return default RowSetFactory Implementation
     */
    static private RowSetFactory loadViaServiceLoader() throws SQLException {
        RowSetFactory theFactory = null;
        try {
            trace("***in loadViaServiceLoader():");
            for (RowSetFactory factory : ServiceLoader.load(javax.sql.rowset.RowSetFactory.class)) {
                trace(" Loading done by the java.util.ServiceLoader :" + factory.getClass().getName());
                theFactory = factory;
                break;
            }
        } catch (ServiceConfigurationError e) {
            throw new SQLException(
                    "RowSetFactory: Error locating RowSetFactory using Service "
                    + "Loader API: " + e, e);
        }
        return theFactory;

    }

    /**
     * Returns the requested System Property.  If a {@code SecurityException}
     * occurs, just return NULL
     * @param propName - System property to retrieve
     * @return The System property value or NULL if the property does not exist
     * or a {@code SecurityException} occurs.
     */
    @SuppressWarnings("removal")
    static private String getSystemProperty(final String propName) {
        String property = null;
        try {
            property = AccessController.doPrivileged(new PrivilegedAction<String>() {

                public String run() {
                    return System.getProperty(propName);
                }
            }, null, new PropertyPermission(propName, "read"));
        } catch (SecurityException se) {
            trace("error getting " + propName + ":  "+ se);
            if (debug) {
                se.printStackTrace();
            }
        }
        return property;
    }

    /**
     * Debug routine which will output tracing if the System Property
     * -Djavax.sql.rowset.RowSetFactory.debug is set
     * @param msg - The debug message to display
     */
    private static void trace(String msg) {
        if (debug) {
            System.err.println("###RowSets: " + msg);
        }
    }
}

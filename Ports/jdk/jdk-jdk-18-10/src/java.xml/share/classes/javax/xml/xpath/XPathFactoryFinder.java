/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.xpath;

import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import jdk.xml.internal.SecuritySupport;

/**
 * Implementation of {@link XPathFactory#newInstance(String)}.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.5
 */
class XPathFactoryFinder  {
    private static final String DEFAULT_PACKAGE = "com.sun.org.apache.xpath.internal";

    /** debug support code. */
    private static boolean debug = false;
    static {
        // Use try/catch block to support applets
        try {
            debug = SecuritySupport.getSystemProperty("jaxp.debug") != null;
        } catch (Exception unused) {
            debug = false;
        }
    }

    /**
     * <p>Cache properties for performance.</p>
     */
    private static final Properties cacheProps = new Properties();

    /**
     * <p>First time requires initialization overhead.</p>
     */
    private volatile static boolean firstTime = true;

    /**
     * <p>Conditional debug printing.</p>
     *
     * @param msgGen Supplier function that returns debug message
     */
    private static void debugPrintln(Supplier<String> msgGen) {
        if (debug) {
            System.err.println("JAXP: " + msgGen.get());
        }
    }

    /**
     * <p><code>ClassLoader</code> to use to find <code>XPathFactory</code>.</p>
     */
    private final ClassLoader classLoader;

    /**
     * <p>Constructor that specifies <code>ClassLoader</code> to use
     * to find <code>XPathFactory</code>.</p>
     *
     * @param loader
     *      to be used to load resource and {@link XPathFactory}
     *      implementations during the resolution process.
     *      If this parameter is null, the default system class loader
     *      will be used.
     */
    public XPathFactoryFinder(ClassLoader loader) {
        this.classLoader = loader;
        if( debug ) {
            debugDisplayClassLoader();
        }
    }

    private void debugDisplayClassLoader() {
        try {
            if( classLoader == SecuritySupport.getContextClassLoader() ) {
                debugPrintln(() -> "using thread context class loader ("+classLoader+") for search");
                return;
            }
        } catch( Throwable unused ) {
             // getContextClassLoader() undefined in JDK1.1
        }

        if( classLoader==ClassLoader.getSystemClassLoader() ) {
            debugPrintln(() -> "using system class loader ("+classLoader+") for search");
            return;
        }

        debugPrintln(() -> "using class loader ("+classLoader+") for search");
    }

    /**
     * <p>Creates a new {@link XPathFactory} object for the specified
     * object model.</p>
     *
     * @param uri
     *       Identifies the underlying object model.
     *
     * @return <code>null</code> if the callee fails to create one.
     *
     * @throws NullPointerException
     *      If the parameter is null.
     */
    public XPathFactory newFactory(String uri) throws XPathFactoryConfigurationException {
        if (uri == null) {
            throw new NullPointerException();
        }
        XPathFactory f = _newFactory(uri);
        if (f != null) {
            debugPrintln(()->"factory '" + f.getClass().getName() + "' was found for " + uri);
        } else {
            debugPrintln(()->"unable to find a factory for " + uri);
        }
        return f;
    }

    /**
     * <p>Lookup a {@link XPathFactory} for the given object model.</p>
     *
     * @param uri identifies the object model.
     *
     * @return {@link XPathFactory} for the given object model.
     */
    private XPathFactory _newFactory(String uri) throws XPathFactoryConfigurationException {
        XPathFactory xpathFactory = null;

        String propertyName = SERVICE_CLASS.getName() + ":" + uri;

        // system property look up
        try {
            debugPrintln(()->"Looking up system property '"+propertyName+"'" );
            String r = SecuritySupport.getSystemProperty(propertyName);
            if(r!=null) {
                debugPrintln(()->"The value is '"+r+"'");
                xpathFactory = createInstance(r);
                if (xpathFactory != null) {
                    return xpathFactory;
                }
            } else
                debugPrintln(()->"The property is undefined.");
        } catch( Throwable t ) {
            if( debug ) {
                debugPrintln(()->"failed to look up system property '"+propertyName+"'" );
                t.printStackTrace();
            }
        }

        String javah = SecuritySupport.getSystemProperty( "java.home" );
        String configFile = javah + File.separator +
        "conf" + File.separator + "jaxp.properties";

        // try to read from $java.home/conf/jaxp.properties
        try {
            if(firstTime){
                synchronized(cacheProps){
                    if(firstTime){
                        File f=new File( configFile );
                        firstTime = false;
                        if(SecuritySupport.doesFileExist(f)){
                            debugPrintln(()->"Read properties file " + f);
                            cacheProps.load(SecuritySupport.getFileInputStream(f));
                        }
                    }
                }
            }
            final String factoryClassName = cacheProps.getProperty(propertyName);
            debugPrintln(()->"found " + factoryClassName + " in $java.home/conf/jaxp.properties");

            if (factoryClassName != null) {
                xpathFactory = createInstance(factoryClassName);
                if(xpathFactory != null){
                    return xpathFactory;
                }
            }
        } catch (Exception ex) {
            if (debug) {
                ex.printStackTrace();
            }
        }

        // Try with ServiceLoader
        assert xpathFactory == null;
        xpathFactory = findServiceProvider(uri);

        // The following assertion should always be true.
        // Uncomment it, recompile, and run with -ea in case of doubts:
        // assert xpathFactory == null || xpathFactory.isObjectModelSupported(uri);

        if (xpathFactory != null) {
            return xpathFactory;
        }

        // platform default
        if(uri.equals(XPathFactory.DEFAULT_OBJECT_MODEL_URI)) {
            debugPrintln(()->"attempting to use the platform default W3C DOM XPath lib");
            return new XPathFactoryImpl();
        }

        debugPrintln(()->"all things were tried, but none was found. bailing out.");
        return null;
    }

    /** <p>Create class using appropriate ClassLoader.</p>
     *
     * @param className Name of class to create.
     * @return Created class or <code>null</code>.
     */
    @SuppressWarnings("removal")
    private Class<?> createClass(String className) {
        Class<?> clazz;
        // make sure we have access to restricted packages
        boolean internal = false;
        if (System.getSecurityManager() != null) {
            if (className != null && className.startsWith(DEFAULT_PACKAGE)) {
                internal = true;
            }
        }

        // use approprite ClassLoader
        try {
            if (classLoader != null && !internal) {
                    clazz = Class.forName(className, false, classLoader);
            } else {
                    clazz = Class.forName(className);
            }
        } catch (Throwable t) {
            if(debug) {
                t.printStackTrace();
            }
            return null;
        }

        return clazz;
    }

    /**
     * <p>Creates an instance of the specified and returns it.</p>
     *
     * @param className
     *      fully qualified class name to be instantiated.
     *
     * @return null
     *      if it fails. Error messages will be printed by this method.
     */
    XPathFactory createInstance(String className)
            throws XPathFactoryConfigurationException
    {
        XPathFactory xPathFactory = null;

        debugPrintln(()->"createInstance(" + className + ")");

        // get Class from className
        Class<?> clazz = createClass(className);
        if (clazz == null) {
            debugPrintln(()->"failed to getClass(" + className + ")");
            return null;
        }
        debugPrintln(()->"loaded " + className + " from " + which(clazz));

        // instantiate Class as a XPathFactory
        try {
            xPathFactory = (XPathFactory) clazz.getConstructor().newInstance();
        } catch (ClassCastException | IllegalAccessException | IllegalArgumentException |
            InstantiationException | InvocationTargetException | NoSuchMethodException |
            SecurityException ex) {
            debugPrintln(()->"could not instantiate " + clazz.getName());
            if (debug) {
                    ex.printStackTrace();
            }
            return null;
        }

        return xPathFactory;
    }

    // Call isObjectModelSupportedBy with initial context.
    @SuppressWarnings("removal")
    private boolean isObjectModelSupportedBy(final XPathFactory factory,
            final String objectModel,
            AccessControlContext acc) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        return factory.isObjectModelSupported(objectModel);
                    }
                }, acc);
    }

    /**
     * Finds a service provider subclass of XPathFactory that supports the
     * given object model using the ServiceLoader.
     *
     * @param objectModel URI of object model to support.
     * @return An XPathFactory supporting the specified object model, or null
     *         if none is found.
     * @throws XPathFactoryConfigurationException if a configuration error is found.
     */
    @SuppressWarnings("removal")
    private XPathFactory findServiceProvider(final String objectModel)
            throws XPathFactoryConfigurationException {

        assert objectModel != null;
        // store current context.
        final AccessControlContext acc = AccessController.getContext();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<XPathFactory>() {
                public XPathFactory run() {
                    final ServiceLoader<XPathFactory> loader =
                            ServiceLoader.load(SERVICE_CLASS);
                    for (XPathFactory factory : loader) {
                        // restore initial context to call
                        // factory.isObjectModelSupportedBy
                        if (isObjectModelSupportedBy(factory, objectModel, acc)) {
                            return factory;
                        }
                    }
                    return null; // no factory found.
                }
            });
        } catch (ServiceConfigurationError error) {
            throw new XPathFactoryConfigurationException(error);
        }
    }

    private static final Class<XPathFactory> SERVICE_CLASS = XPathFactory.class;

    // Used for debugging purposes
    private static String which( Class<?> clazz ) {
        return SecuritySupport.getClassSource(clazz);
    }

}

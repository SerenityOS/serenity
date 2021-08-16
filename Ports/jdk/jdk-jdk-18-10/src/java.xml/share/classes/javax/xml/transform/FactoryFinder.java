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

package javax.xml.transform;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import jdk.xml.internal.SecuritySupport;

/**
 * <p>Implements pluggable Datatypes.</p>
 *
 * <p>This class is duplicated for each JAXP subpackage so keep it in
 * sync.  It is package private for secure class loading.</p>
 *
 * @author Santiago PericasGeertsen
 */
class FactoryFinder {
    private static final String DEFAULT_PACKAGE = "com.sun.org.apache.xalan.internal.";

    /**
     * Internal debug flag.
     */
    private static boolean debug = false;

    /**
     * Cache for properties in java.home/conf/jaxp.properties
     */
    private final static Properties cacheProps = new Properties();

    /**
     * Flag indicating if properties from java.home/conf/jaxp.properties
     * have been cached.
     */
    static volatile boolean firstTime = true;

    // Define system property "jaxp.debug" to get output
    static {
        // Use try/catch block to support applets, which throws
        // SecurityException out of this code.
        try {
            String val = SecuritySupport.getSystemProperty("jaxp.debug");
            // Allow simply setting the prop to turn on debug
            debug = val != null && !"false".equals(val);
        }
        catch (SecurityException se) {
            debug = false;
        }
    }

    private static void dPrint(Supplier<String> msgGen) {
        if (debug) {
            System.err.println("JAXP: " + msgGen.get());
        }
    }

    /**
     * Attempt to load a class using the class loader supplied. If that fails
     * and fall back is enabled, the current (i.e. bootstrap) class loader is
     * tried.
     *
     * If the class loader supplied is <code>null</code>, first try using the
     * context class loader followed by the current (i.e. bootstrap) class
     * loader.
     *
     * Use bootstrap classLoader if cl = null and useBSClsLoader is true
     */
    static private Class<?> getProviderClass(String className, ClassLoader cl,
            boolean doFallback, boolean useBSClsLoader) throws ClassNotFoundException
    {
        try {
            if (cl == null) {
                if (useBSClsLoader) {
                    return Class.forName(className, false, FactoryFinder.class.getClassLoader());
                } else {
                    cl = SecuritySupport.getContextClassLoader();
                    if (cl == null) {
                        throw new ClassNotFoundException();
                    }
                    else {
                        return Class.forName(className, false, cl);
                    }
                }
            }
            else {
                return Class.forName(className, false, cl);
            }
        }
        catch (ClassNotFoundException e1) {
            if (doFallback) {
                // Use current class loader - should always be bootstrap CL
                return Class.forName(className, false, FactoryFinder.class.getClassLoader());
            }
            else {
                throw e1;
            }
        }
    }

    /**
     * Create an instance of a class. Delegates to method
     * <code>getProviderClass()</code> in order to load the class.
     *
     * @param type Base class / Service interface  of the factory to
     *             instantiate.
     *
     * @param className Name of the concrete class corresponding to the
     * service provider
     *
     * @param cl <code>ClassLoader</code> used to load the factory class. If <code>null</code>
     * current <code>Thread</code>'s context classLoader is used to load the factory class.
     *
     * @param doFallback True if the current ClassLoader should be tried as
     * a fallback if the class is not found using cl
     *
     * @param overrideDefaultParser True to allow overriding the system-default
     * parser.
     */
    @SuppressWarnings("removal")
    static <T> T newInstance(Class<T> type, String className, ClassLoader cl,
                             boolean doFallback)
        throws TransformerFactoryConfigurationError
    {
        assert type != null;

        boolean useBSClsLoader = false;
        // make sure we have access to restricted packages
        if (System.getSecurityManager() != null) {
            if (className != null && className.startsWith(DEFAULT_PACKAGE)) {
                cl = null;
                useBSClsLoader = true;
            }
        }

        try {
            Class<?> providerClass = getProviderClass(className, cl, doFallback, useBSClsLoader);
            if (!type.isAssignableFrom(providerClass)) {
                throw new ClassCastException(className + " cannot be cast to " + type.getName());
            }
            Object instance = providerClass.getConstructor().newInstance();

            final ClassLoader clD = cl;
            dPrint(()->"created new instance of " + providerClass +
                       " using ClassLoader: " + clD);
            return type.cast(instance);
        }
        catch (ClassNotFoundException x) {
            throw new TransformerFactoryConfigurationError(x,
                "Provider " + className + " not found");
        }
        catch (Exception x) {
            throw new TransformerFactoryConfigurationError(x,
                "Provider " + className + " could not be instantiated: " + x);
        }
    }

    /**
     * Finds the implementation Class object in the specified order.  Main
     * entry point.
     * @return Class object of factory, never null
     *
     * @param type                  Base class / Service interface  of the
     *                              factory to find.
     *
     * @param fallbackClassName     Implementation class name, if nothing else
     *                              is found.  Use null to mean no fallback.
     *
     * Package private so this code can be shared.
     */
    static <T> T find(Class<T> type, String fallbackClassName)
        throws TransformerFactoryConfigurationError
    {
        assert type != null;

        final String factoryId = type.getName();

        dPrint(()->"find factoryId =" + factoryId);
        // Use the system property first
        try {
            String systemProp = SecuritySupport.getSystemProperty(factoryId);
            if (systemProp != null) {
                dPrint(()->"found system property, value=" + systemProp);
                return newInstance(type, systemProp, null, true);
            }
        }
        catch (SecurityException se) {
            if (debug) se.printStackTrace();
        }

        // try to read from $java.home/conf/jaxp.properties
        try {
            if (firstTime) {
                synchronized (cacheProps) {
                    if (firstTime) {
                        String configFile = SecuritySupport.getSystemProperty("java.home") + File.separator +
                            "conf" + File.separator + "jaxp.properties";
                        File f = new File(configFile);
                        firstTime = false;
                        if (SecuritySupport.doesFileExist(f)) {
                            dPrint(()->"Read properties file "+f);
                            cacheProps.load(SecuritySupport.getFileInputStream(f));
                        }
                    }
                }
            }
            final String factoryClassName = cacheProps.getProperty(factoryId);

            if (factoryClassName != null) {
                dPrint(()->"found in ${java.home}/conf/jaxp.properties, value=" + factoryClassName);
                return newInstance(type, factoryClassName, null, true);
            }
        }
        catch (Exception ex) {
            if (debug) ex.printStackTrace();
        }

        // Try Jar Service Provider Mechanism
        T provider = findServiceProvider(type);
        if (provider != null) {
            return provider;
        }
        if (fallbackClassName == null) {
            throw new TransformerFactoryConfigurationError(null,
                "Provider for " + factoryId + " cannot be found");
        }

        dPrint(()->"loaded from fallback value: " + fallbackClassName);
        return newInstance(type, fallbackClassName, null, true);
    }

    /*
     * Try to find provider using the ServiceLoader.
     *
     * @param type Base class / Service interface  of the factory to find.
     *
     * @return instance of provider class if found or null
     */
    @SuppressWarnings("removal")
    private static <T> T findServiceProvider(final Class<T> type)
        throws TransformerFactoryConfigurationError
    {
      try {
            return AccessController.doPrivileged(new PrivilegedAction<T>() {
                public T run() {
                    final ServiceLoader<T> serviceLoader = ServiceLoader.load(type);
                    final Iterator<T> iterator = serviceLoader.iterator();
                    if (iterator.hasNext()) {
                        return iterator.next();
                    } else {
                        return null;
                    }
                 }
            });
        } catch(ServiceConfigurationError e) {
            // It is not possible to wrap an error directly in
            // FactoryConfigurationError - so we need to wrap the
            // ServiceConfigurationError in a RuntimeException.
            // The alternative would be to modify the logic in
            // FactoryConfigurationError to allow setting a
            // Throwable as the cause, but that could cause
            // compatibility issues down the road.
            final RuntimeException x = new RuntimeException(
                    "Provider for " + type + " cannot be created", e);
            final TransformerFactoryConfigurationError error =
                    new TransformerFactoryConfigurationError(x, x.getMessage());
            throw error;
        }
    }
}

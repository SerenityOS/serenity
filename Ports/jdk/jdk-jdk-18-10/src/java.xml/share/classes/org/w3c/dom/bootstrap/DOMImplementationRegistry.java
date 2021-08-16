/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2004 World Wide Web Consortium,
 *
 * (Massachusetts Institute of Technology, European Research Consortium for
 * Informatics and Mathematics, Keio University). All Rights Reserved. This
 * work is distributed under the W3C(r) Software License [1] in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */


package org.w3c.dom.bootstrap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMImplementationList;
import org.w3c.dom.DOMImplementationSource;

/**
 * A factory that enables applications to obtain instances of
 * <code>DOMImplementation</code>.
 *
 * <p>
 * Example:
 * </p>
 *
 * <pre class='example'>
 *  // get an instance of the DOMImplementation registry
 *  DOMImplementationRegistry registry =
 *       DOMImplementationRegistry.newInstance();
 *  // get a DOM implementation the Level 3 XML module
 *  DOMImplementation domImpl =
 *       registry.getDOMImplementation("XML 3.0");
 * </pre>
 *
 * <p>
 * This provides an application with an implementation-independent starting
 * point. DOM implementations may modify this class to meet new security
 * standards or to provide *additional* fallbacks for the list of
 * DOMImplementationSources.
 * </p>
 *
 * @see DOMImplementation
 * @see DOMImplementationSource
 * @since 1.5, DOM Level 3
 */
public final class DOMImplementationRegistry {
    /**
     * The system property to specify the
     * DOMImplementationSource class names.
     */
    public static final String PROPERTY =
        "org.w3c.dom.DOMImplementationSourceList";

    /**
     * Default columns per line.
     */
    private static final int DEFAULT_LINE_LENGTH = 80;

    /**
     * The list of DOMImplementationSources.
     */
    private List<DOMImplementationSource> sources;

    /**
     * Default class name.
     */
    private static final String FALLBACK_CLASS =
            "com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl";
    private static final String DEFAULT_PACKAGE =
            "com.sun.org.apache.xerces.internal.dom";
    /**
     * Private constructor.
     * @param srcs List of DOMImplementationSources
     */
    private DOMImplementationRegistry(final List<DOMImplementationSource> srcs) {
        sources = srcs;
    }

    /**
     * Obtain a new instance of a <code>DOMImplementationRegistry</code>.
     *

     * The <code>DOMImplementationRegistry</code> is initialized by the
     * application or the implementation, depending on the context, by
     * first checking the value of the Java system property
     * <code>org.w3c.dom.DOMImplementationSourceList</code> and
     * the service provider whose contents are at
     * "<code>META_INF/services/org.w3c.dom.DOMImplementationSourceList</code>".
     * The value of this property is a white-space separated list of
     * names of availables classes implementing the
     * <code>DOMImplementationSource</code> interface. Each class listed
     * in the class name list is instantiated and any exceptions
     * encountered are thrown to the application.
     *
     * @return an initialized instance of DOMImplementationRegistry
     * @throws ClassNotFoundException
     *     If any specified class can not be found
     * @throws InstantiationException
     *     If any specified class is an interface or abstract class
     * @throws IllegalAccessException
     *     If the default constructor of a specified class is not accessible
     * @throws ClassCastException
     *     If any specified class does not implement
     * <code>DOMImplementationSource</code>
     */
    @SuppressWarnings("removal")
    public static DOMImplementationRegistry newInstance()
        throws
        ClassNotFoundException,
        InstantiationException,
        IllegalAccessException,
        ClassCastException {
        List<DOMImplementationSource> sources = new ArrayList<>();

        ClassLoader classLoader = getClassLoader();
        // fetch system property:
        String p = getSystemProperty(PROPERTY);

        //
        // if property is not specified then use contents of
        // META_INF/org.w3c.dom.DOMImplementationSourceList from classpath
        if (p == null) {
            p = getServiceValue(classLoader);
        }
        if (p == null) {
            //
            // DOM Implementations can modify here to add *additional* fallback
            // mechanisms to access a list of default DOMImplementationSources.
            //fall back to JAXP implementation class DOMXSImplementationSourceImpl
            p = FALLBACK_CLASS;
        }
        if (p != null) {
            StringTokenizer st = new StringTokenizer(p);
            while (st.hasMoreTokens()) {
                String sourceName = st.nextToken();
                // make sure we have access to restricted packages
                boolean internal = false;
                if (System.getSecurityManager() != null) {
                    if (sourceName != null && sourceName.startsWith(DEFAULT_PACKAGE)) {
                        internal = true;
                    }
                }
                Class<?> sourceClass = null;
                if (classLoader != null && !internal) {
                    sourceClass = classLoader.loadClass(sourceName);
                } else {
                    sourceClass = Class.forName(sourceName);
                }
                try {
                    DOMImplementationSource source =
                        (DOMImplementationSource) sourceClass.getConstructor().newInstance();
                    sources.add(source);
                } catch (NoSuchMethodException | InvocationTargetException e) {
                    throw new InstantiationException(e.getMessage());
                }
            }
        }
        return new DOMImplementationRegistry(sources);
    }

    /**
     * Return the first implementation that has the desired
     * features, or <code>null</code> if none is found.
     *
     * @param features
     *            A string that specifies which features are required. This is
     *            a space separated list in which each feature is specified by
     *            its name optionally followed by a space and a version number.
     *            This is something like: "XML 1.0 Traversal +Events 2.0"
     * @return An implementation that has the desired features,
     *         or <code>null</code> if none found.
     */
    public DOMImplementation getDOMImplementation(final String features) {
        int size = sources.size();
        String name = null;
        for (int i = 0; i < size; i++) {
            DOMImplementationSource source = sources.get(i);
            DOMImplementation impl = source.getDOMImplementation(features);
            if (impl != null) {
                return impl;
            }
        }
        return null;
    }

    /**
     * Return a list of implementations that support the
     * desired features.
     *
     * @param features
     *            A string that specifies which features are required. This is
     *            a space separated list in which each feature is specified by
     *            its name optionally followed by a space and a version number.
     *            This is something like: "XML 1.0 Traversal +Events 2.0"
     * @return A list of DOMImplementations that support the desired features.
     */
    public DOMImplementationList getDOMImplementationList(final String features) {
        final List<DOMImplementation> implementations = new ArrayList<>();
        int size = sources.size();
        for (int i = 0; i < size; i++) {
            DOMImplementationSource source = sources.get(i);
            DOMImplementationList impls =
                source.getDOMImplementationList(features);
            for (int j = 0; j < impls.getLength(); j++) {
                DOMImplementation impl = impls.item(j);
                implementations.add(impl);
            }
        }
        return new DOMImplementationList() {
                public DOMImplementation item(final int index) {
                    if (index >= 0 && index < implementations.size()) {
                        try {
                            return implementations.get(index);
                        } catch (IndexOutOfBoundsException e) {
                            return null;
                        }
                    }
                    return null;
                }

                public int getLength() {
                    return implementations.size();
                }
            };
    }

    /**
     * Register an implementation.
     *
     * @param s The source to be registered, may not be <code>null</code>
     */
    public void addSource(final DOMImplementationSource s) {
        if (s == null) {
            throw new NullPointerException();
        }
        if (!sources.contains(s)) {
            sources.add(s);
        }
    }

    /**
     *
     * Gets a class loader.
     *
     * @return A class loader, possibly <code>null</code>
     */
    private static ClassLoader getClassLoader() {
        try {
            ClassLoader contextClassLoader = getContextClassLoader();

            if (contextClassLoader != null) {
                return contextClassLoader;
            }
        } catch (Exception e) {
            // Assume that the DOM application is in a JRE 1.1, use the
            // current ClassLoader
            return DOMImplementationRegistry.class.getClassLoader();
        }
        return DOMImplementationRegistry.class.getClassLoader();
    }

    /**
     * This method attempts to return the first line of the resource
     * META_INF/services/org.w3c.dom.DOMImplementationSourceList
     * from the provided ClassLoader.
     *
     * @param classLoader classLoader, may not be <code>null</code>.
     * @return first line of resource, or <code>null</code>
     */
    private static String getServiceValue(final ClassLoader classLoader) {
        String serviceId = "META-INF/services/" + PROPERTY;
        // try to find services in CLASSPATH
        try {
            InputStream is = getResourceAsStream(classLoader, serviceId);

            if (is != null) {
                BufferedReader rd;
                try {
                    rd =
                        new BufferedReader(new InputStreamReader(is, "UTF-8"),
                                           DEFAULT_LINE_LENGTH);
                } catch (java.io.UnsupportedEncodingException e) {
                    rd =
                        new BufferedReader(new InputStreamReader(is),
                                           DEFAULT_LINE_LENGTH);
                }
                String serviceValue = rd.readLine();
                rd.close();
                if (serviceValue != null && serviceValue.length() > 0) {
                    return serviceValue;
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    /**
     * This method returns the ContextClassLoader.
     *
     * @return The Context Classloader
     */
    @SuppressWarnings("removal")
    private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    ClassLoader classLoader = null;
                    try {
                        classLoader =
                            Thread.currentThread().getContextClassLoader();
                    } catch (SecurityException ex) {
                    }
                    return classLoader;
                }
            });
    }

    /**
     * This method returns the system property indicated by the specified name
     * after checking access control privileges.
     *
     * @param name the name of the system property
     * @return the system property
     */
    @SuppressWarnings("removal")
    private static String getSystemProperty(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty(name);
                    }
                });
    }

    /**
     * This method returns an Inputstream for the reading resource
     * META_INF/services/org.w3c.dom.DOMImplementationSourceList after checking
     * access control privileges.
     *
     * @param classLoader classLoader
     * @param name the resource
     * @return an Inputstream for the resource specified
     */
    @SuppressWarnings("removal")
    private static InputStream getResourceAsStream(final ClassLoader classLoader,
                                                   final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    InputStream ris;
                    if (classLoader == null) {
                        ris =
                            ClassLoader.getSystemResourceAsStream(name);
                    } else {
                        ris = classLoader.getResourceAsStream(name);
                    }
                    return ris;
                }
            });
    }
}

/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.naming.internal;

import javax.naming.NamingEnumeration;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

/**
 * VersionHelper was used by JNDI to accommodate differences between
 * JDK 1.1.x and the Java 2 platform. As this is no longer necessary
 * since JNDI's inclusion in the platform, this class currently
 * serves as a set of utilities for performing system-level things,
 * such as class-loading and reading system properties.
 *
 * @author Rosanna Lee
 * @author Scott Seligman
 */

public final class VersionHelper {
    private static final VersionHelper helper = new VersionHelper();

    /**
     * Determines whether classes may be loaded from an arbitrary URL code base.
     */
    private static final boolean TRUST_URL_CODE_BASE;

    static {
        // System property to control whether classes may be loaded from an
        // arbitrary URL code base
        PrivilegedAction<String> act
                = () -> System.getProperty("com.sun.jndi.ldap.object.trustURLCodebase", "false");
        @SuppressWarnings("removal")
        String trust = AccessController.doPrivileged(act);
        TRUST_URL_CODE_BASE = "true".equalsIgnoreCase(trust);
    }

    static final String[] PROPS = new String[]{
        javax.naming.Context.INITIAL_CONTEXT_FACTORY,
        javax.naming.Context.OBJECT_FACTORIES,
        javax.naming.Context.URL_PKG_PREFIXES,
        javax.naming.Context.STATE_FACTORIES,
        javax.naming.Context.PROVIDER_URL,
        javax.naming.Context.DNS_URL,
        // The following shouldn't create a runtime dependence on ldap package.
        javax.naming.ldap.LdapContext.CONTROL_FACTORIES
    };

    public static final int INITIAL_CONTEXT_FACTORY = 0;
    public static final int OBJECT_FACTORIES = 1;
    public static final int URL_PKG_PREFIXES = 2;
    public static final int STATE_FACTORIES = 3;
    public static final int PROVIDER_URL = 4;
    public static final int DNS_URL = 5;
    public static final int CONTROL_FACTORIES = 6;

    private VersionHelper() {} // Disallow anyone from creating one of these.

    public static VersionHelper getVersionHelper() {
        return helper;
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loadClass(className, getContextClassLoader());
    }

    public Class<?> loadClassWithoutInit(String className) throws ClassNotFoundException {
        return loadClass(className, false, getContextClassLoader());
    }

    /**
     * @param className A non-null fully qualified class name.
     * @param codebase  A non-null, space-separated list of URL strings.
     */
    public Class<?> loadClass(String className, String codebase)
            throws ClassNotFoundException, MalformedURLException {
        if (TRUST_URL_CODE_BASE) {
            ClassLoader parent = getContextClassLoader();
            ClassLoader cl
                    = URLClassLoader.newInstance(getUrlArray(codebase), parent);
            return loadClass(className, cl);
        } else {
            return null;
        }
    }

    /**
     * Package private.
     * <p>
     * This internal method is used with Thread Context Class Loader (TCCL),
     * please don't expose this method as public.
     */
    Class<?> loadClass(String className, boolean initialize, ClassLoader cl)
            throws ClassNotFoundException {
        Class<?> cls = Class.forName(className, initialize, cl);
        return cls;
    }

    Class<?> loadClass(String className, ClassLoader cl)
            throws ClassNotFoundException {
        return loadClass(className, true, cl);
    }

    /*
     * Returns a JNDI property from the system properties. Returns
     * null if the property is not set, or if there is no permission
     * to read it.
     */
    @SuppressWarnings("removal")
    String getJndiProperty(int i) {
        PrivilegedAction<String> act = () -> {
            try {
                return System.getProperty(PROPS[i]);
            } catch (SecurityException e) {
                return null;
            }
        };
        return AccessController.doPrivileged(act);
    }

    /*
     * Reads each property in PROPS from the system properties, and
     * returns their values -- in order -- in an array.  For each
     * unset property, the corresponding array element is set to null.
     * Returns null if there is no permission to call System.getProperties().
     */
    String[] getJndiProperties() {
        PrivilegedAction<Properties> act = () -> {
            try {
                return System.getProperties();
            } catch (SecurityException e) {
                return null;
            }
        };
        @SuppressWarnings("removal")
        Properties sysProps = AccessController.doPrivileged(act);
        if (sysProps == null) {
            return null;
        }
        String[] jProps = new String[PROPS.length];
        for (int i = 0; i < PROPS.length; i++) {
            jProps[i] = sysProps.getProperty(PROPS[i]);
        }
        return jProps;
    }

    private static String resolveName(Class<?> c, String name) {
        if (name == null) {
            return name;
        }
        if (!name.startsWith("/")) {
            if (!c.isPrimitive()) {
                String baseName = c.getPackageName();
                if (!baseName.isEmpty()) {
                    name = baseName.replace('.', '/') + "/"
                            + name;
                }
            }
        } else {
            name = name.substring(1);
        }
        return name;
    }

    /*
     * Returns the resource of a given name associated with a particular
     * class (never null), or null if none can be found.
     */
    @SuppressWarnings("removal")
    InputStream getResourceAsStream(Class<?> c, String name) {
        PrivilegedAction<InputStream> act = () -> {
            try {
                return c.getModule().getResourceAsStream(resolveName(c, name));
             } catch (IOException x) {
                 return null;
             }
        };
        return AccessController.doPrivileged(act);
    }

    /*
     * Returns an input stream for a file in <java.home>/conf,
     * or null if it cannot be located or opened.
     *
     * @param filename  The file name, sans directory.
     */
    @SuppressWarnings("removal")
    InputStream getJavaHomeConfStream(String filename) {
        PrivilegedAction<InputStream> act = () -> {
            try {
                String javahome = System.getProperty("java.home");
                if (javahome == null) {
                    return null;
                }
                return Files.newInputStream(Path.of(javahome, "conf", filename));
            } catch (Exception e) {
                return null;
            }
        };
        return AccessController.doPrivileged(act);
    }

    /*
     * Returns an enumeration (never null) of InputStreams of the
     * resources of a given name associated with a particular class
     * loader.  Null represents the bootstrap class loader in some
     * Java implementations.
     */
    @SuppressWarnings("removal")
    NamingEnumeration<InputStream> getResources(ClassLoader cl,
                                                String name) throws IOException {
        Enumeration<URL> urls;
        PrivilegedExceptionAction<Enumeration<URL>> act = () ->
                (cl == null)
                        ? ClassLoader.getSystemResources(name)
                        : cl.getResources(name);
        try {
            urls = AccessController.doPrivileged(act);
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
        return new InputStreamEnumeration(urls);
    }


    /**
     * Package private.
     * <p>
     * This internal method returns Thread Context Class Loader (TCCL),
     * if null, returns the system Class Loader.
     * <p>
     * Please don't expose this method as public.
     * @throws SecurityException if the class loader is not accessible
     */
    @SuppressWarnings("removal")
    ClassLoader getContextClassLoader() {

        PrivilegedAction<ClassLoader> act = () -> {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                // Don't use bootstrap class loader directly!
                loader = ClassLoader.getSystemClassLoader();
            }
            return loader;
        };
        return AccessController.doPrivileged(act);
    }

    private static URL[] getUrlArray(String codebase)
            throws MalformedURLException {
        // Parse codebase into separate URLs
        StringTokenizer parser = new StringTokenizer(codebase);
        List<URL> list = new ArrayList<>();
        while (parser.hasMoreTokens()) {
            list.add(new URL(parser.nextToken()));
        }
        return list.toArray(new URL[0]);
    }

    /**
     * Given an enumeration of URLs, an instance of this class represents
     * an enumeration of their InputStreams.  Each operation on the URL
     * enumeration is performed within a doPrivileged block.
     * This is used to enumerate the resources under a foreign codebase.
     * This class is not MT-safe.
     */
    private class InputStreamEnumeration implements
            NamingEnumeration<InputStream> {

        private final Enumeration<URL> urls;

        private InputStream nextElement;

        InputStreamEnumeration(Enumeration<URL> urls) {
            this.urls = urls;
        }

        /*
         * Returns the next InputStream, or null if there are no more.
         * An InputStream that cannot be opened is skipped.
         */
        @SuppressWarnings("removal")
        private InputStream getNextElement() {
            PrivilegedAction<InputStream> act = () -> {
                while (urls.hasMoreElements()) {
                    try {
                        return urls.nextElement().openStream();
                    } catch (IOException e) {
                        // skip this URL
                    }
                }
                return null;
            };
            return AccessController.doPrivileged(act);
        }

        public boolean hasMore() {
            if (nextElement != null) {
                return true;
            }
            nextElement = getNextElement();
            return (nextElement != null);
        }

        public boolean hasMoreElements() {
            return hasMore();
        }

        public InputStream next() {
            if (hasMore()) {
                InputStream res = nextElement;
                nextElement = null;
                return res;
            } else {
                throw new NoSuchElementException();
            }
        }

        public InputStream nextElement() {
            return next();
        }

        public void close() {
        }
    }
}

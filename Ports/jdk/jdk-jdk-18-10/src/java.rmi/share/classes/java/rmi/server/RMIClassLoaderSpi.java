/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * <code>RMIClassLoaderSpi</code> is the service provider interface for
 * <code>RMIClassLoader</code>.
 *
 * In particular, an <code>RMIClassLoaderSpi</code> instance provides an
 * implementation of the following static methods of
 * <code>RMIClassLoader</code>:
 *
 * <ul>
 *
 * <li>{@link RMIClassLoader#loadClass(URL,String)}
 * <li>{@link RMIClassLoader#loadClass(String,String)}
 * <li>{@link RMIClassLoader#loadClass(String,String,ClassLoader)}
 * <li>{@link RMIClassLoader#loadProxyClass(String,String[],ClassLoader)}
 * <li>{@link RMIClassLoader#getClassLoader(String)}
 * <li>{@link RMIClassLoader#getClassAnnotation(Class)}
 *
 * </ul>
 *
 * When one of those methods is invoked, its behavior is to delegate
 * to a corresponding method on an instance of this class.
 * The details of how each method delegates to the provider instance is
 * described in the documentation for each particular method.
 * See the documentation for {@link RMIClassLoader} for a description
 * of how a provider instance is chosen.
 *
 * @author      Peter Jones
 * @author      Laird Dornin
 * @see         RMIClassLoader
 * @since       1.4
 */
public abstract class RMIClassLoaderSpi {

    /**
     * Constructor for subclasses to call.
     */
    public RMIClassLoaderSpi() {}

    /**
     * Provides the implementation for
     * {@link RMIClassLoader#loadClass(URL,String)},
     * {@link RMIClassLoader#loadClass(String,String)}, and
     * {@link RMIClassLoader#loadClass(String,String,ClassLoader)}.
     *
     * Loads a class from a codebase URL path, optionally using the
     * supplied loader.
     *
     * Typically, a provider implementation will attempt to
     * resolve the named class using the given <code>defaultLoader</code>,
     * if specified, before attempting to resolve the class from the
     * codebase URL path.
     *
     * <p>An implementation of this method must either return a class
     * with the given name or throw an exception.
     *
     * @param   codebase the list of URLs (separated by spaces) to load
     * the class from, or <code>null</code>
     *
     * @param   name the name of the class to load
     *
     * @param   defaultLoader additional contextual class loader
     * to use, or <code>null</code>
     *
     * @return  the <code>Class</code> object representing the loaded class
     *
     * @throws  MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to load classes is invalid
     *
     * @throws  ClassNotFoundException if a definition for the class
     * could not be found at the specified location
     */
    public abstract Class<?> loadClass(String codebase, String name,
                                       ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException;

    /**
     * Provides the implementation for
     * {@link RMIClassLoader#loadProxyClass(String,String[],ClassLoader)}.
     *
     * Loads a dynamic proxy class (see {@link java.lang.reflect.Proxy}
     * that implements a set of interfaces with the given names
     * from a codebase URL path, optionally using the supplied loader.
     *
     * <p>An implementation of this method must either return a proxy
     * class that implements the named interfaces or throw an exception.
     *
     * @param   codebase the list of URLs (space-separated) to load
     * classes from, or <code>null</code>
     *
     * @param   interfaces the names of the interfaces for the proxy class
     * to implement
     *
     * @return  a dynamic proxy class that implements the named interfaces
     *
     * @param   defaultLoader additional contextual class loader
     * to use, or <code>null</code>
     *
     * @throws  MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to load classes is invalid
     *
     * @throws  ClassNotFoundException if a definition for one of
     * the named interfaces could not be found at the specified location,
     * or if creation of the dynamic proxy class failed (such as if
     * {@link java.lang.reflect.Proxy#getProxyClass(ClassLoader,Class[])}
     * would throw an <code>IllegalArgumentException</code> for the given
     * interface list)
     */
    public abstract Class<?> loadProxyClass(String codebase,
                                            String[] interfaces,
                                            ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException;

    /**
     * Provides the implementation for
     * {@link RMIClassLoader#getClassLoader(String)}.
     *
     * Returns a class loader that loads classes from the given codebase
     * URL path.
     *
     * <p>If there is a security manger, its <code>checkPermission</code>
     * method will be invoked with a
     * <code>RuntimePermission("getClassLoader")</code> permission;
     * this could result in a <code>SecurityException</code>.
     * The implementation of this method may also perform further security
     * checks to verify that the calling context has permission to connect
     * to all of the URLs in the codebase URL path.
     *
     * @param   codebase the list of URLs (space-separated) from which
     * the returned class loader will load classes from, or <code>null</code>
     *
     * @return a class loader that loads classes from the given codebase URL
     * path
     *
     * @throws  MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to identify the class loader is invalid
     *
     * @throws  SecurityException if there is a security manager and the
     * invocation of its <code>checkPermission</code> method fails, or
     * if the caller does not have permission to connect to all of the
     * URLs in the codebase URL path
     */
    public abstract ClassLoader getClassLoader(String codebase)
        throws MalformedURLException; // SecurityException

    /**
     * Provides the implementation for
     * {@link RMIClassLoader#getClassAnnotation(Class)}.
     *
     * Returns the annotation string (representing a location for
     * the class definition) that RMI will use to annotate the class
     * descriptor when marshalling objects of the given class.
     *
     * @param   cl the class to obtain the annotation for
     *
     * @return  a string to be used to annotate the given class when
     * it gets marshalled, or <code>null</code>
     *
     * @throws  NullPointerException if <code>cl</code> is <code>null</code>
     */
    public abstract String getClassAnnotation(Class<?> cl);
}

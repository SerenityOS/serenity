/*
 * Copyright (c) 1999, 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.loading;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.util.Set;
import java.util.Enumeration;

import javax.management.*;



/**
 * Exposes the remote management interface of the MLet
 * MBean.
 *
 * @since 1.5
 */
public interface MLetMBean   {


    /**
     * Loads a text file containing MLET tags that define the MBeans
     * to be added to the MBean server. The location of the text file is
     * specified by a URL. The text file is read using the UTF-8
     * encoding. The MBeans specified in the MLET file will be
     * instantiated and registered in the MBean server.
     *
     * @param url The URL of the text file to be loaded as String object.
     *
     * @return A set containing one entry per MLET tag in the m-let
     * text file loaded.  Each entry specifies either the
     * ObjectInstance for the created MBean, or a throwable object
     * (that is, an error or an exception) if the MBean could not be
     * created.
     *
     * @exception ServiceNotFoundException One of the following errors
     * has occurred: The m-let text file does not contain an MLET tag,
     * the m-let text file is not found, a mandatory attribute of the
     * MLET tag is not specified, the value of url is malformed.
     */
    public Set<Object> getMBeansFromURL(String url)
            throws ServiceNotFoundException;

    /**
     * Loads a text file containing MLET tags that define the MBeans
     * to be added to the MBean server. The location of the text file is
     * specified by a URL. The text file is read using the UTF-8
     * encoding. The MBeans specified in the MLET file will be
     * instantiated and registered in the MBean server.
     *
     * @param url The URL of the text file to be loaded as URL object.
     *
     * @return A set containing one entry per MLET tag in the m-let
     * text file loaded.  Each entry specifies either the
     * ObjectInstance for the created MBean, or a throwable object
     * (that is, an error or an exception) if the MBean could not be
     * created.
     *
     * @exception ServiceNotFoundException One of the following errors
     * has occurred: The m-let text file does not contain an MLET tag,
     * the m-let text file is not found, a mandatory attribute of the
     * MLET tag is not specified, the value of url is null.
     */
    public Set<Object> getMBeansFromURL(URL url)
            throws ServiceNotFoundException;

    /**
     * Appends the specified URL to the list of URLs to search for classes and
     * resources.
     *
     * @param url the URL to add.
     */
    public void addURL(URL url) ;

    /**
     * Appends the specified URL to the list of URLs to search for classes and
     * resources.
     *
     * @param url the URL to add.
     *
     * @exception ServiceNotFoundException The specified URL is malformed.
     */
    public void addURL(String url) throws ServiceNotFoundException;

    /**
     * Returns the search path of URLs for loading classes and resources.
     * This includes the original list of URLs specified to the constructor,
     * along with any URLs subsequently appended by the addURL() method.
     *
     * @return the list of URLs.
     */
    public URL[] getURLs();

    /** Finds the resource with the given name.
     * A resource is some data (images, audio, text, etc) that can be accessed by class code in a way that is
     *   independent of the location of the code.
     *   The name of a resource is a "/"-separated path name that identifies the resource.
     *
     * @param name The resource name
     *
     * @return  An URL for reading the resource, or null if the resource could not be found or the caller doesn't have adequate privileges to get the
     * resource.
     */
    public URL getResource(String name);

    /** Returns an input stream for reading the specified resource. The search order is described in the documentation for
     *  getResource(String).
     *
     * @param name  The resource name
     *
     * @return An input stream for reading the resource, or null if the resource could not be found
     *
     */
    public InputStream getResourceAsStream(String name);

    /**
     * Finds all the resources with the given name. A resource is some
     * data (images, audio, text, etc) that can be accessed by class
     * code in a way that is independent of the location of the code.
     * The name of a resource is a "/"-separated path name that
     * identifies the resource.
     *
     * @param name The  resource name.
     *
     * @return An enumeration of URL to the resource. If no resources
     * could be found, the enumeration will be empty. Resources that
     * cannot be accessed will not be in the enumeration.
     *
     * @exception IOException if an I/O exception occurs when
     * searching for resources.
     */
    public Enumeration<URL> getResources(String name) throws IOException;

    /**
     * Gets the current directory used by the library loader for
     * storing native libraries before they are loaded into memory.
     *
     * @return The current directory used by the library loader.
     *
     * @see #setLibraryDirectory
     *
     * @throws UnsupportedOperationException if this implementation
     * does not support storing native libraries in this way.
     */
    public String getLibraryDirectory();

    /**
     * Sets the directory used by the library loader for storing
     * native libraries before they are loaded into memory.
     *
     * @param libdir The directory used by the library loader.
     *
     * @see #getLibraryDirectory
     *
     * @throws UnsupportedOperationException if this implementation
     * does not support storing native libraries in this way.
     */
    public void setLibraryDirectory(String libdir);

 }

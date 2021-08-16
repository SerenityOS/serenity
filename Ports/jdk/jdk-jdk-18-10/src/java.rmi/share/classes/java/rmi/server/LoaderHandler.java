/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * <code>LoaderHandler</code> is an interface used internally by the RMI
 * runtime in previous implementation versions.  It should never be accessed
 * by application code.
 *
 * @author  Ann Wollrath
 * @since   1.1
 *
 * @deprecated no replacement
 */
@Deprecated
public interface LoaderHandler {

    /** package of system <code>LoaderHandler</code> implementation. */
    final static String packagePrefix = "sun.rmi.server";

    /**
     * Loads a class from the location specified by the
     * <code>java.rmi.server.codebase</code> property.
     *
     * @param  name the name of the class to load
     * @return the <code>Class</code> object representing the loaded class
     * @throws MalformedURLException
     *         if the system property <b>java.rmi.server.codebase</b>
     *         contains an invalid URL
     * @throws ClassNotFoundException
     *         if a definition for the class could not
     *         be found at the codebase location.
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    Class<?> loadClass(String name)
        throws MalformedURLException, ClassNotFoundException;

    /**
     * Loads a class from a URL.
     *
     * @param codebase  the URL from which to load the class
     * @param name      the name of the class to load
     * @return the <code>Class</code> object representing the loaded class
     * @throws MalformedURLException
     *         if the <code>codebase</code> paramater
     *         contains an invalid URL
     * @throws ClassNotFoundException
     *         if a definition for the class could not
     *         be found at the specified URL
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    Class<?> loadClass(URL codebase, String name)
        throws MalformedURLException, ClassNotFoundException;

    /**
     * Returns the security context of the given class loader.
     *
     * @param loader  a class loader from which to get the security context
     * @return the security context
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    Object getSecurityContext(ClassLoader loader);
}

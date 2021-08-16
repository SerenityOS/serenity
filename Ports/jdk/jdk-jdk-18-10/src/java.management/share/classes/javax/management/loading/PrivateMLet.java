/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.net.URLStreamHandlerFactory;

/**
 * An MLet that is not added to the {@link ClassLoaderRepository}.
 * This class acts exactly like its parent class, {@link MLet}, with
 * one exception.  When a PrivateMLet is registered in an MBean
 * server, it is not added to that MBean server's {@link
 * ClassLoaderRepository}.  This is true because this class implements
 * the interface {@link PrivateClassLoader}.
 *
 * @since 1.5
 */
@SuppressWarnings("serial") // Externalizable class w/o no-arg c'tor
public class PrivateMLet extends MLet implements PrivateClassLoader {
    private static final long serialVersionUID = 2503458973393711979L;

    /**
      * Constructs a new PrivateMLet for the specified URLs using the
      * default delegation parent ClassLoader.  The URLs will be
      * searched in the order specified for classes and resources
      * after first searching in the parent class loader.
      *
      * @param  urls  The URLs from which to load classes and resources.
      * @param  delegateToCLR  True if, when a class is not found in
      * either the parent ClassLoader or the URLs, the MLet should delegate
      * to its containing MBeanServer's {@link ClassLoaderRepository}.
      *
      */
    public PrivateMLet(URL[] urls, boolean delegateToCLR) {
        super(urls, delegateToCLR);
    }

    /**
      * Constructs a new PrivateMLet for the given URLs. The URLs will
      * be searched in the order specified for classes and resources
      * after first searching in the specified parent class loader.
      * The parent argument will be used as the parent class loader
      * for delegation.
      *
      * @param  urls  The URLs from which to load classes and resources.
      * @param  parent The parent class loader for delegation.
      * @param  delegateToCLR  True if, when a class is not found in
      * either the parent ClassLoader or the URLs, the MLet should delegate
      * to its containing MBeanServer's {@link ClassLoaderRepository}.
      *
      */
    public PrivateMLet(URL[] urls, ClassLoader parent, boolean delegateToCLR) {
        super(urls, parent, delegateToCLR);
    }

    /**
      * Constructs a new PrivateMLet for the specified URLs, parent
      * class loader, and URLStreamHandlerFactory. The parent argument
      * will be used as the parent class loader for delegation. The
      * factory argument will be used as the stream handler factory to
      * obtain protocol handlers when creating new URLs.
      *
      * @param  urls  The URLs from which to load classes and resources.
      * @param  parent The parent class loader for delegation.
      * @param  factory  The URLStreamHandlerFactory to use when creating URLs.
      * @param  delegateToCLR  True if, when a class is not found in
      * either the parent ClassLoader or the URLs, the MLet should delegate
      * to its containing MBeanServer's {@link ClassLoaderRepository}.
      *
      */
    public PrivateMLet(URL[] urls,
                       ClassLoader parent,
                       URLStreamHandlerFactory factory,
                       boolean delegateToCLR) {
        super(urls, parent, factory, delegateToCLR);
    }
}

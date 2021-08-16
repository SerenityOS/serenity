/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.mbeanserver;


// JMX import
import javax.management.ObjectName;
import javax.management.loading.ClassLoaderRepository;

/**
 * This interface keeps the list of Class Loaders registered in the
 * MBean Server.
 * It provides the necessary methods to load classes using the
 * registered Class Loaders, and to add/remove class loaders from the
 * list.
 *
 * @since 1.5
 */
public interface ModifiableClassLoaderRepository
    extends ClassLoaderRepository {

    /**
     * Add an anonymous ClassLoader to the repository.
     **/
    public void addClassLoader(ClassLoader loader);

    /**
     * Remove the specified ClassLoader to the repository.
     * The class loader may or may not be anonymous.
     **/
    public void removeClassLoader(ClassLoader loader);

    /**
     * Add a named ClassLoader to the repository.
     **/
    public void addClassLoader(ObjectName name, ClassLoader loader);

    /**
     * Remove a named ClassLoader from the repository.
     **/
    public void removeClassLoader(ObjectName name);

    /**
     * Get a named ClassLoader from the repository.
     **/
    public ClassLoader getClassLoader(ObjectName name);
}

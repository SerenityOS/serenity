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

package javax.management.loading;

import javax.management.MBeanServer; // for Javadoc

/**
 * <p>Instances of this interface are used to keep the list of ClassLoaders
 * registered in an MBean Server.
 * They provide the necessary methods to load classes using the registered
 * ClassLoaders.</p>
 *
 * <p>The first ClassLoader in a <code>ClassLoaderRepository</code> is
 * always the MBean Server's own ClassLoader.</p>
 *
 * <p>When an MBean is registered in an MBean Server, if it is of a
 * subclass of {@link java.lang.ClassLoader} and if it does not
 * implement the interface {@link PrivateClassLoader}, it is added to
 * the end of the MBean Server's <code>ClassLoaderRepository</code>.
 * If it is subsequently unregistered from the MBean Server, it is
 * removed from the <code>ClassLoaderRepository</code>.</p>
 *
 * <p>The order of MBeans in the <code>ClassLoaderRepository</code> is
 * significant.  For any two MBeans <em>X</em> and <em>Y</em> in the
 * <code>ClassLoaderRepository</code>, <em>X</em> must appear before
 * <em>Y</em> if the registration of <em>X</em> was completed before
 * the registration of <em>Y</em> started.  If <em>X</em> and
 * <em>Y</em> were registered concurrently, their order is
 * indeterminate.  The registration of an MBean corresponds to the
 * call to {@link MBeanServer#registerMBean} or one of the {@link
 * MBeanServer}<code>.createMBean</code> methods.</p>
 *
 * @see javax.management.MBeanServerFactory
 *
 * @since 1.5
 */
public interface ClassLoaderRepository {

    /**
     * <p>Load the given class name through the list of class loaders.
     * Each ClassLoader in turn from the ClassLoaderRepository is
     * asked to load the class via its {@link
     * ClassLoader#loadClass(String)} method.  If it successfully
     * returns a {@link Class} object, that is the result of this
     * method.  If it throws a {@link ClassNotFoundException}, the
     * search continues with the next ClassLoader.  If it throws
     * another exception, the exception is propagated from this
     * method.  If the end of the list is reached, a {@link
     * ClassNotFoundException} is thrown.</p>
     *
     * @param className The name of the class to be loaded.
     *
     * @return the loaded class.
     *
     * @exception ClassNotFoundException The specified class could not be
     *            found.
     */
    public Class<?> loadClass(String className)
            throws ClassNotFoundException;

    /**
     * <p>Load the given class name through the list of class loaders,
     * excluding the given one.  Each ClassLoader in turn from the
     * ClassLoaderRepository, except <code>exclude</code>, is asked to
     * load the class via its {@link ClassLoader#loadClass(String)}
     * method.  If it successfully returns a {@link Class} object,
     * that is the result of this method.  If it throws a {@link
     * ClassNotFoundException}, the search continues with the next
     * ClassLoader.  If it throws another exception, the exception is
     * propagated from this method.  If the end of the list is
     * reached, a {@link ClassNotFoundException} is thrown.</p>
     *
     * <p>Be aware that if a ClassLoader in the ClassLoaderRepository
     * calls this method from its {@link ClassLoader#loadClass(String)
     * loadClass} method, it exposes itself to a deadlock if another
     * ClassLoader in the ClassLoaderRepository does the same thing at
     * the same time.  The {@link #loadClassBefore} method is
     * recommended to avoid the risk of deadlock.</p>
     *
     * @param className The name of the class to be loaded.
     * @param exclude The class loader to be excluded.  May be null,
     * in which case this method is equivalent to {@link #loadClass
     * loadClass(className)}.
     *
     * @return the loaded class.
     *
     * @exception ClassNotFoundException The specified class could not
     * be found.
     */
    public Class<?> loadClassWithout(ClassLoader exclude,
                                     String className)
            throws ClassNotFoundException;

    /**
     * <p>Load the given class name through the list of class loaders,
     * stopping at the given one.  Each ClassLoader in turn from the
     * ClassLoaderRepository is asked to load the class via its {@link
     * ClassLoader#loadClass(String)} method.  If it successfully
     * returns a {@link Class} object, that is the result of this
     * method.  If it throws a {@link ClassNotFoundException}, the
     * search continues with the next ClassLoader.  If it throws
     * another exception, the exception is propagated from this
     * method.  If the search reaches <code>stop</code> or the end of
     * the list, a {@link ClassNotFoundException} is thrown.</p>
     *
     * <p>Typically this method is called from the {@link
     * ClassLoader#loadClass(String) loadClass} method of
     * <code>stop</code>, to consult loaders that appear before it
     * in the <code>ClassLoaderRepository</code>.  By stopping the
     * search as soon as <code>stop</code> is reached, a potential
     * deadlock with concurrent class loading is avoided.</p>
     *
     * @param className The name of the class to be loaded.
     * @param stop The class loader at which to stop.  May be null, in
     * which case this method is equivalent to {@link #loadClass(String)
     * loadClass(className)}.
     *
     * @return the loaded class.
     *
     * @exception ClassNotFoundException The specified class could not
     * be found.
     *
     */
    public Class<?> loadClassBefore(ClassLoader stop,
                                    String className)
            throws ClassNotFoundException;

}

/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Contains classes related to developing <em>beans</em> -- components based on
 * the JavaBeans architecture. A few of the classes are used by beans
 * while they run in an application. For example, the event classes are used by
 * beans that fire property and vetoable change events (see
 * {@link java.beans.PropertyChangeEvent}). However, most of the classes in this
 * package are meant to be used by a bean editor (that is, a development
 * environment for customizing and putting together beans to create an
 * application). In particular, these classes help the bean editor create a user
 * interface that the user can use to customize the bean. For example, a bean
 * may contain a property of a special type that a bean editor may not know how
 * to handle. By using the {@code PropertyEditor} interface, a bean developer
 * can provide an editor for this special type.
 * <p>
 * To minimize the resources used by a bean, the classes used by bean editors
 * are loaded only when the bean is being edited. They are not needed while the
 * bean is running in an application and therefore not loaded. This information
 * is kept in what's called a bean-info (see {@link java.beans.BeanInfo}).
 * <p>
 * Unless explicitly stated, null values or empty Strings are not valid
 * parameters for the methods in this package. You may expect to see exceptions
 * if these parameters are used.
 *
 * <h2>Long-Term Persistence</h2>
 * As of v1.4, the {@code java.beans} package provides support for <em>long-term
 * persistence</em> -- reading and writing a bean as a textual representation of
 * its property values. The property values are treated as beans, and are
 * recursively read or written to capture their publicly available state. This
 * approach is suitable for long-term storage because it relies only on public
 * API, rather than the likely-to-change private implementation.
 *
 * <blockquote><hr><b>Note:</b> The persistence scheme cannot automatically
 * instantiate custom inner classes, such as you might use for event handlers.
 * By using the {@link java.beans.EventHandler} class instead of inner classes
 * for custom event handlers, you can avoid this problem.<hr></blockquote>
 * <p>
 * You read and write beans in XML format using the
 * {@link java.beans.XMLDecoder} and {@link java.beans.XMLEncoder} classes,
 * respectively. One notable feature of the persistence scheme is that reading
 * in a bean requires no special knowledge of the bean.
 * <p>
 * Writing out a bean, on the other hand, sometimes requires special knowledge
 * of the bean's type. If the bean's state can be expressed using only the
 * no-argument constructor and public getter and setter methods for properties,
 * no special knowledge is required. Otherwise, the bean requires a custom
 * <em>persistence delegate</em> -- an object that is in charge of writing out
 * beans of a particular type. All classes provided in the JDK that descend from
 * {@code java.awt.Component}, as well as all their properties, automatically
 * have persistence delegates.
 * <p>
 * If you need (or choose) to provide a persistence delegate for a bean, you can
 * do so either by using a {@link java.beans.DefaultPersistenceDelegate}
 * instance or by creating your own subclass of {@code PersistenceDelegate}. If
 * the only reason a bean needs a persistence delegate is because you want to
 * invoke the bean's constructor with property values as arguments, you can
 * create the bean's persistence delegate with the one-argument
 * {@code DefaultPersistenceDelegate} constructor. Otherwise, you need to
 * implement your own persistence delegate, for which you're likely to need the
 * following classes:
 * <dl>
 *     <dt>{@link java.beans.PersistenceDelegate}</dt>
 *     <dd>The abstract class from which all persistence delegates descend. Your
 *     subclass should use its knowledge of the bean's type to provide whatever
 *     {@code Statement}s and {@code Expression}s are necessary to create the
 *     bean and restore its state.</dd>
 *     <dt>{@link java.beans.Statement}</dt>
 *     <dd>Represents the invocation of a single method on an object. Includes
 *     a set of arguments to the method.</dd>
 *     <dt>{@link java.beans.Expression}</dt>
 *     <dd>A subclass of {@code Statement} used for methods that return a
 *     value.</dd>
 * </dl>
 * <p>
 * Once you create a persistence delegate, you register it using the
 * {@code setPersistenceDelegate} method of {@code XMLEncoder}.
 *
 * <h2>Related Documentation</h2>
 * For overview, architecture, and tutorial documentation, please see:
 * <ul>
 *     <li><a href="https://docs.oracle.com/javase/tutorial/javabeans/">
 *         JavaBeans</a>, a trail in <em>The Java Tutorial</em>.</li>
 *     <li><a href="http://www.oracle.com/technetwork/java/persistence2-141443.html">
 *         Long-Term Persistence</a>, an article in
 *         <em>The Swing Connection</em>.</li>
 * </ul>
 */
package java.beans;

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
 * Contains all of the classes for creating user interfaces and for painting
 * graphics and images. A user interface object such as a button or a scrollbar
 * is called, in AWT terminology, a component. The Component class is the root
 * of all AWT components. See Component for a detailed description of properties
 * that all AWT components share.
 * <p>
 * Some components fire events when a user interacts with the components. The
 * AWTEvent class and its subclasses are used to represent the events that AWT
 * components can fire. See AWTEvent for a description of the AWT event model.
 * <p>
 * A container is a component that can contain components and other containers.
 * A container can also have a layout manager that controls the visual placement
 * of components in the container. The AWT package contains several layout
 * manager classes and an interface for building your own layout manager. See
 * Container and LayoutManager for more information.
 * <p>
 * Each {@code Component} object is limited in its maximum size and its location
 * because the values are stored as an integer. Also, a platform may further
 * restrict maximum size and location coordinates. The exact maximum values are
 * dependent on the platform. There is no way to change these maximum values,
 * either in Java code or in native code. These limitations also impose
 * restrictions on component layout. If the bounds of a Component object exceed
 * a platform limit, there is no way to properly arrange them within a Container
 * object. The object's bounds are defined by any object's coordinate in
 * combination with its size on a respective axis.
 *
 * <h2>Additional Specification</h2>
 * <ul>
 *     <li><a href="doc-files/FocusSpec.html">The AWT Focus Subsystem</a>
 *     <li><a href="doc-files/Modality.html">The AWT Modality</a>
 *     <li><a href="{@docRoot}/../specs/AWT_Native_Interface.html">
 *                  The Java AWT Native Interface (JAWT)</a>
 * </ul>
 *
 * @since 1.0
 */
package java.awt;

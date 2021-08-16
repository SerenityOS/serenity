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
 * Provides interfaces and classes for transferring data between and within
 * applications. It defines the notion of a "transferable" object, which is an
 * object capable of being transferred between or within applications. An object
 * identifies itself as being transferable by implementing the Transferable
 * interface.
 * <p>
 * It also provides a clipboard mechanism, which is an object that temporarily
 * holds a transferable object that can be transferred between or within an
 * application. The clipboard is typically used for copy and paste operations.
 * Although it is possible to create a clipboard to use within an application,
 * most applications will use the system clipboard to ensure the data can be
 * transferred across applications running on the platform.
 *
 * @since 1.1
 */
package java.awt.datatransfer;

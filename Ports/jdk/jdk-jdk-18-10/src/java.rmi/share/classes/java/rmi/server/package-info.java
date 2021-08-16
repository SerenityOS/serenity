/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Provides classes and interfaces for supporting the server side of RMI.
 * One group of classes are used by the static stubs and skeletons.
 * Another group of classes implements the RMI Transport protocol.
 *
 * <p><strong>Deprecated: Skeletons and Static Stubs.</strong>
 *
 * <em>Skeletons and statically generated stubs are deprecated.  This
 * includes the APIs in this package that require the use of skeletons
 * or static stubs and the runtime support for them.  Support for skeletons
 * and static stubs may be removed in a future release of the
 * platform. Skeletons are unnecessary, as server-side method dispatching
 * is handled directly by the RMI runtime. Statically generated stubs are
 * unnecessary, as stubs are generated dynamically using {@link
 * java.lang.reflect.Proxy Proxy} objects. See {@link
 * java.rmi.server.UnicastRemoteObject UnicastRemoteObject} for
 * information about dynamic stub generation.</em>
 *
 * @since 1.1
 */
package java.rmi.server;

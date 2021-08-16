/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * Provides the classes necessary to create an applet and the classes an applet
 * uses to communicate with its applet context.
 * <p>
 * The applet framework involves two entities: the <i>applet</i> and the
 * <i>applet context</i>. An applet is an embeddable window (see the Panel
 * class) with a few extra methods that the applet context can use to
 * initialize, start, and stop the applet.
 * <p>
 * The applet context is an application that is responsible for loading and
 * running applets. For example, the applet context could be a Web browser or an
 * applet development environment.
 * <p>
 * This package has been deprecated and may be removed in
 * a future version of the Java Platform. There is no replacement.
 * All of the classes and interfaces in this package have been terminally
 * deprecated.
 * Users are advised to migrate their applications to other technologies.
 *
 * @since 1.0
 */
package java.applet;

/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 *  Provides the front end for the javadoc tool.
 *
 *  <p>The main entry points are in {@link jdk.javadoc.internal.tool.Main}
 *  which calls the (poorly-named) {@link jdk.javadoc.internal.tool.Start}
 *  which provides the overall functionality of the tool.
 *
 *  <p>The classes provide a framework for processing command-line options
 *  and determining the set of elements (modules, packages, types and members)
 *  to be documented.
 *
 *  <p>The classes also provide the means to use the javac front end to read
 *  source files, including the documentation comments.
 *
 *  <p>Finally, once the appropriate files have been read, the classes invoke
 *  the selected doclet to process those files, typically to generate API
 *  documentation.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
package jdk.javadoc.internal.tool;

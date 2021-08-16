/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Provides a mechanism to launch an instance of a Java shell tool.
 * Allows configuration of the tool before launching. A builder is used
 * to configure and launch the tool.
 * <p>
 * At the simplest, a builder is retrieved, and the builder is used to start the
 * tool:
 * <pre>
 * {@code
 *       JavaShellToolBuilder
 *             .builder()
 *             .start();
 * }
 * </pre>
 * The builder can be configured and the start can have arguments:
 * <pre>
 * {@code
 *       JavaShellToolBuilder
 *             .builder()
 *             .out(myCommandPrintStream, myOutputPrintStream)
 *             .locale(Locale.CANADA)
 *             .start("--feedback", "silent", "MyStart");
 * }
 * </pre>
 *
 * @since 9
 */


package jdk.jshell.tool;


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
 * Defines the Service Provider Interface for pluggable JShell execution engines.
 * The JShell core tracks and compiles Snippets then sends them
 * (represented in a wrapper class) to the execution engine for loading,
 * and in the case of executable Snippets, execution.  The JShell
 * implementation includes a default execution engine (currently a remote
 * process which is JDI controlled).  By implementing the
 * {@link jdk.jshell.spi.ExecutionControl} interface and its generating class,
 * an implementation of the {@link jdk.jshell.spi.ExecutionControlProvider}
 * interface, and installing it with
 * {@link jdk.jshell.JShell.Builder#executionEngine(String)}
 * other execution engines can be used. Where the passed String is an
 * {@code ExecutionControl} spec.
 * <p>
 * The {@code ExecutionControl} spec is the {@code ExecutionControlProvider}
 * name optionally followed by a parameter specification.
 * The syntax of the spec is:
 * <pre>
 * spec   := name : params
 *        | name
 * name   := identifier
 * params := param , params
 *        | param
 *        |
 * param  := identifier ( value )
 * </pre>
 * Where 'name' is the {@code ExecutionControlProvider}
 * {@linkplain ExecutionControlProvider#name() name}.
 * Where 'param' is a Map key from
 * {@link ExecutionControlProvider#defaultParameters()} and the parenthesized
 * value; See, for example,
 * {@link jdk.jshell.execution.JdiExecutionControlProvider}.
 * Where 'identifier' is a sequence of
 * {@linkplain java.lang.Character#isJavaIdentifierPart(char)
 * Java identifier part characters} from the Basic Multilingual Plane.
 * <p>
 * For example:
 * <ul>
 *   <li>local</li>
 *   <li>jdi:hostname(localhost)</li>
 *   <li>failover:1(jdi),2(jdi:launch(true),timeout(3000)),3(local)</li>
 * </ul>
 *
 * @since 9
 * @see jdk.jshell.execution for execution implementation support
 */
package jdk.jshell.spi;

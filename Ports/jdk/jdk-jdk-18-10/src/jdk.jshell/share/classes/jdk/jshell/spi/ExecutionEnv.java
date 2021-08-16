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

package jdk.jshell.spi;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * Functionality made available to a pluggable JShell execution engine.  It is
 * provided to the execution engine by the core JShell implementation.
 * <p>
 * This interface is designed to provide the access to core JShell functionality
 * needed to implement ExecutionControl.
 *
 * @since 9
 * @see ExecutionControl
 */
public interface ExecutionEnv {

    /**
     * Returns the user's input stream.
     *
     * @return the user's input stream
     */
    InputStream userIn();

    /**
     * Returns the user's output stream.
     *
     * @return the user's output stream
     */
    PrintStream userOut();

    /**
     * Returns the user's error stream.
     *
     * @return the user's error stream
     */
    PrintStream userErr();

    /**
     * Returns the additional VM options to be used when launching the remote
     * JVM. This is advice to the execution engine.
     * <p>
     * Note: an execution engine need not launch a remote JVM.
     *
     * @return the additional options with which to launch the remote JVM
     */
    List<String> extraRemoteVMOptions();

    /**
     * Reports that the execution engine has shutdown.
     */
    void closeDown();

}

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

package jdk.jshell.execution;

import java.util.Map;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

/**
 * A provider of execution engines which run in the same process as JShell.
 *
 * @author Robert Field
 * @since 9
 */
public class LocalExecutionControlProvider implements ExecutionControlProvider{

    /**
     * Create an instance.  An instance can be used to
     * {@linkplain  #generate generate} an {@link ExecutionControl} instance
     * that executes code in the same process.
     */
    public LocalExecutionControlProvider() {
    }

    /**
     * The unique name of this {@code ExecutionControlProvider}.
     *
     * @return "local"
     */
    @Override
    public String name() {
        return "local";
    }

    /**
     * Create and return the default parameter map for
     * {@code LocalExecutionControlProvider}.
     * {@code LocalExecutionControlProvider} has no parameters.
     *
     * @return an empty parameter map
     */
    @Override
    public Map<String,String> defaultParameters() {
        return ExecutionControlProvider.super.defaultParameters();
    }

    /**
     * Create and return a locally executing {@code ExecutionControl} instance.
     *
     * @param env the execution environment, provided by JShell
     * @param parameters the {@linkplain #defaultParameters()  default} or
     * modified parameter map.
     * @return the execution engine
     */
    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) {
        return new LocalExecutionControl();
    }

}

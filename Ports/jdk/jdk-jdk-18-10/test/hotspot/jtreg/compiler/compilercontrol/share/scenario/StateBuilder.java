/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package compiler.compilercontrol.share.scenario;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.Map;

public interface StateBuilder<T extends CompileCommand> {
    /**
     * Adds CompileCommand to builder
     *
     * @param command command used to build states
     */
    void add(T command);

    /**
     * Gets final states of methods
     *
     * @return a map with pairs of executables and their states
     */
    Map<Executable, State> getStates();

    /**
     * Gets list of VM options that will change states of methods
     *
     * @return a list of VM options
     */
    List<String> getOptions();

    /**
     * Gets list of passed commands {@link CompileCommand}
     *
     * @return a list of compile commands
     */
    List<T> getCompileCommands();

    /**
     * Shows that this state builder created a valid input for the test vm
     *
     * @return true if this is a valid input
     */
    boolean isValid();
}

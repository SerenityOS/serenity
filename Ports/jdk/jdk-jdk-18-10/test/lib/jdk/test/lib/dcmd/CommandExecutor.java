/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.dcmd;

import jdk.test.lib.process.OutputAnalyzer;

/**
 * Abstract base class for Diagnostic Command executors
 */
public abstract class CommandExecutor {

    /**
     * Execute a diagnostic command
     *
     * @param cmd The diagnostic command to execute
     * @return an {@link jdk.test.lib.process.OutputAnalyzer} encapsulating the output of the command
     * @throws CommandExecutorException if there is an exception on the "calling side" while trying to execute the
     *          Diagnostic Command. Exceptions thrown on the remote side are available as textual representations in
     *          stderr, regardless of the specific executor used.
     */
    public final OutputAnalyzer execute(String cmd) throws CommandExecutorException {
        return execute(cmd, false);
    }

    /**
     * Execute a diagnostic command
     *
     * @param cmd The diagnostic command to execute
     * @param silent Do not print the command output
     * @return an {@link jdk.test.lib.process.OutputAnalyzer} encapsulating the output of the command
     * @throws CommandExecutorException if there is an exception on the "calling side" while trying to execute the
     *          Diagnostic Command. Exceptions thrown on the remote side are available as textual representations in
     *          stderr, regardless of the specific executor used.
     */
    public final OutputAnalyzer execute(String cmd, boolean silent) throws CommandExecutorException {
        if (!silent) {
            System.out.printf("Running DCMD '%s' through '%s'%n", cmd, this.getClass().getSimpleName());
        }

        OutputAnalyzer oa = executeImpl(cmd);

        if (!silent) {
            System.out.println("---------------- stdout ----------------");
            System.out.println(oa.getStdout());
            System.out.println("---------------- stderr ----------------");
            System.out.println(oa.getStderr());
            System.out.println("----------------------------------------");
            System.out.println();
        }
        return oa;
    }

    protected abstract OutputAnalyzer executeImpl(String cmd) throws CommandExecutorException;
}

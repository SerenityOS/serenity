/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.util.List;

/**
 * Base class for Diagnostic Command Executors using the jcmd tool
 */
public abstract class JcmdExecutor extends CommandExecutor {
    protected String jcmdBinary;

    protected abstract List<String> createCommandLine(String cmd) throws CommandExecutorException;

    protected JcmdExecutor() {
        jcmdBinary = JDKToolFinder.getJDKTool("jcmd");
    }

    protected OutputAnalyzer executeImpl(String cmd) throws CommandExecutorException {
        List<String> commandLine = createCommandLine(cmd);

        try {
            System.out.printf("Executing command '%s'%n", commandLine);
            OutputAnalyzer output = ProcessTools.executeProcess(new ProcessBuilder(commandLine));
            System.out.printf("Command returned with exit code %d%n", output.getExitValue());

            return output;
        } catch (Exception e) {
            String message = String.format("Caught exception while executing '%s'", commandLine);
            throw new CommandExecutorException(message, e);
        }
    }
}

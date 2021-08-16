/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package toolbox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A task to configure and run a general command.
 */
public class ExecTask extends AbstractTask<ExecTask> {
    private final String command;
    private List<String> args;

    /**
     * Create a task to execute a given command, to be run using {@code EXEC} mode.
     * @param toolBox the {@code ToolBox} to use
     * @param command the command to be executed
     */
    public ExecTask(ToolBox toolBox, String command) {
        super(toolBox, Task.Mode.EXEC);
        this.command = command;
    }

    /**
     * Create a task to execute a given command, to be run using {@code EXEC} mode.
     * @param toolBox the {@code ToolBox} to use
     * @param command the command to be executed
     */
    public ExecTask(ToolBox toolBox, Path command) {
        super(toolBox, Task.Mode.EXEC);
        this.command = command.toString();
    }

    /**
     * Sets the arguments for the command to be executed
     * @param args the arguments
     * @return this task object
     */
    public ExecTask args(String... args) {
        this.args = Arrays.asList(args);
        return this;
    }

    /**
     * {@inheritDoc}
     * @return the name "exec"
     */
    @Override
    public String name() {
        return "exec";
    }

    /**
     * Calls the command with the arguments as currently configured.
     * @return a Result object indicating the outcome of the task
     * and the content of any output written to stdout or stderr.
     * @throws TaskError if the outcome of the task is not as expected.
     */
    @Override
    public Task.Result run() {
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(command);
        if (args != null)
            cmdArgs.addAll(args);
        ProcessBuilder pb = getProcessBuilder();
        pb.command(cmdArgs);
        try {
            return runProcess(toolBox, this, pb.start());
        } catch (IOException | InterruptedException e) {
            throw new Error(e);
        }
    }
}

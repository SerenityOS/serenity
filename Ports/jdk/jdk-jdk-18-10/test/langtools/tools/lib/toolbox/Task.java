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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static toolbox.ToolBox.lineSeparator;

/**
 * The supertype for tasks.
 * Complex operations are modeled by building and running a "Task" object.
 * Tasks are typically configured in a fluent series of calls.
 */
public interface Task {
    /**
     * Returns the name of the task.
     * @return the name of the task
     */
    String name();

    /**
     * Executes the task as currently configured.
     * @return a Result object containing the results of running the task
     * @throws TaskError if the outcome of the task was not as expected
     */
    Result run() throws TaskError;

    /**
     * Exception thrown by {@code Task.run} when the outcome is not as
     * expected.
     */
    public static class TaskError extends Error {
        /**
         * Creates a TaskError object with the given message.
         * @param message the message
         */
        public TaskError(String message) {
            super(message);
        }
    }

    /**
     * An enum to indicate the mode a task should use it is when executed.
     */
    public enum Mode {
        /**
         * The task should use the interface used by the command
         * line launcher for the task.
         * For example, for javac: com.sun.tools.javac.Main.compile
         */
        CMDLINE,
        /**
         * The task should use a publicly defined API for the task.
         * For example, for javac: javax.tools.JavaCompiler
         */
        API,
        /**
         * The task should use the standard launcher for the task.
         * For example, $JAVA_HOME/bin/javac
         */
        EXEC
    }

    /**
     * An enum to indicate the expected success or failure of executing a task.
     */
    public enum Expect {
        /** It is expected that the task will complete successfully. */
        SUCCESS,
        /** It is expected that the task will not complete successfully. */
        FAIL
    }

    /**
     * An enum to identify the streams that may be written by a {@code Task}.
     */
    public enum OutputKind {
        /** Identifies output written to {@code System.out} or {@code stdout}. */
        STDOUT,
        /** Identifies output written to {@code System.err} or {@code stderr}. */
        STDERR,
        /** Identifies output written to a stream provided directly to the task. */
        DIRECT
    };

    /**
     * The results from running a {@link Task}.
     * The results contain the exit code returned when the tool was invoked,
     * and a map containing the output written to any streams during the
     * execution of the tool.
     * All tools support "stdout" and "stderr".
     * Tools that take an explicit PrintWriter save output written to that
     * stream as "main".
     */
    public static class Result {
        final ToolBox toolBox;
        final Task task;
        final int exitCode;
        final Map<OutputKind, String> outputMap;

        Result(ToolBox toolBox, Task task, int exitCode, Map<OutputKind, String> outputMap) {
            this.toolBox = toolBox;
            this.task = task;
            this.exitCode = exitCode;
            this.outputMap = outputMap;
        }

        /**
         * Returns the content of a specified stream.
         * @param outputKind the kind of the selected stream
         * @return the content that was written to that stream when the tool
         *  was executed.
         */
        public String getOutput(OutputKind outputKind) {
            return outputMap.get(outputKind);
        }

        /**
         * Returns the content of named streams as a list of lines.
         * @param outputKinds the kinds of the selected streams
         * @return the content that was written to the given streams when the tool
         *  was executed.
         */
        public List<String> getOutputLines(OutputKind... outputKinds) {
            List<String> result = new ArrayList<>();
            for (OutputKind outputKind : outputKinds) {
                result.addAll(Arrays.asList(outputMap.get(outputKind).split(lineSeparator)));
            }
            return result;
        }

        /**
         * Writes the content of the specified stream to the log.
         * @param kind the kind of the selected stream
         * @return this Result object
         */
        public Result write(OutputKind kind) {
            PrintStream out = toolBox.out;
            String text = getOutput(kind);
            if (text == null || text.isEmpty())
                out.println("[" + task.name() + ":" + kind + "]: empty");
            else {
                out.println("[" + task.name() + ":" + kind + "]:");
                out.print(text);
            }
            return this;
        }

        /**
         * Writes the content of all streams with any content to the log.
         * @return this Result object
         */
        public Result writeAll() {
            PrintStream out = toolBox.out;
            outputMap.forEach((name, text) -> {
                if (!text.isEmpty()) {
                    out.println("[" + name + "]:");
                    out.print(text);
                }
            });
            return this;
        }
    }
}


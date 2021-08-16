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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import static toolbox.ToolBox.lineSeparator;

/**
 * A utility base class to simplify the implementation of tasks.
 * Provides support for running the task in a process and for
 * capturing output written by the task to stdout, stderr and
 * other writers where applicable.
 * @param <T> the implementing subclass
 */
abstract class AbstractTask<T extends AbstractTask<T>> implements Task {
    protected final ToolBox toolBox;
    protected final Mode mode;
    private final Map<OutputKind, String> redirects = new EnumMap<>(OutputKind.class);
    private final Map<String, String> envVars = new HashMap<>();
    private Expect expect = Expect.SUCCESS;
    int expectedExitCode = 0;

    /**
     * Create a task that will execute in the specified mode.
     * @param mode the mode
     */
    protected AbstractTask(ToolBox tb, Mode mode) {
        toolBox = tb;
        this.mode = mode;
    }

    /**
     * Sets the expected outcome of the task and calls {@code run()}.
     * @param expect the expected outcome
     * @return the result of calling {@code run()}
     */
    public Result run(Expect expect) {
        expect(expect, Integer.MIN_VALUE);
        return run();
    }

    /**
     * Sets the expected outcome of the task and calls {@code run()}.
     * @param expect the expected outcome
     * @param exitCode the expected exit code if the expected outcome
     *      is {@code FAIL}
     * @return the result of calling {@code run()}
     */
    public Result run(Expect expect, int exitCode) {
        expect(expect, exitCode);
        return run();
    }

    /**
     * Sets the expected outcome and expected exit code of the task.
     * The exit code will not be checked if the outcome is
     * {@code Expect.SUCCESS} or if the exit code is set to
     * {@code Integer.MIN_VALUE}.
     * @param expect the expected outcome
     * @param exitCode the expected exit code
     */
    protected void expect(Expect expect, int exitCode) {
        this.expect = expect;
        this.expectedExitCode = exitCode;
    }

    /**
     * Checks the exit code contained in a {@code Result} against the
     * expected outcome and exit value
     * @param result the result object
     * @return the result object
     * @throws TaskError if the exit code stored in the result object
     *      does not match the expected outcome and exit code.
     */
    protected Result checkExit(Result result) throws TaskError {
        switch (expect) {
            case SUCCESS:
                if (result.exitCode != 0) {
                    result.writeAll();
                    throw new TaskError("Task " + name() + " failed: rc=" + result.exitCode);
                }
                break;

            case FAIL:
                if (result.exitCode == 0) {
                    result.writeAll();
                    throw new TaskError("Task " + name() + " succeeded unexpectedly");
                }

                if (expectedExitCode != Integer.MIN_VALUE
                        && result.exitCode != expectedExitCode) {
                    result.writeAll();
                    throw new TaskError("Task " + name() + "failed with unexpected exit code "
                        + result.exitCode + ", expected " + expectedExitCode);
                }
                break;
        }
        return result;
    }

    /**
     * Sets an environment variable to be used by this task.
     * @param name the name of the environment variable
     * @param value the value for the environment variable
     * @return this task object
     * @throws IllegalStateException if the task mode is not {@code EXEC}
     */
    public T envVar(String name, String value) {
        if (mode != Mode.EXEC)
            throw new IllegalStateException();
        envVars.put(name, value);
        return (T) this;
    }

    /**
     * Redirects output from an output stream to a file.
     * @param outputKind the name of the stream to be redirected.
     * @param path the file
     * @return this task object
     * @throws IllegalStateException if the task mode is not {@code EXEC}
     */
    public T redirect(OutputKind outputKind, String path) {
        if (mode != Mode.EXEC)
            throw new IllegalStateException();
        redirects.put(outputKind, path);
        return (T) this;
    }

    /**
     * Returns a {@code ProcessBuilder} initialized with any
     * redirects and environment variables that have been set.
     * @return a {@code ProcessBuilder}
     */
    protected ProcessBuilder getProcessBuilder() {
        if (mode != Mode.EXEC)
            throw new IllegalStateException();
        ProcessBuilder pb = new ProcessBuilder();
        if (redirects.get(OutputKind.STDOUT) != null)
            pb.redirectOutput(new File(redirects.get(OutputKind.STDOUT)));
        if (redirects.get(OutputKind.STDERR) != null)
            pb.redirectError(new File(redirects.get(OutputKind.STDERR)));
        pb.environment().putAll(envVars);
        return pb;
    }

    /**
     * Collects the output from a process and saves it in a {@code Result}.
     * @param tb the {@code ToolBox} containing the task {@code t}
     * @param t the task initiating the process
     * @param p the process
     * @return a Result object containing the output from the process and its
     *      exit value.
     * @throws InterruptedException if the thread is interrupted
     */
    protected Result runProcess(ToolBox tb, Task t, Process p) throws InterruptedException {
        if (mode != Mode.EXEC)
            throw new IllegalStateException();
        ProcessOutput sysOut = new ProcessOutput(p.getInputStream()).start();
        ProcessOutput sysErr = new ProcessOutput(p.getErrorStream()).start();
        sysOut.waitUntilDone();
        sysErr.waitUntilDone();
        int rc = p.waitFor();
        Map<OutputKind, String> outputMap = new EnumMap<>(OutputKind.class);
        outputMap.put(OutputKind.STDOUT, sysOut.getOutput());
        outputMap.put(OutputKind.STDERR, sysErr.getOutput());
        return checkExit(new Result(toolBox, t, rc, outputMap));
    }

    /**
     * Thread-friendly class to read the output from a process until the stream
     * is exhausted.
     */
    static class ProcessOutput implements Runnable {
        ProcessOutput(InputStream from) {
            in = new BufferedReader(new InputStreamReader(from));
            out = new StringBuilder();
        }

        ProcessOutput start() {
            new Thread(this).start();
            return this;
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    out.append(line).append(lineSeparator);
                }
            } catch (IOException e) {
            }
            synchronized (this) {
                done = true;
                notifyAll();
            }
        }

        synchronized void waitUntilDone() throws InterruptedException {
            boolean interrupted = false;

            // poll interrupted flag, while waiting for copy to complete
            while (!(interrupted = Thread.interrupted()) && !done)
                wait(1000);

            if (interrupted)
                throw new InterruptedException();
        }

        String getOutput() {
            return out.toString();
        }

        private final BufferedReader in;
        private final StringBuilder out;
        private boolean done;
    }

    /**
     * Utility class to simplify the handling of temporarily setting a
     * new stream for System.out or System.err.
     */
    static class StreamOutput {
        // Functional interface to set a stream.
        // Expected use: System::setOut, System::setErr
        interface Initializer {
            void set(PrintStream s);
        }

        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private final PrintStream ps = new PrintStream(baos);
        private final PrintStream prev;
        private final Initializer init;

        StreamOutput(PrintStream s, Initializer init) {
            prev = s;
            init.set(ps);
            this.init = init;
        }

        /**
         * Closes the stream and returns the contents that were written to it.
         * @return the contents that were written to it.
         */
        String close() {
            init.set(prev);
            ps.close();
            return baos.toString();
        }
    }

    /**
     * Utility class to simplify the handling of creating an in-memory PrintWriter.
     */
    static class WriterOutput {
        private final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        /**
         * Closes the stream and returns the contents that were written to it.
         * @return the contents that were written to it.
         */
        String close() {
            pw.close();
            return sw.toString();
        }
    }
}

/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Executor {

    Executor() {
    }

    Executor setOutputConsumer(Consumer<Stream<String>> v) {
        outputConsumer = v;
        return this;
    }

    Executor saveOutput(boolean v) {
        saveOutput = v;
        return this;
    }

    Executor setWriteOutputToFile(boolean v) {
        writeOutputToFile = v;
        return this;
    }

    Executor setTimeout(long v) {
        timeout = v;
        if (timeout != INFINITE_TIMEOUT) {
            // Redirect output to file if timeout is requested, otherwise we will
            // reading until process ends and timeout will never be reached.
            setWriteOutputToFile(true);
        }
        return this;
    }

    Executor setProcessBuilder(ProcessBuilder v) {
        pb = v;
        return this;
    }

    Executor setCommandLine(String... cmdline) {
        return setProcessBuilder(new ProcessBuilder(cmdline));
    }

    Executor setQuiet(boolean v) {
        quietCommand = v;
        return this;
    }

    List<String> getOutput() {
        return output;
    }

    Executor executeExpectSuccess() throws IOException {
        int ret = execute();
        if (0 != ret) {
            throw new IOException(
                    String.format("Command %s exited with %d code",
                            createLogMessage(pb, false), ret));
        }
        return this;
    }

    int execute() throws IOException {
        output = null;

        boolean needProcessOutput = outputConsumer != null || Log.isVerbose() || saveOutput;
        Path outputFile = null;
        if (needProcessOutput) {
            pb.redirectErrorStream(true);
            if (writeOutputToFile) {
                outputFile = Files.createTempFile("jpackageOutputTempFile", ".tmp");
                pb.redirectOutput(outputFile.toFile());
            }
        } else {
            // We are not going to read process output, so need to notify
            // ProcessBuilder about this. Otherwise some processes might just
            // hang up (`ldconfig -p`).
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        }

        Log.verbose(String.format("Running %s", createLogMessage(pb, true)));
        Process p = pb.start();

        int code = 0;
        if (writeOutputToFile) {
            try {
                code = waitForProcess(p);
            } catch (InterruptedException ex) {
                Log.verbose(ex);
                throw new RuntimeException(ex);
            }
        }

        if (needProcessOutput) {
            final List<String> savedOutput;
            Supplier<Stream<String>> outputStream;

            if (writeOutputToFile) {
                output = savedOutput = Files.readAllLines(outputFile);
                Files.delete(outputFile);
                outputStream = () -> {
                    if (savedOutput != null) {
                        return savedOutput.stream();
                    }
                    return null;
                };
                if (outputConsumer != null) {
                    outputConsumer.accept(outputStream.get());
                }
            } else {
                try (var br = new BufferedReader(new InputStreamReader(
                        p.getInputStream()))) {

                    if ((outputConsumer != null || Log.isVerbose())
                            || saveOutput) {
                        savedOutput = br.lines().toList();
                    } else {
                        savedOutput = null;
                    }
                    output = savedOutput;

                    outputStream = () -> {
                        if (savedOutput != null) {
                            return savedOutput.stream();
                        }
                        return br.lines();
                    };
                    if (outputConsumer != null) {
                        outputConsumer.accept(outputStream.get());
                    }

                    if (savedOutput == null) {
                        // For some processes on Linux if the output stream
                        // of the process is opened but not consumed, the process
                        // would exit with code 141.
                        // It turned out that reading just a single line of process
                        // output fixes the problem, but let's process
                        // all of the output, just in case.
                        br.lines().forEach(x -> {});
                    }
                }
            }
        }

        try {
            if (!writeOutputToFile) {
                code = p.waitFor();
            }
            if (!quietCommand) {
                Log.verbose(pb.command(), getOutput(), code, IOUtils.getPID(p));
            }
            return code;
        } catch (InterruptedException ex) {
            Log.verbose(ex);
            throw new RuntimeException(ex);
        }
    }

    private int waitForProcess(Process p) throws InterruptedException {
        if (timeout == INFINITE_TIMEOUT) {
            return p.waitFor();
        } else {
            if (p.waitFor(timeout, TimeUnit.SECONDS)) {
                return p.exitValue();
            } else {
                Log.verbose(String.format("Command %s timeout after %d seconds",
                            createLogMessage(pb, false), timeout));
                p.destroy();
                return -1;
            }
        }
    }

    static Executor of(String... cmdline) {
        return new Executor().setCommandLine(cmdline);
    }

    static Executor of(ProcessBuilder pb) {
        return new Executor().setProcessBuilder(pb);
    }

    private static String createLogMessage(ProcessBuilder pb, boolean quiet) {
        StringBuilder sb = new StringBuilder();
        sb.append((quiet) ? pb.command().get(0) : pb.command());
        if (pb.directory() != null) {
            sb.append(String.format(" in %s", pb.directory().getAbsolutePath()));
        }
        return sb.toString();
    }

    public static final int INFINITE_TIMEOUT = -1;

    private ProcessBuilder pb;
    private boolean saveOutput;
    private boolean writeOutputToFile;
    private boolean quietCommand;
    private long timeout = INFINITE_TIMEOUT;
    private List<String> output;
    private Consumer<Stream<String>> outputConsumer;
}

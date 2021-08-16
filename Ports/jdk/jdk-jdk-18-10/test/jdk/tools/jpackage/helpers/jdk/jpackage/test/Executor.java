/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jpackage.test.Functional.ThrowingSupplier;

public final class Executor extends CommandArguments<Executor> {

    public static Executor of(String... cmdline) {
        return new Executor().setExecutable(cmdline[0]).addArguments(
                Arrays.copyOfRange(cmdline, 1, cmdline.length));
    }

    public Executor() {
        saveOutputType = new HashSet<>(Set.of(SaveOutputType.NONE));
        removePath = false;
    }

    public Executor setExecutable(String v) {
        return setExecutable(Path.of(v));
    }

    public Executor setExecutable(Path v) {
        executable = Objects.requireNonNull(v);
        toolProvider = null;
        return this;
    }

    public Executor setToolProvider(ToolProvider v) {
        toolProvider = Objects.requireNonNull(v);
        executable = null;
        return this;
    }

    public Executor setToolProvider(JavaTool v) {
        return setToolProvider(v.asToolProvider());
    }

    public Executor setDirectory(Path v) {
        directory = v;
        return this;
    }

    public Executor setExecutable(JavaTool v) {
        return setExecutable(v.getPath());
    }

    public Executor setRemovePath(boolean value) {
        removePath = value;
        return this;
    }

    public Executor setWindowsTmpDir(String tmp) {
        TKit.assertTrue(TKit.isWindows(),
                "setWindowsTmpDir is only valid on Windows platform");
        winTmpDir = tmp;
        return this;
    }

    /**
     * Configures this instance to save full output that command will produce.
     * This function is mutual exclusive with
     * saveFirstLineOfOutput() function.
     *
     * @return this
     */
    public Executor saveOutput() {
        saveOutputType.remove(SaveOutputType.FIRST_LINE);
        saveOutputType.add(SaveOutputType.FULL);
        return this;
    }

    /**
     * Configures how to save output that command will produce. If
     * <code>v</code> is <code>true</code>, the function call is equivalent to
     * <code>saveOutput()</code> call. If <code>v</code> is <code>false</code>,
     * the function will result in not preserving command output.
     *
     * @return this
     */
    public Executor saveOutput(boolean v) {
        if (v) {
            saveOutput();
        } else {
            saveOutputType.remove(SaveOutputType.FIRST_LINE);
            saveOutputType.remove(SaveOutputType.FULL);
        }
        return this;
    }

    /**
     * Configures this instance to save only the first line out output that
     * command will produce. This function is mutual exclusive with
     * saveOutput() function.
     *
     * @return this
     */
    public Executor saveFirstLineOfOutput() {
        saveOutputType.add(SaveOutputType.FIRST_LINE);
        saveOutputType.remove(SaveOutputType.FULL);
        return this;
    }

    /**
     * Configures this instance to dump all output that command will produce to
     * System.out and System.err. Can be used together with saveOutput() and
     * saveFirstLineOfOutput() to save command output and also copy it in the
     * default output streams.
     *
     * @return this
     */
    public Executor dumpOutput() {
        return dumpOutput(true);
    }

    public Executor dumpOutput(boolean v) {
        if (v) {
            saveOutputType.add(SaveOutputType.DUMP);
        } else {
            saveOutputType.remove(SaveOutputType.DUMP);
        }
        return this;
    }

    public class Result {

        Result(int exitCode) {
            this.exitCode = exitCode;
        }

        public String getFirstLineOfOutput() {
            return output.get(0);
        }

        public List<String> getOutput() {
            return output;
        }

        public String getPrintableCommandLine() {
            return Executor.this.getPrintableCommandLine();
        }

        public Result assertExitCodeIs(int expectedExitCode) {
            TKit.assertEquals(expectedExitCode, exitCode, String.format(
                    "Check command %s exited with %d code",
                    getPrintableCommandLine(), expectedExitCode));
            return this;
        }

        public Result assertExitCodeIsZero() {
            return assertExitCodeIs(0);
        }

        public int getExitCode() {
            return exitCode;
        }

        final int exitCode;
        private List<String> output;
    }

    public Result executeWithoutExitCodeCheck() {
        if (toolProvider != null && directory != null) {
            throw new IllegalArgumentException(
                    "Can't change directory when using tool provider");
        }

        return ThrowingSupplier.toSupplier(() -> {
            if (toolProvider != null) {
                return runToolProvider();
            }

            if (executable != null) {
                return runExecutable();
            }

            throw new IllegalStateException("No command to execute");
        }).get();
    }

    public Result execute(int expectedCode) {
        return executeWithoutExitCodeCheck().assertExitCodeIs(expectedCode);
    }

    public Result execute() {
        return execute(0);
    }

    public String executeAndGetFirstLineOfOutput() {
        return saveFirstLineOfOutput().execute().getFirstLineOfOutput();
    }

    public List<String> executeAndGetOutput() {
        return saveOutput().execute().getOutput();
    }

    /*
     * Repeates command "max" times and waits for "wait" seconds between each
     * execution until command returns expected error code.
     */
    public Result executeAndRepeatUntilExitCode(int expectedCode, int max, int wait) {
        Result result;
        int count = 0;

        do {
            result = executeWithoutExitCodeCheck();
            if (result.getExitCode() == expectedCode) {
                return result;
            }

            try {
                Thread.sleep(wait * 1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            count++;
        } while (count < max);

        return result.assertExitCodeIs(expectedCode);
    }

    public List<String> executeWithoutExitCodeCheckAndGetOutput() {
        return saveOutput().executeWithoutExitCodeCheck().getOutput();
    }

    private boolean withSavedOutput() {
        return saveOutputType.contains(SaveOutputType.FULL) || saveOutputType.contains(
                SaveOutputType.FIRST_LINE);
    }

    private Path executablePath() {
        if (directory == null
                || executable.isAbsolute()
                || !Set.of(".", "..").contains(executable.getName(0).toString())) {
            return executable;
        }

        // If relative path to executable is used it seems to be broken when
        // ProcessBuilder changes the directory. On Windows it changes the
        // directory first and on Linux it looks up for executable before
        // changing the directory. So to stay of safe side, use absolute path
        // to executable.
        return executable.toAbsolutePath();
    }

    private Result runExecutable() throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(executablePath().toString());
        command.addAll(args);
        ProcessBuilder builder = new ProcessBuilder(command);
        if (winTmpDir != null) {
            builder.environment().put("TMP", winTmpDir);
        }
        StringBuilder sb = new StringBuilder(getPrintableCommandLine());
        if (withSavedOutput()) {
            builder.redirectErrorStream(true);
            sb.append("; save output");
        } else if (saveOutputType.contains(SaveOutputType.DUMP)) {
            builder.inheritIO();
            sb.append("; inherit I/O");
        } else {
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            sb.append("; discard I/O");
        }
        if (directory != null) {
            builder.directory(directory.toFile());
            sb.append(String.format("; in directory [%s]", directory));
        }
        if (removePath) {
            // run this with cleared Path in Environment
            TKit.trace("Clearing PATH in environment");
            builder.environment().remove("PATH");
        }

        trace("Execute " + sb.toString() + "...");
        Process process = builder.start();

        List<String> outputLines = null;
        if (withSavedOutput()) {
            try (BufferedReader outReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                if (saveOutputType.contains(SaveOutputType.DUMP)
                        || saveOutputType.contains(SaveOutputType.FULL)) {
                    outputLines = outReader.lines().collect(Collectors.toList());
                } else {
                    outputLines = Arrays.asList(
                            outReader.lines().findFirst().orElse(null));
                }
            } finally {
                if (saveOutputType.contains(SaveOutputType.DUMP) && outputLines != null) {
                    outputLines.stream().forEach(System.out::println);
                    if (saveOutputType.contains(SaveOutputType.FIRST_LINE)) {
                        // Pick the first line of saved output if there is one
                        for (String line: outputLines) {
                            outputLines = List.of(line);
                            break;
                        }
                    }
                }
            }
        }

        Result reply = new Result(process.waitFor());
        trace("Done. Exit code: " + reply.exitCode);

        if (outputLines != null) {
            reply.output = Collections.unmodifiableList(outputLines);
        }
        return reply;
    }

    private Result runToolProvider(PrintStream out, PrintStream err) {
        trace("Execute " + getPrintableCommandLine() + "...");
        Result reply = new Result(toolProvider.run(out, err, args.toArray(
                String[]::new)));
        trace("Done. Exit code: " + reply.exitCode);
        return reply;
    }


    private Result runToolProvider() throws IOException {
        if (!withSavedOutput()) {
            if (saveOutputType.contains(SaveOutputType.DUMP)) {
                return runToolProvider(System.out, System.err);
            }

            PrintStream nullPrintStream = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    // Nop
                }
            });
            return runToolProvider(nullPrintStream, nullPrintStream);
        }

        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(buf)) {
            Result reply = runToolProvider(ps, ps);
            ps.flush();
            try (BufferedReader bufReader = new BufferedReader(new StringReader(
                    buf.toString()))) {
                if (saveOutputType.contains(SaveOutputType.FIRST_LINE)) {
                    String firstLine = bufReader.lines().findFirst().orElse(null);
                    if (firstLine != null) {
                        reply.output = List.of(firstLine);
                    }
                } else if (saveOutputType.contains(SaveOutputType.FULL)) {
                    reply.output = bufReader.lines().collect(
                            Collectors.toUnmodifiableList());
                }

                if (saveOutputType.contains(SaveOutputType.DUMP)) {
                    Stream<String> lines;
                    if (saveOutputType.contains(SaveOutputType.FULL)) {
                        lines = reply.output.stream();
                    } else {
                        lines = bufReader.lines();
                    }
                    lines.forEach(System.out::println);
                }
            }
            return reply;
        }
    }

    public String getPrintableCommandLine() {
        final String exec;
        String format = "[%s](%d)";
        if (toolProvider == null && executable == null) {
            exec = "<null>";
        } else if (toolProvider != null) {
            format = "tool provider " + format;
            exec = toolProvider.name();
        } else {
            exec = executablePath().toString();
        }

        return String.format(format, printCommandLine(exec, args),
                args.size() + 1);
    }

    private static String printCommandLine(String executable, List<String> args) {
        // Want command line printed in a way it can be easily copy/pasted
        // to be executed manally
        Pattern regex = Pattern.compile("\\s");
        return Stream.concat(Stream.of(executable), args.stream()).map(
                v -> (v.isEmpty() || regex.matcher(v).find()) ? "\"" + v + "\"" : v).collect(
                        Collectors.joining(" "));
    }

    private static void trace(String msg) {
        TKit.trace(String.format("exec: %s", msg));
    }

    private ToolProvider toolProvider;
    private Path executable;
    private Set<SaveOutputType> saveOutputType;
    private Path directory;
    private boolean removePath;
    private String winTmpDir = null;

    private static enum SaveOutputType {
        NONE, FULL, FIRST_LINE, DUMP
    };
}

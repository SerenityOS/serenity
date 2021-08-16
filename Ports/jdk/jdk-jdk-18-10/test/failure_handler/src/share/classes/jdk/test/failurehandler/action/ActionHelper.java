/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.failurehandler.action;

import jdk.test.failurehandler.value.InvalidValueException;
import jdk.test.failurehandler.value.Value;
import jdk.test.failurehandler.value.ValueHandler;
import jdk.test.failurehandler.HtmlSection;
import jdk.test.failurehandler.Stopwatch;
import jdk.test.failurehandler.Utils;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ActionHelper {
    private final Path workDir;
    @Value(name = "execSuffix")
    private String executableSuffix = "";
    private Path[] paths;

    private final PatternAction getChildren;

    public ActionHelper(Path workDir, String prefix, Properties properties,
                        Path... jdks) throws InvalidValueException {
        this.workDir = workDir.toAbsolutePath();
        getChildren = new PatternAction(null,
                Utils.prependPrefix(prefix, "getChildren"), properties);
        ValueHandler.apply(this, properties, prefix);
        String[] pathStrings = System.getenv("PATH").split(File.pathSeparator);
        paths = new Path[pathStrings.length];
        for (int i = 0; i < paths.length; ++i) {
            paths[i] = Paths.get(pathStrings[i]);
        }
        addJdks(jdks);
    }

    public List<Long> getChildren(HtmlSection section, long pid) {
        String pidStr = "" + pid;
        ProcessBuilder pb = getChildren.prepareProcess(section, this, pidStr);
        PrintWriter log = getChildren.getSection(section).getWriter();
        CharArrayWriter writer = new CharArrayWriter();
        ExitCode code = run(log, writer, pb, getChildren.getParameters());
        Reader output = new CharArrayReader(writer.toCharArray());

        if (!ExitCode.OK.equals(code)) {
            log.println("WARNING: get children pids action failed");
            try {
                Utils.copyStream(output, log);
            } catch (IOException e) {
                e.printStackTrace(log);
            }
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>();
        try {
            try (BufferedReader reader = new BufferedReader(output)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String value = line.trim();
                    if (value.isEmpty()) {
                        // ignore empty lines
                        continue;
                    }
                    try {
                        result.add(Long.valueOf(value));
                    } catch (NumberFormatException e) {
                        log.printf("WARNING: can't parse child pid %s : %s%n",
                                line, e.getMessage());
                        e.printStackTrace(log);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(log);
        }
        return result;
    }

    public ProcessBuilder prepareProcess(PrintWriter log, String app,
                                         String... args) {
        File appBin = findApp(app);
        if (appBin == null) {
            log.printf("ERROR: can't find %s in %s.%n",
                    app, Arrays.toString(paths));
            return null;
        }
        List<String> command = new ArrayList<>(args.length + 1);
        command.add(appBin.toString());
        Collections.addAll(command, args);
        return new ProcessBuilder()
                .command(command)
                .directory(workDir.toFile());
    }

    public File findApp(String app) {
        String name = app + executableSuffix;
        for (Path pathElem : paths) {
            File result = pathElem.resolve(name).toFile();
            if (result.exists()) {
                return result;
            }
        }
        return null;
    }

    private void addJdks(Path[] jdkPaths) {
        if (jdkPaths != null && jdkPaths.length != 0) {
            Path[] result = new Path[jdkPaths.length + paths.length];
            for (int i = 0; i < jdkPaths.length; ++i) {
                result[i] = jdkPaths[i].resolve("bin");
            }
            System.arraycopy(paths, 0, result, jdkPaths.length, paths.length);
            paths = result;
        }
    }

    private ExitCode run(PrintWriter log, Writer out, ProcessBuilder pb,
                    ActionParameters params) {
        char[] lineChars = new char[40];
        Arrays.fill(lineChars, '-');
        String line = new String(lineChars);
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        log.printf("%s%n[%tF %<tT] %s timeout=%s%n%1$s%n", line, new Date(), pb.command(), params.timeout);

        Process process;
        KillerTask killer;

        ExitCode result = ExitCode.NEVER_STARTED;

        try {
            process = pb.start();
            killer = new KillerTask(process);
            killer.schedule(params.timeout);
            Utils.copyStream(new InputStreamReader(process.getInputStream()),
                    out);
            try {
                result = new ExitCode(process.waitFor());
            } catch (InterruptedException e) {
                log.println("WARNING: interrupted when waiting for the tool:%n");
                e.printStackTrace(log);
            } finally {
                killer.cancel();
            }
            if (killer.hasTimedOut()) {
                log.printf(
                        "WARNING: tool timed out: killed process after %d ms%n",
                        params.timeout);
                result = ExitCode.TIMED_OUT;
            }
        } catch (IOException e) {
            log.printf("WARNING: caught IOException while running tool%n");
            e.printStackTrace(log);
            result = ExitCode.LAUNCH_ERROR;
        }

        stopwatch.stop();
        log.printf("%s%n[%tF %<tT] exit code: %d time: %d ms%n%1$s%n",
                line, new Date(), result.value,
                TimeUnit.NANOSECONDS.toMillis(stopwatch.getElapsedTimeNs()));
        return result;
    }

    public void runPatternAction(SimpleAction action, HtmlSection section) {
        if (action != null) {
            HtmlSection subSection = action.getSection(section);
            PrintWriter log = subSection.getWriter();
            ProcessBuilder pb = action.prepareProcess(log, this);
            exec(subSection, pb, action.getParameters());
        }
    }

    public void runPatternAction(PatternAction action, HtmlSection section,
                                 String value) {
        if (action != null) {
            ProcessBuilder pb = action.prepareProcess(section, this, value);
            HtmlSection subSection = action.getSection(section);
            exec(subSection, pb, action.getParameters());
        }
    }

    public boolean isJava(long pid, PrintWriter log) {
        ProcessBuilder pb = prepareProcess(log, "jps", "-q");
        if (pb == null) {
            return false;
        }
        pb.redirectErrorStream(true);
        boolean result = false;
        String pidStr = "" + pid;
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null){
                    if (pidStr.equals(line)) {
                        result = true;
                    }
                }
            }
            process.waitFor();
        } catch (IOException e) {
            log.printf("WARNING: can't run jps : %s%n", e.getMessage());
            e.printStackTrace(log);
        } catch (InterruptedException e) {
            log.printf("WARNING: interrupted%n");
            e.printStackTrace(log);
        }
        return result;
    }

    private static class KillerTask extends TimerTask {
        private static final Timer WATCHDOG = new Timer("WATCHDOG", true);
        private final Process process;
        private boolean timedOut;

        public KillerTask(Process process) {
            this.process = process;
        }

        public void run() {
            try {
                process.exitValue();
            } catch (IllegalThreadStateException e) {
                process.destroyForcibly();
                timedOut = true;
            }
        }

        public boolean hasTimedOut() {
            return timedOut;
        }

        public void schedule(long timeout) {
            if (timeout > 0) {
                WATCHDOG.schedule(this, timeout);
            }
        }
    }

    private void exec(HtmlSection section, ProcessBuilder process,
                      ActionParameters params) {
        if (process == null) {
            return;
        }
        PrintWriter sectionWriter = section.getWriter();
        if (params.repeat > 1) {
            for (int i = 0, n = params.repeat; i < n; ++i) {
                HtmlSection iteration = section.createChildren(
                        String.format("iteration_%d", i));
                PrintWriter writer = iteration.getWriter();
                ExitCode exitCode = run(writer, writer, process, params);
                if (params.stopOnError && !ExitCode.OK.equals(exitCode)) {
                    sectionWriter.printf(
                            "ERROR: non zero exit code[%d] -- break.",
                            exitCode.value);
                    break;
                }
                // sleep, if this is not the last iteration
                if (i < n - 1) {
                    try {
                        Thread.sleep(params.pause);
                    } catch (InterruptedException e) {
                        sectionWriter.printf(
                                "WARNING: interrupted while sleeping between invocations");
                        e.printStackTrace(sectionWriter);
                    }
                }
            }
        } else {
            run(section.getWriter(), section.getWriter(), process, params);
        }
    }

    /**
     * Special values for prepareProcess exit code.
     *
     * <p>Can we clash with normal codes?
     * On Linux, only [0..255] are returned.
     * On Windows, prepareProcess exit codes are stored in unsigned int.
     * On MacOSX no limits (except it should fit C int type)
     * are defined in the exit() man pages.
     */
    private static class ExitCode {
        /** Process exits gracefully */
        public static final ExitCode OK = new ExitCode(0);
        /** Error launching prepareProcess */
        public static final ExitCode LAUNCH_ERROR = new ExitCode(-1);
        /** Application prepareProcess has been killed by watchdog due to timeout */
        public static final ExitCode TIMED_OUT = new ExitCode(-2);
        /** Application prepareProcess has never been started due to program logic */
        public static final ExitCode NEVER_STARTED = new ExitCode(-3);

        public final int value;

        private ExitCode(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ExitCode exitCode = (ExitCode) o;
            return value == exitCode.value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

}

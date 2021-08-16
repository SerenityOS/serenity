/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.hotspot.tools.ctw;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Runs CompileTheWorld for exact one target. If an error occurs during
 * compilation of class N, this driver class saves error information and
 * restarts CTW from class N + 1. All saved errors are reported at the end.
 * <pre>
 * Usage: <target to compile>
 * </pre>
 */
public class CtwRunner {
    private static final Predicate<String> IS_CLASS_LINE = Pattern.compile(
            "^\\[\\d+\\]\\s*\\S+\\s*$").asPredicate();

    private static final String USAGE = "Usage: CtwRunner <artifact to compile> [start[%] stop[%]]";

    public static void main(String[] args) throws Exception {
        CtwRunner runner;
        switch (args.length) {
            case 1: runner = new CtwRunner(args[0]); break;
            case 3: runner = new CtwRunner(args[0], args[1], args[2]); break;
            default: throw new Error(USAGE);
        }

        runner.run();
    }

    private final List<Throwable> errors;
    private final String target;
    private final Path targetPath;
    private final String targetName;

    private final int start, stop;
    private final boolean isStartStopPercentage;

    private CtwRunner(String target, String start, String stop) {
        if (target.startsWith("modules")) {
            targetPath = Paths
                    .get(Utils.TEST_JDK)
                    .resolve("lib")
                    .resolve("modules");
            if (target.equals("modules")){
                target = targetPath.toString();
            }
            targetName = target.replace(':', '_')
                               .replace('.', '_')
                               .replace(',', '_');
        } else {
            targetPath = Paths.get(target).toAbsolutePath();
            targetName = targetPath.getFileName().toString();
        }
        this.target = target;
        errors = new ArrayList<>();

        if (start.endsWith("%") && stop.endsWith("%")) {
            int startPercentage = Integer.parseInt(start.substring(0, start.length() - 1));;
            int stopPercentage = Integer.parseInt(stop.substring(0, stop.length() - 1));
            if (startPercentage < 0 || startPercentage > 100 ||
                stopPercentage < 0 || stopPercentage > 100) {
                throw new Error(USAGE);
            }
            this.start = startPercentage;
            this.stop = stopPercentage;
            this.isStartStopPercentage = true;
        } else if (!start.endsWith("%") && !stop.endsWith("%")) {
            this.start = Integer.parseInt(start);
            this.stop = Integer.parseInt(stop);
            this.isStartStopPercentage = false;
        } else {
            throw new Error(USAGE);
        }
    }

    private CtwRunner(String target) {
        this(target, "0%", "100%");
    }

    private void run() {
        startCtwforAllClasses();
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("There were ")
              .append(errors.size())
              .append(" errors:[");
            System.err.println(sb.toString());
            for (Throwable e : errors) {
                sb.append("{")
                  .append(e.getMessage())
                  .append("}");
                e.printStackTrace(System.err);
                System.err.println();
            }
            sb.append("]");
            throw new AssertionError(sb.toString());
        }
    }

    private long start(long totalClassCount) {
        if (isStartStopPercentage) {
            return totalClassCount * start / 100;
        } else if (start > totalClassCount) {
            System.err.println("WARNING: start [" + start + "] > totalClassCount [" + totalClassCount + "]");
            return totalClassCount;
        } else {
            return start;
        }
    }

    private long stop(long totalClassCount) {
        if (isStartStopPercentage) {
            return totalClassCount * stop / 100;
        } else if (stop > totalClassCount) {
            System.err.println("WARNING: stop [" + start + "] > totalClassCount [" + totalClassCount + "]");
            return totalClassCount;
        } else {
            return stop;
        }
    }

    private void startCtwforAllClasses() {
        long totalClassCount = classCount();

        long classStart = start(totalClassCount);
        long classStop = stop(totalClassCount);

        long classCount = classStop - classStart;
        Asserts.assertGreaterThan(classCount, 0L,
                target + "(at " + targetPath + ") does not have any classes");

        System.out.printf("Compiling %d classes (of %d total classes) starting at %d and ending at %d\n",
                          classCount, totalClassCount, classStart, classStop);

        boolean done = false;
        while (!done) {
            String[] cmd = cmd(classStart, classStop);
            try {
                ProcessBuilder pb = ProcessTools.createTestJvm(cmd);
                String commandLine = pb.command()
                        .stream()
                        .collect(Collectors.joining(" "));
                String phase = phaseName(classStart);
                Path out = Paths.get(".", phase + ".out");
                Path err = Paths.get(".", phase + ".err");
                System.out.printf("%s %dms START : [%s]%n" +
                        "cout/cerr are redirected to %s%n",
                        phase, TimeUnit.NANOSECONDS.toMillis(System.nanoTime()),
                        commandLine, phase);
                int exitCode = pb.redirectOutput(out.toFile())
                                 .redirectError(err.toFile())
                                 .start()
                                 .waitFor();
                System.out.printf("%s %dms END : exit code = %d%n",
                        phase, TimeUnit.NANOSECONDS.toMillis(System.nanoTime()),
                        exitCode);
                Pair<String, Long> lastClass = getLastClass(out);
                if (exitCode == 0) {
                    long lastIndex = lastClass == null ? -1 : lastClass.second;
                    if (lastIndex != classStop) {
                        errors.add(new Error(phase + ": Unexpected zero exit code"
                                + "before finishing all compilations."
                                + " lastClass[" + lastIndex
                                + "] != classStop[" + classStop + "]"));
                    } else {
                        System.out.println("Executed CTW for all " + classCount
                                + " classes in " + target + "(at " + targetPath + ")");
                    }
                    done = true;
                } else {
                    if (lastClass == null) {
                        errors.add(new Error(phase + ": failed during preload"
                                + " with classStart = " + classStart));
                        // skip one class
                        ++classStart;
                    } else {
                        errors.add(new Error(phase + ": failed during"
                                + " compilation of class #" + lastClass.second
                                + " : " + lastClass.first));
                        // continue with the next class
                        classStart = lastClass.second + 1;
                    }
                }
            } catch (Exception e) {
                throw new Error("failed to run from " + classStart, e);
            }
        }
    }

    private long classCount() {
        List<PathHandler> phs = PathHandler.create(target);
        long result = phs.stream()
                         .mapToLong(PathHandler::classCount)
                         .sum();
        phs.forEach(PathHandler::close);
        return result;
    }

    private Pair<String, Long> getLastClass(Path errFile) {
        try (BufferedReader reader = Files.newBufferedReader(errFile)) {
            String line = reader.lines()
                    .filter(IS_CLASS_LINE)
                    .reduce((a, b) -> b)
                    .orElse(null);
            if (line != null) {
                int open = line.indexOf('[') + 1;
                int close = line.indexOf(']');
                long index = Long.parseLong(line.substring(open, close));
                String name = line.substring(close + 1).trim().replace('.', '/');
                return new Pair<>(name, index);
            }
        } catch (IOException ioe) {
            throw new Error("can not read " + errFile + " : "
                    + ioe.getMessage(), ioe);
        }
        return null;
    }

    private String[] cmd(long classStart, long classStop) {
        String phase = phaseName(classStart);
        Path file = Paths.get(phase + ".cmd");
        var rng = Utils.getRandomInstance();
        try {
            Files.write(file, List.of(
                    "-Xbatch",
                    "-XX:-UseCounterDecay",
                    "-XX:-ShowMessageBoxOnError",
                    "-XX:+UnlockDiagnosticVMOptions",
                    // redirect VM output to cerr so it won't collide w/ ctw output
                    "-XX:+DisplayVMOutputToStderr",
                    // define phase start
                    "-DCompileTheWorldStartAt=" + classStart,
                    "-DCompileTheWorldStopAt=" + classStop,
                    // CTW library uses WhiteBox API
                    "-XX:+WhiteBoxAPI", "-Xbootclasspath/a:.",
                    // export jdk.internal packages used by CTW library
                    "--add-exports", "java.base/jdk.internal.jimage=ALL-UNNAMED",
                    "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED",
                    "--add-exports", "java.base/jdk.internal.reflect=ALL-UNNAMED",
                    "--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED",
                    // enable diagnostic logging
                    "-XX:+LogCompilation",
                    // use phase specific log, hs_err and ciReplay files
                    String.format("-XX:LogFile=hotspot_%s_%%p.log", phase),
                    String.format("-XX:ErrorFile=hs_err_%s_%%p.log", phase),
                    String.format("-XX:ReplayDataFile=replay_%s_%%p.log", phase),
                    // MethodHandle MUST NOT be compiled
                    "-XX:CompileCommand=exclude,java/lang/invoke/MethodHandle.*",
                    // Stress* are c2-specific stress flags, so IgnoreUnrecognizedVMOptions is needed
                    "-XX:+IgnoreUnrecognizedVMOptions",
                    "-XX:+StressLCM",
                    "-XX:+StressGCM",
                    "-XX:+StressIGVN",
                    "-XX:+StressCCP",
                    // StressSeed is uint
                    "-XX:StressSeed=" + Math.abs(rng.nextLong()),
                    // CTW entry point
                    CompileTheWorld.class.getName(),
                    target));
        } catch (IOException e) {
            throw new Error("can't create " + file, e);
        }
        return new String[]{ "@" + file.toAbsolutePath() };
    }

    private String phaseName(long classStart) {
        return String.format("%s_%d", targetName, classStart);
    }

}

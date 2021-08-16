/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8234808
 *
 * @library /test/lib
 * @run main/othervm JdbOptions
 */

import jdk.test.lib.Platform;
import lib.jdb.Jdb;
import lib.jdb.JdbCommand;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

class JbdOptionsTarg {
    static final String OK_MSG = "JbdOptionsTarg: OK";

    static String argString(String s) {
        return "arg >" + s + "<";
    }

    static String propString(String name, String value) {
        return "prop[" + name + "] = >" + value + "<";
    }

    /**
     * 1st argument is a filename to redirect application output,
     * the rest are names of the properties to dump.
     */
    public static void main(String[] args) throws IOException {
        String outFile = args[0];
        try (PrintStream out = new PrintStream(outFile, StandardCharsets.UTF_8)) {
            out.println(OK_MSG);
            // print all args
            List<String> vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String s : vmArgs) {
                out.println(argString(s));
            }
            // print requested sys.props (skip 1st arg which is output filename)
            for (int i=1; i < args.length; i++) {
                String p = args[i];
                out.println(propString(p, System.getProperty(p)));
            }
        }
    }
}

public class JdbOptions {
    private static final String outFilename = UUID.randomUUID().toString() + ".out";
    private static final Path outPath = Paths.get(outFilename);
    private static final String targ = JbdOptionsTarg.class.getName();

    public static void main(String[] args) throws Exception {
        // the simplest case
        test("-connect",
                "com.sun.jdi.CommandLineLaunch:vmexec=java,options=-client -XX:+PrintVMOptions"
                + ",main=" + targ + " " + outFilename)
            .expectedArg("-XX:+PrintVMOptions");

        // pass property through 'options'
        test("-connect",
                "com.sun.jdi.CommandLineLaunch:vmexec=java,options='-Dboo=foo'"
                + ",main=" + targ + " " + outFilename + " boo")
            .expectedProp("boo", "foo");

        // property with spaces
        test("-connect",
                "com.sun.jdi.CommandLineLaunch:vmexec=java,options=\"-Dboo=foo 2\""
                + ",main=" + targ + " " + outFilename + " boo")
            .expectedProp("boo", "foo 2");

        // property with spaces (with single quotes)
        test("-connect",
                "com.sun.jdi.CommandLineLaunch:vmexec=java,options='-Dboo=foo 2'"
                + ",main=" + targ + " " + outFilename + " boo")
                .expectedProp("boo", "foo 2");

        // properties with spaces (with single quotes)
        test("-connect",
                "com.sun.jdi.CommandLineLaunch:vmexec=java,options=-Dboo=foo '-Dboo2=foo 2'"
                + ",main=" + targ + " " + outFilename + " boo boo2")
                .expectedProp("boo", "foo")
                .expectedProp("boo2", "foo 2");

        // 'options' contains commas - values are quoted (double quotes)
        test("-connect",
                "com.sun.jdi.CommandLineLaunch:vmexec=java,options=\"-client\" \"-XX:+PrintVMOptions\""
                + " -XX:+IgnoreUnrecognizedVMOptions"
                + " \"-XX:StartFlightRecording:dumponexit=true,maxsize=500M\" \"-XX:FlightRecorderOptions:repository=jfrrep\""
                + ",main=" + targ + " " + outFilename)
            .expectedArg("-XX:StartFlightRecording:dumponexit=true,maxsize=500M")
            .expectedArg("-XX:FlightRecorderOptions:repository=jfrrep");

        // 'options' contains commas - values are quoted (single quotes)
        test("-connect",
                "com.sun.jdi.CommandLineLaunch:vmexec=java,options='-client' '-XX:+PrintVMOptions'"
                        + " -XX:+IgnoreUnrecognizedVMOptions"
                        + " '-XX:StartFlightRecording:dumponexit=true,maxsize=500M' '-XX:FlightRecorderOptions:repository=jfrrep'"
                        + ",main=" + targ + " " + outFilename)
            .expectedArg("-XX:StartFlightRecording:dumponexit=true,maxsize=500M")
            .expectedArg("-XX:FlightRecorderOptions:repository=jfrrep");

        // java options are specified in 2 ways, with and without spaces
        // options are quoted by using single and double quotes.
        test("-Dprop1=val1",
                "-Dprop2=val 2",
                "-connect",
                "com.sun.jdi.CommandLineLaunch:vmexec=java,options=-Dprop3=val3 '-Dprop4=val 4'"
                        + " -XX:+IgnoreUnrecognizedVMOptions"
                        + " \"-XX:StartFlightRecording:dumponexit=true,maxsize=500M\""
                        + " '-XX:FlightRecorderOptions:repository=jfrrep'"
                        + ",main=" + targ + " " + outFilename + " prop1 prop2 prop3 prop4")
                .expectedProp("prop1", "val1")
                .expectedProp("prop2", "val 2")
                .expectedProp("prop3", "val3")
                .expectedProp("prop4", "val 4")
                .expectedArg("-XX:StartFlightRecording:dumponexit=true,maxsize=500M")
                .expectedArg("-XX:FlightRecorderOptions:repository=jfrrep");

    }

    private static class TestResult {
        OutputAnalyzer out;
        TestResult(OutputAnalyzer output) {
            out = output;
        }
        TestResult expectedArg(String s) {
            out.shouldContain(JbdOptionsTarg.argString(s));
            return this;
        }
        TestResult expectedProp(String name, String value) {
            out.shouldContain(JbdOptionsTarg.propString(name, value));
            return this;
        }
    }

    private static TestResult test(String... args) throws Exception {
        System.out.println();
        System.out.println("...testcase...");
        if (Platform.isWindows()) {
            // on Windows we need to escape quotes
            args = Arrays.stream(args)
                    .map(s -> s.replace("\"", "\\\""))
                    .toArray(String[]::new);
        }

        try (Jdb jdb = new Jdb(args)) {
            jdb.waitForSimplePrompt(1024, true); // 1024 lines should be enough
            jdb.command(JdbCommand.run().allowExit());
        }
        String output = Files.readAllLines(outPath, StandardCharsets.UTF_8).stream()
                .collect(Collectors.joining(System.getProperty("line.separator")));
        Files.deleteIfExists(outPath);
        System.out.println("Debuggee output: [");
        System.out.println(output);
        System.out.println("]");
        OutputAnalyzer out = new OutputAnalyzer(output);
        return new TestResult(out);
    }
}

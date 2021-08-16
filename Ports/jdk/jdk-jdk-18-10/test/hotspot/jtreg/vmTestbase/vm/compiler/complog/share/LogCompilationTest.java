/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.complog.share;

import jdk.test.lib.Utils;
import nsk.share.TestBug;
import nsk.share.TestFailure;
import nsk.share.log.Log;
import nsk.share.log.LogSupport;
import vm.share.options.Option;
import vm.share.options.OptionSupport;
import vm.share.process.ProcessExecutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test executor for all tests that require compilation log analysis.
 * Next options should be passed to LogCompilationTest:
 * <b>-testClass</b> test to be executed;
 * <b>-testedJava</b> path to testing java binary;
 * <b>-parserClass</b> parser that will be used for compilation log analysis;
 * <b>-timeout</b> timeout in secoonds.
 */

public class LogCompilationTest extends OptionSupport implements Runnable {

    @Option(name="testClass", description="Test to be executed.")
    protected String testClass;

    @Option(name="parserClass", description="Parser for compilation log.")
    protected String parserClass;

    @Option(name="timeout", description="Timeout value in seconds.", default_value="1800")
    protected int timeout;

    @Option(name="testedJava", description="Java binary to be tested.")
    protected String testedJava;

    @Option(name="parserOptions", description="Options that will be passed to compilation log parser.", default_value="")
    protected String parserOptions;

    protected Log log = new LogSupport(System.out);
    protected Log testLog;

    public static final String compilationLog = "hotspot.log";

    public static void main(String[] args) {
        LogCompilationTest.setupAndRun(new LogCompilationTest(), args);
    }

    public void run() {
        execute();
        parse();
    }

    private String[] getJVMOptions() {
        List<String> options = new ArrayList<>();
        Collections.addAll(options, Utils.getTestJavaOpts());
        options.add("-XX:+UnlockDiagnosticVMOptions");
        options.add("-XX:+LogCompilation");
        options.add("-XX:LogFile=" + compilationLog);
        return options.toArray(new String[0]);
    }

    private LogCompilationParser getParser() {
        try {
            Class<?> parser = Class.forName(parserClass);
            Constructor<?> ctor = parser.getConstructor();
            return (LogCompilationParser) ctor.newInstance();
        } catch (Throwable e) {
            throw new TestBug("Parser could not be instantiated.", e);
        }
    }

    private void execute() {
        String[] options = getJVMOptions();
        ProcessExecutor executor = new ProcessExecutor();
        try {
            testLog = new LogSupport(new PrintStream(new FileOutputStream("test.log")));
        } catch (FileNotFoundException e) {
            throw new TestFailure("Can't create test log file.", e);
        }

        executor.logStdOutErr("Test>>", testLog);
        executor.addArg(testedJava);
        executor.addArgs(options);
        executor.addArg(testClass);
        executor.start();
        int exitCode = executor.waitFor(timeout * 1000);

        if (exitCode != 0) {
            if (new File("hs_err_pid" + executor.getPid() + ".log").exists()) {
                throw new TestFailure("Test crashed. Exit code: " + exitCode);
            } else {
                throw new TestFailure("Test exited with non-zero code: " + exitCode);
            }
        }
    }

    private void parse() {
        File hotspotLog = new File(compilationLog);
        LogCompilationParser parser = getParser();
        parser.setOptions(parserOptions);
        parser.setLog(log);
        try {
            parser.parse(hotspotLog);
        } catch (Throwable e) {
            throw new TestFailure("Error occurred during compilation log parsing.",e);
        }
    }

}

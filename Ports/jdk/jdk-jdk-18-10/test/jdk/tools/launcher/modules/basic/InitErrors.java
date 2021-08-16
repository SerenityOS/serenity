/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @library /test/lib
 * @build InitErrors
 * @run testng InitErrors
 * @summary Basic test to ensure that module system initialization errors
 *          go the right stream and with the right level of verbosity
 */


import java.util.Arrays;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class InitErrors {

    // the option to cause module initialization to fail
    private static final String ADD_UNKNOWN_MODULE = "--add-modules=XXX";

    // the expected error message
    private static final String UNKNOWN_MODULE_NOT_FOUND= "Module XXX not found";

    // output expected in the stack trace when using -Xlog:init=debug
    private static final String STACK_FRAME = "java.base/java.lang.System.initPhase2";


    /**
     * Default behavior, send error message to stdout
     */
    @Test
    public void testDefaultOutput() throws Exception {
        expectFail(showVersion(ADD_UNKNOWN_MODULE)
                .stdoutShouldContain(UNKNOWN_MODULE_NOT_FOUND)
                .stdoutShouldNotContain(STACK_FRAME)
                .stderrShouldNotContain(UNKNOWN_MODULE_NOT_FOUND)
                .stderrShouldNotContain(STACK_FRAME));
    }

    /**
     * -XX:+DisplayVMOutputToStderr should send error message to stderr
     */
    @Test
    public void testOutputToStderr() throws Exception {
        expectFail(showVersion(ADD_UNKNOWN_MODULE, "-XX:+DisplayVMOutputToStderr")
                .stdoutShouldNotContain(UNKNOWN_MODULE_NOT_FOUND)
                .stdoutShouldNotContain(STACK_FRAME)
                .stderrShouldContain(UNKNOWN_MODULE_NOT_FOUND)
                .stderrShouldNotContain(STACK_FRAME));
    }

    /**
     * -Xlog:init=debug should print stack trace to stdout
     */
    @Test
    public void testStackTrace() throws Exception {
        expectFail(showVersion(ADD_UNKNOWN_MODULE, "-Xlog:init=debug")
                .stdoutShouldContain(UNKNOWN_MODULE_NOT_FOUND)
                .stdoutShouldContain(STACK_FRAME)
                .stderrShouldNotContain(UNKNOWN_MODULE_NOT_FOUND)
                .stderrShouldNotContain(STACK_FRAME));
    }

    /**
     * -Xlog:init=debug -XX:+DisplayVMOutputToStderr should print stack trace
     * to stderr
     */
    @Test
    public void testStackTraceToStderr() throws Exception {
        expectFail(showVersion(ADD_UNKNOWN_MODULE,
                               "-Xlog:init=debug",
                               "-XX:+DisplayVMOutputToStderr")
                .stdoutShouldNotContain(UNKNOWN_MODULE_NOT_FOUND)
                .stdoutShouldNotContain(STACK_FRAME)
                .stderrShouldContain(UNKNOWN_MODULE_NOT_FOUND)
                .stderrShouldContain(STACK_FRAME));
    }

    private OutputAnalyzer showVersion(String... args) throws Exception {
        int len = args.length;
        args = Arrays.copyOf(args, len+1);
        args[len] = "-version";
        return ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out);
    }

    private void expectFail(OutputAnalyzer output) {
        assertFalse(output.getExitValue() == 0);
    }

}

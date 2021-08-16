/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;

/*
 * @test
 * @bug 7104647
 * @summary Unit test for jcmd utility. Tests jcmd options which do not send
 * requests to a specific JVM process.
 *
 * @library /test/lib
 *
 * @run main TestJcmdDefaults
 */
public class TestJcmdDefaults {

    private static final String TEST_SRC = System.getProperty("test.src").trim();
    private static final String[] VM_ARGS = new String[] { "-XX:+UsePerfData" };
    private static final String JCMD_LIST_REGEX = "(?s)^\\d+\\s*.*";

    public static void main(String[] args) throws Exception {
        testJcmdUsage("-?");
        testJcmdUsage("-h");
        testJcmdUsage("--help");
        testJcmdDefaults();
        testJcmdDefaults("-l");
    }

    /**
     * jcmd -J-XX:+UsePerfData -?
     * jcmd -J-XX:+UsePerfData -h
     * jcmd -J-XX:+UsePerfData --help
     */
    private static void testJcmdUsage(String... jcmdArgs) throws Exception {
        OutputAnalyzer output = JcmdBase.jcmdNoPid(VM_ARGS, jcmdArgs);

        assertEquals(output.getExitValue(), 0);
        verifyOutputAgainstFile(output);
    }

    /**
     * jcmd -J-XX:+UsePerfData
     * jcmd -J-XX:+UsePerfData -l
     */
    private static void testJcmdDefaults(String... jcmdArgs) throws Exception {
        OutputAnalyzer output = JcmdBase.jcmdNoPid(VM_ARGS, jcmdArgs);

        output.shouldHaveExitValue(0);
        output.shouldContain("sun.tools.jcmd.JCmd");
        matchListedProcesses(output);
    }

    /**
     * Verifies the listed processes match a certain pattern.
     *
     * The output should look like:
     * 12246 sun.tools.jcmd.JCmd
     * 24428 com.sun.javatest.regtest.MainWrapper /tmp/jtreg/jtreg-workdir/classes/sun/tools/jcmd/TestJcmdDefaults.jta
     *
     * @param output The generated output from the jcmd.
     */
    private static void matchListedProcesses(OutputAnalyzer output) {
        output.stdoutShouldMatchByLine(JCMD_LIST_REGEX);
        output.stderrShouldBeEmptyIgnoreDeprecatedWarnings();
    }

    private static void verifyOutputAgainstFile(OutputAnalyzer output) throws IOException {
        Path path = Paths.get(TEST_SRC, "usage.out");
        List<String> fileOutput = Files.readAllLines(path);
        List<String> outputAsLines = output.asLines();
        assertTrue(outputAsLines.containsAll(fileOutput),
                "The ouput should contain all content of " + path.toAbsolutePath());
    }

}

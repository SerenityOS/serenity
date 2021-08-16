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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

/*
 * @test
 * @bug 7104647 7154822
 * @summary Unit test for jcmd utility. The test will send different diagnostic
 * command requests to the current java process.
 *
 * @library /test/lib
 *
 * @run main/othervm -XX:+UsePerfData TestJcmdSanity
 */
public class TestJcmdSanity {

    private static final String TEST_SRC = System.getProperty("test.src").trim();
    private static final String[] VM_ARGS = new String[] { "-XX:+UsePerfData" };
    private static final String JCMD_COMMAND_REGEX = "(\\w|\\.)*";
    private static final String PERF_COUNTER_REGEX = "(\\w|\\.)*\\=.*";

    public static void main(String[] args) throws Exception {
        testJcmdPidHelp();
        testJcmdPidHelpHelp();
        testJcmdPid_f();
        testJcmdPidPerfCounterPrint();
        testJcmdPidBigScript();
    }

    /**
     * jcmd -J-XX:+UsePerfData pid help
     */
    private static void testJcmdPidHelp() throws Exception {
        OutputAnalyzer output = JcmdBase.jcmd(VM_ARGS,
                new String[] {"help"});

        output.shouldHaveExitValue(0);
        output.shouldNotContain("Exception");
        output.shouldContain(Long.toString(ProcessTools.getProcessId()) + ":");
        matchJcmdCommands(output);
        output.shouldContain("For more information about a specific command use 'help <command>'.");
    }

    /**
     * jcmd -J-XX:+UsePerfData pid help help
     */
    private static void testJcmdPidHelpHelp() throws Exception {
        OutputAnalyzer output = JcmdBase.jcmd(VM_ARGS,
                new String[] {"help", "help"});

        output.shouldHaveExitValue(0);
        verifyOutputAgainstFile(output);
    }

    /**
     * jcmd -J-XX:+UsePerfData pid PerfCounter.print
     */
    private static void testJcmdPidPerfCounterPrint() throws Exception {
        OutputAnalyzer output = JcmdBase.jcmd(VM_ARGS,
                new String[] {"PerfCounter.print"});

        output.shouldHaveExitValue(0);
        matchPerfCounters(output);
    }

    /**
     * jcmd -J-XX:+UsePerfData pid -f dcmd-script.txt
     */
    private static void testJcmdPid_f() throws Exception {
        File scrpitFile = new File(TEST_SRC, "dcmd-script.txt");
        OutputAnalyzer output = JcmdBase.jcmd(VM_ARGS,
                new String[] {"-f", scrpitFile.getAbsolutePath()});

        output.shouldHaveExitValue(0);
        verifyOutputAgainstFile(output);
    }

    /**
     * Tests that it possible send a file over 1024 bytes large via jcmd -f.
     *
     * jcmd -J-XX:+UsePerfData pid -f dcmd-big-script.txt
     */
    private static void testJcmdPidBigScript() throws Exception {
        File scrpitFile = new File(TEST_SRC, "dcmd-big-script.txt");
        OutputAnalyzer output = JcmdBase.jcmd(VM_ARGS,
                new String[] {"-f", scrpitFile.getAbsolutePath()});

        output.shouldHaveExitValue(0);
        output.shouldNotContain("Exception");
        output.shouldContain(System.getProperty("java.vm.name").trim());
    }

    /**
     * Verifies the listed jcmd commands match a certain pattern.
     *
     * The output of the jcmd commands should look like:
     * VM.uptime
     * VM.flags
     * VM.system_properties
     *
     * @param output The generated output from the jcmd.
     * @throws Exception
     */
    private static void matchJcmdCommands(OutputAnalyzer output) {
        output.shouldMatchByLine(JCMD_COMMAND_REGEX,
                "help",
                JCMD_COMMAND_REGEX);
    }

    /**
     * Verifies the generated output from the PerfCounter.print command
     * matches a certain pattern.
     *
     * The output of perf counters should look like:
     * java.property.java.vm.name="Java HotSpot(TM) 64-Bit Server VM"
     * java.threads.daemon=7
     * sun.rt.javaCommand="com.sun.javatest.regtest.MainWrapper /tmp/jtreg/jtreg-workdir/classes/sun/tools/jcmd/TestJcmdSanity.jta"
     *
     * @param output The generated output from the PerfCounter.print command.
     * @throws Exception
     */
    private static void matchPerfCounters(OutputAnalyzer output) {
        output.stdoutShouldMatchByLine(PERF_COUNTER_REGEX, null, PERF_COUNTER_REGEX);
        output.stderrShouldBeEmptyIgnoreDeprecatedWarnings();
    }

    private static void verifyOutputAgainstFile(OutputAnalyzer output) throws IOException {
        Path path = Paths.get(TEST_SRC, "help_help.out");
        List<String> fileOutput = Files.readAllLines(path);
        List<String> outputAsLines = output.asLines();
        assertTrue(outputAsLines.containsAll(fileOutput),
                "The ouput should contain all content of " + path.toAbsolutePath());
    }

}

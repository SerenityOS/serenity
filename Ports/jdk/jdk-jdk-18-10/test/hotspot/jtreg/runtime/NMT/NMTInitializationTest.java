/*
 * Copyright (c) 2021 SAP SE. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * This test tests the ability of NMT to work correctly when masses of allocations happen before NMT is initialized;
 * That pre-NMT-init phase starts when the libjvm is loaded and the C++ dynamic initialization runs, and ends when
 * NMT is initialized after the VM parsed its arguments in CreateJavaVM.
 *
 * During that phase, NMT is not yet initialized fully; C-heap allocations are kept in a special lookup table to
 * be able to tell them apart from post-NMT-init initializations later. For details, see nmtPreInit.hpp.
 *
 * The size of this table is limited, and its load factor affects lookup time; that lookup time is paid throughout
 * the VM life for all os::free() calls, regardless if NMT is on or not. Therefore we are interested in keeping the
 * number of pre-NMT-init allocations low.
 *
 * Normally, the VM allocates about 500 surviving allocations (allocations which are not freed before NMT initialization
 * finishes). The number is mainly influenced by the number of VM arguments, since those get strdup'ed around.
 * Therefore, the only direct way to test pre-NMT-init allocations is by feeding the VM a lot of arguments, and this is
 * what this test does.
 *
 */

/**
 * @test id=normal-off
 * @bug 8256844
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver NMTInitializationTest normal off
 */

/**
 * @test id=normal-detail
 * @bug 8256844
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver NMTInitializationTest normal detail
 */

/**
 * @test id=default_long-off
 * @bug 8256844
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver NMTInitializationTest long off
 */

/**
 * @test id=default_long-detail
 * @bug 8256844
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver NMTInitializationTest long detail
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMTInitializationTest {

    final static boolean debug = true;

    static String randomString() {
        Random r = new Random();
        int len = r.nextInt(100) + 100;
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < len; i ++) {
            bld.append(r.nextInt(26) + 'A');
        }
        return bld.toString();
    }

    static Path createCommandFile(int numlines) throws Exception {
        String fileName = "commands_" + numlines + ".txt";
        FileWriter fileWriter = new FileWriter(fileName);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        String line = "-XX:ErrorFile=" + randomString();
        for (long i = 0; i < numlines / 2; i++) {
            printWriter.println(line);
        }
        printWriter.close();
        return Paths.get(fileName);
    }

    enum TestMode {
        // call the VM with a normal-ish command line (long but not oudlandishly so). We expect the lookup table after
        // initialization to be sparsely populated and sport very short chain lengths.
        mode_normal(30, 5),
        // call the VM with an outlandishly long command line. We expect the lookup table after initialization
        // to be densely populated but hopefully evenly distributed.
        mode_long(20000, 20);

        final int num_command_line_args;
        final int expected_max_chain_len;

        TestMode(int num_command_line_args, int expected_max_chain_len) {
            this.num_command_line_args = num_command_line_args;
            this.expected_max_chain_len = expected_max_chain_len;
        }
    };

    enum NMTMode {
      off, summary, detail
    };

    public static void main(String args[]) throws Exception {
        TestMode testMode = TestMode.valueOf("mode_" + args[0]);
        NMTMode nmtMode = NMTMode.valueOf(args[1]);

        System.out.println("Test mode: " + testMode + ", NMT mode: " + nmtMode);

        Path commandLineFile = createCommandFile(testMode.num_command_line_args);

        ArrayList<String> vmArgs = new ArrayList<>();
        vmArgs.add("-Xlog:nmt");
        vmArgs.add("-XX:NativeMemoryTracking=" + nmtMode.name());
        vmArgs.add("-XX:+UnlockDiagnosticVMOptions");
        vmArgs.add("-XX:+PrintNMTStatistics");

        if (commandLineFile != null) {
            vmArgs.add("@" + commandLineFile.getFileName());
        }
        vmArgs.add("-version");

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(vmArgs);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        if (debug) {
            output.reportDiagnosticSummary();
        }

        output.shouldHaveExitValue(0);

        // Now evaluate the output of -Xlog:nmt
        // We expect something like:
        // [0.001s][info][nmt] NMT initialized: detail
        // [0.001s][info][nmt] Preinit state:
        // [0.001s][info][nmt] entries: 342 (primary: 342, empties: 7577), sum bytes: 12996, longest chain length: 1
        // [0.001s][info][nmt] pre-init mallocs: 375, pre-init reallocs: 6, pre-init frees: 33, pre-to-post reallocs: 4, pre-to-post frees: 0

        output.shouldContain("NMT initialized: " + nmtMode.name());
        output.shouldContain("Preinit state:");
        String regex = ".*entries: (\\d+).*sum bytes: (\\d+).*longest chain length: (\\d+).*";
        output.shouldMatch(regex);
        String line = output.firstMatch(regex, 0);
        if (line == null) {
            throw new RuntimeException("expected: " + regex);
        }
        System.out.println(line);
        Pattern p = Pattern.compile(regex);
        Matcher mat = p.matcher(line);
        mat.matches();
        int entries = Integer.parseInt(mat.group(1));
        int sum_bytes = Integer.parseInt(mat.group(2));
        int longest_chain = Integer.parseInt(mat.group(3));
        System.out.println("found: " + entries + " - " + sum_bytes + longest_chain + ".");

        // Now we test the state of the internal lookup table, and through our assumptions about
        //   early pre-NMT-init allocations:
        // The normal allocation count of surviving pre-init allocations is around 300-500, with the sum of allocated
        //   bytes of a few dozen KB. We check these boundaries (with a very generous overhead) to see if the numbers are
        //   way off. If they are, we may either have a leak or just a lot more allocations than we thought before
        //   NMT initialization. Both cases should be investigated. Even if the allocations are valid, too many of them
        //   stretches the limits of the lookup map, and therefore may cause slower lookup. We should then either change
        //   the coding, reducing the number of allocations. Or enlarge the lookup table.

        // Apply some sensible assumptions
        if (entries > testMode.num_command_line_args + 2000) { // Note: normal baseline is 400-500
            throw new RuntimeException("Suspiciously high number of pre-init allocations.");
        }
        if (sum_bytes > 128 * 1024 * 1024) { // Note: normal baseline is ~30-40KB
            throw new RuntimeException("Suspiciously high pre-init memory usage.");
        }
        if (longest_chain > testMode.expected_max_chain_len) {
            // Under normal circumstances, load factor of the map should be about 0.1. With a good hash distribution, we
            // should rarely see even a chain > 1. Warn if we see exceedingly long bucket chains, since this indicates
            // either that the hash algorithm is inefficient or we have a bug somewhere.
            throw new RuntimeException("Suspiciously long bucket chains in lookup table.");
        }

        // Finally, check that we see our final NMT report:
        if (nmtMode != NMTMode.off) {
            output.shouldContain("Native Memory Tracking:");
            output.shouldMatch("Total: reserved=\\d+, committed=\\d+.*");
        }
    }
}

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

package gc.arguments;

/*
 * @test TestVerifyBeforeAndAfterGCFlags
 * @bug 8000831
 * @summary Runs an simple application (GarbageProducer) with various
         combinations of -XX:{+|-}Verify{After|Before}GC flags and checks that
         output contain or doesn't contain expected patterns
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @library /test/lib
 * @library /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.arguments.TestVerifyBeforeAndAfterGCFlags
 */

import java.util.ArrayList;
import java.util.Collections;

import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import sun.hotspot.WhiteBox;

public class TestVerifyBeforeAndAfterGCFlags {

    // VerifyBeforeGC:[Verifying threads heap tenured eden syms strs zone dict metaspace chunks hand code cache ]
    public static final String VERIFY_BEFORE_GC_PATTERN = "Verifying Before GC";
    // VerifyBeforeGC: VerifyBeforeGC: VerifyBeforeGC:
    public static final String VERIFY_BEFORE_GC_CORRUPTED_PATTERN = "VerifyBeforeGC:(?!\\[Verifying[^]]+\\])";

    // VerifyAfterGC:[Verifying threads heap tenured eden syms strs zone dict metaspace chunks hand code cache ]
    public static final String VERIFY_AFTER_GC_PATTERN = "Verifying After GC";
    // VerifyAfterGC: VerifyAfterGC: VerifyAfterGC:
    public static final String VERIFY_AFTER_GC_CORRUPTED_PATTERN = "VerifyAfterGC:(?!\\[Verifying[^]]+\\])";

    public static void main(String args[]) throws Exception {
        String[] filteredOpts = Utils.getFilteredTestJavaOpts(
                                    new String[] { "-Xlog:gc+verify=debug",
                                                   "-XX:+UseGCLogFileRotation",
                                                   "-XX:-DisplayVMOutput",
                                                   "VerifyBeforeGC",
                                                   "VerifyAfterGC" });
        // Young GC
        testVerifyFlags(false, false, false, filteredOpts);
        testVerifyFlags(true,  true,  false, filteredOpts);
        testVerifyFlags(true,  false, false, filteredOpts);
        testVerifyFlags(false, true,  false, filteredOpts);
        // Full GC
        testVerifyFlags(false, false, true, filteredOpts);
        testVerifyFlags(true,  true,  true, filteredOpts);
        testVerifyFlags(true,  false, true, filteredOpts);
        testVerifyFlags(false, true,  true, filteredOpts);
    }

    public static void testVerifyFlags(boolean verifyBeforeGC,
                                       boolean verifyAfterGC,
                                       boolean doFullGC,
                                       String[] opts) throws Exception {
        ArrayList<String> vmOpts = new ArrayList<>();
        if (opts != null && (opts.length > 0)) {
            Collections.addAll(vmOpts, opts);
        }
        Collections.addAll(vmOpts, new String[] {
                                       "-Xbootclasspath/a:.",
                                       "-XX:+UnlockDiagnosticVMOptions",
                                       "-XX:+WhiteBoxAPI",
                                       "-Xlog:gc+verify=debug",
                                       "-Xmx5m",
                                       "-Xms5m",
                                       "-Xmn3m",
                                       (verifyBeforeGC ? "-XX:+VerifyBeforeGC"
                                                       : "-XX:-VerifyBeforeGC"),
                                       (verifyAfterGC ? "-XX:+VerifyAfterGC"
                                                      : "-XX:-VerifyAfterGC"),
                                       GarbageProducer.class.getName(),
                                       doFullGC ? "t" : "f" });
        ProcessBuilder pb = GCArguments.createJavaProcessBuilder(vmOpts);
        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());

        analyzer.shouldHaveExitValue(0);
        analyzer.shouldNotMatch(VERIFY_BEFORE_GC_CORRUPTED_PATTERN);
        analyzer.shouldNotMatch(VERIFY_AFTER_GC_CORRUPTED_PATTERN);

        if (verifyBeforeGC) {
            analyzer.shouldMatch(VERIFY_BEFORE_GC_PATTERN);
        } else {
            analyzer.shouldNotMatch(VERIFY_BEFORE_GC_PATTERN);
        }

        if (verifyAfterGC) {
            analyzer.shouldMatch(VERIFY_AFTER_GC_PATTERN);
        } else {
            analyzer.shouldNotMatch(VERIFY_AFTER_GC_PATTERN);
        }
    }

    public static class GarbageProducer {
        static long[][] garbage = new long[10][];

        public static void main(String args[]) {
            WhiteBox wb = WhiteBox.getWhiteBox();

            if (args[0].equals("t")) {
                wb.fullGC();
            } else {
                wb.youngGC();
            }
        }
    }
}

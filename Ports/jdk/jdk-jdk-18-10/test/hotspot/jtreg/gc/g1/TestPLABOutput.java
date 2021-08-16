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

package gc.g1;

/*
 * @test TestPLABOutput
 * @bug 8140585
 * @summary Check that G1 does not report empty PLAB statistics in the first evacuation.
 * @requires vm.gc.G1
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.g1.TestPLABOutput
 */

import sun.hotspot.WhiteBox;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import static jdk.test.lib.Asserts.*;

public class TestPLABOutput {

    public static void runTest() throws Exception {
        final String[] arguments = {
            "-Xbootclasspath/a:.",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-XX:+UseG1GC",
            "-Xmx10M",
            "-Xlog:gc+plab=debug",
            GCTest.class.getName()
            };

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(arguments);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        output.shouldHaveExitValue(0);

        System.out.println(output.getStdout());

        String pattern = ".*GC\\(0\\) .*allocated: (\\d+).*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(output.getStdout());

        if (!m.find()) {
            throw new RuntimeException("Could not find any PLAB statistics output");
        }
        int allocated = Integer.parseInt(m.group(1));
        assertGT(allocated, 0, "Did not allocate any memory during test");
    }

    public static void main(String[] args) throws Exception {
        runTest();
    }

    static class GCTest {
        private static final WhiteBox WB = WhiteBox.getWhiteBox();

        public static Object holder;

        public static void main(String [] args) {
            holder = new byte[100];
            WB.youngGC();
            System.out.println(holder);
        }
    }
}


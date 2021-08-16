/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestSelectDefaultGC
 * @summary Test selection of GC when no GC option is specified
 * @bug 8068582
 * @library /test/lib
 * @library /
 * @requires vm.gc.Serial & vm.gc.G1
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestSelectDefaultGC
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestSelectDefaultGC {
    public static void assertVMOption(OutputAnalyzer output, String option, boolean value) {
        output.shouldMatch(" " + option + " .*=.* " + value + " ");
    }

    public static void testDefaultGC(boolean actAsServer) throws Exception {
        // Start VM without specifying GC
        ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
            "-XX:" + (actAsServer ? "+" : "-") + "AlwaysActAsServerClassMachine",
            "-XX:" + (actAsServer ? "-" : "+") + "NeverActAsServerClassMachine",
            "-XX:+PrintFlagsFinal",
            "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        final boolean isServer = actAsServer;

        // Verify GC selection
        // G1 is default for server class machines
        assertVMOption(output, "UseG1GC",            isServer);
        // Serial is default for non-server class machines
        assertVMOption(output, "UseSerialGC",        !isServer);
    }

    public static void main(String[] args) throws Exception {
        // Test server class machine
        testDefaultGC(false);

        // Test non-server class machine
        testDefaultGC(true);
    }
}

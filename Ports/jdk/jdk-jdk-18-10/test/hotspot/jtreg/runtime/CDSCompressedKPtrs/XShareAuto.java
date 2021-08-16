/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.cds
 * @requires vm.flagless
 * @bug 8005933
 * @summary -Xshare:auto is the default when -Xshare is not specified
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver XShareAuto
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class XShareAuto {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-server", "-XX:+UnlockDiagnosticVMOptions",
            "-XX:SharedArchiveFile=./XShareAuto.jsa", "-Xshare:dump", "-Xlog:cds");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Loading classes to share");
        output.shouldHaveExitValue(0);


        // We have 2 test cases:
        String cases[] = {
            "-Xshare:auto",    // case [1]: -Xshare:auto is explicitly specified.
            "-showversion"     // case [2]: -Xshare:auto is not explicitly specified,
                               //           but VM should still use it by default.
        };

        for (String x : cases) {
            pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:SharedArchiveFile=./XShareAuto.jsa",
                "-Xlog:cds",
                x,
                "-version");
            output = new OutputAnalyzer(pb.start());
            String outputString = output.getOutput();

            if (!outputString.contains("Unable to map")) {
                // sharing may not be enabled if XShareAuto.jsa cannot be mapped due to
                // ASLR.
                output.shouldContain("sharing");
            }
            output.shouldHaveExitValue(0);
        }
    }
}

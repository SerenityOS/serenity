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

/*
 * @test
 * @bug 8026949 8164091
 * @summary Test ensures correct VM output during startup
 * @library /test/lib
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run driver compiler.startup.StartupOutput
 */

package compiler.startup;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class StartupOutput {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer out;

        pb = ProcessTools.createJavaProcessBuilder("-Xint", "-XX:+DisplayVMOutputToStdout", "-version");
        out = new OutputAnalyzer(pb.start());
        out.shouldNotContain("no space to run compilers");
        out.shouldHaveExitValue(0);

        pb = ProcessTools.createJavaProcessBuilder("-Xint", "-XX:ReservedCodeCacheSize=1770K", "-XX:InitialCodeCacheSize=4K", "-version");
        out = new OutputAnalyzer(pb.start());
        // The VM should not crash but may return an error message because we don't have enough space for adapters
        int exitCode = out.getExitValue();
        if (exitCode != 1 && exitCode != 0) {
            throw new Exception("VM crashed with exit code " + exitCode);
        }
    }
}

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
 * @bug 8023014
 * @summary Test ensures that there is no crash if there is not enough ReservedCodeCacheSize
 *          to initialize all compiler threads. The option -Xcomp gives the VM more time to
 *          trigger the old bug.
 * @library /test/lib
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run driver compiler.startup.SmallCodeCacheStartup
 */

package compiler.startup;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import static jdk.test.lib.Asserts.assertTrue;

public class SmallCodeCacheStartup {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:ReservedCodeCacheSize=3m",
                                                                  "-XX:CICompilerCount=64",
                                                                  "-Xcomp",
                                                                  "-version");
        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());
        try {
            analyzer.shouldHaveExitValue(0);
        } catch (RuntimeException e) {
            // Error occurred during initialization, did we run out of adapter space?
            assertTrue(analyzer.getOutput().contains("VirtualMachineError: Out of space in CodeCache"),
                    "Expected VirtualMachineError");
        }

        System.out.println("TEST PASSED");
  }
}

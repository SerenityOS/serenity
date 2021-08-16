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

/*
 * @test TestBasicLogOutput
 * @bug 8203370
 * @summary Ensure -XX:-JVMCIPrintProperties can be enabled and successfully prints expected output to stdout.
 * @requires vm.flagless
 * @requires vm.jvmci
 * @library /test/lib
 * @run driver TestJVMCIPrintProperties
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestJVMCIPrintProperties {

    public static void main(String[] args) throws Exception {
        test("-XX:+EnableJVMCI");
        test("-XX:+UseJVMCICompiler");
    }

    static void test(String enableFlag) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UnlockExperimentalVMOptions",
            enableFlag, "-Djvmci.Compiler=null",
            "-XX:+JVMCIPrintProperties");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("[JVMCI properties]"); // expected message
        output.shouldContain("jvmci.Compiler := \"null\""); // expected message
        output.shouldContain("jvmci.InitTimer = false"); // expected message
        output.shouldContain("jvmci.PrintConfig = false"); // expected message
        output.shouldContain("jvmci.TraceMethodDataFilter = null"); // expected message
        output.shouldHaveExitValue(0);
    }
}

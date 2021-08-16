/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027314
 * @summary Warn if diagnostic or experimental vm option is used and -XX:+UnlockDiagnosticVMOptions or -XX:+UnlockExperimentalVMOptions, respectively, isn't specified. Warn if develop or notproduct vm option is used with product version of VM.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver VMOptionWarning
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;

public class VMOptionWarning {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+AlwaysSafeConstructors", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Error: VM option 'AlwaysSafeConstructors' is experimental and must be enabled via -XX:+UnlockExperimentalVMOptions.");

        if (Platform.isDebugBuild()) {
            System.out.println("Skip the rest of the tests on debug builds since diagnostic, develop, and notproduct options are available on debug builds.");
            return;
        }

        pb = ProcessTools.createJavaProcessBuilder("-XX:+PrintInlining", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Error: VM option 'PrintInlining' is diagnostic and must be enabled via -XX:+UnlockDiagnosticVMOptions.");

        pb = ProcessTools.createJavaProcessBuilder("-XX:+VerifyStack", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Error: VM option 'VerifyStack' is develop and is available only in debug version of VM.");

        pb = ProcessTools.createJavaProcessBuilder("-XX:+CheckCompressedOops", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Error: VM option 'CheckCompressedOops' is notproduct and is available only in debug version of VM.");
    }
}

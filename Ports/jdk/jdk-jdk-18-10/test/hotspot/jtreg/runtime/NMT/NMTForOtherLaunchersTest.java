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
 * With JDK-8256844 "Make NMT late-initializable", NMT should work out of the box with jdk launchers other than
 * java.exe.
 *
 * Test that assumption (we test with javac and jar and leave it at that, other tools should be fine as well)
 */

/**
 * @test id=javac
 * @bug 8256844
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver NMTForOtherLaunchersTest javac
 */

/**
 * @test id=jar
 * @bug 8256844
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver NMTForOtherLaunchersTest jar
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.JDKToolFinder;

public class NMTForOtherLaunchersTest {
    public static void main(String args[]) throws Exception {
        String tool = args[0];
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(new String[]{
                JDKToolFinder.getJDKTool(tool),
                "-J-XX:NativeMemoryTracking=summary",
                "-J-XX:+UnlockDiagnosticVMOptions",
                "-J-XX:+PrintNMTStatistics",
                "--help"});
        System.out.println(pb.command());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        // We should not see the "wrong launcher?" message, which would indicate
        // an older JDK, and we should see the NMT stat output when the VM shuts down.
        output.shouldNotContain("wrong launcher");
        output.shouldContain("Native Memory Tracking:");
        output.shouldMatch("Total: reserved=\\d+, committed=\\d+.*");
    }
}

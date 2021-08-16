/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * Test to verify GetObjectSize does not overflow on a 600M element int[]
 *
 * @test
 * @bug 8027230
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.instrument
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @requires vm.bits == 64
 * @requires vm.jvmti
 * @requires os.maxMemory > 6G
 * @build GetObjectSizeOverflowAgent
 * @run driver jdk.test.lib.helpers.ClassFileInstaller GetObjectSizeOverflowAgent
 * @run driver GetObjectSizeOverflow
 */

import java.io.PrintWriter;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class GetObjectSizeOverflow {
    public static void main(String[] args) throws Exception  {

        try (var pw = new PrintWriter("MANIFEST.MF")) {
            pw.println("Premain-Class: GetObjectSizeOverflowAgent");
        }

        var jar = new ProcessBuilder(JDKToolFinder.getJDKTool("jar"), "cmf", "MANIFEST.MF", "agent.jar", "GetObjectSizeOverflowAgent.class");
        new OutputAnalyzer(jar.start()).shouldHaveExitValue(0);

        ProcessBuilder pt = ProcessTools.createTestJvm("-Xmx4000m", "-javaagent:agent.jar",  "GetObjectSizeOverflowAgent");
        OutputAnalyzer output = new OutputAnalyzer(pt.start());
        output.stdoutShouldContain("GetObjectSizeOverflow passed");
        output.shouldHaveExitValue(0);
    }
}

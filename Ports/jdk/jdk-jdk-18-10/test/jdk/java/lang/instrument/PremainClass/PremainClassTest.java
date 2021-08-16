/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

/*
 * @test
 * @bug 5055293
 * @summary Test non ascii characters in the Premain-Class attribute.
 * @library /test/lib
 * @modules java.management
 * @run build DummyMain
 * @run main PremainClassTest
 */
public class PremainClassTest {
    // Use a javaagent where the manifest Premain-Class contains
    // a non ascii character.
    // Verify that the premain() function is executed correctly.
    public static void main(String[] a) throws Exception {
        String testArgs = String.format(
                "-javaagent:%s/Agent.jar -classpath %s DummyMain",
                System.getProperty("test.src"),
                System.getProperty("test.classes", "."));

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                Utils.addTestJavaOpts(testArgs.split("\\s+")));
        System.out.println("testjvm.cmd:" + Utils.getCommandLine(pb));

        OutputAnalyzer output = ProcessTools.executeProcess(pb);
        System.out.println("testjvm.stdout:" + output.getStdout());
        System.out.println("testjvm.stderr:" + output.getStderr());

        output.shouldHaveExitValue(0);
        output.stdoutShouldContain("premain running");
        output.stdoutShouldContain("Hello from DummyMain!");
    }
}

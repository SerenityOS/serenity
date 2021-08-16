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
 * @test
 * @bug 8173912
 * @requires vm.jvmci
 * @library / /test/lib
 * @library ../common/patches
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot:+open
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                  -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  compiler.jvmci.compilerToVM.GetFlagValueTest
 */

package compiler.jvmci.compilerToVM;

import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import sun.hotspot.WhiteBox;

public class GetFlagValueTest {
    public static void main(String[] args) throws Exception {
        try {
            CompilerToVMHelper.getFlagValue(null);
            Asserts.fail("Expected NullPointerException when calling getFlagValue(null)");
        } catch (NullPointerException e) {
            // expected
        }

        Object missing = CompilerToVMHelper.getFlagValue("this is surely not a flag");
        Asserts.assertEquals(CompilerToVMHelper.CTVM, missing);

        ProcessBuilder pb;
        OutputAnalyzer out;

        pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCI",
            "-XX:+PrintFlagsFinal",
            "-version");
        out = new OutputAnalyzer(pb.start());

        out.shouldHaveExitValue(0);
        String[] lines = out.getStdout().split("\\r?\\n");
        Asserts.assertTrue(lines.length > 1, "Expected output from -XX:+PrintFlagsFinal");

        final WhiteBox wb = WhiteBox.getWhiteBox();

        // Line example: ccstr PrintIdealGraphAddress = 127.0.0.1 {C2 notproduct} {default}
        Pattern flagLine = Pattern.compile("(\\w+)\\s+(\\w+)\\s+:?= (?:(.+))\\{[^}]+\\}\\s+\\{[^}]+\\}");
        for (String line : lines) {
            if (line.indexOf('=') != -1) {
                line = line.trim();
                Matcher m = flagLine.matcher(line);
                Asserts.assertTrue(m.matches(), "Unexpected line in -XX:+PrintFlagsFinal output: " + line);
                String type = m.group(1);
                String name = m.group(2);
                String expect = m.group(3).trim();
                Object value = CompilerToVMHelper.getFlagValue(name);
                Object wbValue = wb.getVMFlag(name);
                Asserts.assertEquals(value, wbValue, "Value of flag " + name);
            }
        }
    }
}

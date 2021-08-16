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
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @requires !(vm.cpu.features ~= ".*aes.*")
 * @requires vm.compiler1.enabled | !vm.graal.enabled
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *       -XX:+WhiteBoxAPI -Xbatch
 *       compiler.cpuflags.TestAESIntrinsicsOnUnsupportedConfig
 */

package compiler.cpuflags;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.cli.predicate.NotPredicate;
import static jdk.test.lib.cli.CommandLineOptionTest.*;

public class TestAESIntrinsicsOnUnsupportedConfig extends AESIntrinsicsBase {

    private static final String INTRINSICS_NOT_AVAILABLE_MSG = "warning: AES "
            + "intrinsics are not available on this CPU";
    private static final String AES_NOT_AVAILABLE_MSG = "warning: AES "
            + "instructions are not available on this CPU";

    protected void runTestCases() throws Throwable {
        testUseAES();
        testUseAESIntrinsics();
    }

    /**
     * Test checks following situation: <br/>
     * UseAESIntrinsics flag is set to true, TestAESMain is executed <br/>
     * Expected result: UseAESIntrinsics flag is set to false <br/>
     * UseAES flag is set to false <br/>
     * Output shouldn't contain intrinsics usage <br/>
     * Output should contain message about intrinsics unavailability
     * @throws Throwable
     */
    private void testUseAESIntrinsics() throws Throwable {
        OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(
                AESIntrinsicsBase.prepareArguments(prepareBooleanFlag(
                        AESIntrinsicsBase.USE_AES_INTRINSICS, true)));
        final String errorMessage = "Case testUseAESIntrinsics failed";
        verifyOutput(new String[] {INTRINSICS_NOT_AVAILABLE_MSG},
                new String[] {AESIntrinsicsBase.CIPHER_INTRINSIC,
                        AESIntrinsicsBase.AES_INTRINSIC},
                errorMessage, outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES_INTRINSICS, "false",
                errorMessage, outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES, "false", errorMessage,
                outputAnalyzer);
    }

    /**
     * Test checks following situation: <br/>
     * UseAESIntrinsics flag is set to true, TestAESMain is executed <br/>
     * Expected result: UseAES flag is set to false <br/>
     * UseAES flag is set to false <br/>
     * Output shouldn't contain intrinsics usage <br/>
     * Output should contain message about AES unavailability <br/>
     * @throws Throwable
     */
    private void testUseAES() throws Throwable {
        OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(
                AESIntrinsicsBase.prepareArguments(prepareBooleanFlag
                        (AESIntrinsicsBase.USE_AES, true)));
        final String errorMessage = "Case testUseAES failed";
        verifyOutput(new String[] {AES_NOT_AVAILABLE_MSG},
                new String[] {AESIntrinsicsBase.CIPHER_INTRINSIC,
                AESIntrinsicsBase.AES_INTRINSIC}, errorMessage, outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES_INTRINSICS, "false",
                errorMessage, outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES, "false", errorMessage,
                outputAnalyzer);
    }

    public static void main(String args[]) throws Throwable {
        new TestAESIntrinsicsOnUnsupportedConfig().runTestCases();
    }
}

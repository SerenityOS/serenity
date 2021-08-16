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
 * @requires vm.cpu.features ~= ".*aes.*" & !vm.graal.enabled
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=600 -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -Xbatch
 *                   compiler.cpuflags.TestAESIntrinsicsOnSupportedConfig
 */

package compiler.cpuflags;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;
import static jdk.test.lib.cli.CommandLineOptionTest.*;

public class TestAESIntrinsicsOnSupportedConfig extends AESIntrinsicsBase {

    protected void runTestCases() throws Throwable {
        testUseAES();
        testUseAESUseSSE2();
        testNoUseAES();
        testNoUseAESUseSSE2();
        testNoUseAESIntrinsic();
    }

    /**
     * Check if value of TieredStopAtLevel flag is greater than specified level.
     *
     * @param level tiered compilation level to compare with
     */
    private boolean isTieredLevelGreaterThan(int level) {
        Long val = WhiteBox.getWhiteBox().getIntxVMFlag("TieredStopAtLevel");
        return (val != null && val > level);
    }

    /**
     * Test checks following situation: <br/>
     * UseAES flag is set to true, TestAESMain is executed <br/>
     * Expected result: UseAESIntrinsics flag is set to true <br/>
     * If vm type is server then output should contain intrinsics usage <br/>
     *
     * @throws Throwable
     */
    private void testUseAES() throws Throwable {
        OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(
                prepareArguments(prepareBooleanFlag(AESIntrinsicsBase
                        .USE_AES, true)));
        final String errorMessage = "Case testUseAES failed";
        if (Platform.isServer() && !Platform.isEmulatedClient() && isTieredLevelGreaterThan(3)) {
            verifyOutput(new String[]{AESIntrinsicsBase.CIPHER_INTRINSIC,
                    AESIntrinsicsBase.AES_INTRINSIC}, null, errorMessage,
                    outputAnalyzer);
        } else {
            verifyOutput(null, new String[]{AESIntrinsicsBase.CIPHER_INTRINSIC,
                    AESIntrinsicsBase.AES_INTRINSIC}, errorMessage,
                    outputAnalyzer);
        }
        verifyOptionValue(AESIntrinsicsBase.USE_AES, "true", errorMessage,
                outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES_INTRINSICS, "true",
                errorMessage, outputAnalyzer);
    }

    /**
     * Test checks following situation: <br/>
     * UseAES flag is set to true, UseSSE flag is set to 2,
     * Platform should support UseSSE (x86 or x64) <br/>
     * TestAESMain is executed <br/>
     * Expected result: UseAESIntrinsics flag is set to false <br/>
     * Output shouldn't contain intrinsics usage <br/>
     *
     * @throws Throwable
     */
    private void testUseAESUseSSE2() throws Throwable {
        if (Platform.isX86() || Platform.isX64()) {
            OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(
                    prepareArguments(prepareBooleanFlag(AESIntrinsicsBase
                                    .USE_AES_INTRINSICS, true),
                            prepareNumericFlag(AESIntrinsicsBase.USE_SSE, 2)));
            final String errorMessage = "Case testUseAESUseSSE2 failed";
            verifyOutput(null, new String[]{AESIntrinsicsBase.CIPHER_INTRINSIC,
                            AESIntrinsicsBase.AES_INTRINSIC},
                    errorMessage, outputAnalyzer);
            verifyOptionValue(AESIntrinsicsBase.USE_AES, "true", errorMessage,
                    outputAnalyzer);
            verifyOptionValue(AESIntrinsicsBase.USE_AES_INTRINSICS, "false",
                    errorMessage, outputAnalyzer);
            verifyOptionValue(AESIntrinsicsBase.USE_SSE, "2", errorMessage,
                    outputAnalyzer);
        }
    }

    /**
     * Test checks following situation: <br/>
     * UseAES flag is set to false, UseSSE flag is set to 2,
     * Platform should support UseSSE (x86 or x64) <br/>
     * TestAESMain is executed <br/>
     * Expected result: UseAESIntrinsics flag is set to false <br/>
     * Output shouldn't contain intrinsics usage <br/>
     *
     * @throws Throwable
     */
    private void testNoUseAESUseSSE2() throws Throwable {
        if (Platform.isX86() || Platform.isX64()) {
            OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(
                    prepareArguments(prepareBooleanFlag(AESIntrinsicsBase
                                    .USE_AES, false),
                            prepareNumericFlag(AESIntrinsicsBase.USE_SSE, 2)));
            final String errorMessage = "Case testNoUseAESUseSSE2 failed";
            verifyOutput(null, new String[]{AESIntrinsicsBase.CIPHER_INTRINSIC,
                            AESIntrinsicsBase.AES_INTRINSIC},
                    errorMessage, outputAnalyzer);
            verifyOptionValue(AESIntrinsicsBase.USE_AES, "false", errorMessage,
                    outputAnalyzer);
            verifyOptionValue(AESIntrinsicsBase.USE_AES_INTRINSICS, "false",
                    errorMessage, outputAnalyzer);
            verifyOptionValue(AESIntrinsicsBase.USE_SSE, "2", errorMessage,
                    outputAnalyzer);
        }
    }

    /**
     * Test checks following situation: <br/>
     * UseAES flag is set to false, TestAESMain is executed <br/>
     * Expected result: UseAESIntrinsics flag is set to false <br/>
     * Output shouldn't contain intrinsics usage <br/>
     *
     * @throws Throwable
     */
    private void testNoUseAES() throws Throwable {
        OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(
                prepareArguments(prepareBooleanFlag(AESIntrinsicsBase
                        .USE_AES, false)));
        final String errorMessage = "Case testNoUseAES failed";
        verifyOutput(null, new String[]{AESIntrinsicsBase.CIPHER_INTRINSIC,
                        AESIntrinsicsBase.AES_INTRINSIC},
                errorMessage, outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES, "false", errorMessage,
                outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES_INTRINSICS, "false",
                errorMessage, outputAnalyzer);
    }

    /**
     * Test checks following situation: <br/>
     * UseAESIntrinsics flag is set to false, TestAESMain is executed <br/>
     * Expected result: UseAES flag is set to true <br/>
     * Output shouldn't contain intrinsics usage <br/>
     *
     * @throws Throwable
     */
    private void testNoUseAESIntrinsic() throws Throwable {
        OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(
                prepareArguments(prepareBooleanFlag(AESIntrinsicsBase
                        .USE_AES_INTRINSICS, false)));
        final String errorMessage = "Case testNoUseAESIntrinsic failed";
        verifyOutput(null, new String[]{AESIntrinsicsBase.CIPHER_INTRINSIC,
                        AESIntrinsicsBase.AES_INTRINSIC}, errorMessage,
                outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES, "true", errorMessage,
                outputAnalyzer);
        verifyOptionValue(AESIntrinsicsBase.USE_AES_INTRINSICS, "false",
                errorMessage, outputAnalyzer);
    }

    public static void main(String args[]) throws Throwable {
        new TestAESIntrinsicsOnSupportedConfig().runTestCases();
    }
}

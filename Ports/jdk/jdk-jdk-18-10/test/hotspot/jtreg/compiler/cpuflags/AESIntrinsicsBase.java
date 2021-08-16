/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.cpuflags;

import compiler.codegen.aes.TestAESMain;
import java.util.Arrays;

public abstract class AESIntrinsicsBase {
    public static final String CIPHER_INTRINSIC = "com\\.sun\\.crypto\\"
            + ".provider\\.CipherBlockChaining::"
            + "(implEncrypt|implDecrypt) \\([0-9]+ bytes\\)\\s+\\(intrinsic[,\\)]";
    public static final String AES_INTRINSIC = "com\\.sun\\.crypto\\"
            + ".provider\\.AESCrypt::(implEncryptBlock|implDecryptBlock) \\([0-9]+ "
            + "bytes\\)\\s+\\(intrinsic[,\\)]";
    public static final String USE_AES = "UseAES";
    public static final String USE_AES_INTRINSICS = "UseAESIntrinsics";
    public static final String USE_SSE = "UseSSE";
    public static final String[] USE_DIAGNOSTIC_CMD
            = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintIntrinsics"};
    public static final String[] TEST_AES_CMD
            = {"-XX:+IgnoreUnrecognizedVMOptions", "-XX:+PrintFlagsFinal",
            "-Xbatch", "-XX:CompileThresholdScaling=0.01", "-DcheckOutput=true", "-Dmode=CBC",
            TestAESMain.class.getName(), "100", "1000"};

    /**
     * Prepares command for TestAESMain execution.
     * Intrinsics flags are of diagnostic type
     * and must be preceded by UnlockDiagnosticVMOptions.
     * @param args flags that must be added to command
     * @return command for TestAESMain execution
     */
    public static String[] prepareArguments(String... args) {
        String[] command = Arrays.copyOf(USE_DIAGNOSTIC_CMD, args.length
                + USE_DIAGNOSTIC_CMD.length + TEST_AES_CMD.length);
        System.arraycopy(args, 0, command, USE_DIAGNOSTIC_CMD.length,
                args.length);
        System.arraycopy(TEST_AES_CMD, 0, command, args.length
                + USE_DIAGNOSTIC_CMD.length, TEST_AES_CMD.length);
        return command;
    }
}

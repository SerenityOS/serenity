/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jvmti/SetNativeMethodPrefix/SetNativeMethodPrefix002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test is designed to test whether SetNativeMethodPrefix()
 *     JVMTI functions work according to specification when multiple agents are set.
 *     The test verifies that:
 *         - the order of applied prefixes is preserved.
 *     Also there are some checks that JVMTI error codes to be correctly returned.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment duplicate SetNativeMethodPrefix001 in current directory
 * @run driver nsk.jvmti.NativeLibraryCopier
 *      SetNativeMethodPrefix002
 *      SetNativeMethodPrefix002-01
 *      SetNativeMethodPrefix002-02
 *      SetNativeMethodPrefix002-03
 *
 * @run main/othervm/native TestDriver
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

import java.io.File;
import java.util.Arrays;

public class TestDriver {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
                "-agentlib:SetNativeMethodPrefix001=trace=all",
                "-agentlib:SetNativeMethodPrefix002-01=trace=all prefix=wa_",
                "-agentlib:SetNativeMethodPrefix002-02=trace=all prefix=wb_",
                "-agentlib:SetNativeMethodPrefix002-03=trace=all prefix=wc_",
                nsk.jvmti.SetNativeMethodPrefix.SetNativeMethodPrefix002.class.getName()
        );

        String envName = Platform.sharedLibraryPathVariableName();
        pb.environment()
          .merge(envName, ".", (x, y) -> y + File.pathSeparator + x);

        String command = Arrays.toString(args);
        System.out.println("exec " + command);
        pb.inheritIO();

        int exitCode = pb.start().waitFor();

        if (exitCode != 95 && exitCode !=0 ) {
            throw new AssertionError(command + " exit code is " + exitCode);
        }
    }
}



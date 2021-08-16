/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8270925
 * @library / /test/lib
 * @summary testing of ciReplay with inlining
 * @requires vm.flightRecorder != true & vm.compMode != "Xint" & vm.debug == true & vm.compiler2.enabled
 * @modules java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      compiler.ciReplay.TestInlining
 */

package compiler.ciReplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.List;
import java.util.StringTokenizer;
import jdk.test.lib.Asserts;

public class TestInlining extends CiReplayBase {
    public static void main(String args[]) {
        new TestInlining().runTest(false, TIERED_DISABLED_VM_OPTION);
    }

    @Override
    public void testAction() {
        try {
            Path replayFilePath = Paths.get(REPLAY_FILE_NAME);
            List<String> replayContent = Files.readAllLines(replayFilePath);
            boolean found = false;
            for (int i = 0; i < replayContent.size(); i++) {
                String line = replayContent.get(i);
                if (line.startsWith("compile ")) {
                    StringTokenizer tokenizer = new StringTokenizer(line, " ");
                    Asserts.assertEQ(tokenizer.nextToken(), "compile");
                    tokenizer.nextToken(); // class
                    tokenizer.nextToken(); // method
                    tokenizer.nextToken(); // signature
                    tokenizer.nextToken(); // bci
                    Asserts.assertEQ(tokenizer.nextToken(), "4"); // level
                    Asserts.assertEQ(tokenizer.nextToken(), "inline");
                    found = true;
                }
            }
            Asserts.assertEQ(found, true);
        } catch (IOException ioe) {
            throw new Error("Failed to read/write replay data: " + ioe, ioe);
        }
    }
}


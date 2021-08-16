/*
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
 *
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

import sun.hotspot.WhiteBox;

/*
 * @test HandshakeTimeoutTest
 * @bug 8262454 8267651
 * @summary Test handshake timeout.
 * @requires vm.debug
 * @library /testlibrary /test/lib
 * @build HandshakeTimeoutTest
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver HandshakeTimeoutTest
 */

public class HandshakeTimeoutTest {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb =
            ProcessTools.createTestJvm(
                    "-Xbootclasspath/a:.",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+WhiteBoxAPI",
                    "-XX:+HandshakeALot",
                    "-XX:GuaranteedSafepointInterval=10",
                    "-XX:ParallelGCThreads=1",
                    "-XX:ConcGCThreads=1",
                    "-XX:CICompilerCount=2",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:HandshakeTimeout=50",
                    "-XX:-CreateCoredumpOnCrash",
                    "HandshakeTimeoutTest$Test");

        OutputAnalyzer output = ProcessTools.executeProcess(pb);
        output.shouldNotHaveExitValue(0);
        output.reportDiagnosticSummary();
        // In rare cases the target wakes up and performs the handshake at the same time as we timeout.
        // Therefore it's not certain the timeout will find any thread.
        output.shouldMatch("has not cleared handshake op|No thread with an unfinished handshake op");
    }

    static class Test implements Runnable {
        public static void main(String[] args) throws Exception {
            Test test = new Test();
            Thread thread = new Thread(test);
            thread.start();
            thread.join();
        }

        @Override
        public void run() {
            while (true) {
                // If there is a safepoint this thread might still be able to perform
                // it's handshake in time. Therefore we loop util failure.
                WhiteBox.getWhiteBox().waitUnsafe(100);
            }
        }
    }
}

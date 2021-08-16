/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2016 Azul Systems, Inc.  All Rights Reserved.
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
 * @test TestOnSpinWaitC1
 * @summary (x86 only) checks that java.lang.Thread.onSpinWait is intrinsified
 * @bug 8147844
 * @library /test/lib
 *
 * @requires vm.flagless
 * @requires os.arch=="x86" | os.arch=="amd64" | os.arch=="x86_64"
 * @requires vm.compiler1.enabled
 *
 * @run driver compiler.onSpinWait.TestOnSpinWaitC1
 */

package compiler.onSpinWait;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestOnSpinWaitC1 {

    public static void main(String[] args) throws Exception {

        // Test C1 compiler
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
          "-XX:+IgnoreUnrecognizedVMOptions", "-showversion",
          "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-Xbatch",
          "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions",
          "-XX:+PrintInlining", Launcher.class.getName());

        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());

        analyzer.shouldHaveExitValue(0);
        analyzer.shouldContain("java.lang.Thread::onSpinWait (1 bytes)   intrinsic");
    }

    static class Launcher {

        public static void main(final String[] args) throws Exception {
            int end = 20_000;

            for (int i=0; i < end; i++) {
                test();
            }
        }
        static void test() {
            java.lang.Thread.onSpinWait();
        }
    }
}

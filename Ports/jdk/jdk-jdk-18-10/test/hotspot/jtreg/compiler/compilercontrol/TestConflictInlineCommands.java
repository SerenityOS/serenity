/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @bug 8270459
 * @summary the last specified inlining option should overwrite all previous
 * @library /test/lib
 * @requires vm.flagless
 * @requires vm.compiler1.enabled | vm.compiler2.enabled
 *
 * @run driver compiler.compilercontrol.TestConflictInlineCommands
 */

package compiler.compilercontrol;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestConflictInlineCommands {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:CompileCommand=inline,*TestConflictInlineCommands::caller",
                "-XX:CompileCommand=dontinline,*TestConflictInlineCommands::caller",
                "-XX:CompileCommand=quiet", "-XX:CompileCommand=compileonly,*Launcher::main",
                "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining",
                Launcher.class.getName());

        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());
        analyzer.shouldHaveExitValue(0);
        analyzer.shouldContain("disallowed by CompileCommand");
        analyzer.shouldNotContain("force inline by CompileCommand");

        pb = ProcessTools.createJavaProcessBuilder(
                "-XX:CompileCommand=dontinline,*TestConflictInlineCommands::*caller",
                "-XX:CompileCommand=inline,*TestConflictInlineCommands::caller",
                "-XX:CompileCommand=quiet", "-XX:CompileCommand=compileonly,*Launcher::main",
                "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining",
                Launcher.class.getName());

        analyzer = new OutputAnalyzer(pb.start());
        analyzer.shouldHaveExitValue(0);
        analyzer.shouldContain("force inline by CompileCommand");
        analyzer.shouldNotContain("disallowed by CompileCommand");
    }

    static int sum;

    public static int caller(int a , int b) {
        return a + b;
    }

    static class Launcher {
        public static void main(String[] args) {
            for (int i = 0; i < 1000; i++) {
                for (int j = 0; j < 1000; j++) {
                    sum += caller(i, 0);
                }
            }
        }
    }
}

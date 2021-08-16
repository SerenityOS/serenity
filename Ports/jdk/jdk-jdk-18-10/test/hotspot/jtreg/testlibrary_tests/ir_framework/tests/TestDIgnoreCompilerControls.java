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
 */

package ir_framework.tests;

import compiler.lib.ir_framework.*;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

/*
 * @test
 * @requires vm.debug == true & vm.flagless
 * @summary Test -DIgnoreCompilerControls property flag.
 * @library /test/lib /
 * @run driver ir_framework.tests.TestDIgnoreCompilerControls
 */

public class TestDIgnoreCompilerControls {
    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            TestFramework.run();
        } else {
            OutputAnalyzer oa = run("true");
            oa.shouldHaveExitValue(0);
            oa = run("false");
            oa.shouldNotHaveExitValue(0);
            Asserts.assertTrue(oa.getOutput().contains("fail run"), "did not find run: " + oa.getOutput());
            Asserts.assertTrue(oa.getOutput().contains("fail check"), "did not find check" + oa.getOutput());
        }
    }

    private static OutputAnalyzer run(String flagValue) throws Exception {
        OutputAnalyzer oa;
        ProcessBuilder process = ProcessTools.createJavaProcessBuilder(
                "-Dtest.class.path=" + Utils.TEST_CLASS_PATH, "-Dtest.jdk=" + Utils.TEST_JDK,
                "-Dtest.vm.opts=-DIgnoreCompilerControls=" + flagValue,
                "ir_framework.tests.TestDIgnoreCompilerControls", flagValue);
        oa = ProcessTools.executeProcess(process);
        return oa;
    }

    @Test
    public void test() { }

    @Run(test = "test")
    @Warmup(10000)
    public void run(RunInfo info) throws NoSuchMethodException {
        if (!info.isWarmUp()) {
            // Should be compiled with -DIgnoreCompilerControls=true
            Asserts.assertTrue(WhiteBox.getWhiteBox().isMethodCompiled(getClass().getDeclaredMethod("run", RunInfo.class)), "fail run");
        }
    }

    @Test
    @Warmup(10000)
    public void test2() {}


    @Check(test = "test2")
    public void check(TestInfo info) throws NoSuchMethodException {
        if (!info.isWarmUp()) {
            // Should be compiled with -DIgnoreCompilerControls=true
            Asserts.assertTrue(WhiteBox.getWhiteBox().isMethodCompiled(getClass().getDeclaredMethod("check", TestInfo.class)), "fail check");
        }
    }
}

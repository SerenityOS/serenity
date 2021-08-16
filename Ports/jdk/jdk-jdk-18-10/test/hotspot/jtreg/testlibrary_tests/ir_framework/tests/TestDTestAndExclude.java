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

import compiler.lib.ir_framework.Check;
import compiler.lib.ir_framework.Run;
import compiler.lib.ir_framework.Test;
import compiler.lib.ir_framework.TestFramework;
import compiler.lib.ir_framework.driver.TestVMException;
import compiler.lib.ir_framework.shared.NoTestsRunException;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @requires vm.flagless
 * @summary Test -DTest and -DExclude property flag.
 * @library /test/lib /
 * @run driver ir_framework.tests.TestDTestAndExclude
 */

public class TestDTestAndExclude {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            run("good1,good2", "", "good");
            run("good1,good2", "bad1", "good");
            run("good1,bad1", "bad1", "good");
            run("good1,bad1", "bad1,good", "good");
            run("good3,bad2", "bad1,bad2", "good");
            run("goodMulti1,goodMulti2", "", "good");
            run("bad1,good1", "", "bad1");
            run("bad1,good1", "good1", "bad1");
            run("bad1,good1", "asdf", "bad1");
            run("bad2,good1", "", "runBadSingle");
            run("bad2", "runBadSingle", "runBadSingle");
            run("badMulti1,badMulti2", "", "runBadMulti");
            run("badMulti1", "", "runBadMulti");
            run("badMulti1", "badMulti2", "runBadMulti");
            run("badMulti2", "badMulti1", "runBadMulti");
            run("runBadSingle", "", "empty");
            run("runBadMulti", "", "empty");
            run("asdf", "", "empty");
            run("", "good1,good2,good3,bad1,bad2,goodMulti1,goodMulti2,badMulti1,badMulti2", "empty");
            run("asdf", "good1,good2,good3,bad1,bad2,goodMulti1,goodMulti2,badMulti1,badMulti2", "empty");
            run("bad1", "bad1", "empty");
            run("good1", "asdf,good,good1", "empty");
        } else {
            switch (args[0]) {
                case "good" -> TestFramework.run();
                case "bad1", "runBadMulti", "runBadSingle" -> {
                    try {
                        TestFramework.run();
                        throw new RuntimeException("should not reach");
                    } catch (TestVMException e) {
                        Asserts.assertTrue(e.getExceptionInfo().contains("expected " + args[0] + " exception"));
                    }
                }
                case "empty" -> {
                    try {
                        TestFramework.run();
                        throw new RuntimeException("should not reach");
                    } catch (NoTestsRunException e) {
                        // Expected
                    }
                }
                default -> throw new RuntimeException("should not reach");
            }
        }
    }

    /**
     * Create a VM and simulate as if it was a driver VM spawned by JTreg that has -DTest/DExclude set as VM or Javaopts
     */
    protected static void run(String dTest, String dExclude, String arg) throws Exception {
        System.out.println("Run -DTest=" + dTest + " -DExclude=" + dExclude + " arg=" + arg);
        OutputAnalyzer oa;
        ProcessBuilder process = ProcessTools.createJavaProcessBuilder(
                "-Dtest.class.path=" + Utils.TEST_CLASS_PATH, "-Dtest.jdk=" + Utils.TEST_JDK,
                "-Dtest.vm.opts=-DTest=" + dTest + " -DExclude=" + dExclude,
                "ir_framework.tests.TestDTestAndExclude", arg);
        oa = ProcessTools.executeProcess(process);
        oa.shouldHaveExitValue(0);
    }

    @Test
    public void good1() { }

    @Test
    public void good2() { }

    @Check(test = "good2")
    public void check2() {}

    @Test
    public void bad1() {
        throw new RuntimeException("expected bad1 exception");
    }

    @Test
    public void good3() {}

    @Test
    public void goodMulti1() {}

    @Test
    public void goodMulti2() {}

    @Run(test = "good3")
    public void runGoodSingle() {
        good3();
    }

    @Run(test = {"goodMulti1", "goodMulti2"})
    public void runGoodMulti() {
        goodMulti1();
        goodMulti2();
    }

    @Test
    public void bad2() {
    }

    @Test
    public void badMulti1() {
    }

    @Test
    public void badMulti2() {
    }

    @Run(test = "bad2")
    public void runBadSingle() {
        throw new RuntimeException("expected runBadSingle exception");
    }

    @Run(test = {"badMulti1", "badMulti2"})
    public void runBadMulti() {
        throw new RuntimeException("expected runBadMulti exception");
    }
}


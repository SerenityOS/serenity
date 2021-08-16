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
 * @test ClassInitializationTest
 * @bug 8142976
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile BadMap50.jasm
 * @run driver ClassInitializationTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

public class ClassInitializationTest {

    public static void main(String... args) throws Exception {

        // (1)
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:class+init=info",
                                                                  "-Xverify:all",
                                                                  "-Xmx128m",
                                                                  "BadMap50");
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldNotHaveExitValue(0);
        out.shouldContain("Start class verification for:");
        out.shouldContain("End class verification for:");
        out.shouldContain("Initializing");
        out.shouldContain("Verification for BadMap50 failed");
        out.shouldContain("Fail over class verification to old verifier for: BadMap50");

        // (2)
        if (Platform.isDebugBuild()) {
            pb = ProcessTools.createJavaProcessBuilder("-Xlog:class+init=info",
                                                       "-Xverify:all",
                                                       "-XX:+EagerInitialization",
                                                       "-Xmx128m",
                                                       InnerClass.class.getName());
            out = new OutputAnalyzer(pb.start());
            out.shouldContain("[Initialized").shouldContain("without side effects]");
            out.shouldHaveExitValue(0);
        }

        // (3) class+init should turn off.
        pb = ProcessTools.createJavaProcessBuilder("-Xlog:class+init=off",
                                                   "-Xverify:all",
                                                   "-Xmx128m",
                                                   "BadMap50");
        out = new OutputAnalyzer(pb.start());
        out.shouldNotHaveExitValue(0);
        out.shouldNotContain("[class,init]");
        out.shouldNotContain("Fail over class verification to old verifier for: BadMap50");

    }
    public static class InnerClass {
        public static void main(String[] args) throws Exception {
            System.out.println("Inner Class");
        }
    }
}

/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test -Xlog:methodhandles with a test that contains both a condy and indy.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile CondyIndyMathOperation.jasm
 * @compile CondyIndy.jasm
 * @run driver CondyIndyTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class CondyIndyTest {
    public static void main(String... args) throws Exception {

        // (1) methodhandles should turn on, no indy, no condy
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:methodhandles",
                                                                  "CondyIndy");
        OutputAnalyzer o = new OutputAnalyzer(pb.start());
        o.shouldHaveExitValue(0);
        o.shouldContain("[info][methodhandles");
        o.shouldNotContain("[debug][methodhandles,indy");
        o.shouldNotContain("[debug][methodhandles,condy");

        // (2) methodhandles+condy=debug only
        pb = ProcessTools.createJavaProcessBuilder("-Xlog:methodhandles+condy=debug",
                                                   "CondyIndy");
        o = new OutputAnalyzer(pb.start());
        o.shouldHaveExitValue(0);
        o.shouldNotContain("[info ][methodhandles");
        o.shouldNotContain("[debug][methodhandles,indy");
        o.shouldContain("[debug][methodhandles,condy");

        // (3) methodhandles+indy=debug only
        pb = ProcessTools.createJavaProcessBuilder("-Xlog:methodhandles+indy=debug",
                                                   "CondyIndy");
        o = new OutputAnalyzer(pb.start());
        o.shouldHaveExitValue(0);
        o.shouldNotContain("[info ][methodhandles");
        o.shouldContain("[debug][methodhandles,indy");
        o.shouldNotContain("[debug][methodhandles,condy");

        // (4) methodhandles, condy, indy all on
        pb = ProcessTools.createJavaProcessBuilder("-Xlog:methodhandles=info",
                                                   "-Xlog:methodhandles+condy=debug",
                                                   "-Xlog:methodhandles+indy=debug",
                                                   "CondyIndy");
        o = new OutputAnalyzer(pb.start());
        o.shouldHaveExitValue(0);
        o.shouldContain("[info ][methodhandles");
        o.shouldContain("[debug][methodhandles,indy");
        o.shouldContain("[debug][methodhandles,condy");
    };
}

/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test TestNullTerminatedFlags
 * @bug 6522873
 * @summary Test that the VM don't allow random junk characters at the end of valid command line flags.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestNullTerminatedFlags
 */
public class TestNullTerminatedFlags {
   public static String[] options = {
            "-Xnoclassgc",
            "-Xbatch",
            "-green",
            "-native",
            "-Xrs",
            "-Xinternalversion",
            "-Xprintflags",
            "-Xint",
            "-Xmixed",
            "-Xcomp",
            "-Xshare:dump",
            "-Xshare:on",
            "-Xshare:auto",
            "-Xshare:off",
            "-Xdebug",
            "-Xnoagent"
        };

    public static void main(String args[]) throws Exception{
        for (String option : options) {
            String testOption = option + "junk";
            ProcessBuilder pb =
                ProcessTools.createJavaProcessBuilder(testOption, "-version");
            new OutputAnalyzer(pb.start())
                    .shouldContain("Unrecognized option: " + testOption)
                    .shouldHaveExitValue(1);
        }
    }
}


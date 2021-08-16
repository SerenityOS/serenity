/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4627316 6743526
 * @summary Test option to limit direct memory allocation,
 *          various bad values fail to launch the VM
 * @requires (os.arch == "x86_64") | (os.arch == "amd64")
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *
 * @run main LimitDirectMemoryNegativeTest foo
 * @run main LimitDirectMemoryNegativeTest 10kmt
 * @run main LimitDirectMemoryNegativeTest -1
 */

import jdk.test.lib.process.ProcessTools;

public class LimitDirectMemoryNegativeTest {

    private static final String ERR = "Improperly specified VM option 'MaxDirectMemorySize=";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("missing size argument");
        }

        int exitCode = ProcessTools.executeTestJava(
                                    "-XX:MaxDirectMemorySize=" + args[0],
                                    LimitDirectMemoryNegativeTest.class.getName())
                                   .shouldContain(ERR + args[0])
                                   .getExitValue();
        if (exitCode != 1) {
            throw new RuntimeException("Unexpected exit code: " + exitCode);
        }
    }
}

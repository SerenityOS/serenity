/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6824477
 * @summary Selector.select can fail with IOException "Invalid argument" on
 *     Solaris if maximum number of file descriptors is less than 10000
 * @requires (os.family != "windows")
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        LotsOfUpdates
 * @run main LotsOfUpdatesTest
 */

import jdk.test.lib.process.ProcessTools;

public class LotsOfUpdatesTest {

    //hard limit needs to be less than 10000 for this bug
    private static final String ULIMIT_SET_CMD = "ulimit -n 2048";

    private static final String JAVA_CMD = ProcessTools.getCommandLine(
            ProcessTools.createJavaProcessBuilder(LotsOfUpdates.class.getName()));

    public static void main(String[] args) throws Throwable {
        ProcessTools.executeCommand("sh", "-c", ULIMIT_SET_CMD + " && " + JAVA_CMD)
                    .shouldHaveExitValue(0);
    }
}

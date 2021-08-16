/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4531526
 * @summary Test that more than one debuggee cannot bind to same port
 *          at the same time.
 * @library /test/lib
 *
 * @build ExclusiveBind HelloWorld
 * @run driver ExclusiveBind
 */

import jdk.test.lib.process.ProcessTools;
import lib.jdb.Debuggee;

public class ExclusiveBind {
    /*
     * - Launch a debuggee with server=y,suspend=y
     * - Parse listening port
     * - Launch a second debuggee in server=y,suspend=n with the parsed port
     * - Second debuggee should fail with an error (address already in use)
     * - For clean-up we attach to the first debuggee and resume it.
     */
    public static void main(String args[]) throws Exception {
        // launch the first debuggee
        try (Debuggee process1 = Debuggee.launcher("HelloWorld").launch("process1")) {
            // launch a second debuggee with the same address
            ProcessBuilder process2 = Debuggee.launcher("HelloWorld")
                    .setSuspended(false)
                    .setAddress(process1.getAddress())
                    .prepare();

            // get exit status from second debuggee
            int exitCode = ProcessTools.startProcess("process2", process2).waitFor();

            // if the second debuggee ran to completion then we've got a problem
            if (exitCode == 0) {
                throw new RuntimeException("Test failed - second debuggee didn't fail to bind");
            } else {
                System.out.println("Test passed - second debuggee correctly failed to bind");
            }
        }
    }
}

/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7190813 8022719
 * @summary Check for extended  RPATHs on *nixes
 * @compile -XDignore.symbol.file RunpathTest.java
 * @run main RunpathTest
 * @author ksrini
 */

import java.io.File;

public class RunpathTest extends TestHelper {

    final String elfreaderCmd;
    RunpathTest() {
        elfreaderCmd = findElfReader();
    }

    final String findElfReader() {
        String[] paths = {"/usr/sbin", "/usr/bin"};
        final String cmd = "readelf";
        for (String x : paths) {
            File p = new File(x);
            File e = new File(p, cmd);
            if (e.canExecute()) {
                return e.getAbsolutePath();
            }
        }
        System.err.println("Warning: no suitable elf reader!");
        return null;
    }

    void elfCheck(String javacmd, String expectedRpath) {
        final TestResult tr = doExec(elfreaderCmd, "-d", javacmd);
        if (!tr.matches(expectedRpath)) {
            System.out.println(tr);
            throw new RuntimeException("FAILED: RPATH/RUNPATH strings " +
                    expectedRpath + " not found in " + javaCmd);
        }
        System.out.println(javacmd + " contains expected RPATHS/RUNPATH");
    }

    void testRpath() {
        String expectedRpath = ".*R(UN)?PATH.*\\$ORIGIN/../lib.*";
        elfCheck(javaCmd, expectedRpath);
    }

    public static void main(String... args) throws Exception {
        if (isLinux) {
            RunpathTest rp = new RunpathTest();
            if (rp.elfreaderCmd == null) {
                System.err.println("Warning: test passes vacuously");
                return;
            }
            rp.testRpath();
        }
    }
}

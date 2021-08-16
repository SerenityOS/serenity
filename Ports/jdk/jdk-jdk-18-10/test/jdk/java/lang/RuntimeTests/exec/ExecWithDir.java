/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4750978
 * @run main/othervm/timeout=300 ExecWithDir
 * @summary Ensure that we can fork-and-exec repeatedly when a new working
 *          directory is specified
 */

import java.io.File;

public class ExecWithDir {

    private static final int N = 500;

    public static void main(String args[]) throws Exception {
        if (! UnixCommands.isUnix) {
            System.out.println("For UNIX only");
            return;
        }
        UnixCommands.ensureCommandsAvailable("true");

        final String trueCmd = UnixCommands.findCommand("true");
        File dir = new File(".");
        for (int i = 1; i <= N; i++) {
            System.out.print(i);
            System.out.print(" e");
            Process p = Runtime.getRuntime().exec(trueCmd, null, dir);
            System.out.print('w');
            int s = p.waitFor();
            System.out.println("x " + s);
            if (s != 0) throw new Error("Unexpected return code " + s);

            // Avoid "Too many open files"
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        }
    }
}

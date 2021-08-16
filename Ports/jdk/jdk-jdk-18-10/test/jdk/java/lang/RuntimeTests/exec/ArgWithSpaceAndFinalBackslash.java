/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4794652
 * @summary Ensure that a command argument that contains a space and a final
 *          backslash is handled correctly
 */

import java.io.*;
import java.util.*;


public class ArgWithSpaceAndFinalBackslash {

    private static String getJavaCommand() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && javaHome.length() > 0)
            return (javaHome
                    + File.separatorChar + "bin"
                    + File.separatorChar + "java");
        else
            return "java";
    }

    public static void main(String[] args) throws Exception {

        if (args.length > 0) {
            System.err.println(args[0]);
            return;
        }

        String[] cmd = new String[5];
        int i = 0;
        cmd[i++] = getJavaCommand();
        cmd[i++] = "-cp";
        String cp = System.getProperty("test.classes");
        if (cp == null)
            cp = ".";
        cmd[i++] = cp;
        cmd[i++] = "ArgWithSpaceAndFinalBackslash";
        cmd[i++] = "foo bar\\baz\\";

        Process process = Runtime.getRuntime().exec(cmd);
        InputStream in = process.getErrorStream();
        byte[] buf = new byte[1024];
        int n = 0, d;
        while ((d = in.read(buf, n, buf.length - n)) >= 0)
            n += d;
        String s = new String(buf, 0, n, "US-ASCII").trim();
        if (!s.equals(cmd[i - 1]))
            throw new Exception("Test failed: Got \"" + s
                                + "\", expected \"" + cmd[i - 1] + "\"");
    }

}

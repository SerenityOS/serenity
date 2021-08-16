/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

/*
 * Wrapper for the EarlyAssert test to run the test in a JVM without assertions
 * enabled.
 */
public class EarlyAssertWrapper {
    public static void main(String... args) throws Exception {
        EarlyAssertWrapper w = new EarlyAssertWrapper();
        w.run();
    }

    void run() throws Exception {
        List<String> cmd = new ArrayList<String>();
        File java_home = new File(System.getProperty("java.home"));
        cmd.add(new File(new File(java_home, "bin"), "java").getPath());

        // propogate classpath
        cmd.add("-classpath");
        cmd.add(System.getProperty("java.class.path"));

        // ensure all assertions disabled in target VM
        cmd.add("-da");
        cmd.add("-dsa");

        cmd.add("EarlyAssert");

        System.err.println("Running command: " + cmd);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getOutputStream().close();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        String line;
        DataInputStream in = new DataInputStream(p.getInputStream());
        try {
            while ((line = in.readLine()) != null) {
                if (!line.matches("^Picked up .*JAVA.*OPTIONS:.*")) {
                    pw.println(line);
                }
            }
        } finally {
            in.close();
        }
        pw.close();

        String out = sw.toString();
        int rc = p.waitFor();
        if (rc != 0 || out.length() > 0)
            throw new Error("failed: rc=" + rc + (out.length() > 0 ? ": " + out : ""));
    }
}

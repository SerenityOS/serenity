/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027411 8032869
 * @summary test an invalid option
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.zip.*;

public class InvalidOptions {
    int errorCount;
    String log;

    public static void main(String[] args) throws Exception {
        new InvalidOptions().run();
    }

    void run() throws Exception {
        test(2, "-b", "Error: unknown option: -b",
                      "Usage: javap <options> <classes>",
                      "use --help for a list of possible options");
        if (errorCount > 0)
            throw new Exception(errorCount + " errors received");
    }

    void test(int expect, String option, String ... expectedOutput) {
        String output = runJavap(expect, option);
        verify(output, expectedOutput);
    }

    String runJavap(int expect, String... option) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(option, pw);
        pw.close();
        System.out.println("javap prints:");
        System.out.println(sw);
        if (rc != expect)
           throw new Error("Expect to return " + expect + ", but return " + rc);
        return sw.toString();
    }

    void verify(String output, String... expects) {
        for (String expect: expects) {
            if (!output.contains(expect))
                error(expect + " not found");
        }
    }

    void error(String msg) {
        System.err.println(msg);
        errorCount++;
    }
}

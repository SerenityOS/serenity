/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6986895
 * @bug 8201544
 * @summary compiler gives misleading message for no input files
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;

public class T6986895 {
    public static void main(String... args) throws Exception {
        new T6986895().run();
    }

    String noSourceFiles = "no source files";
    String noSourceFilesOrClasses = "no source files or class names";
    String invalidFileName = "Invalid filename";
    boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    void run() throws Exception {
        Locale prev = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            test(noSourceFiles,           "-Werror");
            test(noSourceFilesOrClasses,  "-Werror", "-Xprint");
            if (isWindows)
                test(invalidFileName, "-Werror", "someNonExistingFile*.java");
        } finally {
            Locale.setDefault(prev);
        }
    }

    void test(String expect, String... args) throws Exception {
        System.err.println("Test " + expect + ": " + Arrays.asList(args));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        System.err.println("compilation failed; rc=" + rc);
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (!out.contains(expect))
            throw new Exception("expected text not found: " + expect);
        System.err.println();
    }
}

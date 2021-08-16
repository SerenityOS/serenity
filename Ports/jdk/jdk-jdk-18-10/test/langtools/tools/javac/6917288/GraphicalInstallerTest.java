/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6917288
 * @summary Unnamed nested class is not generated
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;

public class GraphicalInstallerTest {
    public static void main(String... args) throws Exception {
        new GraphicalInstallerTest().run();
    }

    void run() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File classes = new File("classes");
        classes.mkdirs();
        List<String> opts = Arrays.asList("-d", classes.getPath());
        int rc = compile(opts, new File(testSrc, "GraphicalInstaller.java"));
        if (rc != 0) {
            error("compilation failed: rc=" + rc);
            return;
        }

        check(classes,
            "GraphicalInstaller$1X$1.class",
            "GraphicalInstaller$1X.class",
            "GraphicalInstaller$BackgroundInstaller.class",
            "GraphicalInstaller.class");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    /**
     *  Compile files with given options.
     *  Display any output from compiler, and return javac return code.
     */
    int compile(List<String> opts, File... files) {
        List<String> args = new ArrayList<String>();
        args.addAll(opts);
        for (File f: files)
            args.add(f.getPath());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]), pw);
        pw.close();
        String out = sw.toString();
        if (out.length() > 0)
            System.err.println(out);
        return rc;
    }

    /**
     *  Check that a directory contains the expected files.
     */
    void check(File dir, String... paths) {
        Set<String> found = new TreeSet<String>(Arrays.asList(dir.list()));
        Set<String> expect = new TreeSet<String>(Arrays.asList(paths));
        if (found.equals(expect))
            return;
        for (String f: found) {
            if (!expect.contains(f))
                error("Unexpected file found: " + f);
        }
        for (String e: expect) {
            if (!found.contains(e))
                error("Expected file not found: " + e);
        }
    }

    /**
     *  Record an error message.
     */
    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;
}

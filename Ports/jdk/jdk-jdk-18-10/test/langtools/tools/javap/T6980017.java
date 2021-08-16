/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6980017
 * @summary javap -XDdetail:source behaves badly if source not available.
 * @modules jdk.compiler
 *          jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;

public class T6980017 {
    public static void main(String... args) throws Exception {
        new T6980017().run();
    }

    void run() throws Exception {

        String[] args = {
            "-v",
            "-XDdetails:source",
            "java.lang.Object"
        };

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, pw);
        pw.close();
        if (rc != 0)
            error("Unexpected exit code: " + rc);

        boolean foundBlankSourceLine = false;
        boolean foundNoSourceLine = false;
        for (String line: sw.toString().split("[\r\n]+")) {
            System.err.println(line);
            if (line.contains("Source code not available"))
                foundNoSourceLine = true;
            if (line.matches("\\s*\\( *[0-9]+\\)\\s*"))
                foundBlankSourceLine = true;
        }

        if (foundBlankSourceLine)
            error("found blank source lines");

        if (!foundNoSourceLine)
            error("did not find \"Source code not available\" message");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;
}

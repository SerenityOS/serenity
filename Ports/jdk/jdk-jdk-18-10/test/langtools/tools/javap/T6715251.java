/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6715251
 * @summary javap should be consistent with javac and return 2 if given no arguments
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;

public class T6715251 {
    public static void main(String... args) throws Exception {
        new T6715251().run();
    }

    void run() throws Exception {
        String testClasses = System.getProperty("test.classes", ".");

        test(2);
        test(0, "-help");
        test(0, "-version");
        test(0, "-fullversion");
        test(0, "-classpath", testClasses, "T6715251");

        if (errors > 0)
            throw new Exception(errors + " errors received");
    }

    void test(int expect, String ... args) {
        int rc = javap(args);
        if (rc != expect)
            error("bad result: expected: " + expect + ", found " + rc + "\n"
                  + log);

    }

    int javap(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, pw);
        log = sw.toString();
        return rc;
    }

    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    String log;
    int errors;
}

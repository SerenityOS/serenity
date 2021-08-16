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
 * @bug 6937244
 * @summary fields display with JVMS names, not Java names
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;

public class T6937244A {
    public static void main(String[] args) throws Exception {
        new T6937244A().run();
    }

    void run() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] args = { "Test" };
        int rc = com.sun.tools.javap.Main.run(args, pw);
        pw.close();
        String out = sw.toString();
        System.err.println(out);
        if (rc != 0)
            throw new Exception("unexpected exit from javap: " + rc);

        int count = 0;

        for (String line: out.split("[\r\n]+")) {
            if (line.contains("implements")) {
                verify(line, "implements java.util.List<java.lang.String>");
                count++;
            }

            if (line.contains("field")) {
                verify(line, "java.util.List<java.lang.String> field");
                count++;
            }

            if (line.contains("method")) {
                verify(line, "java.util.List<java.lang.String> method(java.util.List<java.lang.String>) throws java.lang.Exception");
                count++;
            }
        }

        // final backstop check
        if (out.contains("/"))
            throw new Exception("unexpected \"/\" in output");

        if (count != 3)
            throw new Exception("wrong number of matches found: " + count);
    }

    void verify(String line, String expect) throws Exception {
        if (!line.contains(expect)) {
            System.err.println("line:   " + line);
            System.err.println("expect: " + expect);
            throw new Exception("expected string not found in line");
        }
    }
}


abstract class Test implements List<String> {
    public List<String> field;
    public List<String> method(List<String> arg) throws Exception { return null; }
}


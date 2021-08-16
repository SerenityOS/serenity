/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006037
 * @summary extra space in javac -help for -J and @ options
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;

public class Option_J_At_Test {
    public static void main(String... args) throws Exception {
        new Option_J_At_Test().run();
    }

    void run() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] help = { "-help" };
        int rc = com.sun.tools.javac.Main.compile(help, pw);
        pw.flush();
        String out = sw.toString();
        System.out.println(out);
        check(out, "-J<flag>",     true);
        check(out, "-J <flag>",    false);
        check(out, "@<filename>",  true);
        check(out, "@ <filename>", false);
        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void check(String out, String text, boolean expect) {
        if (out.contains(text) != expect) {
            if (expect)
                error("expected string not found: " + text);
            else
                error("unexpected string found: " + text);
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}

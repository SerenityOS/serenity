/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6668802
 * @summary javac handles diagnostics for last line badly, if line not terminated by newline
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;

public class T6668802
{
    public static void main(String[] args) throws Exception {
        new T6668802().run();
    }

    void run() throws Exception {
        String test = "public class Test {";
        File f = writeTestFile("Test.java", test);
        String[] out = compileBadFile(f);
        for (String line: out)
            System.err.println(">>>" + line + "<<<");
        if (!out[1].equals(test)) {
            show("expected", test);
            show("  actual", out[1]);
            throw new Error("test failed");
        }
    }

    File writeTestFile(String path, String contents) throws IOException {
        File f = new File(path);
        FileWriter out = new FileWriter(f);
        out.write(contents);
        out.close();
        return f;
    }

    String[] compileBadFile(File file) throws IOException {
        List<String> options = new ArrayList<String>();
        options.add(file.getPath());
        System.err.println("compile: " + options);
        String[] opts = options.toArray(new String[options.size()]);
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(opts, out);
        if (rc == 0)
            throw new Error("compilation succeeded unexpectedly");
        out.close();
        return sw.toString().split("[\n\r]+");
    }

    void show(String prefix, String text) {
        System.err.println(prefix + ": (" + text.length() + ") " + text);
    }
}

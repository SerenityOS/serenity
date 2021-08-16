/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7010608
 * @summary the string 'error' should appear in error messages
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.*;
import java.net.URI;
import java.util.*;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;

public class Test {
    public static void main(String... args) throws Exception {
        new Test().run();
    }

    void run() throws Exception {
        Locale prev = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        try {
            test(Arrays.<String>asList(),
                    "myfo://test:1: error: cannot find symbol");
            test(Arrays.asList("--diags=layout=OLD"),
                    "myfo://test:1: cannot find symbol");
            test(Arrays.asList("--diags=legacy"),
                    "myfo://test:1: cannot find symbol");
        } finally {
            Locale.setDefault(prev);
        }
    }

    void test(List<String> options, String expect) throws Exception {
        System.err.println("test: " + options);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        JavaFileObject f = new MyFileObject("myfo://test", "class Bad { Missing x; }");
        List<? extends JavaFileObject> files = Arrays.asList(f);
        CompilationTask task = javac.getTask(pw, null, null, options, null, files);
        boolean ok = task.call();
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (ok)
            throw new Exception("Compilation succeeded unexpectedly");
        if (!out.contains(expect))
            throw new Exception("expected text not found: " + expect);
    }

    class MyFileObject extends SimpleJavaFileObject {
        MyFileObject(String uri, String text) {
            super(URI.create(uri), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public String getName() {
            return uri.toString();
        }
        @Override
        public String getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
        final String text;
    }
}



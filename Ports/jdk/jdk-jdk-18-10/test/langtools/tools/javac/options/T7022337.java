/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7022337
 * @summary repeated warnings about bootclasspath not set
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor T7022337
 * @run main T7022337
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;

public class T7022337 extends JavacTestingAbstractProcessor {
    public static void main(String... args) throws Exception {
        new T7022337().run();
    }

    void run() throws Exception {
        String myName = T7022337.class.getSimpleName();
        File testSrc = new File(System.getProperty("test.src"));
        File file = new File(testSrc, myName + ".java");

        String out = compile(
            "-XDrawDiagnostics",
            "-d", ".",
            "-processor", myName,
            "-source", "8", // explicit use of older source value without bootclasspath
            file.getPath());

        int count = 0;
        for (String line: out.split("[\r\n]+")) {
            if (line.contains("compiler.warn.source.no.bootclasspath"))
                count++;
        }
        if (count != 1)
            throw new Exception("unexpected number of warnings found: " + count + ", expected: 1");
    }

    String compile(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Exception("compilation failed unexpectedly: rc=" + rc);
        return out;
    }

    // ----------

    int round = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        round++;

        final int MAXROUNDS = 3;
        if (round < MAXROUNDS)
            generate("Gen" + round);

        return true;
    }

    void generate(String name) {
        try {
            JavaFileObject fo = filer.createSourceFile(name);
            Writer out = fo.openWriter();
            try {
                out.write("class " + name + " { }");
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}

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
 * @bug 8022163
 * @summary javac exits with 0 status and no messages on error to construct an ann-procesor
 * @modules jdk.compiler
 */

import java.io.*;

public class TestBadProcessor {
    public static void main(String... args) throws Exception {
        new TestBadProcessor().run();
    }

    public static final String badAnnoProcSrc =
        "import java.util.*;\n" +
        "import javax.annotation.processing.*;\n" +
        "import javax.lang.model.element.*;\n" +

        "public class AnnoProc extends AbstractProcessor {\n" +
        "    public AnnoProc() {\n" +
        "        throw new Error();\n" +
        "    }\n" +

        "    public boolean process(Set<? extends TypeElement> elems, \n" +
        "                        RoundEnvironment rEnv) {\n" +
        "        return false;\n" +
        "    }\n" +
        "}\n";

    public void run() throws Exception {
        // setup
        File srcDir = new File("src");
        File classesDir = new File("classes");
        classesDir.mkdirs();
        File srcFile = writeFile(srcDir, "AnnoProc.java", badAnnoProcSrc);
        compile("-d", classesDir.getPath(), srcFile.getPath());
        writeFile(classesDir, "META-INF/services/javax.annotation.processing.Processor", "AnnoProc");

        // run the primary compilation
        int rc;
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            String[] args = { "-processorpath", classesDir.getPath(), srcFile.getPath() };
            rc = com.sun.tools.javac.Main.compile(args, pw);
        }

        // verify that it failed as expected, with the expected message
        String out = sw.toString();
        System.err.println(out);
        String expect = "error: Bad service configuration file, " +
                "or exception thrown while constructing Processor object: " +
                "javax.annotation.processing.Processor: " +
                "Provider AnnoProc could not be instantiated\n" +
                "1 error";
        String lineSeparator = System.getProperty("line.separator");
        if (!out.trim().replace(lineSeparator, "\n").equals(expect)) {
            System.err.println("expected: " + expect);
            error("output not as expected");
        }

        if (rc == 0) {
            error("unexpected exit code: " + rc + "; expected: not zero");
        }

        // summary
        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void compile(String... args) throws Exception {
        int rc;
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            rc = com.sun.tools.javac.Main.compile(args, pw);
        }
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Exception("compilation failed");
    }

    File writeFile(File dir, String path, String body) throws IOException {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        try (FileWriter out = new FileWriter(f)) {
            out.write(body);
        }
        return f;
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}

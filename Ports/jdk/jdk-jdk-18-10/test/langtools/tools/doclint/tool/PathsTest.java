/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006263
 * @summary Supplementary test cases needed for doclint
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.doclint
 * @build toolbox.ToolBox toolbox.JarTask
 * @run main PathsTest
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;
import javax.tools.StandardLocation;
import javax.tools.JavaFileManager;
import javax.tools.ToolProvider;

import jdk.javadoc.internal.doclint.DocLint;
import jdk.javadoc.internal.doclint.DocLint.BadArgs;

import toolbox.JarTask;
import toolbox.ToolBox;

public class PathsTest {
    public static void main(String... args) throws Exception {
        new PathsTest().run();
    }

    void run() throws Exception {
        String PS = File.pathSeparator;
        writeFile("src1/p/A.java",
                "package p; public class A { }");
        compile("-d", "classes1", "src1/p/A.java");

        writeFile("src2/q/B.java",
                "package q; public class B extends p.A { }");
        compile("-d", "classes2", "-classpath", "classes1", "src2/q/B.java");

        writeFile("src/Test.java",
                "/** &0; */ class Test extends q.B { }");

        test("src/Test.java", "-sourcepath", "src1" + PS + "src2");
        test("src/Test.java", "-classpath", "classes1" + PS + "classes2");

        File testJar = createJar();
        test("src/Test.java", "-bootclasspath",
                testJar + PS + "classes1" + PS + "classes2");

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    Pattern pkgNotFound = Pattern.compile("package [a-z]+ does not exist");
    Pattern badHtmlEntity = Pattern.compile("bad HTML entity");

    void test(String file, String pathOpt, String path) throws BadArgs, IOException {
        System.err.println("test " + pathOpt);
        String out1 = doclint("-Xmsgs", file);
        if (!pkgNotFound.matcher(out1).find())
            error("message not found: " + pkgNotFound);

        String out2;
        if (needTarget8(pathOpt)) {
            out2 = doclint("-Xmsgs", "-source", "8", "-target", "8", pathOpt, path, file);
        } else {
            out2 = doclint("-Xmsgs", pathOpt, path, file);
        }
        if (pkgNotFound.matcher(out2).find())
            error("unexpected message found: " + pkgNotFound);
        if (!badHtmlEntity.matcher(out1).find())
            error("message not found: " + badHtmlEntity);

        try {
            doclint("-Xmsgs", pathOpt);
            error("expected exception not thrown");
        } catch (BadArgs e) {
            System.err.println(e);
        }
    }

    boolean needTarget8(String opt) {
        switch (opt) {
            case "-bootclasspath":
                return true;
            default:
                return false;
        }
    }

    File createJar() throws IOException {
        File f = new File("test.jar");
        try (JavaFileManager fm = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null)) {
            ToolBox tb = new ToolBox();
            new JarTask(tb, f.getPath())
                .files(fm, StandardLocation.PLATFORM_CLASS_PATH, "java.lang.*")
                .run();
        }
        return f;
    }

    void compile(String... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-d")) {
                new File(args[++i]).mkdirs();
                break;
            }
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            error("compilation failed: rc=" + rc);
    }

    String doclint(String... args) throws BadArgs, IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DocLint dl = new DocLint();
        dl.run(pw, args);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        return out;
    }

    File writeFile(String path, String body) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(path)) {
            fw.write(body);
        }
        return f;
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}

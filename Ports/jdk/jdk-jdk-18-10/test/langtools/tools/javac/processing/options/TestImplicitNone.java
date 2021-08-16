/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6935638
 * @summary -implicit:none prevents compilation with annotation processing
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;


@SupportedAnnotationTypes("*")
public class TestImplicitNone extends AbstractProcessor {
    public static void main(String... args) throws Exception {
        new TestImplicitNone().run();
    }

    void run() throws Exception {
        File classesDir = new File("tmp", "classes");
        classesDir.mkdirs();
        File test_java = new File(new File("tmp", "src"), "Test.java");
        writeFile(test_java, "class Test { }");

        // build up list of options and files to be compiled
        List<String> opts = new ArrayList<String>();
        List<File> files = new ArrayList<File>();

        opts.add("-d");
        opts.add(classesDir.getPath());
        opts.add("-processorpath");
        opts.add(System.getProperty("test.classes"));
        opts.add("-implicit:none");
        opts.add("-processor");
        opts.add(TestImplicitNone.class.getName());
        files.add(test_java);

        compile(opts, files);

        File test_class = new File(classesDir, "Test.class");
        if (!test_class.exists())
            throw new Exception("Test.class not generated");
    }

    /** Compile files with options provided. */
    void compile(List<String> opts, List<File> files) throws Exception {
        System.err.println("javac: " + opts + " " + files);
        List<String> args = new ArrayList<String>();
        args.addAll(opts);
        for (File f: files)
            args.add(f.getPath());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]), pw);
        pw.flush();
        if (sw.getBuffer().length() > 0)
            System.err.println(sw.toString());
        if (rc != 0)
            throw new Exception("compilation failed: rc=" + rc);
    }

    /** Write a file with a given body. */
    void writeFile(File f, String body) throws Exception {
        if (f.getParentFile() != null)
            f.getParentFile().mkdirs();
        Writer out = new FileWriter(f);
        try {
            out.write(body);
        } finally {
            out.close();
        }
    }

    //----- annotation processor methods -----

    public boolean process(Set<? extends TypeElement> annotations,
                         RoundEnvironment roundEnv) {
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}

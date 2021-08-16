/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6350124 6410012
 * @modules java.compiler
 *          jdk.compiler
 * @summary javac -s does not have the generated source files
 */

import java.io.File;
import java.io.PrintWriter;
import javax.tools.Tool;
import javax.tools.ToolProvider;

public class T6350124 {
    public static void main(String[] args) {
        String classpath = System.getProperty("java.class.path");
        File srcDir = new File(System.getProperty("test.src"));

        // ensure the output directories are empty
        mkCleanDir(new File("aClasses"));
        mkCleanDir(new File("newClasses"));
        mkCleanDir(new File("newSrc"));

        // compile the annotation processor
        compile("-cp", classpath,
                "-d", "aClasses", path(srcDir, "HelloWorldAP.java"));

        // compile the test program, invoking the anotation processor
        compile("-cp", classpath,
                "-sourcepath", srcDir.getPath(),
                "-d", "newClasses",
                "-s", "newSrc",
                "-processorpath", "aClasses",
                "-processor", "HelloWorldAP", // specify processor for simplicity
                "-proc:only",
                path(srcDir, "Marked.java"));

        File hw = new File("newSrc", "HelloWorld.java");
        if (!hw.exists())
            throw new AssertionError("generated source file not found");

        File dc = new File("newClasses", "HelloWorldAP.class");
        if (!dc.exists())
            throw new AssertionError("generated class file not found");
    }

    //--- the following can be considered "library code" for the test

    // note: jtreg @clean will only clean class files; not source files
    static void clean(File file) {
        if (!file.exists())
            return;
        if (file.isDirectory()) {
            for (File f: file.listFiles())
                clean(f);
        }
        file.delete();
    }

    static void mkCleanDir(File dir) {
        clean(dir);
        dir.mkdirs();
    }

    // note: jtreg @compile does not allow -d to be specified
    static void compile(String... args) {
        StringBuffer sb = new StringBuffer("compile:");
        for (String a: args)
            sb.append(' ').append(a);
        System.err.println(sb);

        Tool t = ToolProvider.getSystemJavaCompiler();
        int rc = t.run(System.in, System.out, System.err, args);
        System.out.flush();
        System.err.flush();
        if (rc != 0)
            throw new Error("compilation failed");
    }

    static String path(File dir, String name) {
        return new File(dir, name).getPath();
    }
}

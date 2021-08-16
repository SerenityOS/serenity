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
 * @bug 6400872
 * @summary REGRESSION:  Java Compiler cannot find jar files referenced by other
 * @modules java.compiler
 *          jdk.compiler
 * @run main T6400872
 */

// ${TESTJAVA}/bin/javac -d ${TESTCLASSES} ${TESTSRC}/A.java ${TESTSRC}/B.java
// ${TESTJAVA}/bin/jar -cfm A.jar ${TESTSRC}/A/META-INF/MANIFEST.MF -C ${TESTCLASSES} A.class
// ${TESTJAVA}/bin/jar -cfm B.jar ${TESTSRC}/B/META-INF/MANIFEST.MF -C ${TESTCLASSES} B.class
// ${TESTJAVA}/bin/javac -cp A.jar ${TESTSRC}/C.java

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.*;
import javax.tools.StandardJavaFileManager.*;

public class T6400872 {
    static File testSrc = new File(System.getProperty("test.src", "."));
    static File testClasses = new File(System.getProperty("test.classes", "."));

    public static void main(String... args) throws Exception {
        // compile A.java and B.java
        compile(testClasses, null, new File(testSrc, "A.java"), new File(testSrc, "B.java"));
        // put them in mutually referential class files
        jar(new File("A.jar"), iterable(new File(".", "B.jar")), testClasses, new File("A.class"));
        jar(new File("B.jar"), iterable(new File(".", "A.jar")), testClasses, new File("B.class"));
        // verify we can successfully use the class path entries in the jar files
        compile(new File("."), iterable(new File("A.jar")), new File(testSrc, "C.java"));
    }

    static void compile(File classOutDir, Iterable<File> classPath, File... files)
                throws IOException {
        System.err.println("compile...");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> fileObjects =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(files));

            List<String> options = new ArrayList<String>();
            if (classOutDir != null) {
                options.add("-d");
                options.add(classOutDir.getPath());
            }
            if (classPath != null) {
                options.add("-classpath");
                options.add(join(classPath, File.pathSeparator));
            }
            options.add("-verbose");

            JavaCompiler.CompilationTask task =
                compiler.getTask(null, fm, null, options, null, fileObjects);
            if (!task.call())
                throw new AssertionError("compilation failed");
        }
    }

    static void jar(File jar, Iterable<File> classPath, File base, File... files)
            throws IOException {
        System.err.println("jar...");
        Manifest m = new Manifest();
        if (classPath != null) {
            Attributes mainAttrs = m.getMainAttributes();
            mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            mainAttrs.put(Attributes.Name.CLASS_PATH, join(classPath, " "));
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(jar));
        JarOutputStream j = new JarOutputStream(out, m);
        add(j, base, files);
        j.close();
    }

    static void add(JarOutputStream j, File base, File... files) throws IOException {
        if (files == null)
            return;

        for (File f: files)
            add(j, base, f);
    }

    static void add(JarOutputStream j, File base, File file) throws IOException {
        File f = new File(base, file.getPath());
        if (f.isDirectory()) {
            String[] children = f.list();
            if (children != null)
                for (String c: children)
                    add(j, base, new File(file, c));
        } else {
            JarEntry e = new JarEntry(file.getPath());
            e.setSize(f.length());
            j.putNextEntry(e);
            j.write(read(f));
            j.closeEntry();
        }

    }

    static byte[] read(File f) throws IOException {
        byte[] buf = new byte[(int) f.length()];
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        int offset = 0;
        while (offset < buf.length) {
            int n = in.read(buf, offset, buf.length - offset);
            if (n < 0)
                throw new EOFException();
            offset += n;
        }
        return buf;
    }

    static <T> Iterable<T> iterable(T single) {
        return Collections.singleton(single);
    }

    static <T> String join(Iterable<T> iter, String sep) {
        StringBuilder p = new StringBuilder();
        for (T t: iter) {
            if (p.length() > 0)
                p.append(' ');
            p.append(t);
        }
        return p.toString();
    }
}

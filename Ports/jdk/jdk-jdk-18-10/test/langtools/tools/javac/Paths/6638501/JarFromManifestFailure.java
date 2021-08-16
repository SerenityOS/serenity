/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6638501
 * @summary REGRESSION:  Java Compiler cannot find jar files referenced by other
 * @modules jdk.compiler
 * @run main JarFromManifestFailure
 */

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.*;
import javax.tools.StandardJavaFileManager.*;

public class JarFromManifestFailure {
    static File testSrc = new File(System.getProperty("test.src", "."));
    static File testClasses = new File(System.getProperty("test.classes", "."));

    public static void main(String... args) throws Exception {
        compile(testClasses, null, new File(testSrc, "HelloLib/test/HelloImpl.java"), new File(testSrc, "WsCompileExample.java"));
        File libFile = new File(testClasses, "lib");
        libFile.mkdir();
        jar(new File(libFile, "HelloLib.jar"), new ArrayList(), testClasses, new File("test"));

        ArrayList arList = new ArrayList();
        arList.add(new File("HelloLib.jar"));
        jar(new File(libFile, "JarPointer.jar"), arList, testClasses);

        String[] args1 = {
            "-d", ".",
            "-cp", new File(libFile, "JarPointer.jar").getPath().replace('\\', '/'),
            new File(testSrc, "test/SayHello.java").getPath().replace('\\', '/')
        };
        System.err.println("First compile!!!");
        if (com.sun.tools.javac.Main.compile(args1) != 0) {
            throw new AssertionError("Failure in first compile!");
        }

        System.err.println("Second compile!!!");

        args1 = new String[] {
            "-d", ".",
            "-cp", new File(libFile, "JarPointer.jar").getPath().replace('\\', '/'),
            new File(testSrc, "test1/SayHelloToo.java").getPath().replace('\\', '/')
        };
        if (com.sun.tools.javac.Main.compile(args1) != 0) {
            throw new AssertionError("Failure in second compile!");
        }
    }

    static void compile(File classOutDir, Iterable<File> classPath, File... files) throws IOException {
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
            JarEntry e = new JarEntry(new String(file.getPath() + File.separator).replace('\\', '/'));
            e.setSize(file.length());
            j.putNextEntry(e);
            String[] children = f.list();
            if (children != null) {
                for (String c: children) {
                    add(j, base, new File(file, c));
                }
            }
        } else {
            JarEntry e = new JarEntry(file.getPath().replace('\\', '/'));
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

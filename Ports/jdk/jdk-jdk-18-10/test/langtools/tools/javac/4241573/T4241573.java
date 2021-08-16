/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4241573
 * @summary SourceFile attribute includes full path
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.SourceFile_attribute;
import java.io.*;
import java.util.*;
import java.util.jar.*;

public class T4241573 {
    public static void main(String... args) throws Exception {
        new T4241573().run();
    }

    public void run() throws Exception {
        // Selection of files to be compiled
        File absJar = createJar(new File("abs.jar").getAbsoluteFile(), "j.A");
        File relJar = createJar(new File("rel.jar"), "j.R");
        File absDir = createDir(new File("abs.dir").getAbsoluteFile(), "d.A");
        File relDir = createDir(new File("rel.dir"), "d.R");
        File absTestFile = writeFile(new File("AbsTest.java").getAbsoluteFile(), "class AbsTest { class Inner { } }");
        File relTestFile = writeFile(new File("RelTest.java"), "class RelTest { class Inner { } }");
        File relTest2File = writeFile(new File("p/RelTest2.java"), "package p; class RelTest2 { class Inner { } }");
        // This next class references other classes that will be found on the source path
        // and which will therefore need to be compiled as well.
        File mainFile = writeFile(new File("Main.java"),
                "class Main { j.A ja; j.R jr; d.A da; d.R dr; }" +
                "");

        String sourcePath = createPath(absJar, relJar, absDir, relDir);
        File outDir = new File("classes");
        outDir.mkdirs();

        String[] args = {
            "-sourcepath", sourcePath,
            "-d", outDir.getPath(),
            absTestFile.getPath(),
            relTestFile.getPath(),
            relTest2File.getPath(),
            mainFile.getPath(),
        };
        System.err.println("compile: " + Arrays.asList(args));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        if (rc != 0) {
            System.err.println(sw.toString());
            throw new Exception("unexpected exit from javac: " + rc);
        }

        Set<File> expect = getFiles(outDir,
            "d/A.class",        "d/A$Inner.class",
            "d/R.class",        "d/R$Inner.class",
            "j/A.class",        "j/A$Inner.class",
            "j/R.class",        "j/R$Inner.class",
            "AbsTest.class",    "AbsTest$Inner.class",
            "RelTest.class",    "RelTest$Inner.class",
            "p/RelTest2.class", "p/RelTest2$Inner.class",
            "Main.class" );

        Set<File> found = findFiles(outDir);

        if (!found.equals(expect)) {
            if (found.containsAll(expect))
                throw new Exception("unexpected files found: " + diff(found, expect));
            else if (expect.containsAll(found))
                throw new Exception("expected files not found: " + diff(expect, found));
        }

        for (File f: found)
            verifySourceFileAttribute(f);

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    /** Check the SourceFileAttribute is the simple name of the original source file. */
    void verifySourceFileAttribute(File f) {
        System.err.println("verify: " + f);
        try {
            ClassFile cf = ClassFile.read(f);
            SourceFile_attribute sfa = (SourceFile_attribute) cf.getAttribute(Attribute.SourceFile);
            String found = sfa.getSourceFile(cf.constant_pool);
            String expect = f.getName().replaceAll("([$.].*)?\\.class", ".java");
            if (!expect.equals(found)) {
                error("bad value found: " + found + ", expected: " + expect);
            }
        } catch (Exception e) {
            error("error reading " + f +": " + e);
        }
    }

    /** Create a directory containing one or more files. */
    File createDir(File dir, String... entries) throws Exception {
        if (!dir.mkdirs())
            throw new Exception("cannot create directories " + dir);
        for (String e: entries) {
            writeFile(new File(dir, getPathForDirEntry(e)), getBodyForEntry(e));
        }
        return dir;
    }

    /** Create a jar file containing one or more entries. */
    File createJar(File jar, String... entries) throws IOException {
        OutputStream out = new FileOutputStream(jar);
        try {
            JarOutputStream jos = new JarOutputStream(out);
            for (String e: entries) {
                jos.putNextEntry(new JarEntry(getPathForZipEntry(e)));
                jos.write(getBodyForEntry(e).getBytes());
            }
            jos.close();
        } finally {
            out.close();
        }
        return jar;
    }

    /** Return the path for an entry given to createDir */
    String getPathForDirEntry(String e) {
        return e.replace(".", File.separator) + ".java";
    }

    /** Return the path for an entry given to createJar. */
    String getPathForZipEntry(String e) {
        return e.replace(".", "/") + ".java";
    }

    /** Return the body text for an entry given to createDir or createJar. */
    String getBodyForEntry(String e) {
        int sep = e.lastIndexOf(".");
        String pkgName = e.substring(0, sep);
        String className = e.substring(sep + 1);
        return "package " + pkgName + "; public class " + className + "{ class Inner { } }";
    }

    /** Write a file containing the given string. Parent directories are
     * created as needed. */
    File writeFile(File f, String s) throws IOException {
        if (f.getParentFile() != null)
            f.getParentFile().mkdirs();
        FileWriter out = new FileWriter(f);
        try {
            out.write(s);
        } finally {
            out.close();
        }
        return f;
    }

    /** Create a path value from a list of directories and jar files. */
    String createPath(File... files) {
        StringBuilder sb = new StringBuilder();
        for (File f: files) {
            if (sb.length() > 0)
                sb.append(File.pathSeparatorChar);
            sb.append(f.getPath());
        }
        return sb.toString();
    }

    /** Create a set of files from a base directory and a set of relative paths. */
    Set<File> getFiles(File dir, String... paths) {
        Set<File> files = new LinkedHashSet<File>();
        for (String p: paths)
            files.add(new File(dir, p));
        return files;
    }

    /** Find all the files in a directory and its subdirectories. */
    Set<File> findFiles(File dir) {
        Set<File> files = new LinkedHashSet<File>();
        findFiles(dir, files);
        return files;
    }
    // where
    void findFiles(File dir, Set<File> files) {
        for (File f: dir.listFiles()) {
            if (f.isDirectory())
                findFiles(f, files);
            else
                files.add(f);
        }
    }

    /** Return the difference of two sets, a - b. */
    <T> Set<T> diff(Set<T> a, Set<T> b) {
        if (b.isEmpty())
            return a;
        Set<T> result = new LinkedHashSet<T>(a);
        result.removeAll(b);
        return result;
    }

    /** Report an error. */
    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;
}

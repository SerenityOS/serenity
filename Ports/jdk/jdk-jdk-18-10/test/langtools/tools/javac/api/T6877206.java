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
 * @bug 6877206
 * @summary JavaFileObject.toUri returns bogus URI (win)
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import javax.tools.*;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

// Test URIs returned from JavacFileManager and its support classes.
// For a variety of file objects, verify the validity of FileObject.toUri()
// by verifying the URI exists and points to the same contents as the file
// object itself

public class T6877206 {
    public static void main(String... args) throws Exception {
        new T6877206().run();
    }

    Set<String> foundClasses = new TreeSet<String>();
    Set<String> foundJars = new TreeSet<String>();

    void run() throws Exception {
        // names for entries to be created in directories and jar files
        String[] entries = { "p/A.class", "p/resources/A-1.jpg" };

        // test various combinations of directories and jar files, intended to
        // cover all sources of URIs within JavacFileManager's support classes

        test(createFileManager(), createDir("dir", entries), "p", entries.length);
        test(createFileManager(), createDir("a b/dir", entries), "p", entries.length);
        test(createFileManager(), createJar("jar", entries), "p", entries.length);
        test(createFileManager(), createJar("jar jar", entries), "p", entries.length);

        // Verify that we hit the files we intended
        checkCoverage("classes", foundClasses,
                "DirectoryFileObject", "JarFileObject");

        // Verify that we hit the jar files we intended
        checkCoverage("jar files", foundJars, "jar", "jar jar");
    }

    // use a new file manager for each test
    void test(StandardJavaFileManager fm, File f, String pkg, int expect) throws Exception {
        JarURLConnection c;
        System.err.println("Test " + f);
        try {
            fm.setLocation(StandardLocation.CLASS_PATH, Collections.singleton(f));

            int count = 0;
            for (JavaFileObject fo: fm.list(StandardLocation.CLASS_PATH,
                    pkg, EnumSet.allOf(JavaFileObject.Kind.class), true)) {
                System.err.println("checking " + fo);
                // record the file object class name for coverage checks later
                foundClasses.add(fo.getClass().getSimpleName());
                testFileObject(fo);
                count++;
            }

            if (expect > 0 && count != expect)
                throw new Exception("wrong number of entries found: "
                        + count + ", expected " + expect);
        } finally {
            fm.close();
        }
    }

    void testFileObject(JavaFileObject fo) throws Exception {
        // test the validity of the result of toUri() by using URLConnection
        // and comparing the results of reading from the connection with the
        // result of reading from the file object directly.
        URI uri = fo.toUri();
        System.err.println("uri: " + uri);

        URLConnection urlconn = uri.toURL().openConnection();
        if (urlconn instanceof JarURLConnection) {
            JarURLConnection jarconn = (JarURLConnection) urlconn;
            File f = new File(jarconn.getJarFile().getName());
            // record access to the jar file for coverage checks later
            foundJars.add(f.getName());
        }

        try {
            byte[] uriData = read(urlconn.getInputStream());
            byte[] foData = read(fo.openInputStream());
            if (!Arrays.equals(uriData, foData)) {
                if (uriData.length != foData.length)
                    throw new Exception("data size differs: uri data "
                            + uriData.length + " bytes, fo data " + foData.length+ " bytes");
                for (int i = 0; i < uriData.length; i++) {
                    if (uriData[i] != foData[i])
                    throw new Exception("unexpected data returned at offset " + i
                            + ", uri data " + uriData[i] + ", fo data " + foData[i]);
                }
                throw new AssertionError("cannot find difference");
            }
        } finally {
            // In principle, simply closing the result of urlconn.getInputStream()
            // should have been sufficient. But the internal JarURLConnection
            // does not close the JarFile in an expeditious manner, thus preventing
            // jtreg from deleting the jar file before starting the next test.
            // Therefore we force access to the JarURLConnection to close the
            // JarFile when necessary.
            if (urlconn instanceof JarURLConnection) {
                JarURLConnection jarconn = (JarURLConnection) urlconn;
                jarconn.getJarFile().close();
            }
        }
    }

    void checkCoverage(String label, Set<String> found, String... expect) throws Exception {
        Set<String> e = new TreeSet<String>(Arrays.asList(expect));
        if (!found.equals(e)) {
            e.removeAll(found);
            throw new Exception("expected " + label + " not used: " + e);
        }
    }

    JavacFileManager createFileManager() {
        return createFileManager(false);
    }

    JavacFileManager createFileManager(boolean useSymbolFile) {
        Context ctx = new Context();
        Options options = Options.instance(ctx);
        if (!useSymbolFile) {
            options.put("ignore.symbol.file", "true");
        }
        return new JavacFileManager(ctx, false, null);
    }

    File createDir(String name, String... entries) throws Exception {
        File dir = new File(name);
        if (!dir.mkdirs())
            throw new Exception("cannot create directories " + dir);
        for (String e: entries) {
            writeFile(new File(dir, e), e);
        }
        return dir;
    }

    File createJar(String name, String... entries) throws IOException {
        File jar = new File(name);
        OutputStream out = new FileOutputStream(jar);
        try {
            JarOutputStream jos = new JarOutputStream(out);
            for (String e: entries) {
                jos.putNextEntry(new ZipEntry(e));
                jos.write(e.getBytes());
            }
            jos.close();
        } finally {
            out.close();
        }
        return jar;
    }

    byte[] read(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        int offset = 0;
        try {
            int n;
            while ((n = in.read(data, offset, data.length - offset)) != -1) {
                offset += n;
                if (offset == data.length)
                    data = Arrays.copyOf(data, 2 * data.length);
            }
        } finally {
            in.close();
        }
        return Arrays.copyOf(data, offset);
    }

    void writeFile(File f, String s) throws IOException {
        f.getParentFile().mkdirs();
        FileWriter out = new FileWriter(f);
        try {
            out.write(s);
        } finally {
            out.close();
        }
    }
}

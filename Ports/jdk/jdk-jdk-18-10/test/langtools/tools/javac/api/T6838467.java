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
 * @bug 6838467
 * @summary JSR199 FileObjects don't obey general contract of equals.
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.tools.*;
import javax.tools.JavaFileManager.Location;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;

public class T6838467 {

    enum FileKind {
        DIR("dir"),
        ZIP("zip");
        FileKind(String path) {
            file = new File(path);
        }
        final File file;
    };

    enum CompareKind {
        SAME {
            @Override
            File other(File f) { return f; }
        },
        ABSOLUTE {
            @Override
            File other(File f) { return f.getAbsoluteFile(); }
        },
        DIFFERENT {
            @Override
            File other(File f) { return new File("not_" + f.getPath()); }
        },
        CASEEQUIV {
            @Override
            File other(File f) { return new File(f.getPath().toUpperCase()); }
        };
        abstract File other(File f);
    };

    String[] paths = { "p/A.java", "p/B.java", "p/C.java" };

    public static void main(String... args) throws Exception {
        new T6838467().run();
    }

    void run() throws Exception {
        boolean fileNameIsCaseSignificant = isFileNameCaseSignificant();
        boolean fileLookupIsCaseSignificant = isFileLookupCaseSignificant();

        String osName = System.getProperty("os.name");
        System.err.println("OS: " + osName);
        System.err.println("fileNameIsCaseSignificant:" + fileNameIsCaseSignificant);
        System.err.println("fileLookupIsCaseSignificant:" + fileLookupIsCaseSignificant);

        // on Windows, verify file system is not case significant
        if ((osName.startsWith("windows")) && fileNameIsCaseSignificant) {
            error("fileNameIsCaseSignificant is set on " + osName + ".");
        }

        // create a set of directories and zip files to compare
        createTestDir(new File("dir"), paths);
        createTestDir(new File("not_dir"), paths);
        createTestZip(new File("zip"), paths);
        createTestZip(new File("not_zip"), paths);
        if (fileNameIsCaseSignificant || fileLookupIsCaseSignificant) {
            createTestDir(new File("DIR"), paths);
            createTestZip(new File("ZIP"), paths);
        }

        // test the various sorts of file objects that can be obtained from
        // the file manager, and for various values that may or may not match.
        for (FileKind fk: FileKind.values()) {
            for (CompareKind ck: CompareKind.values()) {
                test(fk, ck);
            }
        }

        // verify that the various different types of file object were all
        // tested
        Set<String> expectClasses = new HashSet<>(Arrays.asList(
                "DirectoryFileObject",
                "JarFileObject" ));
        if (!foundClasses.equals(expectClasses)) {
            error("expected fileobject classes not found\n"
                    + "expected: " + expectClasses + "\n"
                    + "found: " + foundClasses);
        }

        if (errors > 0)
            throw new Exception(errors + " errors");
    }

    void test(FileKind fk, CompareKind ck) throws IOException {
        try (StandardJavaFileManager fm = createFileManager()) {
            File f1 = fk.file;
            Location l1 = createLocation(fm, "l1", f1);

            File f2 = ck.other(fk.file);
            Location l2 = createLocation(fm, "l2", f2);

            // If the directories or zip files match, we expect "n" matches in
            // the "n-squared" comparisons to come, where "n" is the number of
            // entries in the the directories or zip files.
            // If the directories or zip files don't themselves match,
            // we obviously don't expect any of their contents to match either.
            int expectEqualCount = (f1.getCanonicalFile().equals(f2.getCanonicalFile()) ? paths.length : 0);

            System.err.println("test " + (++count) + " " + fk + " " + ck + " " + f1 + " " + f2);
            test(fm, l1, l2, expectEqualCount);
        }
    }

    // For a pair of file managers that may or may not have similar entries
    // on the classpath, compare all files returned from one against all files
    // returned from the other.  For each pair of files, verify that if they
    // are equal, the hashcode is equal as well, and finally verify that the
    // expected number of matches was found.
    void test(JavaFileManager fm, Location l1, Location l2, int expectEqualCount) throws IOException {
        boolean foundFiles1 = false;
        boolean foundFiles2 = false;
        int foundEqualCount = 0;
        Set<JavaFileObject.Kind> kinds =  EnumSet.allOf(JavaFileObject.Kind.class);
        for (FileObject fo1: fm.list(l1, "p", kinds, false)) {
            foundFiles1 = true;
            foundClasses.add(fo1.getClass().getSimpleName());
            for (FileObject fo2: fm.list(l2, "p", kinds, false)) {
                foundFiles2 = true;
                foundClasses.add(fo2.getClass().getSimpleName());
                System.err.println("compare " + fo1 + " " + fo2);
                if (fo1.equals(fo2)) {
                    foundEqualCount++;
                    int hash1 = fo1.hashCode();
                    int hash2 = fo2.hashCode();
                    if (hash1 != hash2)
                        error("hashCode error: " + fo1 + " [" + hash1 + "] "
                                + fo2 + " [" + hash2 + "]");
                }
            }
        }
        if (!foundFiles1)
            error("no files found for location " + l1);
        if (!foundFiles2)
            error("no files found for location " + l2);
        // verify the expected number of matches were found
        if (foundEqualCount != expectEqualCount)
            error("expected matches not found: expected " + expectEqualCount + ", found " + foundEqualCount);
    }

    // create and initialize a location to test a FileKind, with a given directory
    // or zip file placed on the path
    Location createLocation(StandardJavaFileManager fm, String name, File classpath) throws IOException {
        Location l = new Location() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isOutputLocation() {
                return false;
            }

        };
        fm.setLocation(l, Arrays.asList(classpath));
        return l;
    }

    JavacFileManager createFileManager() {
        Context ctx = new Context();
        return new JavacFileManager(ctx, false, null);
    }

    // create a directory containing a given set of paths
    void createTestDir(File dir, String[] paths) throws IOException {
        for (String p: paths) {
            File file = new File(dir, p);
            file.getParentFile().mkdirs();
            try (FileWriter out = new FileWriter(file)) {
                out.write(p);
            }
        }
    }

    // create a zip file containing a given set of entries
    void createTestZip(File zip, String[] paths) throws IOException {
        if (zip.getParentFile() != null)
            zip.getParentFile().mkdirs();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            for (String p: paths) {
                ZipEntry ze = new ZipEntry(p);
                zos.putNextEntry(ze);
                byte[] bytes = p.getBytes();
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();
            }
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    boolean isFileNameCaseSignificant() {
        File lower = new File("test.txt");
        File upper = new File(lower.getPath().toUpperCase());
        return !lower.equals(upper);
    }

    boolean isFileLookupCaseSignificant() throws IOException {
        File lower = new File("test.txt");
        File upper = new File(lower.getPath().toUpperCase());
        if (upper.exists()) {
            upper.delete();
        }
        try (FileWriter out = new FileWriter(lower)) { }
        return !upper.exists();
    }

    int count;
    int errors;
    Set<String> foundClasses = new HashSet<>();
}


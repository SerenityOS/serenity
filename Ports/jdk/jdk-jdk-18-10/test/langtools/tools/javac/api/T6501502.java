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
 * @bug 6501502 6877206 6483788
 * @summary JSR 199: FileObject.toUri should return file:///c:/ or file:/c:/ not file://c:/
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.*;
import java.net.URI;
import javax.tools.*;

public class T6501502 {
    public static void main(String... args) throws Exception {
        new T6501502().run();
    }

    // The spec for java.io.File includes the following:
    //      For a given abstract pathname f it is guaranteed that
    //          new File( f.toURI()).equals( f.getAbsoluteFile())
    // For JavaFileObject we test as follows:
    //      new File( CONVERT_TO_FILEOBJECT(f).toURI()).equals( f.getAbsoluteFile())
    // to verify that we get reasonable URIs returned from toURI.
    // To make this a general test, and not just a Windows test,
    // we test a number of platform-independent paths.
    void run() throws Exception {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager sfm = c.getStandardFileManager(null, null, null)) {
            fm = sfm;
            System.err.println(System.getProperties());
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            File testSrcDir = new File(System.getProperty("test.src"));
            File testClassesDir = new File(System.getProperty("test.classes"));
            test(new File("abc.tmp"));
            test(new File(tmpDir, "bad.file"));
            test(new File(testSrcDir, "T6501501.java"));
            test(new File(testClassesDir, "T6501501.class"));
            test(new File("a b"));
        }
    }

    void test(File f) throws Exception {
        System.err.println("test " + f);
        FileObject fo = fm.getJavaFileObjects(f).iterator().next();
        URI uri = fo.toUri();
        System.err.println("FileObject uri: " + uri);
        if (!new File(uri).equals(f.getAbsoluteFile()))
            throw new Exception("unexpected URI returned");
    }

    StandardJavaFileManager fm;
}


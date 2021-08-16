/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4258405 4973606 8024096
 * @summary This test verifies that the doc-file directory does not
 *          get overwritten when the sourcepath is equal to the destination
 *          directory.
 *          Also test that -docfilessubdirs and -excludedocfilessubdir both work.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestDocFileDir
 */

import javadoc.tester.JavadocTester;

public class TestDocFileDir extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDocFileDir tester = new TestDocFileDir();
        tester.runTests();
    }

    // Output dir = "", Input dir = ""
    @Test
    public void test1() {
        copyDir(testSrc("pkg"), ".");
        setOutputDirectoryCheck(DirectoryCheck.NO_HTML_FILES);
        javadoc("pkg/C.java");
        checkExit(Exit.OK);
        checkOutput("pkg/doc-files/testfile.txt", true,
            "This doc file did not get trashed.");
    }

    // Output dir = Input Dir
    @Test
    public void test2() {
        String outdir = "out2";
        copyDir(testSrc("pkg"), outdir);
        setOutputDirectoryCheck(DirectoryCheck.NO_HTML_FILES);
        javadoc("-d", outdir,
            "-sourcepath", "blah" + PS + outdir + PS + "blah",
            "pkg");
        checkExit(Exit.OK);
        checkOutput("pkg/doc-files/testfile.txt", true,
            "This doc file did not get trashed.");
    }

    // Exercising -docfilessubdirs and -excludedocfilessubdir
    @Test
    public void test3() {
        String outdir = "out3";
        setOutputDirectoryCheck(DirectoryCheck.NONE);
        javadoc("-d", outdir,
                "-sourcepath", testSrc,
                "-docfilessubdirs",
                "-excludedocfilessubdir", "subdir-excluded1:subdir-excluded2",
                "pkg");
        checkExit(Exit.OK);
        checkFiles(true,
                "pkg/doc-files/subdir-used1/testfile.txt",
                "pkg/doc-files/subdir-used2/testfile.txt");
        checkFiles(false,
                "pkg/doc-files/subdir-excluded1/testfile.txt",
                "pkg/doc-files/subdir-excluded2/testfile.txt");
    }
}

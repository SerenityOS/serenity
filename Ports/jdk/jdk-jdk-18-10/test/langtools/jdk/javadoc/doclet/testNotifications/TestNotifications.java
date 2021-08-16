/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4657239 4775743
 * @summary  Make sure a notification is printed when an output directory must
 *           be created.
 *           Make sure classname is not include in javadoc usage message.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestNotifications
 */

import javadoc.tester.JavadocTester;

public class TestNotifications extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestNotifications tester = new TestNotifications();
        tester.runTests();
    }

    @Test
    public void test1() {
        String outDir = "out";

        // Notify that the destination directory must be created.
        javadoc("-d", outDir, "-sourcepath", testSrc, "pkg");
        checkExit(Exit.OK);
        checkOutput(Output.OUT, true,
                """
                    Creating destination directory: \"""" + outDir);

        // No need to notify that the destination must be created because
        // it already exists.
        setOutputDirectoryCheck(DirectoryCheck.NONE);
        javadoc("-d", outDir, "-sourcepath", testSrc, "pkg");
        checkExit(Exit.OK);
        checkOutput(Output.OUT, false,
                """
                    Creating destination directory: \"""" + outDir);
    }

    @Test
    public void test() {
        //Make sure classname is not include in javadoc usage message.
        setOutputDirectoryCheck(DirectoryCheck.NO_HTML_FILES);
        javadoc("-help");
        checkOutput(Output.OUT, false,
                "[classnames]");
    }
}

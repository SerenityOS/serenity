/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8215599
 * @summary Remove support for javadoc "frames" mode
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestNoFrames
 */

import javadoc.tester.JavadocTester;

public class TestNoFrames extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestNoFrames tester = new TestNoFrames();
        tester.runTests();
    }

    @Test
    public void testFrames() {
        javadoc("-d", "out-1",
                "--frames",
                "-sourcepath",testSrc,
                testSrc("TestNoFrames.java"));
        checkExit(Exit.ERROR);
    }

    @Test
    public void testDefault() {
        javadoc("-d", "out-2",
                "--no-frames",
                "-sourcepath",testSrc,
                testSrc("TestNoFrames.java"));
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
                """
                    warning: The --no-frames option is no longer required and may be removed
                      in a future release.""");
    }
}

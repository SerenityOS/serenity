/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044859 8230623
 * @summary test support for info options -help -X -version -fullversion --help-lint
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build OptionModesTester
 * @run main InfoOptsTest
 */

import java.io.IOException;

public class InfoOptsTest extends OptionModesTester {
    public static void main(String... args) throws Exception {
        InfoOptsTest t = new InfoOptsTest();
        t.runTests();
    }

    @Test
    void testInfoOpts() throws IOException {
        testInfoOpt("-help", "-deprecation");
        testInfoOpt("-X", "-Xlint");
        testInfoOpt("--help-lint", "supported keys");

        String specVersion = System.getProperty("java.specification.version");
        testInfoOpt("-version", "javac", specVersion);
        testInfoOpt("-fullversion", "javac", specVersion, "+");
    }

    void testInfoOpt(String opt, String... expect) {
        String[] opts = { opt };
        String[] files = { };

        runMain(opts, files)
                .checkOK()
                .checkLog(expect);

        runCall(opts, files)
                .checkIllegalArgumentException();

        runParse(opts, files)
                .checkIllegalArgumentException();
    }
}

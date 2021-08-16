/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8183582
 * @summary  Rationalize doclet -docencoding and -charset options.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestCharsetDocencodingOptions
 */

import javadoc.tester.JavadocTester;

public class TestCharsetDocencodingOptions extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestCharsetDocencodingOptions tester = new TestCharsetDocencodingOptions();
        tester.runTests();
    }

    @Test
    public void testWithNoOptions() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutputFileEncoding("utf-8");
    }

    @Test
    public void testWithDocencoding() {
        javadoc("-d", "out-1",
                "-docencoding", "ISO-8859-1",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutputFileEncoding("ISO-8859-1");
    }

    @Test
    public void testWithCharset() {
        javadoc("-d", "out-2",
                "-charset", "ISO-8859-1",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutputFileEncoding("ISO-8859-1");
    }

    @Test
    public void testDocencodingWithCharsetSimilar() {
        javadoc("-d", "out-3",
                "-docencoding", "ISO-8859-1",
                "-charset", "ISO-8859-1",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutputFileEncoding("ISO-8859-1");
    }

    @Test
    public void testDocencodingWithCharsetDifferent() {
        javadoc("-d", "out-4",
                "-charset", "UTF-8",
                "-docencoding", "ISO-8859-1",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.ERROR);

        checkOutput(Output.OUT, true,
                "error: Option -charset conflicts with -docencoding");
    }

    @Test
    public void testWithEncoding() {
        javadoc("-d", "out-5",
                "-sourcepath", testSrc,
                "-encoding", "ISO-8859-1",
                "pkg");
        checkExit(Exit.OK);

        checkOutputFileEncoding("ISO-8859-1");
    }


    void checkOutputFileEncoding(String charset) {
        checkOutput("index.html", true,
                """
                    <meta http-equiv="Content-Type" content="text/html; charset=""" + charset + "\">");
        checkOutput("pkg/Foo.html", true,
                """
                    <meta http-equiv="Content-Type" content="text/html; charset=""" + charset + "\">");
    }
}

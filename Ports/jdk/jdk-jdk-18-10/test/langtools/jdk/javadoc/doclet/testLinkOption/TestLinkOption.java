/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4720957 5020118 8026567 8038976 8184969 8164407 8182765 8205593
 *      8216497
 * @summary Test to make sure that -link and -linkoffline link to
 * right files, and URLs with and without trailing slash are accepted.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestLinkOption
 */

import java.io.File;

import javadoc.tester.JavadocTester;

public class TestLinkOption extends JavadocTester {
    /**
     * The entry point of the test.
     * @param args the array of command line arguments.
     */
    public static void main(String... args) throws Exception {
        TestLinkOption tester = new TestLinkOption();
        tester.runTests();
    }

    // The following test runs javadoc multiple times; it is important that the
    // first one is run first, since the subsequent runs refer to the output
    // it generates. Therefore we run everything serially in a single @Test
    // method and not in independent @Test methods.
    @Test
    public void test() {
        String mylib = "mylib";
        String[] javacArgs = {
            "-d", mylib, testSrc + "/extra/StringBuilder.java"
        };
        com.sun.tools.javac.Main.compile(javacArgs);

        // Generate the documentation using -linkoffline and a URL as the first parameter.
        String out1 = "out1";
        String url = "http://acme.com/jdk/";
        javadoc("-d", out1,
                "-source", "8",
                "-classpath", mylib,
                "-sourcepath", testSrc,
                "-linkoffline", url, testSrc + "/jdk",
                "-package",
                "pkg", "mylib.lang");
        checkExit(Exit.OK);

        checkOutput("pkg/C.html", true,
                "<a href=\"" + url + """
                    java/lang/String.html" title="class or interface in java.lang" class="external-l\
                    ink"><code>Link to String Class</code></a>""",
                //Make sure the parameters are formatted properly when the -link option is used.
                """
                    (int&nbsp;p1,
                     int&nbsp;p2,
                     int&nbsp;p3)""",
                """
                    (int&nbsp;p1,
                     int&nbsp;p2,
                     <a href=\"""" + url + """
                    java/lang/Object.html" title="class or interface in java.lang" class="external-link">Object</a>&nbsp;p3)""");

        checkOutput("pkg/B.html", true,
                """
                    <div class="block">A method with html tag the method <a href=\"""" + url + """
                    java/lang/ClassLoader.html#getSystemClassLoader()" title="class or interface in \
                    java.lang" class="external-link"><code><b>getSystemClassLoader()</b></code></a> \
                    as the parent class loader.</div>""",
                """
                    <div class="block">is equivalent to invoking <code><a href="#createTempFile(java\
                    .lang.String,java.lang.String,java.io.File)"><code>createTempFile(prefix,&nbsp;s\
                    uffix,&nbsp;null)</code></a></code>.</div>""",
                "<a href=\"" + url + """
                    java/lang/String.html" title="class or interface in java.lang" class="external-link">Link-Plain to String Class</a>""",
                "<code><b>getSystemClassLoader()</b></code>",
                "<code>createTempFile(prefix,&nbsp;suffix,&nbsp;null)</code>",
                """
                    <dd>
                    <ul class="see-list-long">
                    <li><a href="http://www.ietf.org/rfc/rfc2279.txt"><i>RFC&nbsp;2279: UTF-8, a
                     transformation format of ISO 10646</i></a></li>
                    <li><a href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IPv6 Addressing
                     Architecture</i></a></li>
                    <li><a href="http://www.ietf.org/rfc/rfc2396.txt"><i>RFC&nbsp;2396: Uniform
                     Resource Identifiers (URI): Generic Syntax</i></a></li>
                    <li><a href="http://www.ietf.org/rfc/rfc2732.txt"><i>RFC&nbsp;2732: Format for
                     Literal IPv6 Addresses in URLs</i></a></li>
                    <li><a href="C.html">A nearby file</a></li>
                    </ul>
                    </dd>
                    </dl>""");

        checkOutput("mylib/lang/StringBuilderChild.html", true,
                """
                    <div class="type-signature"><span class="modifiers">public abstract class </span\
                    ><span class="element-name type-name-label">StringBuilderChild</span>
                    <span class="extends-implements">extends <a href=\"""" + url + """
                    java/lang/Object.html" title="class or interface in java.lang" class="external-l\
                    ink">Object</a></span></div>"""
        );

        // Generate the documentation using -linkoffline and a relative path as the first parameter.
        // We will try linking to the docs generated in test 1 with a relative path.
        String out2 = "out2";
        javadoc("-d", out2,
                "-sourcepath", testSrc,
                "-linkoffline", "../" + out1, out1,
                "-package",
                "pkg2");
        checkExit(Exit.OK);
        checkOutput("pkg2/C2.html", true,
            """
                This is a link to <a href="../../""" + out1 + """
                /pkg/C.html" title="class or interface in pkg" class="external-link"><code>Class C</code></a>."""
        );

        String out3 = "out3";
        javadoc(createArguments(out3, out1, true));  // with trailing slash
        checkExit(Exit.OK);

        String out4 = "out4";
        javadoc(createArguments(out4, out1, false)); // without trailing slash
        checkExit(Exit.OK);
        // Note: the following test is very weak, and will fail if ever the text
        // of the message is changed. We should have a separate test to verify
        // this is the text that is given when there is a problem with a URL
        checkOutput(Output.OUT, false,
                "warning - Error fetching URL");

        // check multiple link options
        javadoc("-d", "out5",
                "-sourcepath", testSrc,
                "-link", "../" + "out1",
                "-link", "../" + "out2",
                "--no-platform-links",
                "pkg3");
        checkExit(Exit.OK);
        checkOutput("pkg3/A.html", true,
                """
                    <div class="type-signature"><span class="modifiers">public class </span><span cl\
                    ass="element-name type-name-label">A</span>
                    <span class="extends-implements">extends java.lang.Object</span></div>
                    <div class="block">Test links.
                     <br>
                     <a href="../../out2/pkg2/C2.html" title="class or interface in pkg2" class="ext\
                    ernal-link"><code>link to pkg2.C2</code></a>
                     <br>
                     <a href="../../out1/mylib/lang/StringBuilderChild.html" title="class or interfa\
                    ce in mylib.lang" class="external-link"><code>link to mylib.lang.StringBuilderCh\
                    ild</code></a>.</div>
                    """
        );

        // check multiple linkoffline options
        setAutomaticCheckLinks(false); // The example code has toy/bad links
        javadoc("-d", "out6",
                "-sourcepath", testSrc,
                "-linkoffline", "../copy/out1", "out1",
                "-linkoffline", "../copy/out2", "out2",
                "--no-platform-links",
                "pkg3");
        checkExit(Exit.OK);
        checkOutput("pkg3/A.html", true,
                """
                    <div class="type-signature"><span class="modifiers">public class </span><span cl\
                    ass="element-name type-name-label">A</span>
                    <span class="extends-implements">extends java.lang.Object</span></div>
                    <div class="block">Test links.
                     <br>
                     <a href="../../copy/out2/pkg2/C2.html" title="class or interface in pkg2" class\
                    ="external-link"><code>link to pkg2.C2</code></a>
                     <br>
                     <a href="../../copy/out1/mylib/lang/StringBuilderChild.html" title="class or in\
                    terface in mylib.lang" class="external-link"><code>link to mylib.lang.StringBuil\
                    derChild</code></a>.</div>
                    """
        );

        setAutomaticCheckLinks(true); // re-enable checks
    }

    /*
     * Create the documentation using the -link option, vary the behavior with
     * both trailing and no trailing slash. We are only interested in ensuring
     * that the command executes with no errors or related warnings.
     */
    static String[] createArguments(String outDir, String packageDir, boolean withTrailingSlash) {
        String packagePath = new File(packageDir).getAbsolutePath();
        if (withTrailingSlash) {
            // add the trailing slash, if it is not present!
            if (!packagePath.endsWith(FS)) {
                packagePath = packagePath + FS;
            }
        } else {
            // remove the trailing slash, if it is present!
            if (packagePath.endsWith(FS)) {
                packagePath = packagePath.substring(0, packagePath.length() - 1);
            }
        }
        String args[] = {
            "-d", outDir,
            "-sourcepath", testSrc,
            "-link", "file:///" + packagePath,
            "-package",
            "pkg2"
        };
        System.out.println("packagePath: " + packagePath);
        return args;
    }
}

/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8005092 6469562 8182765
 * @summary  Test repeated annotations output.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestRepeatedAnnotations
 */

import javadoc.tester.JavadocTester;

public class TestRepeatedAnnotations extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestRepeatedAnnotations tester = new TestRepeatedAnnotations();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg", "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg/C.html", true,
                """
                    <a href="ContaineeSynthDoc.html" title="annotation interface in pkg">@ContaineeS\
                    ynthDoc</a> <a href="ContaineeSynthDoc.html" title="annotation interface in pkg"\
                    >@ContaineeSynthDoc</a>""",
                """
                    <a href="ContaineeRegDoc.html" title="annotation interface in pkg">@ContaineeReg\
                    Doc</a> <a href="ContaineeRegDoc.html" title="annotation interface in pkg">@Cont\
                    aineeRegDoc</a>""",
                """
                    <a href="RegContainerDoc.html" title="annotation interface in pkg">@RegContainer\
                    Doc</a>({<a href="RegContaineeNotDoc.html" title="annotation interface in pkg">@\
                    RegContaineeNotDoc</a>,<a href="RegContaineeNotDoc.html" title="annotation inter\
                    face in pkg">@RegContaineeNotDoc</a>})""");

        checkOutput("pkg/D.html", true,
                """
                    <a href="RegDoc.html" title="annotation interface in pkg">@RegDoc</a>(<a href="R\
                    egDoc.html#x()">x</a>=1)""",
                """
                    <a href="RegArryDoc.html" title="annotation interface in pkg">@RegArryDoc</a>(<a\
                     href="RegArryDoc.html#y()">y</a>=1)""",
                """
                    <a href="RegArryDoc.html" title="annotation interface in pkg">@RegArryDoc</a>(<a\
                     href="RegArryDoc.html#y()">y</a>={1,2})""",
                """
                    <a href="NonSynthDocContainer.html" title="annotation interface in pkg">@NonSynt\
                    hDocContainer</a>(<a href="RegArryDoc.html" title="annotation interface in pkg">\
                    @RegArryDoc</a>(<a href="RegArryDoc.html#y()">y</a>=1))""");

        checkOutput("pkg1/C.html", true,
                """
                    <a href="RegContainerValDoc.html" title="annotation interface in pkg1">@RegConta\
                    inerValDoc</a>(<a href="RegContainerValDoc.html#value()">value</a>={<a href="Reg\
                    ContaineeNotDoc.html" title="annotation interface in pkg1">@RegContaineeNotDoc</\
                    a>,<a href="RegContaineeNotDoc.html" title="annotation interface in pkg1">@RegCo\
                    ntaineeNotDoc</a>},<a href="RegContainerValDoc.html#y()">y</a>=3)""",
                """
                    <a href="ContainerValDoc.html" title="annotation interface in pkg1">@ContainerVa\
                    lDoc</a>(<a href="ContainerValDoc.html#value()">value</a>={<a href="ContaineeNot\
                    Doc.html" title="annotation interface in pkg1">@ContaineeNotDoc</a>,<a href="Con\
                    taineeNotDoc.html" title="annotation interface in pkg1">@ContaineeNotDoc</a>},<a\
                     href="ContainerValDoc.html#x(\
                    )">x</a>=1)""");

        checkOutput("pkg/C.html", false,
                """
                    <a href="RegContaineeDoc.html" title="annotation interface in pkg">@RegContainee\
                    Doc</a> <a href="RegContaineeDoc.html" title="annotation interface in pkg">@RegC\
                    ontaineeDoc</a>""",
                """
                    <a href="RegContainerNotDoc.html" title="annotation interface in pkg">@RegContai\
                    nerNotDoc</a>(<a href="RegContainerNotDoc.html#value()">value</a>={<a href="RegC\
                    ontaineeNotDoc.html" title="annotation in pkg">@RegContaineeNotDoc</a>,<a href="\
                    RegContaineeNotDoc.html" title="annotation in pkg">@RegContaineeNotDoc</a>})""");

        checkOutput("pkg1/C.html", false,
                """
                    <a href="ContaineeSynthDoc.html" title="annotation interface in pkg1">@Containee\
                    SynthDoc</a> <a href="ContaineeSynthDoc.html" title="annotation interface in pkg\
                    1">@ContaineeSynthDoc</a>""",
                """
                    <a href="RegContainerValNotDoc.html" title="annotation interface in pkg1">@RegCo\
                    ntainerValNotDoc</a>(<a href="RegContainerValNotDoc.html#value()">value</a>={<a \
                    href="RegContaineeDoc.html" title="annotation interface in pkg1">@RegContaineeDo\
                    c</a>,<a href="RegContaineeDoc.html" title="annotation interface in pkg1">@RegCo\
                    ntaineeDoc</a>},<a href="RegContainerValNotDoc.html#y()">y</a>=4)""",
                """
                    <a href="ContainerValNotDoc.html" title="annotation interface in pkg1">@Containe\
                    rValNotDoc</a>(<a href="ContainerValNotDoc.html#value()">value</a>={<a href="Con\
                    taineeNotDoc.html" title="annotation interface in pkg1">@ContaineeNotDoc</a>,<a \
                    href="ContaineeNotDoc.html" title="annotation interface in pkg1">@ContaineeNotDo\
                    c</a>},<a href="ContainerValNotDoc.html#x()">x</a>=2)""",
                """
                    <a href="ContainerSynthNotDoc.html" title="annotation interface in pkg1">@Contai\
                    nerSynthNotDoc</a>(<a href="ContainerSynthNotDoc.html#value()">value</a>=<a href\
                    ="ContaineeSynthDoc.html" title="annotation interface in pkg1">@ContaineeSynthDo\
                    c</a>)""");
    }
}

/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8190003 8196201 8196202 8184205
 * @summary Special characters in group names should be escaped
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestGroupName
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestGroupName extends JavadocTester {

    public final ToolBox tb;
    public static void main(String... args) throws Exception {
        TestGroupName tester = new TestGroupName();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public TestGroupName() {
        tb = new ToolBox();
    }

    @Test
    public void testPackageGroups(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p1; public class C1 { }",
                "package p2; public class C2 { }",
                "package p3; public class C3 { }");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "-group", "abc < & > def", "p1",
                "p1", "p2", "p3");
        checkExit(Exit.OK);

        checkOutput("index.html", true,
                """
                    <button id="all-packages-table-tab1" role="tab" aria-selected="false" aria-contr\
                    ols="all-packages-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" onc\
                    lick="show('all-packages-table', 'all-packages-table-tab1', 2)" class="table-tab\
                    ">abc &lt; &amp; &gt; def</button>""");
    }

    @Test
    public void testModuleGroups(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("ma"),
                "module ma { exports pa1; }",
                "package pa1; public class CA1 { }",
                "package pa2; public class CA2 { }",
                "package pa3; public class CA3 { }");

        tb.writeJavaFiles(src.resolve("mb"),
                "module mb { exports pb1; }",
                "package pb1; public class CB1 { }",
                "package pb2; public class CB2 { }",
                "package pb3; public class CB3 { }");

        tb.writeJavaFiles(src.resolve("mc"),
                "module mc { exports pc1; }",
                "package pc1; public class CC1 { }",
                "package pc2; public class CC2 { }",
                "package pc3; public class CC3 { }");

        javadoc("-d", base.resolve("out").toString(),
                "--module-source-path", src.toString(),
                "-group", "abc < & > def", "ma",
                "--module", "ma,mb,mc");

        checkExit(Exit.OK);

        checkOutput("index.html", true,
                """
                    <button id="all-modules-table-tab2" role="tab" aria-selected="false" aria-contro\
                    ls="all-modules-table.tabpanel" tabindex="-1" onkeydown="switchTab(event)" oncli\
                    ck="show('all-modules-table', 'all-modules-table-tab2', 2)" class="table-tab">Ot\
                    her Modules</button>""");
    }
}


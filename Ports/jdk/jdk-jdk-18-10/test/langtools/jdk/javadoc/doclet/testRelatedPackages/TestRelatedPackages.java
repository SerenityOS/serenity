/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8260388 8265613
 * @summary Listing (sub)packages at package level of API documentation
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestRelatedPackages
 */

import toolbox.ModuleBuilder;
import toolbox.ToolBox;
import javadoc.tester.JavadocTester;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestRelatedPackages extends JavadocTester {

    ToolBox tb = new ToolBox();

    public static void main(String... args) throws Exception {
        TestRelatedPackages tester = new TestRelatedPackages();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    @Test
    public void testRelatedPackages(Path base) throws Exception {
        Path src = base.resolve("src-packages");
        tb.writeFile(src.resolve("t/p1/package-info.java"), "package t.p1;\n");
        tb.writeFile(src.resolve("t/p1/s1/A.java"), "package t.p1.s1; public class A {}\n");
        tb.writeFile(src.resolve("t/p1/s2/package-info.java"), "package t.p1.s1;\n");
        tb.writeFile(src.resolve("t/p1/s3/B.java"), "package t.p1.s3; public class B {}\n");
        tb.writeFile(src.resolve("t/p1/s3/t1/package-info.java"), "package t.p1.s3.t1;\n");
        tb.writeFile(src.resolve("t/p1/s3/t2/C.java"), "package t.p1.s3.t2; public class C {}\n");
        tb.writeFile(src.resolve("t/p2/X.java"), "package t.p2; public class X {}\n");

        javadoc("-d", "out-packages",
                "-sourcepath", src.toString(),
                "-subpackages", "t.p1:t.p2");
        checkExit(Exit.OK);
        checkOutput("t/p1/package-summary.html", true,
                """
                    <div class="caption"><span>Related Packages</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="s1/package-summary.html">t.p1.s1</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    <div class="col-first odd-row-color"><a href="s2/package-summary.html">t.p1.s2</a></div>
                    <div class="col-last odd-row-color">&nbsp;</div>
                    <div class="col-first even-row-color"><a href="s3/package-summary.html">t.p1.s3</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    </div>""");
        checkOutput("t/p1/s1/package-summary.html", true,
                """
                    <div class="caption"><span>Related Packages</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="../package-summary.html">t.p1</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    <div class="col-first odd-row-color"><a href="../s2/package-summary.html">t.p1.s2</a></div>
                    <div class="col-last odd-row-color">&nbsp;</div>
                    <div class="col-first even-row-color"><a href="../s3/package-summary.html">t.p1.s3</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    </div>""");
        checkOutput("t/p1/s2/package-summary.html", true,
                """
                    <div class="caption"><span>Related Packages</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="../package-summary.html">t.p1</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    <div class="col-first odd-row-color"><a href="../s1/package-summary.html">t.p1.s1</a></div>
                    <div class="col-last odd-row-color">&nbsp;</div>
                    <div class="col-first even-row-color"><a href="../s3/package-summary.html">t.p1.s3</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    </div>""");
        checkOutput("t/p1/s3/package-summary.html", true,
                """
                    <div class="caption"><span>Related Packages</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="../package-summary.html">t.p1</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    <div class="col-first odd-row-color"><a href="t1/package-summary.html">t.p1.s3.t1</a></div>
                    <div class="col-last odd-row-color">&nbsp;</div>
                    <div class="col-first even-row-color"><a href="t2/package-summary.html">t.p1.s3.t2</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    <div class="col-first odd-row-color"><a href="../s1/package-summary.html">t.p1.s1</a></div>
                    <div class="col-last odd-row-color">&nbsp;</div>
                    <div class="col-first even-row-color"><a href="../s2/package-summary.html">t.p1.s2</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    </div>""");
        checkOutput("t/p1/s3/t1/package-summary.html", true,
                """
                    <div class="caption"><span>Related Packages</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="../package-summary.html">t.p1.s3</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    <div class="col-first odd-row-color"><a href="../t2/package-summary.html">t.p1.s3.t2</a></div>
                    <div class="col-last odd-row-color">&nbsp;</div>
                    </div>""");
        checkOutput("t/p1/s3/t2/package-summary.html", true,
                """
                    <div class="caption"><span>Related Packages</span></div>
                    <div class="summary-table two-column-summary">
                    <div class="table-header col-first">Package</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="../package-summary.html">t.p1.s3</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    <div class="col-first odd-row-color"><a href="../t1/package-summary.html">t.p1.s3.t1</a></div>
                    <div class="col-last odd-row-color">&nbsp;</div>
                    </div>""");
        checkOutput("t/p2/package-summary.html", false,
                """
                    <div class="caption"><span>Related Packages</span></div>""");
    }

    @Test
    public void testCrossModuleRelatedPackages(Path base) throws Exception {
        Path src = base.resolve("src-modules");
        new ModuleBuilder(tb, "m")
                .exports("pkg")
                .exports("pkg.sub1")
                .classes("package pkg; public class A { }",
                         "package pkg.sub1; public class B { }")
                .write(src);
        new ModuleBuilder(tb, "o")
                .exports("pkg.sub2")
                .exports("pkg.sub2.sub")
                .classes("package pkg.sub2; public class C { }",
                         "package pkg.sub2.sub; public class D { }")
                .write(src);

        javadoc("-d", "out-modules",
                "-quiet",
                "--module-source-path", src.toString(),
                "--module", "m,o");
        checkExit(Exit.OK);
        checkOutput("m/pkg/package-summary.html", true,
                """
                        <div class="caption"><span>Related Packages</span></div>
                        <div class="summary-table three-column-summary">
                        <div class="table-header col-first">Module</div>
                        <div class="table-header col-second">Package</div>
                        <div class="table-header col-last">Description</div>
                        <div class="col-plain even-row-color"><a href="../module-summary.html">m</a></div>
                        <div class="col-first even-row-color"><a href="sub1/package-summary.html">pkg.sub1</a></div>
                        <div class="col-last even-row-color">&nbsp;</div>
                        <div class="col-plain odd-row-color"><a href="../../o/module-summary.html">o</a></div>
                        <div class="col-first odd-row-color"><a href="../../o/pkg/sub2/package-summary.html">pkg.sub2</a></div>
                        <div class="col-last odd-row-color">&nbsp;</div>
                        </div>""");
        checkOutput("m/pkg/sub1/package-summary.html", true,
                """
                        <div class="caption"><span>Related Packages</span></div>
                        <div class="summary-table three-column-summary">
                        <div class="table-header col-first">Module</div>
                        <div class="table-header col-second">Package</div>
                        <div class="table-header col-last">Description</div>
                        <div class="col-plain even-row-color"><a href="../../module-summary.html">m</a></div>
                        <div class="col-first even-row-color"><a href="../package-summary.html">pkg</a></div>
                        <div class="col-last even-row-color">&nbsp;</div>
                        <div class="col-plain odd-row-color"><a href="../../../o/module-summary.html">o</a></div>
                        <div class="col-first odd-row-color"><a href="../../../o/pkg/sub2/package-summary.html">pkg.sub2</a></div>
                        <div class="col-last odd-row-color">&nbsp;</div>
                        </div>""");
        checkOutput("o/pkg/sub2/package-summary.html", true,
                """
                        <div class="caption"><span>Related Packages</span></div>
                        <div class="summary-table three-column-summary">
                        <div class="table-header col-first">Module</div>
                        <div class="table-header col-second">Package</div>
                        <div class="table-header col-last">Description</div>
                        <div class="col-plain even-row-color"><a href="../../../m/module-summary.html">m</a></div>
                        <div class="col-first even-row-color"><a href="../../../m/pkg/package-summary.html">pkg</a></div>
                        <div class="col-last even-row-color">&nbsp;</div>
                        <div class="col-plain odd-row-color"><a href="../../module-summary.html">o</a></div>
                        <div class="col-first odd-row-color"><a href="sub/package-summary.html">pkg.sub2.sub</a></div>
                        <div class="col-last odd-row-color">&nbsp;</div>
                        <div class="col-plain even-row-color"><a href="../../../m/module-summary.html">m</a></div>
                        <div class="col-first even-row-color"><a href="../../../m/pkg/sub1/package-summary.html">pkg.sub1</a></div>
                        <div class="col-last even-row-color">&nbsp;</div>
                        </div>""");
        checkOutput("o/pkg/sub2/sub/package-summary.html", true,
                """
                        <div class="caption"><span>Related Packages</span></div>
                        <div class="summary-table two-column-summary">
                        <div class="table-header col-first">Package</div>
                        <div class="table-header col-last">Description</div>
                        <div class="col-first even-row-color"><a href="../package-summary.html">pkg.sub2</a></div>
                        <div class="col-last even-row-color">&nbsp;</div>
                        </div>""");
    }

}

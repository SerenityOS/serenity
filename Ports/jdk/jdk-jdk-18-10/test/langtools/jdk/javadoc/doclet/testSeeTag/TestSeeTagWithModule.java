/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164408 8262992
 * @summary Add module support for see, link and linkplain javadoc tags
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build javadoc.tester.*
 * @run main TestSeeTagWithModule
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import builder.ClassBuilder;
import builder.ClassBuilder.*;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestSeeTagWithModule extends JavadocTester {

    final ToolBox tb;
    private final Path src;

    public static void main(String... args) throws Exception {
        TestSeeTagWithModule tester = new TestSeeTagWithModule();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestSeeTagWithModule() throws Exception {
        tb = new ToolBox();
        src = Paths.get("src");
        generateSources();
    }

    @Test
    public void testSeeModuleInternal(Path base) throws Exception {
        Path out = base.resolve("out");

        javadoc("-d", out.toString(),
                "--module-source-path", src.toString(),
                "--module", "m1,m2,m3",
                "m2/com.m2.lib");

        checkExit(Exit.OK);
        checkOutput("m3/com/m3/app/App.html", true,
                """
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li><a href="../../../../m1/module-summary.html"><code>m1</code></a></li>
                    <li><a href="../../../../m1/module-summary.html"><code>m1</code></a></li>
                    <li><a href="../../../../m1/com/m1/lib/package-summary.html"><code>com.m1.lib</code></a></li>
                    <li><a href="../../../../m1/com/m1/lib/Lib.html" title="class in com.m1.lib"><code>Lib</code></a></li>
                    <li><a href="../../../../m1/com/m1/lib/Lib.html#method(java.lang.String)"><code>Lib.method(java.lang.String)</code></a></li>
                    <li><a href="../../../../m1/com/m1/lib/Lib.html#method(java.lang.String)"><code>Lib.method(String)</code></a></li>
                    <li><a href="../../../../m2/module-summary.html"><code>m2</code></a></li>
                    <li><a href="../../../../m2/module-summary.html"><code>m2</code></a></li>
                    <li><a href="../../../../m2/com/m2/lib/package-summary.html"><code>com.m2.lib</code></a></li>
                    <li><a href="../../../../m2/com/m2/lib/Lib.html" title="class in com.m2.lib"><code>Lib</code></a></li>
                    <li><a href="../../../../m2/com/m2/lib/Lib.html#method(java.lang.String)"><code>Lib.method(java.lang.String)</code></a></li>
                    <li><a href="../../../../m2/com/m2/lib/Lib.html#method(java.lang.String)"><code>Lib.method(String)</code></a></li>
                    """);
    }

    @Test
    public void testSeeModuleExternal(Path base) throws Exception {
        Path out1 = base.resolve("out1"), out2 = base.resolve("out2");

        javadoc("-d", out1.toString(),
                "--module-source-path", src.toString(),
                "--module", "m1,m2",
                "m2/com.m2.lib");
        javadoc("-d", out2.toString(),
                "--module-source-path", src.toString(),
                "--add-modules", "m2",
                "--module", "m3",
                "-link", "../" + out1.getFileName());

        checkExit(Exit.OK);
        checkOutput("m3/com/m3/app/App.html", true,
                """
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li><a href="../../../../../out1/m1/module-summary.html" class="external-link"><code>m1</code></a></li>
                    <li><a href="../../../../../out1/m1/module-summary.html" class="external-link"><code>m1</code></a></li>
                    <li><a href="../../../../../out1/m1/com/m1/lib/package-summary.html" class="external-link"><code>m1/com.m1.lib</code></a></li>
                    <li><a href="../../../../../out1/m1/com/m1/lib/Lib.html" title="class or interface in com.m1.lib" class="external-link"><code>Lib</code></a></li>
                    <li><a href="../../../../../out1/m1/com/m1/lib/Lib.html#method(java.lang.String)" title="class or \
                    interface in com.m1.lib" class="external-link"><code>Lib.method(java.lang.String)</code></a></li>
                    <li><a href="../../../../../out1/m1/com/m1/lib/Lib.html#method(java.lang.String)" title="class or \
                    interface in com.m1.lib" class="external-link"><code>Lib.method(String)</code></a></li>
                    <li><a href="../../../../../out1/m2/module-summary.html" class="external-link"><code>m2</code></a></li>
                    <li><a href="../../../../../out1/m2/module-summary.html" class="external-link"><code>m2</code></a></li>
                    <li><a href="../../../../../out1/m2/com/m2/lib/package-summary.html" class="external-link"><code>m2/com.m2.lib</code></a></li>
                    <li><a href="../../../../../out1/m2/com/m2/lib/Lib.html" title="class or interface in com.m2.lib" class="external-link"><code>Lib</code></a></li>
                    <li><a href="../../../../../out1/m2/com/m2/lib/Lib.html#method(java.lang.String)" title="class or \
                    interface in com.m2.lib" class="external-link"><code>Lib.method(java.lang.String)</code></a></li>
                    <li><a href="../../../../../out1/m2/com/m2/lib/Lib.html#method(java.lang.String)" title="class or \
                    interface in com.m2.lib" class="external-link"><code>Lib.method(String)</code></a></li>
                    """);
    }

    @Test
    public void testSeeModuleSameNameInternal(Path base) throws Exception {
        Path out = base.resolve("out");

        javadoc("-d", out.toString(),
                "--module-source-path", src.toString(),
                "--module", "com.ex1,com.ex2");

        checkExit(Exit.OK);
        checkOutput("com.ex2/com/ex2/B.html", true,
                """
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li><a href="../../../com.ex1/com/ex1/package-summary.html"><code>com.ex1</code></a></li>
                    <li><a href="../../../com.ex1/module-summary.html"><code>com.ex1</code></a></li>
                    <li><a href="../../../com.ex1/com/ex1/package-summary.html"><code>com.ex1</code></a></li>
                    <li><a href="../../../com.ex1/com/ex1/A.html" title="class in com.ex1"><code>A</code></a></li>
                    <li><a href="../../../com.ex1/com/ex1/A.html#m()"><code>A.m()</code></a></li>
                    <li><a href="../../../com.ex1/com/ex1/A.html#m()"><code>A.m()</code></a></li>
                    <li><a href="package-summary.html"><code>com.ex2</code></a></li>
                    <li><a href="../../module-summary.html"><code>com.ex2</code></a></li>
                    """);
    }

    @Test
    public void testSeeModuleSameNameExternal(Path base) throws Exception {
        Path out1 = base.resolve("out1"), out2 = base.resolve("out2");

        javadoc("-d", out1.toString(),
                "--module-source-path", src.toString(),
                "--module", "com.ex1");
        javadoc("-d", out2.toString(),
                "--module-source-path", src.toString(),
                "--module", "com.ex2",
                "-link", "../" + out1.getFileName());

        checkExit(Exit.OK);
        checkOutput("com.ex2/com/ex2/B.html", true,
                """
                    <dt>See Also:</dt>
                    <dd>
                    <ul class="see-list">
                    <li><a href="../../../../out1/com.ex1/com/ex1/package-summary.html" class="external-link"><code>com.ex1</code></a></li>
                    <li><a href="../../../../out1/com.ex1/module-summary.html" class="external-link"><code>com.ex1</code></a></li>
                    <li><a href="../../../../out1/com.ex1/com/ex1/package-summary.html" class="external-link"><code>com.ex1/com.ex1</code></a></li>
                    <li><a href="../../../../out1/com.ex1/com/ex1/A.html" title="class or interface in com.ex1" class="external-link"><code>A</code></a></li>
                    <li><a href="../../../../out1/com.ex1/com/ex1/A.html#m()" title="class or interface in com.ex1" class="external-link"><code>A.m()</code></a></li>
                    <li><a href="../../../../out1/com.ex1/com/ex1/A.html#m()" title="class or interface in com.ex1" class="external-link"><code>A.m()</code></a></li>
                    <li><a href="package-summary.html"><code>com.ex2</code></a></li>
                    <li><a href="../../module-summary.html"><code>com.ex2</code></a></li>
                    """);
    }

    @Test
    public void testMissingType(Path base) throws Exception {
        Path out = base.resolve("outMissingType");

        javadoc("-d", out.toString(),
                "--module-source-path", src.toString(),
                "--module", "fail");

        checkExit(Exit.ERROR);
    }

    void generateSources() throws Exception {
        new ModuleBuilder(tb, "m1")
                .exports("com.m1.lib")
                .classes("""
                    package com.m1.lib;
                    public class Lib {
                    public String method(String s) {
                        return s;
                    }
                    }""")
                .write(src);
        new ModuleBuilder(tb, "m2")
                .classes("""
                    package com.m2.lib;
                    public class Lib {
                    public String method(String s) {
                        return s;
                    }
                    }""")
                .write(src);
        new ModuleBuilder(tb, "m3")
                .exports("com.m3.app")
                .requires("m1")
                .classes("""
                    package com.m3.app;\s
                    public class App{
                    /**
                     * @see m1
                     * @see m1/
                     * @see m1/com.m1.lib
                     * @see m1/com.m1.lib.Lib
                     * @see m1/com.m1.lib.Lib#method
                     * @see m1/com.m1.lib.Lib#method(String)
                     * @see m2
                     * @see m2/
                     * @see m2/com.m2.lib
                     * @see m2/com.m2.lib.Lib
                     * @see m2/com.m2.lib.Lib#method
                     * @see m2/com.m2.lib.Lib#method(String)
                     */
                    public App(){}
                    }
                    """)
                .write(src);

        new ModuleBuilder(tb, "com.ex1")
                .exports("com.ex1")
                .classes("""
                    package com.ex1;
                    public class A{
                    public void m() {}
                    }""",
                    """
                    package com.ex1;
                    public class B {}""")
                .write(src);

        new ModuleBuilder(tb, "com.ex2")
                .requires("com.ex1")
                .exports("com.ex2")
                .classes("""
                    package com.ex2;\s
                    import com.ex1.A;
                    public class B{
                    /**
                     * @see com.ex1
                     * @see com.ex1/
                     * @see com.ex1/com.ex1
                     * @see com.ex1/com.ex1.A
                     * @see com.ex1/com.ex1.A#m
                     * @see com.ex1/com.ex1.A#m()
                     * @see com.ex2
                     * @see com.ex2/
                     */
                    public B(A obj){}
                    }
                    """)
                .write(src);

        new ModuleBuilder(tb, "fail")
                .exports("pkg.fail")
                .classes("""
                    package pkg.fail;
                    /**
                     * @see fail/#foo()
                     */
                    public class F {
                        public void foo() {}
                    }
                    """)
                .write(src);

    }

}

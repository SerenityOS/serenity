/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164408
 * @summary Add module support for see, link and linkplain javadoc tags
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build javadoc.tester.*
 * @run main TestLinkTagletWithModule
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import builder.ClassBuilder;
import builder.ClassBuilder.*;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestLinkTagletWithModule extends JavadocTester {

    final ToolBox tb;
    private final Path src;

    public static void main(String... args) throws Exception {
        TestLinkTagletWithModule tester = new TestLinkTagletWithModule();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestLinkTagletWithModule() throws Exception {
        tb = new ToolBox();
        src = Paths.get("src");
        generateSources();
    }

    @Test
    public void testLinkModuleInternal(Path base) throws Exception {
        Path out = base.resolve("out");

        javadoc("-d", out.toString(),
                "--module-source-path", src.toString(),
                "--module", "m1,m2,m3",
                "m2/com.m2.lib");

        checkExit(Exit.OK);
        checkOutput("m3/com/m3/app/App.html", true,
                """
                    <div class="block"><a href="../../../../m1/module-summary.html"><code>m1</code></a>
                     <a href="../../../../m1/module-summary.html"><code>m1</code></a>
                     <a href="../../../../m1/com/m1/lib/package-summary.html"><code>package link</code></a>
                     <a href="../../../../m1/com/m1/lib/Lib.html" title="class in com.m1.lib"><code>Lib</code></a>
                     <a href="../../../../m1/com/m1/lib/Lib.html#method(java.lang.String)"><code>Lib.method(java.lang.String)</code></a>
                     <a href="../../../../m1/com/m1/lib/Lib.html#method(java.lang.String)"><code>Lib.method(String)</code></a>
                     <a href="../../../../m2/module-summary.html">m2</a>
                     <a href="../../../../m2/module-summary.html">m2</a>
                     <a href="../../../../m2/com/m2/lib/package-summary.html">com.m2.lib</a>
                     <a href="../../../../m2/com/m2/lib/Lib.html" title="class in com.m2.lib">Lib</a>
                     <a href="../../../../m2/com/m2/lib/Lib.html#method(java.lang.String)">class link</a>
                     <a href="../../../../m2/com/m2/lib/Lib.html#method(java.lang.String)">Lib.method(String)</a></div>
                    """);
    }

    @Test
    public void testLinkModuleExternal(Path base) throws Exception {
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
                    <div class="block"><a href="../../../../../out1/m1/module-summary.html" class="external-link"><code>m1</code></a>
                     <a href="../../../../../out1/m1/module-summary.html" class="external-link"><code>m1</code></a>
                     <a href="../../../../../out1/m1/com/m1/lib/package-summary.html" class="external-link"><code>package link</code></a>
                     <a href="../../../../../out1/m1/com/m1/lib/Lib.html" title="class or interface in com.m1.lib"\
                     class="external-link"><code>Lib</code></a>
                     <a href="../../../../../out1/m1/com/m1/lib/Lib.html#method(java.lang.String)" title="class or\
                     interface in com.m1.lib" class="external-link"><code>Lib.method(java.lang.String)</code></a>
                     <a href="../../../../../out1/m1/com/m1/lib/Lib.html#method(java.lang.String)" title="class or\
                     interface in com.m1.lib" class="external-link"><code>Lib.method(String)</code></a>
                     <a href="../../../../../out1/m2/module-summary.html" class="external-link">m2</a>
                     <a href="../../../../../out1/m2/module-summary.html" class="external-link">m2</a>
                     <a href="../../../../../out1/m2/com/m2/lib/package-summary.html" class="external-link">m2/com.m2.lib</a>
                     <a href="../../../../../out1/m2/com/m2/lib/Lib.html" title="class or interface in com.m2.lib" class="external-link">Lib</a>
                     <a href="../../../../../out1/m2/com/m2/lib/Lib.html#method(java.lang.String)" title="class or\
                     interface in com.m2.lib" class="external-link">class link</a>
                     <a href="../../../../../out1/m2/com/m2/lib/Lib.html#method(java.lang.String)" title="class or\
                     interface in com.m2.lib" class="external-link">Lib.method(String)</a></div>
                    """);
    }

    @Test
    public void testLinkModuleSameNameInternal(Path base) throws Exception {
        Path out = base.resolve("out");

        javadoc("-d", out.toString(),
                "--module-source-path", src.toString(),
                "--module", "com.ex1,com.ex2");

        checkExit(Exit.OK);
        checkOutput("com.ex2/com/ex2/B.html", true,
                """
                    <div class="block"><a href="../../../com.ex1/com/ex1/package-summary.html"><code>package link</code></a>
                     <a href="../../../com.ex1/module-summary.html"><code>module link</code></a>
                     <a href="../../../com.ex1/com/ex1/package-summary.html"><code>com.ex1</code></a>
                     <a href="../../../com.ex1/com/ex1/A.html" title="class in com.ex1"><code>class link</code></a>
                     <a href="../../../com.ex1/com/ex1/A.html#m()"><code>A.m()</code></a>
                     <a href="../../../com.ex1/com/ex1/A.html#m()"><code>A.m()</code></a>
                     <a href="package-summary.html"><code>com.ex2</code></a>
                     <a href="../../module-summary.html"><code>com.ex2</code></a></div>
                    """);
    }

    @Test
    public void testLinkModuleSameNameExternal(Path base) throws Exception {
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
                    <div class="block"><a href="../../../../out1/com.ex1/com/ex1/package-summary.html" class="external-link"><code>package link</code></a>
                     <a href="../../../../out1/com.ex1/module-summary.html" class="external-link"><code>module link</code></a>
                     <a href="../../../../out1/com.ex1/com/ex1/package-summary.html" class="external-link"><code>com.ex1/com.ex1</code></a>
                     <a href="../../../../out1/com.ex1/com/ex1/A.html" title="class or interface in com.ex1" class="external-link"><code>class link</code></a>
                     <a href="../../../../out1/com.ex1/com/ex1/A.html#m()" title="class or interface in com.ex1" class="external-link"><code>A.m()</code></a>
                     <a href="../../../../out1/com.ex1/com/ex1/A.html#m()" title="class or interface in com.ex1" class="external-link"><code>A.m()</code></a>
                     <a href="package-summary.html"><code>com.ex2</code></a>
                     <a href="../../module-summary.html"><code>com.ex2</code></a></div>
                    """);
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
                     * {@link m1}
                     * {@link m1/}
                     * {@link m1/com.m1.lib package link}
                     * {@link m1/com.m1.lib.Lib}
                     * {@link m1/com.m1.lib.Lib#method}
                     * {@link m1/com.m1.lib.Lib#method(String)}
                     * {@linkplain m2}
                     * {@linkplain m2/}
                     * {@linkplain m2/com.m2.lib}
                     * {@linkplain m2/com.m2.lib.Lib}
                     * {@linkplain m2/com.m2.lib.Lib#method class link}
                     * {@linkplain m2/com.m2.lib.Lib#method(String)}
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
                     * {@link com.ex1 package link}
                     * {@link com.ex1/ module link}
                     * {@link com.ex1/com.ex1}
                     * {@link com.ex1/com.ex1.A class link}
                     * {@link com.ex1/com.ex1.A#m}
                     * {@link com.ex1/com.ex1.A#m()}
                     * {@link com.ex2}
                     * {@link com.ex2/}
                     */
                    public B(A obj){}
                    }
                    """)
                .write(src);

    }

}

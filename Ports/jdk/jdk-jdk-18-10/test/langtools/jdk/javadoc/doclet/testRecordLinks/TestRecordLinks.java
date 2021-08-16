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
 * @bug      8236539 8246774
 * @summary  Relative link tags in record javadoc don't resolve
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestRecordLinks
 */

import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestRecordLinks  extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestRecordLinks tester = new TestRecordLinks();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    @Test
    public void testCrash(Path base) throws Exception {
        // from JDK-8236539
        String example = """
                package example;
                public class JavadocTest {
                  /**
                   * {@link #foo()}
                   * {@link Bar}
                   */
                  public static class Foo {
                    public void foo() { }
                  }

                  /**
                   * {@link #bar()}
                   * {@link Foo}
                   */
                  public record Bar() {
                    public void bar() { }
                  }
                }
                """;

        Path src = base.resolve("src");
        tb.writeJavaFiles(src, example);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "example");
        checkExit(Exit.OK);

        checkOutput("example/JavadocTest.Foo.html", true,
                """
                    <h1 title="Class JavadocTest.Foo" class="title">Class JavadocTest.Foo</h1>
                    """,
                """
                    <div class="block"><a href="#foo()"><code>foo()</code></a>
                     <a href="JavadocTest.Bar.html" title="class in example"><code>JavadocTest.Bar</code></a></div>
                    """);

        checkOutput("example/JavadocTest.Bar.html", true,
                """
                    <h1 title="Record Class JavadocTest.Bar" class="title">Record Class JavadocTest.Bar</h1>
                    """,
                """
                    <div class="block"><a href="#bar()"><code>bar()</code></a>
                     <a href="JavadocTest.Foo.html" title="class in example"><code>JavadocTest.Foo</code></a></div>
                    """);
    }
}

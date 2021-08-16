/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4490068 8075778
 * @summary  General tests for inline or block at-return tag
 * @library  /tools/lib ../../lib
 * @modules  jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestReturnTag
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestReturnTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestReturnTag tester = new TestReturnTag();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    ToolBox tb = new ToolBox();

    @Test // 4490068
    public void testInvalidReturn(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * Trigger warning message when return tag is used on a void method.
                         *
                         * @return I really don't return anything.
                         */
                        public void method() {}
                    }
                    """);

        javadoc("-Xdoclint:none",
                "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
            "warning: @return tag cannot be used in method with void return type.");
    }

    @Test
    public void testBlock(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * First sentence. Second sentence.
                         * @return the result
                         */
                        public int m() { return 0; }
                    }
                    """);

        javadoc("-Xdoclint:none",
                "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput("C.html", true,
                """
                    <div class="block">First sentence. Second sentence.</div>
                    <dl class="notes">
                    <dt>Returns:</dt>
                    <dd>the result</dd>
                    </dl>
                    """);
    }

    @Test
    public void testInlineShort(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * {@return the result}
                         */
                        public int m() { return 0; }
                    }
                    """);

        javadoc("-Xdoclint:none",
                "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput("C.html", true,
                """
                    <div class="block">Returns the result.</div>
                    <dl class="notes">
                    <dt>Returns:</dt>
                    <dd>the result</dd>
                    </dl>
                    """);
    }

    @Test
    public void testInlineLong(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * {@return the result} More text.
                         */
                        public int m() { return 0; }
                    }
                    """);

        javadoc("-Xdoclint:none",
                "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput("C.html", true,
                """
                    <div class="block">Returns the result. More text.</div>
                    <dl class="notes">
                    <dt>Returns:</dt>
                    <dd>the result</dd>
                    </dl>
                    """);
    }

    @Test
    public void testInlineMarkup(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * {@return abc {@code def} <b>ghi</b> jkl}
                         */
                        public int m() { return 0; }
                    }
                    """);

        javadoc("-Xdoclint:none",
                "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput("C.html", true,
                """
                    <div class="block">Returns abc <code>def</code> <b>ghi</b> jkl.</div>
                    <dl class="notes">
                    <dt>Returns:</dt>
                    <dd>abc <code>def</code> <b>ghi</b> jkl</dd>
                    </dl>
                    """);
    }

    @Test
    public void testBlockMarkup(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * @return abc {@code def} <b>ghi</b> jkl
                         */
                        public int m() { return 0; }
                    }
                    """);

        javadoc("-Xdoclint:none",
                "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput("C.html", true,
                """
                    <dl class="notes">
                    <dt>Returns:</dt>
                    <dd>abc <code>def</code> <b>ghi</b> jkl</dd>
                    </dl>
                    """);
    }

    @Test
    public void testEmptyInline(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * {@return}
                         */
                        public int m() { return 0; }
                    }
                    """);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
                "C.java:4: warning: no description for @return");

        checkOutput("C.html", true,
                """
                    <div class="block">Returns .</div>
                    <dl class="notes">
                    <dt>Returns:</dt>
                    </dl>
                    """);
    }

    @Test
    public void testInlineNotFirst(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * Some text. {@return the result} More text.
                         */
                        public int m() { return 0; }
                    }
                    """);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
                "C.java:4: warning: {@return} not at beginning of description");

        checkOutput("C.html", true,
                """
                    <div class="block">Some text. Returns the result. More text.</div>
                    </section>
                    """);
    }

    @Test
    public void testDuplicate(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class C {
                        /**
                         * {@return the result} More text.
                         * @return again
                         */
                        public int m() { return 0; }
                    }
                    """);

        javadoc( "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
                "C.java:5: warning: @return has already been specified");

        checkOutput("C.html", true,
            """
                <div class="block">Returns the result. More text.</div>
                <dl class="notes">
                <dt>Returns:</dt>
                <dd>again</dd>
                """);
    }

    @Test
    public void testSimpleInheritBlock(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class Super {
                        /**
                         * @return the result
                         */
                        public int m() { return 0; }
                    }
                    """,
                """
                    /** Comment. */
                    public class C extends Super {
                        @Override
                        public int m() { return 1; }
                    }
                    """);

        javadoc( "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput("C.html", true,
                """
                    <dl class="notes">
                    <dt>Overrides:</dt>
                    <dd><code>m</code>&nbsp;in class&nbsp;<code>Super</code></dd>
                    <dt>Returns:</dt>
                    <dd>the result</dd>
                    </dl>
                    """);
    }

    @Test
    public void testSimpleInheritInline(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class Super {
                        /**
                         * {@return the result}
                         */
                        public int m() { return 0; }
                    }
                    """,
                """
                    /** Comment. */
                    public class C extends Super {
                        @Override
                        public int m() { return 1; }
                    }
                    """);

        javadoc( "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput("C.html", true,
                """
                    <div class="block"><span class="descfrm-type-label">Description copied from class:&nbsp;<code>Super</code></span></div>
                    <div class="block">Returns the result.</div>
                    <dl class="notes">
                    <dt>Overrides:</dt>
                    <dd><code>m</code>&nbsp;in class&nbsp;<code>Super</code></dd>
                    <dt>Returns:</dt>
                    <dd>the result</dd>""");
    }

    @Test
    public void testPreferInlineOverInherit(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Comment. */
                    public class Super {
                        /**
                         * {@return the result}
                         */
                        public int m() { return 0; }
                    }
                    """,
                """
                    /** Comment. */
                    public class C extends Super {
                        /**
                         * {@return the overriding result}
                         */
                        @Override
                        public int m() { return 1; }
                    }
                    """);

        javadoc( "-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("C.java").toString());
        checkExit(Exit.OK);

        checkOutput("C.html", true,
                """
                    <div class="block">Returns the overriding result.</div>
                    <dl class="notes">
                    <dt>Overrides:</dt>
                    <dd><code>m</code>&nbsp;in class&nbsp;<code>Super</code></dd>
                    <dt>Returns:</dt>
                    <dd>the overriding result</dd>
                    </dl>
                    """);
    }
}

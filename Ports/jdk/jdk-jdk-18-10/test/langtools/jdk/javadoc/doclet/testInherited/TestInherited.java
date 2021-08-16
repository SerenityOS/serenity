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
 * @bug 8269722 8270866
 * @summary NPE in HtmlDocletWriter, reporting errors on inherited tags
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestInherited
 */

import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestInherited extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestInherited tester = new TestInherited();
        tester.runTests(m -> new Object[] { Path.of(m.getName())});
    }

    private final ToolBox tb = new ToolBox();

    @Test
    public void testBadInheritedParam(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, """
                public class BadParam {
                    public static class Base {
                        /**
                         * @param i a < b
                         */
                        public void m(int i) { }
                    }

                    public static class Sub extends Base {
                        public void m(int i) { }
                    }
                }
                """);

        javadoc("-d", base.resolve("out").toString(),
                "-Xdoclint:-missing", "-XDdoe",
                src.resolve("BadParam.java").toString());
        checkExit(Exit.OK);
        checkOutput("BadParam.Base.html", true, """
                <dt>Parameters:</dt>
                <dd><code>i</code> - a &lt; b</dd>
                """);
        checkOutput("BadParam.Sub.html", true, """
                <dt>Parameters:</dt>
                <dd><code>i</code> - a &lt; b</dd>
                """);
    }

    @Test
    public void testBadInheritedReturn(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, """
                public class BadReturn {
                    public static class Base {
                        /**
                         * @return  a < b
                         */
                        public int m() { }
                    }

                    public static class Sub extends Base {
                        public int m() { }
                    }
                }
                """);

        javadoc("-d", base.resolve("out").toString(),
                "-Xdoclint:-missing",
                src.resolve("BadReturn.java").toString());
        checkExit(Exit.OK);
        checkOutput("BadReturn.Base.html", true, """
                <dt>Returns:</dt>
                <dd>a &lt; b</dd>
                """);
        checkOutput("BadReturn.Sub.html", true, """
                <dt>Returns:</dt>
                <dd>a &lt; b</dd>
                """);
    }

    @Test
    public void testBadInheritedReference(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, """
                public class BadReference {
                    public interface Intf {
                        /**
                         * {@link NonExistingClass}
                         */
                        public void m();
                    }

                    public static class Impl1 implements Intf {
                        public void m() { }
                    }

                    public static class Impl2 implements Intf {
                        /**
                         * {@inheritDoc}
                         */
                        public void m() { }
                    }

                    // subclass has doc comment but inherits main description
                    public static class Impl3 implements Intf {
                        /**
                         * @since 1
                         */
                        public void m() { }
                    }
                }
                """);

        javadoc("-d", base.resolve("out").toString(),
                "-Xdoclint:-reference",
                src.resolve("BadReference.java").toString());
        checkExit(Exit.OK);
        checkOutput("BadReference.Intf.html", true, """
                <div class="block"><code>NonExistingClass</code></div>
                """);
        checkOutput("BadReference.Impl1.html", true, """
                <div class="block"><code>NonExistingClass</code></div>
                """);
        checkOutput("BadReference.Impl2.html", true, """
                <div class="block"><code>NonExistingClass</code></div>
                """);
        checkOutput("BadReference.Impl3.html", true, """
                <div class="block"><code>NonExistingClass</code></div>
                """);
    }
}

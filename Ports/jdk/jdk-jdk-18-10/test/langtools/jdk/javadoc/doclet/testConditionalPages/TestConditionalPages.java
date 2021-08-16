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
 * @bug      8254721
 * @summary  Improve support for conditionally generated files
 * @library  /tools/lib ../../lib
 * @modules  jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.* TestConditionalPages
 * @run main TestConditionalPages
 */


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import javadoc.tester.JavadocTester;
import toolbox.Task;
import toolbox.ToolBox;

public class TestConditionalPages extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestConditionalPages tester = new TestConditionalPages();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    ToolBox tb = new ToolBox();

    @Test
    public void testConstantValues(Path base) throws IOException {
        test(base, """
                package p;
                public class C {
                    public static final int ZERO = 0;
                }
                """,
                "constant-values.html",
                b -> checkOutput("index-all.html", b, "Constant&nbsp;Field&nbsp;Values"));
    }

    @Test
    public void testDeprecated(Path base) throws IOException {
        test(base, """
                package p;
                @Deprecated
                public class C {  }
                """,
                "deprecated-list.html",
                b -> checkOutput("index-all.html", b, "Deprecated"));
    }

    @Test
    public void testSerializedForm(Path base) throws IOException {
        test(base, """
                package p;
                import java.io.Serializable;
                public class C implements Serializable {  }
                """,
                "serialized-form.html",
                b -> checkOutput("index-all.html", b, "Serialized&nbsp;Form"));
    }

    @Test
    public void testSystemProperties(Path base) throws IOException {
        test(base, """
                package p;
                /** This class uses {@systemProperty line.separator}. */
                public class C {  }
                """,
                "system-properties.html",
                b -> checkOutput("index-all.html", b, "System&nbsp;Properties"));
    }

    void test(Path base, String code, String file, Consumer<Boolean> extraChecks) throws IOException {
        test(base.resolve("a"), code, file, extraChecks, true);
        test(base.resolve("b"), "package p; public class C { }", file, extraChecks, false);
    }

    void test(Path base, String code, String file, Consumer<Boolean> extraChecks, boolean expect) throws IOException {
        Path src = Files.createDirectories(base.resolve("src"));
        tb.writeJavaFiles(src, code);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkFiles(expect, file);
        checkOutput(Output.OUT, expect, "Generating " + base.resolve("out").resolve(file));
        extraChecks.accept(expect);
    }
}

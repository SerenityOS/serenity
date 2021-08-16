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
 * @bug      8258002
 * @summary  Update "type" terminology in generated docs
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestTerminology
 */

import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.SourceVersion;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestTerminology extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestTerminology tester = new TestTerminology();
        tester.runTests(m -> new Object[]{Path.of(m.getName())});
    }

    private final ToolBox tb = new ToolBox();

    @Test
    public void testAnnotationInterface(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p; /** Comment. */ public @interface A {
                        int m();
                    }
                    """
        );
        testAnnotationInterface(base, src, SourceVersion.RELEASE_15);
        testAnnotationInterface(base, src, SourceVersion.latest());
    }

    void testAnnotationInterface(Path base, Path src, SourceVersion sv) {
        String v = asOption(sv);
        javadoc("-d", base.resolve("out" + v).toString(),
                "--source-path", src.toString(),
                "--source", v,
                "-use",
                "p");
        checkExit(Exit.OK);

        checkOutput("p/A.html", sv.compareTo(SourceVersion.RELEASE_16) < 0,
                """
                    <h1 title="Annotation Type A" class="title">Annotation Type A</h1>"""
        );
        checkOutput("p/A.html", sv.compareTo(SourceVersion.RELEASE_16) >= 0,
                """
                    <h1 title="Annotation Interface A" class="title">Annotation Interface A</h1>"""
        );


        checkOutput("p/class-use/A.html", sv.compareTo(SourceVersion.RELEASE_16) < 0,
                """
                    <h1 title="Uses of Annotation Type p.A" class="title">Uses of Annotation Type<br>p.A</h1>"""
        );
        checkOutput("p/class-use/A.html", sv.compareTo(SourceVersion.RELEASE_16) >= 0,
                """
                    <h1 title="Uses of Annotation Interface p.A" class="title">Uses of Annotation Interface<br>p.A</h1>"""
        );


    }

    @Test
    public void testEnumClass(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p;  /** Comment. */ public enum E { }
                    """
        );
        testEnumClass(base, src, SourceVersion.RELEASE_15);
        testEnumClass(base, src, SourceVersion.latest());
    }

    void testEnumClass(Path base, Path src, SourceVersion sv) {
        String v = asOption(sv);
        javadoc("-d", base.resolve("out" + v).toString(),
                "--source-path", src.toString(),
                "--source", v,
                "p");
        checkExit(Exit.OK);

        checkOutput("p/E.html", sv.compareTo(SourceVersion.RELEASE_16) < 0,
                """
                        <h1 title="Enum E" class="title">Enum E</h1>"""
        );
        checkOutput("p/E.html", sv.compareTo(SourceVersion.RELEASE_16) >= 0,
                """
                        <h1 title="Enum Class E" class="title">Enum Class E</h1>"""
        );
    }

    @Test
    public void testSearch(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p;  /** Comment. */ public class C { }
                    """
        );
        testSearch(base, src, SourceVersion.RELEASE_15);
        testSearch(base, src, SourceVersion.latest());
    }

    public void testSearch(Path base, Path src, SourceVersion sv) {
        String v = asOption(sv);
        javadoc("-d", base.resolve("out" + v).toString(),
                "--source-path", src.toString(),
                "--source", v,
                "p");
        checkExit(Exit.OK);

        checkOutput("search.js", sv.compareTo(SourceVersion.RELEASE_16) < 0,
                """
                        var catTypes = "Types";""" //
        );
        checkOutput("search.js", sv.compareTo(SourceVersion.RELEASE_16) >= 0,
                """
                        var catTypes = "Classes and Interfaces";"""
        );
    }

    private String asOption(SourceVersion sv) {
        return sv.name().replace("RELEASE_", "");
    }
}

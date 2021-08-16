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
 * @bug      8265042
 * @summary  javadoc HTML files not generated for types nested in records
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestNestedClasses
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestNestedClasses extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestNestedClasses tester = new TestNestedClasses();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
        tester.runLater();
    }

    public final ToolBox tb;
    public final Set<ElementKind> testedKinds = new TreeSet<>();


    public TestNestedClasses() {
        tb = new ToolBox();
    }

    // This is testing the possibility that new kinds of class or interface may be added in future,
    // in which case, this test will fail until it is updated.
    public void runLater() throws Exception {
        Set<ElementKind> expectKinds = Arrays.stream(ElementKind.values())
                .filter(k -> k.isClass() || k.isInterface())
                .collect(Collectors.toCollection(TreeSet::new));
        if (!testedKinds.equals(expectKinds)) {
            out.println("Expected: " + expectKinds);
            out.println("  Tested: " + testedKinds);
            throw new Exception("tested element kinds do not match expected element kinds");
        }
    }

    @Test
    public void testClass(Path base) throws IOException {
        testedKinds.add(ElementKind.CLASS);
        test(base, "C", """
                package p;
                public class C {
                    ##INSERT##
                    public void m() { }
                }
                """);
    }

    @Test
    public void testEnumClass(Path base) throws IOException {
        testedKinds.add(ElementKind.ENUM);
        test(base, "E", """
                package p;
                public enum E {
                    E1, E2, E3;
                    ##INSERT##
                    public void m() { }
                }
                """);
    }

    @Test
    public void testRecordClass(Path base) throws IOException {
        testedKinds.add(ElementKind.RECORD);
        test(base, "R", """
                package p;
                public record R(int x, int y) {
                    ##INSERT##
                    public void m() { }
                }
                """);
    }

    @Test
    public void testInterface(Path base) throws IOException {
        testedKinds.add(ElementKind.INTERFACE);
        test(base, "I", """
                package p;
                public interface I {
                    ##INSERT##
                    public void m();
                }
                """);
    }

    @Test
    public void testAnnotationInterface(Path base) throws IOException {
        testedKinds.add(ElementKind.ANNOTATION_TYPE);
        test(base, "A", """
                package p;
                public @interface A {
                ##INSERT##
                    public static final int a = 0;
                }
                """);
    }

    void test(Path base, String name, String template) throws IOException {
        Path src = base.resolve("src");
        String nested = """
                public class NC { }
                public enum NE { NE1, NE2 }
                public record NR(float r, float theta) { }
                public interface NI { }
                public @interface NA { }
                public class ND { public class NE { } }
            """;
        tb.writeJavaFiles(src, template.replace("##INSERT##", nested));

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "-Xdoclint:-missing",
                "p");
        checkExit(Exit.OK);

        List<String> nestedClasses = List.of("NC", "NE", "NR", "NI", "NA", "ND", "ND.NE");

        List<String> files = Stream.concat(
                List.of(name, "package-summary", "package-tree").stream()
                        .map(n -> String.format("p/%s.html", n)),
                nestedClasses.stream()
                        .map(n -> String.format("p/%s.%s.html", name, n))
                ).toList();
        checkFiles(true, files);
    }
}

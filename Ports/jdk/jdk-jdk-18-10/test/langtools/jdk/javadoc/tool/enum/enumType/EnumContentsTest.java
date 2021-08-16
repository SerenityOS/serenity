/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4421066
 * @summary Verify the contents of an enum type.
 * @library ../../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main EnumContentsTest
 */

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import jdk.javadoc.doclet.DocletEnvironment;

import javadoc.tester.JavadocTester;
import javadoc.tester.TestDoclet;

public class EnumContentsTest extends JavadocTester {

    public static void main(String[] args) throws Exception {
        JavadocTester t = new EnumContentsTest();
        t.runTests();
    }

    @Test
    public void testEnumContents() throws Exception {
        javadoc("-sourcepath", testSrc,
                "-docletpath", System.getProperty("test.class.path"),
                "-doclet", "EnumContentsTest$ThisDoclet",
                "pkg1");
        checkExit(Exit.OK);

        String expect = Files.readAllLines(Path.of(testSrc).resolve("expected.out"))
                .stream()
                .collect(Collectors.joining("\n"));
        checkOutput(Output.STDOUT, true, expect);
    }


    public static class ThisDoclet extends TestDoclet {
        public boolean run(DocletEnvironment root) {
            try {
                for (Element e : root.getIncludedElements()) {
                    if (e.getKind() == ElementKind.ENUM) {
                        printClass((TypeElement) e);
                    }
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        // this method mimics the printClass method from the old
        // tester framework
        void printClass(TypeElement te) {
            PrintStream out = System.out;
            out.format("%s %s%n",
                    te.getKind().toString().toLowerCase(),
                    te.getQualifiedName());
            out.format("  name: %s / %s / %s%n",
                    te.getSimpleName(), te.asType(), te.getQualifiedName());
            out.format("  superclass:%n    %s%n",
                    te.getSuperclass());
            out.format("  enum constants:%n");
            te.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
                    .forEach(e -> out.format("    %s%n", e.getSimpleName()));
            out.format("  methods:%n");
            te.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.METHOD)
                    .map(e -> (ExecutableElement) e)
                    .forEach(e -> out.format("    %s %s(%s)%n",
                            e.getReturnType(),
                            e.getSimpleName(),
                            e.getParameters().stream()
                                .map(this::paramToString)
                                .collect(Collectors.joining(", "))
                    ));

        }
        private String paramToString(Element e) {
            return ((DeclaredType) e.asType()).asElement().getSimpleName().toString();
        }
    }
}

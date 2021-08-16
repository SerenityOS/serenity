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
 * @summary Verify the comments in an enum type.
 * @library ../../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main EnumCommentTest
 */

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import javadoc.tester.JavadocTester;
import javadoc.tester.TestDoclet;
import jdk.javadoc.doclet.DocletEnvironment;

public class EnumCommentTest extends JavadocTester {

    public static void main(String[] args) throws Exception {
        JavadocTester t = new EnumCommentTest();
        t.runTests();
    }

    @Test
    public void testEnumComments() {
        javadoc("-sourcepath", testSrc,
                "-docletpath", System.getProperty("test.class.path"),
                "-doclet", "EnumCommentTest$ThisDoclet",
                "pkg1");
        checkExit(Exit.OK);
    }

    public static class ThisDoclet extends TestDoclet {
        public boolean run(DocletEnvironment env) {
            Elements elements = env.getElementUtils();

            System.err.println("incl " + env.getIncludedElements());
            TypeElement operation = env.getIncludedElements()
                    .stream()
                    .filter(e -> e.getKind() == ElementKind.ENUM)
                    .map(e -> (TypeElement) e)
                    .findFirst()
                    .orElseThrow(() -> new Error("can't find enum Operation"));

            boolean ok = checkComment(elements.getDocComment(operation).trim(),
                    "Arithmetic operations.");

            for (VariableElement f : ElementFilter.fieldsIn(operation.getEnclosedElements())) {
                if (f.getSimpleName().contentEquals("plus")) {
                    ok = checkComment(elements.getDocComment(f).trim(),
                            "Addition")
                            && ok;
                    for (ExecutableElement m : ElementFilter.methodsIn(operation.getEnclosedElements())) {
                        if (m.getSimpleName().contentEquals("eval")) {
                            ok = checkComment(elements.getDocComment(m).trim(),
                                    "Perform arithmetic operation represented by this constant.")
                                    && ok;
                            break;
                        }
                    }
                    break;
                }
            }
            if (!ok) {
                throw new Error("Comments don't match expectations.");
            } else {
                return true;
            }
        }

        private boolean checkComment(String found, String expected) {
            System.out.println("expected: \"" + expected + "\"");
            System.out.println("found:    \"" + found + "\"");
            return expected.equals(found);
        }
    }
}

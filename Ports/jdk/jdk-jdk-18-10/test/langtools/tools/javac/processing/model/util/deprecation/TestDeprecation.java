/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6392818
 * @summary Tests Elements.isDeprecated(Element)
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor
 * @compile TestDeprecation.java
 * @compile -processor TestDeprecation -proc:only Dep1.java
 * @compile Dep1.java
 * @compile -processor TestDeprecation -proc:only Dep1 TestDeprecation.java
 */

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.tools.Diagnostic.Kind.*;
import java.io.Writer;

/**
 * This processor verifies that the information returned by
 * getElementsAnnotatedWith is consistent with the expected results
 * stored in an AnnotatedElementInfo annotation.
 */
public class TestDeprecation extends JavacTestingAbstractProcessor {

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        boolean failure = false;
        if (!roundEnv.processingOver()) {
            DeprecationChecker deprecationChecker = new DeprecationChecker();

            for(Element element: roundEnv.getRootElements() ) {
                System.out.println("\nRoot Element: " + element.getSimpleName());
                failure = deprecationChecker.scan(element);
            }

            if (failure)
                processingEnv.getMessager().printMessage(ERROR, "Deprecation mismatch found!");
        }
        return true;
    }

    private class DeprecationChecker extends ElementScanner<Boolean,Void> {
        private Elements elementUtils;
        private boolean failure;
        DeprecationChecker() {
            super(false);
            elementUtils = processingEnv.getElementUtils();
            failure = false;
        }

        @Override
        public Boolean scan(Element e, Void p) {
            boolean expectedDeprecation = false;
            ExpectedDeprecation tmp = e.getAnnotation(ExpectedDeprecation.class);
            if (tmp != null)
                expectedDeprecation = tmp.value();
            boolean actualDeprecation = elementUtils.isDeprecated(e);

            System.out.printf("\tVisiting %s\t%s%n", e.getKind(), e.getSimpleName());

            if (expectedDeprecation != actualDeprecation) {
                failure = true;
                java.io.StringWriter w = new java.io.StringWriter();
                elementUtils.printElements(w, e);
                System.out.printf("For the deprecation of %n\t%s\t, expected %b, got %b.%n",
                                  w.getBuffer().toString(),
                                  expectedDeprecation, actualDeprecation);
            }
            super.scan(e, p);
            return failure;
        }
    }
}

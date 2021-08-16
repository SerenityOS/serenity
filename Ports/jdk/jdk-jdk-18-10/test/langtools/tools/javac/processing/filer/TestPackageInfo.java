/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6380018 6392177 6993311 8193462
 * @summary Test the ability to create and process package-info.java files
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile TestPackageInfo.java
 * @compile -processor TestPackageInfo -proc:only foo/bar/package-info.java TestPackageInfo.java
 */

import java.util.Set;
import java.util.Map;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import static javax.lang.model.SourceVersion.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;
import static javax.tools.Diagnostic.Kind.*;
import static javax.tools.StandardLocation.*;

import java.io.*;

/**
 * Test the ability to process annotations on package-info.java files:
 * 1) Visibility of package-info files from the command line
 * 2) Visibility of generated package-info.java source files
 */
public class TestPackageInfo extends JavacTestingAbstractProcessor {
    private int round = 0;

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        round++;

        // Verify annotations are as expected
        Set<TypeElement> expectedAnnotations =
            Set.of(eltUtils.getTypeElement("java.lang.Deprecated"));

        if (!roundEnv.processingOver()) {
            System.out.println("\nRound " + round);
            int rootElementSize = roundEnv.getRootElements().size();

            for(Element elt : roundEnv.getRootElements()) {
                System.out.printf("%nElement %s\tkind: %s%n", elt.getSimpleName(), elt.getKind());
                eltUtils.printElements(new PrintWriter(System.out), elt);
            }

            switch (round) {
            case 1:
                if (rootElementSize != 2)
                    throw new RuntimeException("Unexpected root element size " + rootElementSize);

                // Note that foo.bar.FuBar, an element of package
                // foo.bar, contains @Deprecated which should *not* be
                // included in the set of annotations to process
                if (!expectedAnnotations.equals(annotations)) {
                    throw new RuntimeException("Unexpected annotations: " + annotations);
                }

                try {
                    try {
                        filer.createClassFile("package-info");
                        throw new RuntimeException("Created class file for \"package-info\".");
                    } catch(FilerException fe) {}

                    try(PrintWriter pw =
                        new PrintWriter(filer.createSourceFile("foo.package-info").openWriter())) {
                        pw.println("@Deprecated");
                        pw.println("package foo;");
                    }

                    attemptReopening("foo.package-info");
                    attemptReopening("TestPackageInfo");      // Initial input
                    attemptReopening("foo.bar.package-info"); // Initial input
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
                break;

            case 2:
                // Expect foo.package-info
                Set<Element> expectedElement = Set.of(eltUtils.getPackageElement("foo"));
                if (!expectedElement.equals(roundEnv.getRootElements()))
                    throw new RuntimeException("Unexpected root element set " + roundEnv.getRootElements());

                if (!expectedAnnotations.equals(annotations)) {
                    throw new RuntimeException("Unexpected annotations: " + annotations);
                }

                attemptReopening("foo.package-info");

                break;

            default:
                throw new RuntimeException("Unexpected round number " + round);
            }
        }
        return false;
    }

    private void attemptReopening(String name) {
        final String SHOULD_NOT_REACH = "Should not reach: created ";
        try {
            try {
                filer.createSourceFile(name);
                throw new AssertionError(SHOULD_NOT_REACH + name + ".java");
            } catch (FilerException fe) {
                ; // Expected
            }

            try {
                filer.createClassFile(name);
                throw new AssertionError(SHOULD_NOT_REACH + name + ".class");
            } catch (FilerException fe) {
                ; // Expected
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }
}

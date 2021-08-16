/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6453386 8216404 8230337
 * @summary Test Elements.getPackageOf
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestGetPackageOf
 * @compile -processor TestGetPackageOf -proc:only TestGetPackageOf.java
 */

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import static javax.lang.model.SourceVersion.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;
import static javax.tools.Diagnostic.Kind.*;
import static javax.tools.StandardLocation.*;

/**
 * Test basic workings of Elements.getPackageOf
 */
public class TestGetPackageOf extends JavacTestingAbstractProcessor {
    /**
     * Check expected behavior on classes and packages and other elements.
     */
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            TypeElement    charElt     = eltUtils.getTypeElement("java.lang.Character");
            PackageElement javaLangPkg = eltUtils.getPackageElement("java.lang");
            PackageElement unnamedPkg  = eltUtils.getPackageElement("");

            Map<Element, PackageElement> testCases =
                Map.of(javaLangPkg, javaLangPkg,
                       charElt,     javaLangPkg,
                       unnamedPkg,  unnamedPkg);

            for (var testCase : testCases.entrySet()) {
                checkPkg(testCase.getKey(), testCase.getValue());
            }

            // The package of fields and methods and nested types of
            // java.lang.Character is java.lang.
            for (Element e : charElt.getEnclosedElements()) {
                checkPkg(e, javaLangPkg);
            }

            // A module has a null package.
            checkPkg(eltUtils.getModuleElement("java.base"), null);
        }
        return true;
    }

    private void checkPkg(Element e, PackageElement expectedPkg) {
        PackageElement actualPkg = eltUtils.getPackageOf(e);
        if (!Objects.equals(actualPkg, expectedPkg)) {
            throw new RuntimeException(String.format("Unexpected package ``%s''' for %s %s, expected ``%s''%n",
                                                     actualPkg, e.getKind(), e.toString(), expectedPkg));
        }
    }
}

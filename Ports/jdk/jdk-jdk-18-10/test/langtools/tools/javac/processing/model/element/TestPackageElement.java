/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6449798 6399404 8173776 8163989
 * @summary Test basic workings of PackageElement
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestPackageElement
 * @compile -processor TestPackageElement -proc:only             TestPackageElement.java
 * @compile -processor TestPackageElement -proc:only --release 8 TestPackageElement.java
 */

import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import static javax.lang.model.SourceVersion.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;
import static javax.tools.Diagnostic.Kind.*;
import static javax.tools.StandardLocation.*;

/**
 * Test basic workings of PackageElement.
 */
public class TestPackageElement extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            PackageElement unnamedPkg = eltUtils.getPackageElement("");

            testNames(unnamedPkg, "", "");

            // The next line tests an implementation detail upon which
            // some diagnostics depend.
            if (!unnamedPkg.toString().equals("unnamed package"))
                throw new RuntimeException(
                                "toString on unnamed package: " + unnamedPkg);

            if (!unnamedPkg.isUnnamed())
                throw new RuntimeException("The isUnnamed method on the unnamed package returned false!");

            PackageElement javaLang = eltUtils.getPackageElement("java.lang");
            if (javaLang.isUnnamed())
                throw new RuntimeException("Package java.lang is unnamed!");

            testNames(javaLang, "java.lang", "lang");

            testEnclosingElement(javaLang);
        }
        return true;
    }

    void testNames(PackageElement pkg, String expectedQualified, String expectedSimple) {
        String tmp = pkg.getQualifiedName().toString();
        if (!tmp.equals(expectedQualified))
            throw new RuntimeException("Unexpected qualifed name ``" + tmp + "''.");

        tmp = pkg.getSimpleName().toString();
        if (!tmp.equals(expectedSimple))
            throw new RuntimeException("Unexpected simple name ``" + tmp + "''.");
    }

    void testEnclosingElement(PackageElement javaLang) {
        SourceVersion version = processingEnv.getSourceVersion();
        Element enclosing = javaLang.getEnclosingElement();
        Element expectedEnclosing =
            (version.compareTo(RELEASE_9) < 0) ?  // No modules
            null :
            eltUtils.getModuleElement("java.base");

        if (!Objects.equals(enclosing, expectedEnclosing))
            throw new RuntimeException("Unexpected enclosing element under source version " +
                                       version);
    }
}

/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8173945
 * @summary Test Elements.getAll{Type, Package, Module}Elements
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestAllFoos
 * @compile -processor TestAllFoos -proc:only --release 8 --source-path modules/m1/pkg  modules/m1/pkg/C.java
 * @compile -processor TestAllFoos -proc:only --release 8 --source-path modules/m2/pkg  modules/m2/pkg/C.java
 */
// @compile -processor TestAllFoos -proc:only             --module-source-path  modules -m m1,m2

import java.util.Set;
import static java.util.Objects.*;
import javax.annotation.processing.*;
import static javax.lang.model.SourceVersion.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;

/**
 * Test basic workings of Elements.getAll{Type, Package, Module}Elements under
 * pre- and post-modules.
 */
public class TestAllFoos extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            boolean expectModules =
                (processingEnv.getSourceVersion().compareTo(RELEASE_9) >= 0);

            testSetSize(eltUtils.getAllTypeElements("java.lang.String"), 1);
            testSetSize(eltUtils.getAllTypeElements("example.com"), 0);

            if (!expectModules) {
                // Expect empty modules set, single package named "pkg" with one type "pkg.C".
                testSetSize(eltUtils.getAllModuleElements(), 0);
                testSetSize(eltUtils.getAllPackageElements("pkg"), 1);
                testSetSize(eltUtils.getAllTypeElements("pkg.C"),  1);
            } else {
                Set<? extends ModuleElement> modules =
                    requireNonNull(eltUtils.getAllModuleElements());

                ModuleElement m1 = requireNonNull(eltUtils.getModuleElement("m1"));
                ModuleElement m2 = requireNonNull(eltUtils.getModuleElement("m2"));

                if (!modules.contains(m1) ||
                    !modules.contains(m2) ||
                    !modules.contains(requireNonNull(eltUtils.getModuleElement("java.base"))))
                    throw new RuntimeException("Missing modules " + modules);

                // Expect two packages named "pkg" and two types named "pkg.C".
                testSetSize(eltUtils.getAllPackageElements("pkg"), 2);
                testSetSize(eltUtils.getAllTypeElements("pkg.C"),  2);
            }
        }
        return true;
    }

    /**
     * Check the set argument against null and throw an exception if
     * the set is not of the expected size.
     */
    private static <E> Set<E> testSetSize(Set<E> set, int expectedSize) {
        requireNonNull(set);
        if (set.size() != expectedSize)
            throw new RuntimeException("Unexpected size of set " + set);
        return set;
    }
}

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
 * @bug 8244146
 * @summary Test test and class file retention of strictfp.
 * @compile --release 16 TestStrictfpRetention.java StrictfpInSource.java
 * @compile         -processor   TestStrictfpRetention --release 16                       StrictfpHost.java
 * @compile/process -processor   TestStrictfpRetention --release 16  -proc:only           StrictfpHost
 * @compile         -processor   TestStrictfpRetention               -proc:only           StrictfpHost.java
 * @compile         -processor   TestStrictfpRetention -source 16                         StrictfpHost.java
 * @compile/process -processor   TestStrictfpRetention              -AstrictfpNotExpected StrictfpHost
 */

import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import static javax.lang.model.SourceVersion.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.tools.Diagnostic.Kind.*;

import static javax.lang.model.element.Modifier.*;

/**
 * The StrictfpInSource annotation marks whether or not the element in
 * source was declared strictp in its source form. For release 16 (and
 * earlier releases), the strictfp-in-source status and
 * strictfp-in-class file status should generally match. (Per JLS,
 * some elements are implicitly strictfp).
 *
 * Under release 17 and later releases reflecting JEP 306, while
 * strictfp can be present in the source, it is *not* persisted to the
 * class file for methods/constructors where ACC_STRICT can be
 * applied. (The ACC_STRICT modifier is not defined for
 * classes/interfaces in the JVM.)
 *
 * This test checks that the strictfp modifier of the annotated
 * elements is as expected in the four combinations:
 *
 * (source, class file) X (--release 16, current release)
 *
 * As well as the mixed combination of -source 16 and current release
 * as the implicit target.
 */
@SupportedOptions("strictfpNotExpected")
@SupportedAnnotationTypes("StrictfpInSource")
public class TestStrictfpRetention extends AbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            boolean annotatedElementsFound = false;
            boolean strictfpExpected = !processingEnv.getOptions().containsKey("strictfpNotExpected");
            var messager = processingEnv.getMessager();

            for (Element e: roundEnv.getElementsAnnotatedWith(StrictfpInSource.class)) {
                annotatedElementsFound = true;

                boolean strictfpPresent =  e.getModifiers().contains(STRICTFP);
                if (strictfpPresent != strictfpExpected) {
                    messager.printMessage(ERROR, "Unexpected strictfp status: " + strictfpPresent + " " + e, e);
                }
            }

            if (!annotatedElementsFound) {
                messager.printMessage(ERROR, "No annotated elements found");
            }
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}

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
 * @bug 8175335
 * @summary Test Types methods on module and package TypeMirrors
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build   JavacTestingAbstractProcessor TestPseudoTypeHandling
 * @compile -processor TestPseudoTypeHandling -proc:only TestPseudoTypeHandling.java
 */

import java.util.*;
import java.util.function.*;
import static java.util.Objects.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * Test basic handling of module type.
 */
public class TestPseudoTypeHandling extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            TypeMirror objectType  = requireNonNull(eltUtils.getTypeElement("java.lang.Object")).asType();

            List<TypeMirror> typeMirrorsToTest =
                List.of(requireNonNull(eltUtils.getModuleElement("java.base")).asType(),
                        requireNonNull(eltUtils.getPackageElement("java.lang")).asType());

            for (TypeMirror type : typeMirrorsToTest) {
                expectException(t -> typeUtils.isSubtype(t, objectType), type);
                expectException(t -> typeUtils.isSubtype(objectType, t), type);

                expectException(t -> typeUtils.isAssignable(t, objectType), type);
                expectException(t -> typeUtils.isAssignable(objectType, t), type);

                expectException(t -> typeUtils.contains(t, objectType), type);
                expectException(t -> typeUtils.contains(objectType, t), type);

                expectException(t -> typeUtils.capture(t), type);
                expectException(t -> typeUtils.erasure(t), type);

                expectException(t -> typeUtils.getArrayType(t), type);

                expectException(t -> typeUtils.directSupertypes(t), type);
            }
        }
        return true;
    }

    void expectException(Consumer<TypeMirror> argument, TypeMirror type) {
        try {
            argument.accept(type);
            throw new RuntimeException("Should not reach " + type.toString());
        } catch (IllegalArgumentException e) {
            ; // Expected
        }
    }
}

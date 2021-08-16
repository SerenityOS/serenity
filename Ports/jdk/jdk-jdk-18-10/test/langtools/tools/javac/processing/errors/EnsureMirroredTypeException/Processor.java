/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;

import com.sun.tools.javac.util.Assert;
import static com.sun.tools.javac.code.Symbol.TypeSymbol;

public class Processor extends JavacTestingAbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(A.class)) {
            A rtg = e.getAnnotation(A.class);

            try {
                rtg.a();
                Assert.check(false); //Should not reach here
            } catch (MirroredTypeException ex) {
                TypeMirror tm = ex.getTypeMirror();
                Assert.check(tm.getKind() == TypeKind.ERROR);

                TypeElement elm = (TypeElement)((DeclaredType)tm).asElement();
                Assert.check(elm.getQualifiedName().toString().
                        endsWith("some.path.to.SomeUnknownClass$Inner"));

                TypeSymbol sym = (TypeSymbol)elm;
                Assert.check(sym.name.contentEquals("some.path.to.SomeUnknownClass$Inner"));
            }
        }
        for (Element e : roundEnv.getElementsAnnotatedWith(B.class)) {
            B rtg = e.getAnnotation(B.class);

            try {
                rtg.a();
                Assert.check(false); //Should not reach here
            } catch (MirroredTypeException ex) {
                TypeMirror tm = ex.getTypeMirror();
                Assert.check(tm.getKind() == TypeKind.ERROR);

                TypeElement elm = (TypeElement)((DeclaredType)tm).asElement();
                Assert.check(elm.getQualifiedName().toString().
                        endsWith("SomeUnknownClass"));

                TypeSymbol sym = (TypeSymbol)elm;
                Assert.check(sym.name.contentEquals("SomeUnknownClass"));
            }
        }
        for (Element e : roundEnv.getElementsAnnotatedWith(C.class)) {
            C rtg = e.getAnnotation(C.class);

            try {
                rtg.a();
                Assert.check(false); //Should not reach here
            } catch (AnnotationTypeMismatchException ex) {
                ;
            }
        }
        return true;
    }

    @interface A {
        Class<?> a();
    }
    @interface B {
        Class<?> a();
    }
    @interface C {
        Class<?> a();
    }
}

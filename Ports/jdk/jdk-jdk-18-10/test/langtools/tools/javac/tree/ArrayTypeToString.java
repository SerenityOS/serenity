/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8068737
 * @summary Tests ArrayType.toString with type annotations present
 * @modules jdk.compiler/com.sun.tools.javac.code
 * @library /tools/javac/lib
 * @build ArrayTypeToString JavacTestingAbstractProcessor
 * @compile/ref=ArrayTypeToString.out -XDaccessInternalAPI -XDrawDiagnostics -processor ArrayTypeToString -proc:only ArrayTypeToString.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sun.tools.javac.code.Symbol.VarSymbol;

@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE_USE, ElementType.FIELD })
@interface Foo {
    int value();
}

@SupportedAnnotationTypes("Foo")
public class ArrayTypeToString extends JavacTestingAbstractProcessor {
    @Foo(0) String @Foo(1)[] @Foo(2)[] @Foo(3)[] field;

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        for (TypeElement te : tes) {
            for (Element e : renv.getElementsAnnotatedWith(te)) {
                String s = ((VarSymbol) e).type.toString();

                // Normalize output by removing whitespace
                s = s.replaceAll("\\s", "");

                // Expected: "@Foo(0)java.lang.String@Foo(3)[]@Foo(2)[]@Foo(1)[]"
                processingEnv.getMessager().printMessage(Kind.NOTE, s);
            }
        }
        return true;
    }
}

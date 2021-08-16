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

/*
 * @test
 * @bug 8007574
 * @summary Test Elements.isFunctionalInterface
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestIsFunctionalInterface
 * @compile -processor TestIsFunctionalInterface TestIsFunctionalInterface.java
 */

import java.util.Set;
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
 * Test basic workings of Elements.isFunctionalInterface
 */
public class TestIsFunctionalInterface extends JavacTestingAbstractProcessor {
    private int count = 0;
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for(TypeElement type : typesIn(roundEnv.getElementsAnnotatedWith(ExpectedIsFunInt.class))) {
                count++;
                System.out.println(type);
                if (elements.isFunctionalInterface(type) !=
                    type.getAnnotation(ExpectedIsFunInt.class).value()) {
                    messager.printMessage(ERROR,
                                          "Mismatch between expected and computed isFunctionalInterface",
                                          type);
                }
            }
        } else {
            if (count <= 0)
                messager.printMessage(ERROR, "No types with ExpectedIsFunInt processed.");
            }
    return true;
    }
}

@interface ExpectedIsFunInt {
    boolean value();
}

// Examples below from the lambda specification documents.

@ExpectedIsFunInt(false) // Equals is already an implicit member
interface Foo1 { boolean equals(Object obj); }

@ExpectedIsFunInt(true) // Bar has one abstract non-Object method
interface Bar1 extends Foo1 { int compare(String o1, String o2); }


@ExpectedIsFunInt(true) // Comparator has one abstract non-Object method
interface LocalComparator<T> {
 boolean equals(Object obj);
 int compare(T o1, T o2);
}

@ExpectedIsFunInt(false) // Method Object.clone is not public
interface Foo2 {
  int m();
  Object clone();
}

interface X1 { int m(Iterable<String> arg); }
interface Y1 { int m(Iterable<String> arg); }
@ExpectedIsFunInt(true) // Two methods, but they have the same signature
interface Z1 extends X1, Y1 {}

interface X2 { Iterable m(Iterable<String> arg); }
interface Y2 { Iterable<String> m(Iterable arg); }
@ExpectedIsFunInt(true) // Y.m is a subsignature & return-type-substitutable
interface Z2 extends X2, Y2 {}

interface Action<T> {
    T doit();
}
@ExpectedIsFunInt(true)
interface LocalExecutor { <T> T execute(Action<T> a); }

interface X5 { <T> T execute(Action<T> a); }
interface Y5 { <S> S execute(Action<S> a); }
@ExpectedIsFunInt(true) // Functional: signatures are "the same"
interface Exec5 extends X5, Y5 {}

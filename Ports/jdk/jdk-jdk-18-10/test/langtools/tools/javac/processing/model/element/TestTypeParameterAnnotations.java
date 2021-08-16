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
 * @bug 8011027 8046916
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor TestTypeParameterAnnotations
 * @compile -processor TestTypeParameterAnnotations -proc:only TestTypeParameterAnnotations.java
 */

import java.util.*;
import java.lang.annotation.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;

@ExpectedTypeParameterAnnotations(typeParameterName="T1",
                                  annotations={"Foo1", "Bar1", "Baz1"})
@ExpectedTypeParameterAnnotations(typeParameterName="T2", annotations={})
@ExpectedTypeParameterAnnotations(typeParameterName="T3",
                                  annotations={"Foo2", "Bar2", "Baz2"})
@ExpectedTypeParameterAnnotations(typeParameterName="T4", annotations={})
public class TestTypeParameterAnnotations<@Foo1 @Bar1 @Baz1 T1, T2, @Foo2 @Bar2 @Baz2 T3, T4> extends
        JavacTestingAbstractProcessor {
    int round = 0;

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (++round == 1) {
            int found = (new Scanner()).scan(roundEnv.getRootElements(), null);
            if (found == expect) {
                ; //nop
            } else {
                error("unexpected number of results: expected " + expect
                        + ", found " + found);
            }

        }
        return true;
    }

    class Scanner extends JavacTestingAbstractProcessor.ElementScanner<Integer,Void> {
        @Override
        public Integer visitExecutable(ExecutableElement e, Void p) {
            super.visitExecutable(e, p);
            found += check(e, e.getTypeParameters());
            return found;
        }

        @Override
        public Integer visitType(TypeElement e, Void p) {
            super.visitType(e, p);
            found += check(e, e.getTypeParameters());
            return found;
        }

        int found;
    }

    int check(Element e, List<? extends TypeParameterElement> typarams) {
        if (typarams.isEmpty())
            return 0;

        for (TypeParameterElement tpe : typarams) {
            ExpectedTypeParameterAnnotations expected = null;
            for (ExpectedTypeParameterAnnotations a : e.getAnnotationsByType(ExpectedTypeParameterAnnotations.class)) {
                if (tpe.getSimpleName().contentEquals(a.typeParameterName())) {
                    expected = a;
                    break;
                }
            }
            if (expected == null) {
                throw new IllegalStateException("Does not have expected values annotation.");
            }
            checkAnnotationMirrors(tpe, tpe.getAnnotationMirrors(), expected);
            checkAnnotationMirrors(tpe, elements.getAllAnnotationMirrors(tpe), expected);
            checkGetAnnotation(tpe, expected);
            checkGetAnnotations(tpe, expected);
        }

        return typarams.size();
    }

    void checkAnnotationMirrors(TypeParameterElement tpe, List<? extends AnnotationMirror> l, ExpectedTypeParameterAnnotations expected) {
        String[] expectedAnnotations = expected.annotations();

        if (l.size() != expectedAnnotations.length) {
            error("Incorrect number of annotations, got " + l.size() +
                    ", should be " + expectedAnnotations.length, tpe);
            return ;
        }

        for (int i = 0; i < expectedAnnotations.length; i++) {
            AnnotationMirror m = l.get(i);
            if (!m.getAnnotationType().asElement().equals(elements.getTypeElement(expectedAnnotations[i]))) {
                error("Wrong type of annotation, was expecting @Foo", m.getAnnotationType().asElement());
                return ;
            }
        }
    }

    void checkGetAnnotation(TypeParameterElement tpe, ExpectedTypeParameterAnnotations expected) {
        List<String> expectedAnnotations = Arrays.asList(expected.annotations());

        for (Class<? extends Annotation> c : ALL_ANNOTATIONS) {
            Object a = tpe.getAnnotation(c);

            if (a != null ^ expectedAnnotations.indexOf(c.getName()) != (-1)) {
                error("Unexpected behavior for " + c.getName(), tpe);
                return ;
            }
        }
    }

    void checkGetAnnotations(TypeParameterElement tpe, ExpectedTypeParameterAnnotations expected) {
        List<String> expectedAnnotations = Arrays.asList(expected.annotations());

        for (Class<? extends Annotation> c : ALL_ANNOTATIONS) {
            Object[] a = tpe.getAnnotationsByType(c);

            if (a.length > 0 ^ expectedAnnotations.indexOf(c.getName()) != (-1)) {
                error("Unexpected behavior for " + c.getName(), tpe);
                return ;
            }
        }
    }

    void note(String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    void note(String msg, Element e) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg, e);
    }

    void error(String msg, Element e) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    void error(String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg);
    }

    Class<? extends Annotation>[] ALL_ANNOTATIONS = new Class[] {
        Foo1.class, Bar1.class, Baz1.class,
        Foo2.class, Bar2.class, Baz2.class,
    };

    // additional generic elements to test
    @ExpectedTypeParameterAnnotations(typeParameterName="W",
                                      annotations={"Foo1", "Bar1", "Baz1"})
    @ExpectedTypeParameterAnnotations(typeParameterName="X", annotations={})
    @ExpectedTypeParameterAnnotations(typeParameterName="Y",
                                      annotations={"Foo2", "Bar2", "Baz2"})
    @ExpectedTypeParameterAnnotations(typeParameterName="Z", annotations={})
    <@Foo1 @Bar1 @Baz1 W, X, @Foo2 @Bar2 @Baz2 Y, Z> X m(X x) { return x; }

    @ExpectedTypeParameterAnnotations(typeParameterName="W",
                                      annotations={"Foo1", "Bar1", "Baz1"})
    @ExpectedTypeParameterAnnotations(typeParameterName="X", annotations={})
    @ExpectedTypeParameterAnnotations(typeParameterName="Y",
                                      annotations={"Foo2", "Bar2", "Baz2"})
    @ExpectedTypeParameterAnnotations(typeParameterName="Z", annotations={})
    interface Intf<@Foo1 @Bar1 @Baz1 W, X, @Foo2 @Bar2 @Baz2 Y, Z> { X m() ; }

    @ExpectedTypeParameterAnnotations(typeParameterName="W",
                                      annotations={"Foo1", "Bar1", "Baz1"})
    @ExpectedTypeParameterAnnotations(typeParameterName="X", annotations={})
    @ExpectedTypeParameterAnnotations(typeParameterName="Y",
                                      annotations={"Foo2", "Bar2", "Baz2"})
    @ExpectedTypeParameterAnnotations(typeParameterName="Z", annotations={})
    class Clazz<@Foo1 @Bar1 @Baz1 W, X, @Foo2 @Bar2 @Baz2 Y, Z> {
        @ExpectedTypeParameterAnnotations(typeParameterName="W",
                                          annotations={"Foo1", "Bar1", "Baz1"})
        @ExpectedTypeParameterAnnotations(typeParameterName="X", annotations={})
        @ExpectedTypeParameterAnnotations(typeParameterName="Y",
                                          annotations={"Foo2", "Bar2", "Baz2"})
        @ExpectedTypeParameterAnnotations(typeParameterName="Z", annotations={})
        <@Foo1 @Bar1 @Baz1 W, X, @Foo2 @Bar2 @Baz2 Y, Z> Clazz() { }
    }

    final int expect = 5 * 4;  // top level class, plus preceding examples, 4 type variables each
}

@Target(ElementType.TYPE_PARAMETER)
@interface Foo1 {}

@Target(ElementType.TYPE_PARAMETER)
@interface Bar1 {}

@Target(ElementType.TYPE_PARAMETER)
@interface Baz1 {}

@Target(ElementType.TYPE_PARAMETER)
@interface Foo2 {}

@Target(ElementType.TYPE_PARAMETER)
@interface Bar2 {}

@Target(ElementType.TYPE_PARAMETER)
@interface Baz2 {}

@Repeatable(ExpectedTypeParameterAnnotationsCollection.class)
@interface ExpectedTypeParameterAnnotations {
    public String typeParameterName();
    public String[] annotations();
}

@interface ExpectedTypeParameterAnnotationsCollection {
    public ExpectedTypeParameterAnnotations[] value();
}

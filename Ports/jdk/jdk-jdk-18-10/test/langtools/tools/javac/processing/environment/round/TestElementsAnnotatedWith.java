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
 * @bug 6397298 6400986 6425592 6449798 6453386 6508401 6498938 6911854 8030049 8038080 8032230 8190886
 * @summary Tests that getElementsAnnotatedWith[Any] methods work properly.
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile annot/AnnotatedElementInfo.java annot/MarkerAnnot.java
 * @compile TestElementsAnnotatedWith.java
 * @compile InheritedAnnotation.java
 * @compile TpAnno.java
 * @compile Anno.java
 * @compile -processor TestElementsAnnotatedWith -proc:only SurfaceAnnotations.java
 * @compile -processor TestElementsAnnotatedWith -proc:only BuriedAnnotations.java
 * @compile -processor TestElementsAnnotatedWith -proc:only Part1.java Part2.java
 * @compile -processor TestElementsAnnotatedWith -proc:only C2.java
 * @compile -processor TestElementsAnnotatedWith -proc:only Foo.java
 * @compile -processor TestElementsAnnotatedWith -proc:only TypeParameterAnnotations.java
 * @compile -processor TestElementsAnnotatedWith -proc:only ParameterAnnotations.java
 * @compile -processor TestElementsAnnotatedWith -proc:only pkg/package-info.java
 * @compile -processor TestElementsAnnotatedWith -proc:only mod/quux/package-info.java
 * @compile -processor TestElementsAnnotatedWith -proc:only mod/quux/Quux.java
 * @compile  mod/quux/Quux.java mod/quux/package-info.java
 * @compile -processor TestElementsAnnotatedWith -proc:only -AsingleModuleMode=true mod/module-info.java
 * @compile/fail/ref=ErroneousAnnotations.out -processor TestElementsAnnotatedWith -proc:only -XDrawDiagnostics ErroneousAnnotations.java
 * @compile Foo.java
 * @compile/process -processor TestElementsAnnotatedWith -proc:only Foo
 */

import annot.AnnotatedElementInfo;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import static javax.lang.model.util.ElementFilter.*;

/**
 * This processor verifies that the information returned by
 * getElementsAnnotatedWith and getElementsAnnotatedWithAny is
 * consistent with the expected results stored in an
 * AnnotatedElementInfo annotation.
 */
@AnnotatedElementInfo(annotationName="java.lang.SuppressWarnings", expectedSize=0, names={})
public class TestElementsAnnotatedWith extends JavacTestingAbstractProcessor {

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        // First check sets of annotated elements using the round
        // environment from the annotation processing tool framework.
        checkSetOfAnnotatedElements(roundEnv);

        // Next check sets of annotated elements using a round
        // environment which uses the default implementations of the
        // getElementsAnnotatedWithAny methods from the interface.
        checkSetOfAnnotatedElements(new TestingRoundEnvironment(roundEnv));
        return true;
    }

    /**
     * To allow testing of the executable code of the default methods
     * for the two overloaded getElementsAnnotatedWithAny methods
     * defined in the RoundEnvironment interface, this class delegates
     * the non-default methods of RoundEnvironment to a given
     * RoundEnvironment object and then explicitly calls the default
     * methods of the interface instead of relying on the object's
     * implementation of those methods.
     */
    private class TestingRoundEnvironment implements RoundEnvironment {
        private RoundEnvironment re;

        public TestingRoundEnvironment(RoundEnvironment re) {
            this.re = re;
        }

        @Override
        public boolean errorRaised() {
            return re.errorRaised();
        }

        @Override
        public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> a) {
            return re.getElementsAnnotatedWith(a);
        }

        @Override
        public Set<? extends Element> getElementsAnnotatedWithAny(Set<Class<? extends Annotation>> a) {
            // Default method defined in the interface
            return RoundEnvironment.super.getElementsAnnotatedWithAny(a);
        }

        @Override
        public Set<? extends Element> getElementsAnnotatedWith(TypeElement a) {
            return re.getElementsAnnotatedWith(a);
        }

        @Override
        public Set<? extends Element> getElementsAnnotatedWithAny(TypeElement... a) {
            // Default method defined in the interface
            return RoundEnvironment.super.getElementsAnnotatedWithAny(a);
        }

        @Override
        public Set<? extends Element> getRootElements() {
            return re.getRootElements();
        }

        @Override
        public boolean processingOver() {
            return re.processingOver();
        }

    }

    /**
     * The method checks the following conditions:
     *
     * 1) The sets of elements found are equal for the TypeElement and
     * Class<? extends Annotation> methods on logically equivalent
     * arguments.
     *
     * 2) getElementsAnnotatedWithAny(X) is equal to
     * getElementsAnnotatedWith(X') where X is a set/var-args array
     * with one element and X' is the element.
     *
     * 3) Verify the result of getElementsAnnotatedWithAny({X, Y}) is equal to
     * getElementsAnnotatedWith(X) UNION getElementsAnnotatedWith(Y).
     */
    void checkSetOfAnnotatedElements(RoundEnvironment re) {
        // For the "Any" methods, search for both the expected
        // annotation and AnnotatedElementInfo and verify the return
        // set is the union of searching for AnnotatedElementInfo and
        // the other annotation
        Set<? extends Element> resultsMeta         = Collections.emptySet();
        Set<? extends Element> resultsMetaAny      = Collections.emptySet();
        Set<Element>           resultsMetaMulti    = new HashSet<>();
        Set<? extends Element> resultsMetaAnyMulti = Collections.emptySet();
        Set<? extends Element> resultsBase         = Collections.emptySet();
        Set<? extends Element> resultsBaseAny      = Collections.emptySet();
        Set<? extends Element> resultsBaseAnyMulti = Collections.emptySet();


        boolean singleModuleMode = processingEnv.getOptions().get("singleModuleMode") != null;

        TypeElement annotatedElemInfoElem = null;

        if (!re.processingOver()) {
            testNonAnnotations(re);

            // Verify AnnotatedElementInfo is present on the first
            // specified type.

            Element firstElement = re.getRootElements().iterator().next();

            AnnotatedElementInfo annotatedElemInfo =
                firstElement.getAnnotation(AnnotatedElementInfo.class);

            ModuleElement moduleContext;
            if (singleModuleMode) {
                // Should also be the case that firstElement.getKind() == ElementKind.MODULE
                moduleContext = (ModuleElement)firstElement;
            } else {
                moduleContext = elements.getModuleElement(""); // unnamed module
            }

            annotatedElemInfoElem =
                elements.getTypeElement(moduleContext, "annot.AnnotatedElementInfo");

            boolean failed = false;

            Objects.requireNonNull(annotatedElemInfo,
                                   "Missing AnnotatedElementInfo annotation on " + firstElement);

            // Verify that the annotation information is as expected.
            Set<String> expectedNames =
                new HashSet<>(Arrays.asList(annotatedElemInfo.names()));

            String annotationName = annotatedElemInfo.annotationName();
            TypeElement annotationTypeElem = elements.getTypeElement(moduleContext,
                                                                     annotationName);

            resultsMeta         = re.getElementsAnnotatedWith(annotationTypeElem);
            resultsMetaAny      = re.getElementsAnnotatedWithAny(annotationTypeElem);
            resultsMetaMulti.addAll(resultsMeta);
            resultsMetaMulti.addAll(re.getElementsAnnotatedWith(annotatedElemInfoElem));
            resultsMetaAnyMulti = re.getElementsAnnotatedWithAny(annotationTypeElem, annotatedElemInfoElem);

            if (!resultsMeta.isEmpty())
                System.err.println("Results: " + resultsMeta);

            if (!resultsMeta.equals(resultsMetaAny)) {
                failed = true;
                System.err.printf("Inconsistent Meta with vs withAny results");
            }

            if (resultsMeta.size() != annotatedElemInfo.expectedSize()) {
                failed = true;
                System.err.printf("Bad number of elements; expected %d, got %d%n",
                                  annotatedElemInfo.expectedSize(), resultsMeta.size());
            } else {
                for(Element element : resultsMeta) {
                    String simpleName = element.getSimpleName().toString();
                    if (!expectedNames.contains(simpleName) ) {
                        failed = true;
                        System.err.println("Name ``" + simpleName + "'' not expected.");
                    }
                }
            }

            resultsBase    = computeResultsBase(re, annotationName);
            resultsBaseAny = computeResultsBaseAny(re, annotationName);
            try {
                Set<Class<? extends Annotation>> tmp = new HashSet<>();
                tmp.add(AnnotatedElementInfo.class);
                tmp.add(Class.forName(annotationName).asSubclass(Annotation.class));
                resultsBaseAnyMulti = re.getElementsAnnotatedWithAny(tmp);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (!resultsBase.equals(resultsBaseAny)) {
                failed = true;
                System.err.printf("Inconsistent Base with vs withAny results");
            }

            if (!singleModuleMode && !resultsMeta.equals(resultsBase)) {
                failed = true;
                System.err.println("Base and Meta sets unequal;\n meta: " + resultsMeta +
                                   "\nbase: " + resultsBase);
            }

            if (!resultsMetaAnyMulti.equals(resultsMetaMulti)) {
                failed = true;
                System.err.println("MetaMultAny and MetaMulti sets unequal;\n meta: " + resultsMeta +
                                   "\nbase: " + resultsBase);
            }

            if (!singleModuleMode && !resultsBaseAnyMulti.equals(resultsMetaAnyMulti)) {
                failed = true;
                System.err.println("BaseMulti and MetaMulti sets unequal;\n meta: " + resultsMeta +
                                   "\nbase: " + resultsBase);
            }

            if (failed) {
                System.err.println("AnnotatedElementInfo: " + annotatedElemInfo);
                throw new RuntimeException();
            }
        } else {
            // If processing is over without an error, the specified
            // elements should be empty so an empty set should be
            // returned.

            throwOnNonEmpty(re.getElementsAnnotatedWith(AnnotatedElementInfo.class),    "resultsBase");
            throwOnNonEmpty(re.getElementsAnnotatedWithAny(Set.of(AnnotatedElementInfo.class)), "resultsBaseAny");

            if (!singleModuleMode) {
                // Could also use two-argument form of getTypeElement with an unnamed module argument.
                annotatedElemInfoElem = elements.getTypeElement("annot.AnnotatedElementInfo");
                throwOnNonEmpty(re.getElementsAnnotatedWith(annotatedElemInfoElem), "resultsMeta");
                throwOnNonEmpty(re.getElementsAnnotatedWithAny(annotatedElemInfoElem), "resultsMetaAny");
            }
        }
    }

    private void throwOnNonEmpty(Set<? extends Element> results, String message) {
        if (!results.isEmpty()) {
                throw new RuntimeException("Nonempty " + message +  "\t"  + results);
        }
    }

    private Set<? extends Element> computeResultsBase(RoundEnvironment roundEnv, String name) {
        try {
            return roundEnv.
                getElementsAnnotatedWith(Class.forName(name).asSubclass(Annotation.class));
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }

    private Set<? extends Element> computeResultsBaseAny(RoundEnvironment roundEnv, String name) {
        try {
            return roundEnv.
                getElementsAnnotatedWithAny(Set.of(Class.forName(name).asSubclass(Annotation.class)));
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }

    /**
     * Verify non-annotation types result in
     * IllegalArgumentExceptions.
     */
    private void testNonAnnotations(RoundEnvironment roundEnv) {
        Class objectClass = (Class)Object.class;
        Set<? extends Element> elements;
        try {
            elements = roundEnv.getElementsAnnotatedWith(objectClass);
            throw new RuntimeException("Illegal argument exception not thrown");
        } catch (IllegalArgumentException iae) {}

        try {
            elements = roundEnv.getElementsAnnotatedWithAny(Set.of(objectClass));
            throw new RuntimeException("Illegal argument exception not thrown");
        } catch (IllegalArgumentException iae) {}

        TypeElement objectElement = processingEnv.getElementUtils().getTypeElement("java.lang.Object");
        try {
            elements = roundEnv.getElementsAnnotatedWith(objectElement);
            throw new RuntimeException("Illegal argument exception not thrown");
        } catch (IllegalArgumentException iae) {}

        try {
            elements = roundEnv.getElementsAnnotatedWithAny(objectElement);
            throw new RuntimeException("Illegal argument exception not thrown");
        } catch (IllegalArgumentException iae) {}
    }
}

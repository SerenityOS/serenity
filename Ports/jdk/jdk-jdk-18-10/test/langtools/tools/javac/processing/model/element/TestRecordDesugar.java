/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test compiler desugaring of a record type
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @run main TestRecordDesugar
 */

import java.io.*;
import java.lang.annotation.*;
import java.nio.file.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.util.*;
import java.util.spi.ToolProvider;

/**
 * Tests of the desugaring of record types.
 */
public class TestRecordDesugar extends JavacTestingAbstractProcessor {
    public static void main(String... args) {
        String testSrc = System.getProperty("test.src");
        String testClasspath = System.getProperty("test.class.path");
        List<String> options = List.of(
                "-classpath", testClasspath,
                "-processor", "TestRecordDesugar",
                "-proc:only",
                Path.of(testSrc).resolve("TestRecordDesugar.java").toString()
        );

        System.out.println("Options: " + options);
        ToolProvider javac = ToolProvider.findFirst("javac").orElseThrow();
        int rc = javac.run(System.out, System.err, options.toArray(new String[0]));
        System.out.println("Return code: " + rc);
        if (rc != 0) {
            throw new AssertionError("unexpected return code: " + rc);
        }
    }

    int typeCount = 0;
    int failures = 0;

    public boolean process(Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv) {
       if (!roundEnv.processingOver()) {

           for(TypeElement nestedType :
                   ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(TypeElementInfo.class))) {
               typeCount++;
               // elements.printElements(new PrintWriter(System.out), nestedType);
               System.out.println("Testing " + nestedType.getQualifiedName());
               failures += compareWithAnnotation(nestedType);
           }

           if (typeCount <= 0) {
               throw new RuntimeException("Failed to visit elements");
           }

           if (failures > 0) {
               throw new RuntimeException(failures + " failures");
           }
       }
       return true;
    }

    int compareWithAnnotation(TypeElement nested) {
        int errors = 0;
        TypeElementInfo infoOnNested = nested.getAnnotation(TypeElementInfo.class);

        // Build a map of (kind + name) to ElementInfo to allow easier
        // lookups from the enclosed elements. The names of fields and
        // methods may overlap so using name alone is not sufficient
        // to disambiguate the elements. At this stage, do not use a
        // record to store a combined key.
        Map<String, ElementInfo> expectedInfoMap = new HashMap<>();
        for (ElementInfo elementInfo : infoOnNested.elements()) {
            String key = elementInfo.kind().toString() + " " + elementInfo.name();
            // System.out.println("Testing " + key);
            expectedInfoMap.put(elementInfo.kind().toString() + " " + elementInfo.name(),
                                elementInfo);
        }

        for (Element enclosedElement : nested.getEnclosedElements()) {
            System.out.println("\tChecking " + enclosedElement.getKind() + " " + enclosedElement);
            String key = enclosedElement.getKind().toString() + " " + enclosedElement.getSimpleName();
            ElementInfo expected = expectedInfoMap.get(key);
            Objects.requireNonNull(expected, "\t\tMissing mapping for " + elementToString(enclosedElement));

            expectedInfoMap.remove(key);

            // Name and kind must already match; check other values are as expected

            // Modifiers
            if (!enclosedElement.getModifiers().equals(Set.of(expected.modifiers()))) {
                errors++;
                System.out.println("\t\tUnexpected modifiers on " + enclosedElement + ":\t"
                                   + enclosedElement.getModifiers());
            }

            // TypeKind
            TypeKind actualTypeKind = elementToTypeKind(enclosedElement);
            if (!actualTypeKind.equals(expected.type())) {
                errors++;
                System.out.println("\t\tUnexpected type kind of  " +
                                   actualTypeKind + " on " + enclosedElement + "; expected: "
                                   + expected.type());
            }

            // Elements.Origin informatoin
            Elements.Origin actualOrigin = elements.getOrigin(enclosedElement);
            if (!actualOrigin.equals(expected.origin())) {
                errors++;
                System.out.println("\t\tUnexpected origin of " +
                                   actualOrigin + " on " + enclosedElement + "; expected: "
                                   + expected.origin());
            }
        }

        if (!expectedInfoMap.isEmpty()) {
            errors++;
            for (String key : expectedInfoMap.keySet()) {
                System.out.println("\tError: unmatched elements: " +  key);
            }
        }
       return errors;
    }

    private String elementToString(Element element) {
        StringWriter sw = new StringWriter();
        elements.printElements(sw, element);
        return sw.toString();
    }

    private TypeKind elementToTypeKind(Element element) {
        // Extract "primary type" from an element, the type of a field
        // or state component, the return type of a method, etc.
        return eltToTypeKindVisitor.visit(element);
    }

    private static SimpleElementVisitor<TypeKind, Void> eltToTypeKindVisitor =
        new SimpleElementVisitor<>() {
        @Override
        protected TypeKind defaultAction(Element e, Void p) {
            return e.asType().getKind();
        }

        @Override
        public TypeKind visitExecutable(ExecutableElement e, Void p) {
            return e.getReturnType().getKind();
        }
    };

    // Annotations to hold expected values

    @Retention(RetentionPolicy.RUNTIME)
    @interface TypeElementInfo {
        ElementInfo[] elements() default {};
    }

    @interface ElementInfo {
        ElementKind kind() default ElementKind.METHOD;
        Modifier[] modifiers() default {};
        String name();
        TypeKind type();
        // parameters TBD
        Elements.Origin origin() default Elements.Origin.EXPLICIT;
    }

    // Nested types subject to testing

    @TypeElementInfo(elements = {@ElementInfo(modifiers = {Modifier.PUBLIC, Modifier.ABSTRACT},
                                              name = "modulus",
                                              type = TypeKind.DOUBLE)})
    interface ComplexNumber {
        /**
         * Return the magnitude of the complex number.
         */
        double modulus();
    }

    /**
     * Polar coordinate complex number.
     *
     * Expected type after desugaring:
     *
     *static record ComplexPolar(double r, double theta) implements TestRecordDesugar.ComplexNumber {
     *  private final double r;
     *  private final double theta;
     *
     *  @java.lang.Override
     *  public double modulus();
     *
     *  public java.lang.String toString();
     *
     *  public final int hashCode();
     *
     *  public final boolean equals(java.lang.Object o);
     *
     *  public double r();
     *
     *  public double theta();
     *}
     */
    @TypeElementInfo(elements =
                     {@ElementInfo(kind = ElementKind.RECORD_COMPONENT,
                                   modifiers = {Modifier.PUBLIC},
                                   name = "r",
                                   type = TypeKind.DOUBLE),

                      @ElementInfo(kind = ElementKind.FIELD,
                                   modifiers = {Modifier.PRIVATE, Modifier.FINAL},
                                   name = "r",
                                   type = TypeKind.DOUBLE,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(kind = ElementKind.RECORD_COMPONENT,
                                   modifiers = {Modifier.PUBLIC},
                                   name = "theta",
                                   type = TypeKind.DOUBLE),

                      @ElementInfo(kind = ElementKind.FIELD,
                                   modifiers = {Modifier.PRIVATE, Modifier.FINAL},
                                   name = "theta",
                                   type = TypeKind.DOUBLE,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC},
                                   name = "modulus",
                                   type = TypeKind.DOUBLE),

                      @ElementInfo(modifiers = {Modifier.PUBLIC, Modifier.FINAL},
                                   name = "toString",
                                   type = TypeKind.DECLARED,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC, Modifier.FINAL},
                                   name = "hashCode",
                                   type = TypeKind.INT,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC, Modifier.FINAL},
                                   name = "equals",
                                   type = TypeKind.BOOLEAN,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC},
                                   name = "r",
                                   type = TypeKind.DOUBLE,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC},
                                   name = "theta",
                                   type = TypeKind.DOUBLE,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(kind = ElementKind.CONSTRUCTOR,
                                   modifiers = {},
                                   name = "<init>",
                                   type = TypeKind.VOID,
                                   origin = Elements.Origin.MANDATED),
                             })
   record ComplexPolar(double r, double theta) implements ComplexNumber {
        @Override
        public double modulus() {
            return r;
        }
    }

    // Override equals in cartesian complex number record to allow
    // testing of origin information.

    /**
     * Cartesian coordinate complex number.
     */
    @TypeElementInfo(elements =
                     {@ElementInfo(kind = ElementKind.RECORD_COMPONENT,
                                   modifiers = {Modifier.PUBLIC},
                                   name = "real",
                                   type = TypeKind.DOUBLE),

                      @ElementInfo(kind = ElementKind.FIELD,
                                   modifiers = {Modifier.PRIVATE, Modifier.FINAL},
                                   name = "real",
                                   type = TypeKind.DOUBLE,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(kind = ElementKind.RECORD_COMPONENT,
                                   modifiers = {Modifier.PUBLIC},
                                   name = "imag",
                                   type = TypeKind.DOUBLE),

                      @ElementInfo(kind = ElementKind.FIELD,
                                   modifiers = {Modifier.PRIVATE, Modifier.FINAL},
                                   name = "imag",
                                   type = TypeKind.DOUBLE,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC},
                                   name = "modulus",
                                   type = TypeKind.DOUBLE),

                      @ElementInfo(modifiers = {Modifier.PUBLIC, Modifier.FINAL},
                                   name = "toString",
                                   type = TypeKind.DECLARED,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC, Modifier.FINAL},
                                   name = "hashCode",
                                   type = TypeKind.INT,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC},
                                   name = "equals",
                                   type = TypeKind.BOOLEAN),

                      @ElementInfo(modifiers = {Modifier.PUBLIC},
                                   name = "real",
                                   type = TypeKind.DOUBLE,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(modifiers = {Modifier.PUBLIC},
                                   name = "imag",
                                   type = TypeKind.DOUBLE,
                                   origin = Elements.Origin.EXPLICIT),

                      @ElementInfo(kind = ElementKind.FIELD,
                                   modifiers = {Modifier.PRIVATE, Modifier.STATIC},
                                   name = "PROJ_INFINITY",
                                   type = TypeKind.DECLARED),

                      @ElementInfo(modifiers = {Modifier.PRIVATE},
                                   name = "proj",
                                   type = TypeKind.DECLARED),

                      @ElementInfo(kind = ElementKind.CONSTRUCTOR,
                                   modifiers = {Modifier.PUBLIC},
                                   name = "<init>",
                                   type = TypeKind.VOID),
                             })
     record ComplexCartesian(double real, double imag) implements ComplexNumber {
        // Explicit constructor declaration allowed
        public ComplexCartesian(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }

        @Override
        public double modulus() {
            return StrictMath.hypot(real, imag);
        }

        private static ComplexCartesian PROJ_INFINITY =
            new ComplexCartesian(Double.POSITIVE_INFINITY, +0.0);

        // Make private rather than public to test mapping.
        private ComplexCartesian proj() {
            if (Double.isInfinite(real) || Double.isInfinite(imag))
                return PROJ_INFINITY;
            else
                return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ComplexCartesian) {
                ComplexCartesian that = (ComplexCartesian)o;

                ComplexCartesian projThis = this.proj();
                ComplexCartesian projThat = that.proj();

                // Don't worry about NaN values here
                return projThis.real == projThat.real &&
                    projThis.imag == projThat.imag;
            } else {
                return false;
            }
        }
    }
}

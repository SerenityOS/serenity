/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005294
 * @summary Check behavior of default methods of AnnotatedElement
 * @author Joseph D. Darcy
 */

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * For annotation type tokens including, null, DirectlyPresent.class,
 * IndirectlyPresent.class, etc. the behavior of
 * AnnotedElementDelegate.foo(arg) is compared for equality to
 * baseAnnotatedElement.foo(arg) on various kinds of annotated
 * elements.
 */
public class TestAnnotatedElementDefaults {
    public static void main(String... args) throws SecurityException {
        int failures = 0;

        for (AnnotatedElement annotElement : elementsToTest()) {
            System.out.println(annotElement);
            AnnotatedElementDelegate delegate = new AnnotatedElementDelegate(annotElement);
            failures += testNullHandling(delegate);
            for (Class<? extends Annotation> annotType : annotationsToTest()) {
                failures += AnnotatedElementDelegate.testDelegate(delegate, annotType);
            }
        }

        if (failures > 0) {
            System.err.printf("%d failures%n", failures);
            throw new RuntimeException();
        }
    }

    private static List<AnnotatedElement> elementsToTest() {
        List<AnnotatedElement> annotatedElements = new ArrayList<>();
        annotatedElements.add(TestClass1Super.class);
        annotatedElements.add(TestClass1.class);
        for (Method method : TestClass1.class.getDeclaredMethods()) {
            annotatedElements.add(method);
        }
        return annotatedElements;
    }

    private static List<Class<? extends Annotation>> annotationsToTest() {
        List<Class<? extends Annotation>> annotations = new ArrayList<>();
        annotations.add(Missing.class);

        annotations.add(MissingRepeatable.class);

        annotations.add(DirectlyPresent.class);

        annotations.add(IndirectlyPresent.class);
        annotations.add(IndirectlyPresentContainer.class);

        annotations.add(DirectlyAndIndirectlyPresent.class);
        annotations.add(DirectlyAndIndirectlyPresentContainer.class);

        annotations.add(AssociatedDirectOnSuperClass.class);
        annotations.add(AssociatedDirectOnSuperClassContainer.class);

        annotations.add(AssociatedDirectOnSuperClassIndirectOnSubclass.class);
        annotations.add(AssociatedDirectOnSuperClassIndirectOnSubclassContainer.class);

        annotations.add(AssociatedIndirectOnSuperClassDirectOnSubclass.class);
        annotations.add(AssociatedIndirectOnSuperClassDirectOnSubclassContainer.class);
        return annotations;
    }

    private static int testNullHandling(AnnotatedElementDelegate delegate) {
        int failures = 0;
        try {
            Object result = delegate.getDeclaredAnnotationsByType(null);
            failures++;
        } catch (NullPointerException npe) {
            ; // Expected
        }

        try {
            Object result = delegate.getAnnotationsByType(null);
            failures++;
        } catch (NullPointerException npe) {
            ; // Expected
        }

        try {
            Object result = delegate.getDeclaredAnnotation(null);
            failures++;
        } catch (NullPointerException npe) {
            ; // Expected
        }

        return failures;
    }

}

// -----------------------------------------------------

@AssociatedDirectOnSuperClass(123)
@AssociatedIndirectOnSuperClass(234) @AssociatedIndirectOnSuperClass(345)
@AssociatedDirectOnSuperClassIndirectOnSubclass(987)
@AssociatedIndirectOnSuperClassDirectOnSubclass(1111) @AssociatedIndirectOnSuperClassDirectOnSubclass(2222)
class TestClass1Super {}

@DirectlyPresent(1)
@IndirectlyPresent(10) @IndirectlyPresent(11)
@AssociatedDirectOnSuperClassIndirectOnSubclass(876) @AssociatedDirectOnSuperClassIndirectOnSubclass(765)
@AssociatedIndirectOnSuperClassDirectOnSubclass(3333)
class TestClass1 extends TestClass1Super {

    @DirectlyPresent(2)
    @IndirectlyPresentContainer({@IndirectlyPresent(12)})
    @DirectlyAndIndirectlyPresentContainer({@DirectlyAndIndirectlyPresent(84), @DirectlyAndIndirectlyPresent(96)})
    public void foo() {return ;}

    @IndirectlyPresentContainer({})
    @DirectlyAndIndirectlyPresentContainer({@DirectlyAndIndirectlyPresent(11), @DirectlyAndIndirectlyPresent(22)})
    @DirectlyAndIndirectlyPresent(33)
    public void bar()  {return ;}
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@interface Missing {
    int value();
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MissingRepeatableContainer.class)
@interface MissingRepeatable {
    int value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface MissingRepeatableContainer {
    MissingRepeatable[] value();
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@interface DirectlyPresent {
    int value();
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(IndirectlyPresentContainer.class)
@interface IndirectlyPresent {
    int value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface IndirectlyPresentContainer {
    IndirectlyPresent[] value();
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DirectlyAndIndirectlyPresentContainer.class)
@interface DirectlyAndIndirectlyPresent {
    int value();

}

@Retention(RetentionPolicy.RUNTIME)
@interface DirectlyAndIndirectlyPresentContainer {
    DirectlyAndIndirectlyPresent[] value();
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AssociatedDirectOnSuperClassContainer.class)
@interface AssociatedDirectOnSuperClass {
    int value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface AssociatedDirectOnSuperClassContainer {
    AssociatedDirectOnSuperClass[] value();
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AssociatedIndirectOnSuperClassContainer.class)
@interface AssociatedIndirectOnSuperClass {
    int value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface AssociatedIndirectOnSuperClassContainer {
    AssociatedIndirectOnSuperClass[] value();
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AssociatedDirectOnSuperClassIndirectOnSubclassContainer.class)
@interface  AssociatedDirectOnSuperClassIndirectOnSubclass {
    int value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface AssociatedDirectOnSuperClassIndirectOnSubclassContainer {
    AssociatedDirectOnSuperClassIndirectOnSubclass[] value();
}

// -----------------------------------------------------

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AssociatedIndirectOnSuperClassDirectOnSubclassContainer.class)
@interface  AssociatedIndirectOnSuperClassDirectOnSubclass {
    int value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface AssociatedIndirectOnSuperClassDirectOnSubclassContainer {
    AssociatedIndirectOnSuperClassDirectOnSubclass[] value();
}

// -----------------------------------------------------

/**
 * Helper class to ease calling the default methods of {@code
 * AnnotatedElement} and comparing the results to other
 * implementation.
 */
class AnnotatedElementDelegate implements AnnotatedElement {
    private AnnotatedElement base;

    public AnnotatedElementDelegate(AnnotatedElement base) {
        Objects.requireNonNull(base);
        this.base = base;
    }

    // Delegate to base implemenetation of AnnotatedElement methods
    // without defaults.
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return base.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return base.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return base.getDeclaredAnnotations();
    }

    public AnnotatedElement getBase() {
        return base;
    }

    static int testDelegate(AnnotatedElementDelegate delegate,
                            Class<? extends Annotation> annotationClass) {
        int failures = 0;
        AnnotatedElement base = delegate.getBase();

        // System.out.println("\tTesting " + delegate + "\ton\t" + annotationClass);

        // <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass)
        failures += annotationArrayCheck(delegate.getDeclaredAnnotationsByType(annotationClass),
                                         base.getDeclaredAnnotationsByType(annotationClass),
                                         annotationClass,
                                         "Equality failure on getDeclaredAnnotationsByType(%s) on %s)%n");

        // <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass)
        failures += annotationArrayCheck(delegate.getAnnotationsByType(annotationClass),
                                         base.getAnnotationsByType(annotationClass),
                                         annotationClass,
                                         "Equality failure on getAnnotationsByType(%s) on %s)%n");

        // <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass)
        if (!Objects.equals(delegate.getDeclaredAnnotation(annotationClass),
                            base.getDeclaredAnnotation(annotationClass))) {
            failures++;
            System.err.printf("Equality failure on getDeclaredAnnotation(%s) on %s)%n",
                              annotationClass, delegate);
        }
        return failures;
    }
    private static <T extends Annotation> int annotationArrayCheck(T[] delegate,
                                                           T[] base,
                                                           Class<? extends Annotation> annotationClass,
                                                           String message) {
        int failures = 0;

        if (!Objects.deepEquals(delegate,base)) {
            failures = 1;

            System.err.printf(message,
                              annotationClass,
                              delegate);

            System.err.println("Base result:\t" + Arrays.toString(base));
            System.err.println("Delegate result:\t " + Arrays.toString(delegate));
            System.err.println();
        }

        return failures;
    }
}

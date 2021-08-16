/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7154390 8005712 8007278 8004912
 * @summary Unit test for repeated annotation reflection
 *
 * @compile RepeatedUnitTest.java subpackage/package-info.java subpackage/Container.java subpackage/Containee.java subpackage/NonRepeated.java subpackage/InheritedContainee.java subpackage/InheritedContainer.java subpackage/InheritedNonRepeated.java
 * @run main RepeatedUnitTest
 */

import subpackage.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class RepeatedUnitTest {
    public static void main(String[] args) throws Exception {
        // PACKAGE ANNOTATIONS
        Class c = Class.forName("subpackage.NonRepeated"); // force package "subpackage" load
        Package p = Package.getPackage("subpackage");
        packageNonRepeated(p);
        packageRepeated(p);
        packageContainer(p);

        // INHERITED/NON-INHERITED ON CLASS
        inheritedMe1();
        inheritedMe2();
        inheritedMe3();
        inheritedMe4();

        inheritedMe5();    // ContainerOnSuperSingleOnSub
        inheritedMe6();    // RepeatableOnSuperSingleOnSub
        inheritedMe7();    // SingleAnnoOnSuperContainerOnSub
        inheritedMe8();    // SingleOnSuperRepeatableOnSub


        // CONSTRUCTOR
        checkMultiplier(Me1.class.getConstructor(new Class[0]), 10);

        // FIELD
        checkMultiplier(Me1.class.getField("foo"), 1);

        // METHOD
        checkMultiplier(Me1.class.getDeclaredMethod("mee", (Class<?>[])null), 100);

        // INNER CLASS
        checkMultiplier(Me1.MiniMee.class, 1000);

        // ENUM ELEMENT
        checkMultiplier(Me1.E.class.getField("EE"), 10000);

        // ENUM
        checkMultiplier(Me1.E.class, 100000);
    }

    static void packageNonRepeated(AnnotatedElement e) {
        NonRepeated nr = e.getAnnotation(NonRepeated.class);
        check(nr.value() == 10);

        check(1 == countAnnotation(e, NonRepeated.class));

        nr = e.getAnnotationsByType(NonRepeated.class)[0];
        check(nr.value() == 10);

        check(1 == containsAnnotationOfType(e.getAnnotations(), NonRepeated.class));
    }

    static void packageRepeated(AnnotatedElement e) {
        Containee c = e.getAnnotation(Containee.class);
        check(c == null);
        check(2 == countAnnotation(e, Containee.class));

        c = e.getAnnotationsByType(Containee.class)[0];
        check(c.value() == 1);
        c = e.getAnnotationsByType(Containee.class)[1];
        check(c.value() == 2);

        check(0 == containsAnnotationOfType(e.getAnnotations(), Containee.class));
    }

    static void packageContainer(AnnotatedElement e) {
        Container cr = e.getAnnotation(Container.class);
        check(null != cr);
        check(1 == containsAnnotationOfType(e.getAnnotationsByType(Container.class), Container.class));
        check(1 == countAnnotation(e, Container.class));
    }

    static void inheritedMe1() {
        AnnotatedElement e = Me1.class;
        check(null == e.getAnnotation(NonRepeated.class));
        check(e.getAnnotation(InheritedNonRepeated.class).value() == 20);
        check(0 == countAnnotation(e, Containee.class));
        check(4 == countAnnotation(e, InheritedContainee.class));
        check(0 == countAnnotation(e, Container.class));
        check(1 == countAnnotation(e, InheritedContainer.class));
    }

    static void inheritedMe2() {
        AnnotatedElement e = Me2.class;
        check(e.getAnnotation(NonRepeated.class).value() == 100);
        check(e.getAnnotation(InheritedNonRepeated.class).value() == 200);
        check(4 == countAnnotation(e, Containee.class));
        check(4 == countAnnotation(e, InheritedContainee.class));
        check(1 == countAnnotation(e, Container.class));
        check(1 == countAnnotation(e, InheritedContainer.class));
        check(1 == countAnnotation(e, NonRepeated.class));
        check(1 == countAnnotation(e, InheritedNonRepeated.class));

        check(e.getAnnotationsByType(Containee.class)[2].value() == 300);
        check(e.getAnnotationsByType(InheritedContainee.class)[2].value() == 300);
        check(e.getAnnotationsByType(InheritedNonRepeated.class)[0].value() == 200);
        check(e.getAnnotationsByType(NonRepeated.class)[0].value() == 100);
    }

    static void inheritedMe3() {
        AnnotatedElement e = Me3.class;
        check(null == e.getAnnotation(NonRepeated.class));

        check(0 == countAnnotation(e, Containee.class));
        check(4 == countAnnotation(e, InheritedContainee.class));
        check(0 == countAnnotation(e, Container.class));
        check(1 == countAnnotation(e, InheritedContainer.class));

        check(e.getAnnotationsByType(InheritedContainee.class)[2].value() == 350);
        check(e.getAnnotationsByType(InheritedNonRepeated.class)[0].value() == 15);
    }

    static void inheritedMe4() {
        AnnotatedElement e = Me4.class;
        check(e.getAnnotation(NonRepeated.class).value() == 1000);
        check(e.getAnnotation(InheritedNonRepeated.class).value() == 2000);
        check(4 == countAnnotation(e, Containee.class));
        check(4 == countAnnotation(e, InheritedContainee.class));
        check(1 == countAnnotation(e, Container.class));
        check(1 == countAnnotation(e, InheritedContainer.class));
        check(1 == countAnnotation(e, NonRepeated.class));
        check(1 == countAnnotation(e, InheritedNonRepeated.class));

        check(e.getAnnotationsByType(Containee.class)[2].value() == 3000);
        check(e.getAnnotationsByType(InheritedContainee.class)[2].value() == 3000);
        check(e.getAnnotationsByType(InheritedNonRepeated.class)[0].value() == 2000);
        check(e.getAnnotationsByType(NonRepeated.class)[0].value() == 1000);
    }

    static void inheritedMe5() {
        AnnotatedElement e = Me5.class;
        check(2 == e.getAnnotations().length);
        check(1 == countAnnotation(e, InheritedContainee.class));
    }

    static void inheritedMe6() {
        AnnotatedElement e = Me6.class;
        check(2 == e.getAnnotations().length);
        check(1 == countAnnotation(e, InheritedContainee.class));
    }

    static void inheritedMe7() {
        AnnotatedElement e = Me7.class;
        check(2 == e.getAnnotations().length);
        check(2 == countAnnotation(e, InheritedContainee.class));
    }

    static void inheritedMe8() {
        AnnotatedElement e = Me8.class;
        check(2 == e.getAnnotations().length);
        check(2 == countAnnotation(e, InheritedContainee.class));
    }

    static void checkMultiplier(AnnotatedElement e, int m) {
        // Basic sanity of non-repeating getAnnotation(Class)
        check(e.getAnnotation(NonRepeated.class).value() == 5 * m);

        // Check count of annotations returned from getAnnotationsByType(Class)
        check(4 == countAnnotation(e, Containee.class));
        check(1 == countAnnotation(e, Container.class));
        check(1 == countAnnotation(e, NonRepeated.class));

        // Check contents of array returned from getAnnotationsByType(Class)
        check(e.getAnnotationsByType(Containee.class)[2].value() == 3 * m);
        check(e.getAnnotationsByType(NonRepeated.class)[0].value() == 5 * m);

        // Check getAnnotation(Class)
        check(e.getAnnotation(Containee.class) == null);
        check(e.getAnnotation(Container.class) != null);

        // Check count of annotations returned from getAnnotations()
        check(0 == containsAnnotationOfType(e.getAnnotations(), Containee.class));
        check(1 == containsAnnotationOfType(e.getAnnotations(), Container.class));
        check(1 == containsAnnotationOfType(e.getAnnotations(), NonRepeated.class));
    }

    static void check(Boolean b) {
        if (!b) throw new RuntimeException();
    }

    static int countAnnotation(AnnotatedElement e, Class<? extends Annotation> c) {
        return containsAnnotationOfType(e.getAnnotationsByType(c), c);
    }

    static <A extends Annotation> int containsAnnotationOfType(A[] l, Class<? extends Annotation> a) {
        int count = 0;
        for (Annotation an : l) {
            if (an.annotationType().equals(a))
                count++;
        }
        return count;
    }
}

@NonRepeated @InheritedNonRepeated
@InheritedContainee(1) @InheritedContainee(2) @InheritedContainee(3) @InheritedContainee(4)
@Containee(1) @Containee(2) @Containee(3) @Containee(4)
class Grandma {}

class Mother extends Grandma {}

@NonRepeated(5) @InheritedNonRepeated(15)
@InheritedContainee(150) @InheritedContainee(250) @InheritedContainee(350) @InheritedContainee(450)
@Containee(150) @Containee(250) @Containee(350) @Containee(450)
class Father extends Grandma {}

class Me1 extends Mother {

    @NonRepeated(5)
    @Containee(1) @Containee(2) @Containee(3) @Containee(4)
    public String foo = "";

    @NonRepeated(50)
    @Containee(10) @Containee(20) @Containee(30) @Containee(40)
    public Me1() {
    }

    @NonRepeated(500)
    @Containee(100) @Containee(200) @Containee(300) @Containee(400)
    public void mee() {
    }

    @NonRepeated(5000)
    @Containee(1000) @Containee(2000) @Containee(3000) @Containee(4000)
    public class MiniMee {}

    @NonRepeated(500000)
    @Containee(100000) @Containee(200000) @Containee(300000) @Containee(400000)
    public enum E {
        @NonRepeated(50000)
        @Containee(10000) @Containee(20000) @Containee(30000) @Containee(40000)
        EE(),
    }
}

@NonRepeated(100) @InheritedNonRepeated(200)
@InheritedContainee(100) @InheritedContainee(200) @InheritedContainee(300) @InheritedContainee(400)
@Containee(100) @Containee(200) @Containee(300) @Containee(400)
class Me2 extends Mother {}

class Me3 extends Father {}

@NonRepeated(1000) @InheritedNonRepeated(2000)
@InheritedContainee(1000) @InheritedContainee(2000) @InheritedContainee(3000) @InheritedContainee(4000)
@Containee(1000) @Containee(2000) @Containee(3000) @Containee(4000)
class Me4 extends Father {}


@InheritedContainer({@InheritedContainee(1), @InheritedContainee(2)})
class SuperOf5 {}

@InheritedContainee(3)
class Me5 extends SuperOf5{}


@InheritedContainee(1) @InheritedContainee(2)
class SuperOf6 {}

@InheritedContainee(3)
class Me6 extends SuperOf6 {}


@InheritedContainee(1)
class SuperOf7 {}

@InheritedContainer({@InheritedContainee(2), @InheritedContainee(3)})
class Me7 extends SuperOf7 {}


@InheritedContainee(1)
class SuperOf8 {}

@InheritedContainee(2) @InheritedContainee(3)
class Me8 extends SuperOf8 {}

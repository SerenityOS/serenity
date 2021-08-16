/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024915 8044629 8256693
 */

import java.lang.reflect.*;

public class GetAnnotatedReceiverType {
    public void method() {}
    public void method0(GetAnnotatedReceiverType this) {}
    public static void method4() {}

    class Inner0 {
        public Inner0() {}
    }

    class Inner1 {
        public Inner1(GetAnnotatedReceiverType GetAnnotatedReceiverType.this) {}
    }

    public static class Nested {
        public Nested() {}

        public class NestedInner {
            public NestedInner() { }

            public Class<?> getLocalClass () {
                class NestedInnerLocal { public NestedInnerLocal() {} }
                return NestedInnerLocal.class;
            }

            public Class<?> getAnonymousClass() {
                return new Object() {}.getClass();
            }
        }
    }

    public class Inner2 {
        public Inner2() { }
        public void innerMethod2(GetAnnotatedReceiverType.Inner2 this) {}

        public class Inner3 {
            public Inner3() { }
            public void innerMethod3(GetAnnotatedReceiverType.Inner2.Inner3 this) {}

            public class Inner7<T> {
                public void innerMethod7(GetAnnotatedReceiverType.Inner2.Inner3.Inner7<T> this) {}
            }

            public Class<?> getLocalClass () {
                class InnerLocal { public InnerLocal() {} }
                return InnerLocal.class;
            }

            public Class<?> getAnonymousClass() {
                return new Object() {}.getClass();
            }
        }

        public Class<?> getLocalClass () {
            class InnerLocal { public InnerLocal() {} }
                return InnerLocal.class;
        }

        public Class<?> getAnonymousClass() {
            return new Object() {}.getClass();
        }
    }

    public class Inner4<T> {
        public Inner4(GetAnnotatedReceiverType GetAnnotatedReceiverType.this) {}
        public void innerMethod4(GetAnnotatedReceiverType.Inner4<T> this) {}

        public class Inner5 {
            public Inner5(GetAnnotatedReceiverType.Inner4<T> GetAnnotatedReceiverType.Inner4.this) {}
            public void innerMethod5(GetAnnotatedReceiverType.Inner4<T>.Inner5 this) {}

            public class Inner6 {
                public Inner6(GetAnnotatedReceiverType.Inner4<T>.Inner5 GetAnnotatedReceiverType.Inner4.Inner5.this) {}
            }
        }
    }

    private static int failures = 0;
    private static int tests = 0;
    private static final int EXPECTED_TEST_CASES = 25;

    public static void main(String[] args) throws NoSuchMethodException {
        checkEmptyAT(GetAnnotatedReceiverType.class.getMethod("method"),
                "getAnnotatedReceiverType for \"method\" should return an empty AnnotatedType");
        checkEmptyAT(Inner0.class.getConstructor(GetAnnotatedReceiverType.class),
                "getAnnotatedReceiverType for a ctor without a \"this\" should return an empty AnnotatedType");

        checkEmptyAT(GetAnnotatedReceiverType.class.getMethod("method0"),
                "getAnnotatedReceiverType for \"method0\" should return an empty AnnotatedType");
        checkEmptyAT(Inner1.class.getConstructor(GetAnnotatedReceiverType.class),
                "getAnnotatedReceiverType for a ctor with a \"this\" should return an empty AnnotatedType");

        checkNull(GetAnnotatedReceiverType.class.getMethod("method4"),
                "getAnnotatedReceiverType() on a static method should return null");

        // More nested, inner, local and anonymous classes
        Nested nested = new Nested();
        Nested.NestedInner instance = nested.new NestedInner();
        checkNull(nested.getClass().getConstructors()[0],
                "getAnnotatedReceiverType() on a constructor for a static class should return null");
        checkEmptyAT(instance.getClass().getConstructors()[0],
                "getAnnotatedReceiverType for a ctor without a \"this\" should return an empty AnnotatedType");
        checkNull(instance.getLocalClass().getConstructors()[0],
                "getAnnotatedReceiverType() on a constructor for a local class should return null");
        checkNull(instance.getAnonymousClass().getDeclaredConstructors()[0],
                "getAnnotatedReceiverType() on a constructor for an anonymous class should return null");

        GetAnnotatedReceiverType outer = new GetAnnotatedReceiverType();
        Inner2 instance2 = outer.new Inner2();
        checkEmptyAT(instance2.getClass().getConstructors()[0],
                "getAnnotatedReceiverType for a ctor without a \"this\" should return an empty AnnotatedType");
        checkNull(instance2.getLocalClass().getConstructors()[0],
                "getAnnotatedReceiverType() on a constructor for a local class should return null");
        checkNull(instance2.getAnonymousClass().getDeclaredConstructors()[0],
                "getAnnotatedReceiverType() on a constructor for an anonymous class should return null");

        Inner2.Inner3 instance3 = instance2.new Inner3();
        checkEmptyAT(instance3.getClass().getConstructors()[0],
                "getAnnotatedReceiverType for a ctor without a \"this\" should return an empty AnnotatedType");
        checkNull(instance3.getLocalClass().getConstructors()[0],
                "getAnnotatedReceiverType() on a constructor for a local class should return null");
        checkNull(instance3.getAnonymousClass().getDeclaredConstructors()[0],
                "getAnnotatedReceiverType() on a constructor for an anonymous class should return null");

        Inner4<?> instance4 = outer.new Inner4<String>();
        Inner4<?>.Inner5 instance5 = instance4.new Inner5();
        Inner4<?>.Inner5.Inner6 instance6 = instance5.new Inner6();

        checkAnnotatedReceiverType(instance4.getClass().getConstructors()[0], false,
                "The type of .getAnnotatedReceiverType().getType() for this constructor should be");
        checkAnnotatedReceiverType(instance5.getClass().getConstructors()[0], true,
                "The type of .getAnnotatedReceiverType().getType() for this constructor should be");
        checkAnnotatedReceiverType(instance6.getClass().getConstructors()[0], true,
                "The type of .getAnnotatedReceiverType().getType() for this constructor should be");
        checkAnnotatedReceiverType(outer.getClass().getMethod("method0"), false,
                "The type of .getAnnotatedReceiverType().getType() for this method should be");
        checkAnnotatedReceiverType(instance4.getClass().getMethod("innerMethod4"), true,
                "The type of .getAnnotatedReceiverType().getType() for this method should be");
        checkAnnotatedReceiverType(instance5.getClass().getMethod("innerMethod5"), true,
                "The type of .getAnnotatedReceiverType().getType() for this method should be");
        checkAnnotatedReceiverType(instance2.getClass().getMethod("innerMethod2"), false,
                "The type of .getAnnotatedReceiverType().getType() for this method should be");
        checkAnnotatedReceiverType(instance3.getClass().getMethod("innerMethod3"), false,
                "The type of .getAnnotatedReceiverType().getType() for this method should be");

        Inner2.Inner3.Inner7<?> instance7 = instance3.new Inner7<String>();
        checkAnnotatedReceiverType(instance7.getClass().getMethod("innerMethod7"), true,
                "The type of .getAnnotatedReceiverType().getType() for this method should be");
        recursiveCheckAnnotatedOwnerTypes(instance7.getClass().getMethod("innerMethod7").getAnnotatedReceiverType());

        if (failures != 0)
            throw new RuntimeException("Test failed, see log for details");
        else if (tests != EXPECTED_TEST_CASES)
            throw new RuntimeException("Not all cases ran, failing");
    }

    private static void checkNull(Executable e, String msg) {
        AnnotatedType a = e.getAnnotatedReceiverType();
        if (a != null) {
            failures++;
            System.err.println(msg + ": " + e);
        }
        tests++;
    }

    private static void checkEmptyAT(Executable e, String msg) {
        AnnotatedType a = e.getAnnotatedReceiverType();
        if (a.getAnnotations().length != 0) {
            failures++;
            System.err.print(msg + ": " + e);
        }
        tests++;
    }

    private static void checkAnnotatedReceiverType(Executable e, boolean shouldBeParameterized, String msg) {
        Type t = e.getAnnotatedReceiverType().getType();
        if (shouldBeParameterized != (t instanceof ParameterizedType)) {
            failures++;
            System.err.println(e + ", " + msg + " " + (shouldBeParameterized ? "ParameterizedType" : "Class") + ", found: " + t.getClass().getSimpleName());
        }

        // Test we can get the potentially empty annotated actual type arguments array
        if (shouldBeParameterized) {
            try {
                ParameterizedType t1 = (ParameterizedType)t;
                AnnotatedParameterizedType at1 = (AnnotatedParameterizedType)e.getAnnotatedReceiverType();

                if (t1.getActualTypeArguments().length != at1.getAnnotatedActualTypeArguments().length) {
                    System.err.println(t1 + "'s actual type arguments can't match " + at1);
                    failures++;
                }
            } catch (ClassCastException cce) {
                System.err.println("Couldn't get potentially empty actual type arguments: " + cce.getMessage());
                failures++;
            }
        }
        tests++;
    }

    private static void recursiveCheckAnnotatedOwnerTypes(AnnotatedType t) {
        AnnotatedType check = t.getAnnotatedOwnerType();
        do {
            if (!(check.getType() instanceof Class<?>)) {
                failures++;
                System.err.println("Expecting only instances of Class returned for .getType() found " + check.getType().getClass().getSimpleName());
            }
            check = check.getAnnotatedOwnerType();
        } while (check != null);
        tests++;
    }
}

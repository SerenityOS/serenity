/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8058202 8212081 8224012
 * @summary Test java.lang.Object methods on AnnotatedType objects.
 */

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/**
 * Test toString, equals, and hashCode on various AnnotatedType objects.
 */

public class TestObjectMethods {
    private static int errors = 0;

    /*
     * There are various subtypes of AnnotatedType implementations:
     *
     * AnnotatedType
     * AnnotatedArrayType
     * AnnotatedParameterizedType
     * AnnotatedTypeVariable
     * AnnotatedWildcardType
     *
     * The implementations of each these implementations are
     * examined. Wildcards don't appear as top-level types and need to
     * be extracted from bounds.
     *
     * AnnotatedTypes with and without annotations are examined as
     * well.
     */
    public static void main(String... args) {
        Class<?>[] testClasses = {TypeHost.class, AnnotatedTypeHost.class};

        for (Class<?> clazz : testClasses) {
            testEqualsReflexivity(clazz);
            testEquals(clazz);
        }

        testToString(TypeHost.class);
        testToString(AnnotatedTypeHost.class);

        testAnnotationsMatterForEquals(TypeHost.class, AnnotatedTypeHost.class);

        testGetAnnotations(TypeHost.class, false);
        testGetAnnotations(AnnotatedTypeHost.class, true);

        testWildcards();

        testFbounds();

        if (errors > 0) {
            throw new RuntimeException(errors + " errors");
        }
    }

    /*
     * For non-array types, verify toString version of the annotated
     * type ends with the same string as the generic type.
     */
    static void testToString(Class<?> clazz) {
        System.err.println("Testing toString on methods of class " + clazz.getName());
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            // Expected information about the type annotations stored
            // in a *declaration* annotation.
            AnnotTypeInfo annotTypeInfo = m.getAnnotation(AnnotTypeInfo.class);
            int expectedAnnotCount      = annotTypeInfo.count();
            Relation relation           = annotTypeInfo.relation();

            AnnotatedType annotType = m.getAnnotatedReturnType();
            String annotTypeString = annotType.toString();

            Type type = m.getGenericReturnType();
            String typeString = (type instanceof Class) ?
                type.getTypeName() :
                type.toString();

            boolean isArray = annotType instanceof AnnotatedArrayType;
            boolean isVoid = "void".equals(typeString);

            boolean valid;

            switch(relation) {
            case EQUAL:
                valid = annotTypeString.equals(typeString);
                break;

            case POSTFIX:
                valid = annotTypeString.endsWith(typeString) &&
                    !annotTypeString.startsWith(typeString);
                break;

            case STRIPPED:
                String stripped = annotationRegex.matcher(annotTypeString).replaceAll("");
                valid = typeString.replace(" ", "").equals(stripped.replace(" ", ""));
                break;

            case ARRAY:
                // Find final non-array component type and gets its name.
                typeString = null;

                AnnotatedType componentType = annotType;
                while (componentType instanceof AnnotatedArrayType) {
                    AnnotatedArrayType annotatedArrayType = (AnnotatedArrayType) componentType;
                    componentType = annotatedArrayType.getAnnotatedGenericComponentType();
                }

                String componentName = componentType.getType().getTypeName();
                valid = annotTypeString.contains(componentName);
                break;

            case OTHER:
                // No additional checks
                valid = true;
                break;

            default:
                throw new AssertionError("Shouldn't be reached");
            }

            // Verify number of type annotations matches expected value
            Matcher matcher = annotationRegex.matcher(annotTypeString);
            if (expectedAnnotCount > 0) {
                int i = expectedAnnotCount;
                int annotCount = 0;
                while (i > 0) {
                    boolean found = matcher.find();
                    if (found) {
                        i--;
                        annotCount++;
                    } else {
                        errors++;
                        System.err.println("\tExpected annotation not found: " + annotTypeString);
                    }
                }
            }

            boolean found = matcher.find();
            if (found) {
                errors++;
                System.err.println("\tAnnotation found unexpectedly: " + annotTypeString);
            }

            if (!valid) {
                errors++;
                System.err.println(typeString + "\n" + annotTypeString +
                                   "\n " + valid  +
                                   "\n\n");
            }
        }
    }

    private static final Pattern annotationRegex = Pattern.compile("@TestObjectMethods\\$AnnotType\\((\\p{Digit})+\\)");

    static void testGetAnnotations(Class<?> clazz, boolean annotationsExpectedOnMethods) {
        System.err.println("Testing getAnnotations on methods of class " + clazz.getName());
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            Type type = m.getGenericReturnType();
            AnnotatedType annotType = m.getAnnotatedReturnType();
            Annotation[] annotations = annotType.getAnnotations();

            boolean isVoid = "void".equals(type.toString());

            if (annotationsExpectedOnMethods && !isVoid) {
                if (annotations.length == 0 ) {
                    errors++;
                    System.err.println("Expected annotations missing on " + annotType);
                }
            } else {
                if (annotations.length > 0 ) {
                    errors++;
                    System.err.println("Unexpected annotations present on " + annotType);
                }
            }
        }
    }

    static void testEqualsReflexivity(Class<?> clazz) {
        System.err.println("Testing reflexivity of equals on methods of class " + clazz.getName());
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            checkTypesForEquality(m.getAnnotatedReturnType(),
                                  m.getAnnotatedReturnType(),
                                  true);
        }
    }

    private static void checkTypesForEquality(AnnotatedType annotType1,
                                              AnnotatedType annotType2,
                                              boolean expected) {
        boolean comparison = annotType1.equals(annotType2);

        if (comparison) {
            int hash1 = annotType1.hashCode();
            int hash2 = annotType2.hashCode();
            if (hash1 != hash2) {
                errors++;
                System.err.format("Equal AnnotatedTypes with unequal hash codes: %n%s%n%s%n",
                                  annotType1.toString(), annotType2.toString());
            }
        }

        if (comparison != expected) {
            errors++;
            System.err.println(annotType1);
            System.err.println(expected ? " is not equal to " : " is equal to ");
            System.err.println(annotType2);
            System.err.println();
        }
    }

    /*
     * For each of the type host classes, the return type of a method
     * should only equal the return type of that method.
     */
    static void testEquals(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();

        for (int i = 0; i < methods.length; i++) {
            for (int j = 0; j < methods.length; j++) {
                if (i == j)
                    continue;
                else {
                    checkTypesForEquality(methods[i].getAnnotatedReturnType(),
                                          methods[j].getAnnotatedReturnType(),
                                          false);
                }
            }
        }
    }

    /**
     * Roughly, compare the return types of corresponding methods on
     * TypeHost and AnnotatedtypeHost and verify the AnnotatedType
     * objects are *not* equal even if their underlying generic types
     * are.
     */
    static void testAnnotationsMatterForEquals(Class<?> clazz1, Class<?> clazz2) {
        System.err.println("Testing that presence/absence of annotations matters for equals comparison.");

        String methodName = null;
        for (Method method :  clazz1.getDeclaredMethods()) {
            if ("void".equals(method.getReturnType().toString())) {
                continue;
            }

            methodName = method.getName();
            try {
                checkTypesForEquality(method.getAnnotatedReturnType(),
                                      clazz2.getDeclaredMethod(methodName).getAnnotatedReturnType(),
                                      false);
            } catch (Exception e) {
                errors++;
                System.err.println("Method " + methodName + " not found.");
            }
        }
    }

    static void testWildcards() {
        System.err.println("Testing wildcards");
        // public @AnnotType(10) Set<? extends Number> fooNumberSet() {return null;}
        // public @AnnotType(11) Set<@AnnotType(13) ? extends Number> fooNumberSet2() {return null;}
        AnnotatedWildcardType awt1 = extractWildcard("fooNumberSet");
        AnnotatedWildcardType awt2 = extractWildcard("fooNumberSet2");

        if (!awt1.equals(extractWildcard("fooNumberSet")) ||
            !awt2.equals(extractWildcard("fooNumberSet2"))) {
            errors++;
            System.err.println("Bad equality comparison on wildcards.");
        }

        checkTypesForEquality(awt1, awt2, false);

        if (awt2.getAnnotations().length == 0) {
            errors++;
            System.err.println("Expected annotations not found.");
        }
    }

    private static AnnotatedWildcardType extractWildcard(String methodName) {
        try {
            return (AnnotatedWildcardType)
                (((AnnotatedParameterizedType)(AnnotatedTypeHost.class.getMethod(methodName).
                                               getAnnotatedReturnType())).
                 getAnnotatedActualTypeArguments()[0] );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testFbounds() {
        // Make sure equals and hashCode work fine for a type
        // involving an F-bound, in particular Comparable<E> in
        // java.lang.Enum:
        //
        // class Enum<E extends Enum<E>>
        // implements Constable, Comparable<E>, Serializable

        AnnotatedType[] types = Enum.class.getAnnotatedInterfaces();

        for (int i = 0; i < types.length; i ++) {
            for (int j = 0; j < types.length; j ++) {
                checkTypesForEquality(types[i], types[j], i == j);
            }
        }
    }

    // The TypeHost and AnnotatedTypeHost classes declare methods with
    // the same name and signatures but with the AnnotatedTypeHost
    // methods having annotations on their return type, where
    // possible.

    static class TypeHost<E, F extends Number> {
        @AnnotTypeInfo
        public void fooVoid() {return;}

        @AnnotTypeInfo
        public int foo() {return 0;}

        @AnnotTypeInfo
        public String fooString() {return null;}

        @AnnotTypeInfo
        public int[] fooIntArray() {return null;}

        @AnnotTypeInfo
        public String[] fooStringArray() {return null;}

        @AnnotTypeInfo
        public String [][] fooStringArrayArray() {return null;}

        @AnnotTypeInfo
        public Set<String> fooSetString() {return null;}

        @AnnotTypeInfo
        public Set<Number> fooSetNumber() {return null;}

        @AnnotTypeInfo
        public E fooE() {return null;}

        @AnnotTypeInfo
        public F fooF() {return null;}

        @AnnotTypeInfo
        public <G> G fooG() {return null;}

        @AnnotTypeInfo
        public  Set<? extends Number> fooNumberSet() {return null;}

        @AnnotTypeInfo
        public  Set<? extends Integer> fooNumberSet2() {return null;}

        @AnnotTypeInfo
        public  Set<? extends Long> fooNumberSet3() {return null;}

        @AnnotTypeInfo
        public Set<?> fooObjectSet() {return null;}

        @AnnotTypeInfo
        public List<? extends Object> fooObjectList() {return null;}
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    static @interface AnnotType {
        int value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface AnnotTypeInfo {
        /**
         * Expected number of @AnnotType
         */
        int count() default 0;

        /**
         * Relation to genericString output.
         */
        Relation relation() default Relation.EQUAL;
    }

    /**
     * Expected relationship of toString output of AnnotatedType to
     * toGenericString output of underlying type.
     */
    static private enum Relation {
        EQUAL,

        /**
         * The toGenericString output is a postfix of the
         * AnnotatedType output; a leading annotation is expected.
         */
        POSTFIX,

        /**
         * If the annotations are stripped from the AnnotatedType
         * output and whitespace adjusted accordingly, it should equal
         * the toGenericString output.
         */
        STRIPPED,

        /**
         * The output of AnnotatedType for arrays would require more
         * extensive transformation to map to toGenericString output.
         */
        ARRAY,

       /**
         * Some other, harder to characterize, relationship. Currently
         * used for a wildcard where Object in "extends Object" is
         * annotated; the "extends Object" is elided in toGenericString.
         */
        OTHER;
    }

    static class AnnotatedTypeHost<E, F extends Number> {
        @AnnotTypeInfo
        public /*@AnnotType(0)*/ void fooVoid() {return;} // Illegal to annotate void

        @AnnotTypeInfo(count =1, relation = Relation.POSTFIX)
        @AnnotType(1)
        public int foo() {return 0;}

        @AnnotTypeInfo(count = 1, relation = Relation.POSTFIX)
        @AnnotType(2)
        public  String fooString() {return null;}

        @AnnotTypeInfo(count = 1, relation = Relation.ARRAY)
        public int @AnnotType(3) [] fooIntArray() {return null;}

        @AnnotTypeInfo(count = 1, relation = Relation.ARRAY)
        public String @AnnotType(4) [] fooStringArray() {return null;}

        @AnnotTypeInfo(count = 3, relation = Relation.ARRAY)
        @AnnotType(5)
        public String  @AnnotType(0) [] @AnnotType(1) [] fooStringArrayArray() {return null;}

        @AnnotTypeInfo(count = 1, relation = Relation.POSTFIX)
        @AnnotType(6)
        public Set<String> fooSetString() {return null;}

        @AnnotTypeInfo(count = 2, relation = Relation.STRIPPED)
        @AnnotType(7)
        public Set<@AnnotType(8) Number> fooSetNumber() {return null;}

        @AnnotTypeInfo(count = 1, relation = Relation.POSTFIX)
        @AnnotType(9)
        public E fooE() {return null;}

        @AnnotTypeInfo(count = 1, relation = Relation.POSTFIX)
        @AnnotType(10)
        public F fooF() {return null;}

        @AnnotTypeInfo(count = 1, relation = Relation.POSTFIX)
        @AnnotType(11)
        public <G> G fooG() {return null;}

        @AnnotTypeInfo(count = 1, relation = Relation.POSTFIX)
        @AnnotType(12)
        public Set<? extends Number> fooNumberSet() {return null;}

        @AnnotTypeInfo(count = 2, relation = Relation.STRIPPED)
        @AnnotType(13)
        public Set<@AnnotType(14) ? extends Number> fooNumberSet2() {return null;}

        @AnnotTypeInfo(count = 2, relation = Relation.STRIPPED)
        @AnnotType(15)
        public Set< ? extends @AnnotType(16) Long> fooNumberSet3() {return null;}

        @AnnotTypeInfo(count = 2, relation = Relation.STRIPPED)
        @AnnotType(16)
        public Set<@AnnotType(17) ?> fooObjectSet() {return null;}

        @AnnotTypeInfo(count = 2, relation = Relation.OTHER)
        @AnnotType(18)
        public List<? extends @AnnotType(19) Object> fooObjectList() {return null;}
    }
}

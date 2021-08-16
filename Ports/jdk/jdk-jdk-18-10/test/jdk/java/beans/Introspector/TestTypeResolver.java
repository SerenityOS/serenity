/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests com.sun.beans.TypeResolver
 * @author Eamonn McManus
 * @modules java.base/sun.reflect.generics.reflectiveObjects
 *          java.desktop/com.sun.beans
 */

import com.sun.beans.TypeResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public class TestTypeResolver {
    static final List<Class<?>> failedCases = new ArrayList<Class<?>>();

    public static void main(String[] args) throws Exception {
        test(TestTypeResolver.class);
        if (failedCases.isEmpty())
            System.out.println("TEST PASSED");
        else {
            System.out.println("TEST FAILED: failed cases: " + failedCases);
            throw new Error("TEST FAILED");
        }
    }

    private static void test(Class<?> c) throws Exception {
        /* Every public nested class represents a test.  In each case, either
         * the class contains further nested classes, in which case we
         * call this method recursively; or it declares or inherits a
         * method called getThing() and it declares a static field
         * called "expect" which
         * is the Type of that method's return value.  The test consists
         * of checking that the value returned by
         * TypeResolver.resolveInClass is indeed this Type.
         */
        System.out.println("Test " + c);
        Class<?>[] nested = c.getClasses();
        Arrays.sort(nested, classNameComparator);
        for (Class<?> n : nested)
            test(n);
        final Method m;
        try {
            m = c.getMethod("getThing");
        } catch (NoSuchMethodException e) {
            if (nested.length == 0) {
                System.out.println(
                        "TEST ERROR: class " + c.getName() + " has neither " +
                                "nested classes nor getThing() method");
                failedCases.add(c);
            }
            return;
        }
        Object expect = null;
        try {
            Field f = c.getDeclaredField("expect");
            expect = f.get(null);
        } catch (NoSuchFieldException e) {
            Class<?> outer = c.getDeclaringClass();
            if (outer != null) {
                try {
                    Field f = outer.getDeclaredField("expect" + c.getSimpleName());
                    expect = f.get(null);
                } catch (NoSuchFieldException e1) {
                }
            }
        }
        if (expect == null) {
            System.out.println(
                    "TEST ERROR: class " + c.getName() + " has getThing() method " +
                            "but not expect field");
            failedCases.add(c);
            return;
        }
        Type t = m.getGenericReturnType();
//        t = FixType.fixType(t, c);
        t = TypeResolver.resolveInClass(c, t);
        System.out.print("..." + t);
        // check expected value, and incidentally equals method defined
        // by private implementations of the various Type interfaces
        if (expect.equals(t) && t.equals(expect))
            System.out.println(", as expected");
        else if ((expect.equals(t) || t.equals(expect)) && expect.toString().equals(t.toString()))
            System.out.println(", as workaround of the 8023301 bug");
        else {
            System.out.println(" BUT SHOULD BE " + expect);
            failedCases.add(c);
        }
    }

    private static class ClassNameComparator implements Comparator<Class<?>> {
        public int compare(Class<?> a, Class<?> b) {
            return a.getName().compareTo(b.getName());
        }
    }

    private static final Comparator<Class<?>> classNameComparator =
            new ClassNameComparator();

    private static abstract class TypeVariableImpl<D extends GenericDeclaration>
            implements TypeVariable<D> {
        private final String name;
        private final D gd;
        private final Type[] bounds;

        TypeVariableImpl(String name, D gd, Type... bounds) {
            this.name = name;
            this.gd = gd;
            if (bounds.length == 0)
                bounds = new Type[] {Object.class};
            this.bounds = bounds.clone();
        }

        public Type[] getBounds() {
            return bounds.clone();
        }

        public D getGenericDeclaration() {
            return gd;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return name;
        }

        public boolean equals(Object o) {
            if (!(o instanceof TypeVariable))
                return false;
            TypeVariable tv = (TypeVariable) o;
            return equal(name, tv.getName()) &&
                    equal(gd, tv.getGenericDeclaration()) &&
                    Arrays.equals(bounds, tv.getBounds());
        }

        public int hashCode() {
            return hash(name) ^ hash(gd) ^ Arrays.hashCode(bounds);
        }

        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false; // not used
        }

        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null; // not used
        }

        public <T extends Annotation> T[] getAnnotations(Class<T> annotationClass) {
            return null; // not used
        }

        public Annotation[] getAnnotations() {
            return null; // not used
        }

        public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
            return null; // not used
        }

        public <T extends Annotation> T[] getDeclaredAnnotations(Class<T> annotationClass) {
            return null; // not used
        }

        public Annotation[] getDeclaredAnnotations() {
            return null; // not used
        }

        public AnnotatedType[] getAnnotatedBounds() {
            return null; // not used
        }

        public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
            return null; // not used
        }

        public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
            return null; // not used
        }

    }

    private static class ClassTypeVariable extends TypeVariableImpl<Class<?>> {
        ClassTypeVariable(String name, Class<?> gd, Type... bounds) {
            super(name, gd, bounds);
        }
    }

    private static class MethodTypeVariable extends TypeVariableImpl<Method> {
        MethodTypeVariable(String name, Method gd, Type... bounds) {
            super(name, gd, bounds);
        }
    }

    private static class WildcardTypeImpl implements WildcardType {
        private final Type[] upperBounds;
        private final Type[] lowerBounds;

        WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            if (upperBounds == null || upperBounds.length == 0)
                upperBounds = new Type[] {Object.class};
            if (lowerBounds == null)
                lowerBounds = new Type[0];
            this.upperBounds = upperBounds.clone();
            this.lowerBounds = lowerBounds.clone();
        }

        public Type[] getUpperBounds() {
            return upperBounds.clone();
        }

        public Type[] getLowerBounds() {
            return lowerBounds.clone();
        }

        public boolean equals(Object o) {
            if (o instanceof WildcardType) {
                WildcardType wt = (WildcardType) o;
                return Arrays.equals(upperBounds, wt.getUpperBounds()) &&
                        Arrays.equals(lowerBounds, wt.getLowerBounds());
            } else
                return false;
        }

        public int hashCode() {
            return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("?");
            if (upperBounds.length > 1 || upperBounds[0] != Object.class) {
                sb.append(" extends");
                appendBounds(sb, upperBounds);
            }
            if (lowerBounds.length > 0) {
                sb.append(" super");
                appendBounds(sb, lowerBounds);
            }
            return sb.toString();
        }

        private static void appendBounds(StringBuilder sb, Type[] bounds) {
            boolean and = false;
            for (Type bound : bounds) {
                if (and)
                    sb.append(" &");
                sb.append(" ");
                if (bound instanceof Class)
                    sb.append(((Class<?>) bound).getName());
                else
                    sb.append(bound);
                and = true;
            }
        }
    }

    static boolean equal(Object x, Object y) {
        if (x == y)
            return true;
        if (x == null || y == null)
            return false;
        return x.equals(y);
    }

    static int hash(Object x) {
        return (x == null) ? null : x.hashCode();
    }


    public static class Outer<T> {
        public class Inner {
            public T getThing() {
                return null;
            }
        }

        static final Type expectInner = new ClassTypeVariable("T", Outer.class);
    }

    public static class Super<T> {
        static final Type expect = new ClassTypeVariable("T", Super.class);

        public T getThing() {
            return null;
        }
    }

    public static class Int extends Super<Integer> {
        static final Type expect = Integer.class;
    }

    public static class IntOverride extends Int {
        static final Type expect = Integer.class;

        public Integer getThing() {
            return null;
        }
    }

    public static class Mid<X> extends Super<X> {
        static final Type expect = new ClassTypeVariable("X", Mid.class);
    }

    public static class Str extends Mid<String> {
        static final Type expect = String.class;
    }

    public static class ListInt extends Super<List<Integer>> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class, new Type[] {Integer.class}, null);
    }

    public static class ListIntSub extends ListInt {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class, new Type[] {Integer.class}, null);

        public List<Integer> getThing() {
            return null;
        }
    }

    public static class ListU<U> extends Super<List<U>> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class, new Type[] {new ClassTypeVariable("U", ListU.class)}, null);
    }

    public static class ListUInt extends ListU<Integer> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class, new Type[] {Integer.class}, null);
    }

    public static class ListUSub<V> extends ListU<V> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class, new Type[] {new ClassTypeVariable("V", ListUSub.class)}, null);

        public List<V> getThing() {
            return null;
        }
    }

    public static class ListUSubInt extends ListUSub<Integer> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class, new Type[] {Integer.class}, null);
    }

    public static class TwoParams<S, T> extends Super<S> {
        static final Type expect = new ClassTypeVariable("S", TwoParams.class);
    }

    public static class TwoParamsSub<T> extends TwoParams<T, Integer> {
        static final Type expect = new ClassTypeVariable("T", TwoParamsSub.class);
    }

    public static class TwoParamsSubSub extends TwoParamsSub<String> {
        static final Type expect = String.class;
    }

    public static interface Intf<T> {
        static final Type expect = new ClassTypeVariable("T", Intf.class);

        public T getThing();
    }

    public static abstract class Impl implements Intf<String> {
        static final Type expect = String.class;
    }

    public static class Impl2 extends Super<String> implements Intf<String> {
        static final Type expect = String.class;
    }

    public static class Bound<T extends Number> extends Super<T> {
        static final Type expect = new ClassTypeVariable("T", Bound.class, Number.class);
    }

    public static class BoundInt extends Bound<Integer> {
        static final Type expect = Integer.class;
    }

    public static class RawBound extends Bound {
        static final Type expect = Number.class;
    }

    public static class RawBoundInt extends BoundInt {
        static final Type expect = Integer.class;
    }

    public static class MethodParam<T> {
        private static final Method m;

        static {
            try {
                m = MethodParam.class.getMethod("getThing");
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        static final Type expect = new MethodTypeVariable("T", m);

        public <T> T getThing() {
            return null;
        }
    }

    public static class Raw extends Super {
        static final Type expect = Object.class;
    }

    public static class RawSub extends Raw {
        static final Type expect = Object.class;
    }

    public static class SimpleArray extends Super<String[]> {
        static final Type expect = String[].class;
    }

    public static class GenericArray extends Super<List<String>[]> {
        static final Type expect = GenericArrayTypeImpl.make(
                ParameterizedTypeImpl.make(List.class, new Type[] {String.class}, null));
    }

    public static class GenericArrayT<T> extends Super<T[]> {
        static final Type expect = GenericArrayTypeImpl.make(
                new ClassTypeVariable("T", GenericArrayT.class));
    }

    public static class GenericArrayTSub extends GenericArrayT<String[]> {
        static final Type expect = String[][].class;
    }

    public static class Wildcard extends Super<List<?>> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class, new Type[] {new WildcardTypeImpl(null, null)}, null);
    }

    public static class WildcardT<T> extends Super<List<? extends T>> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class,
                new Type[] {
                        new WildcardTypeImpl(
                                new Type[] {new ClassTypeVariable("T", WildcardT.class)},
                                null)},
                null);
    }

    public static class WildcardTSub extends WildcardT<Integer> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class,
                new Type[] {
                        new WildcardTypeImpl(
                                new Type[] {Integer.class},
                                null)},
                null);
    }

    public static class WildcardTSubSub<X> extends WildcardTSub {
        // X is just so we can have a raw subclass
        static final Type expect = WildcardTSub.expect;
    }

    public static class RawWildcardTSubSub extends WildcardTSubSub {
        static final Type expect = List.class;
    }

    public static class WildcardTSuper<T> extends Super<List<? super T>> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class,
                new Type[] {
                        new WildcardTypeImpl(
                                null,
                                new Type[] {new ClassTypeVariable("T", WildcardTSuper.class)})},
                null);
    }

    public static class WildcardTSuperSub extends WildcardTSuper<Integer> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class,
                new Type[] {
                        new WildcardTypeImpl(
                                null,
                                new Type[] {Integer.class})},
                null);
    }

    public static class SuperMap<K, V> {
        static final Type expect = ParameterizedTypeImpl.make(
                Map.class,
                new Type[] {
                        new ClassTypeVariable("K", SuperMap.class),
                        new ClassTypeVariable("V", SuperMap.class)},
                null);

        public Map<K, V> getThing() {
            return null;
        }
    }

    public static class SubMap extends SuperMap<String, Integer> {
        static final Type expect = ParameterizedTypeImpl.make(
                Map.class,
                new Type[] {String.class, Integer.class},
                null);
    }

    public static class ListListT<T> extends Super<List<List<T>>> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class,
                new Type[] {
                        ParameterizedTypeImpl.make(
                                List.class,
                                new Type[] {new ClassTypeVariable("T", ListListT.class)},
                                null)},
                null);
    }

    public static class ListListString extends ListListT<String> {
        static final Type expect = ParameterizedTypeImpl.make(
                List.class,
                new Type[] {
                        ParameterizedTypeImpl.make(
                                List.class,
                                new Type[] {String.class},
                                null)},
                null);
    }

    public static class UExtendsT<T, U extends T> extends Super<U> {
        static final Type expect = new ClassTypeVariable(
                "U", UExtendsT.class, new ClassTypeVariable("T", UExtendsT.class));
    }

    public static class UExtendsTSub extends UExtendsT<Number, Integer> {
        static final Type expect = Integer.class;
    }

    public static class SelfRef<T extends SelfRef<T>> extends Super<T> {
        static final Type expect =
                SelfRef.class.getTypeParameters()[0];
    }

    public static class SelfRefSub extends SelfRef<SelfRefSub> {
        static final Type expect = SelfRefSub.class;
    }
}

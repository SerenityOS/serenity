/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8039916
 * @summary Test that a call to getType() on an AnnotatedType returned from an
 *          Executable.getAnnotated* returns the same type as the corresponding
 *          Executable.getGeneric* call.
 * @run testng TestExecutableGetAnnotatedType
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.*;

public class TestExecutableGetAnnotatedType {
    @Test(dataProvider = "genericExecutableData")
    public void testGenericMethodExceptions(Executable e) throws Exception {
        testExceptions(e);
    }

    @Test(dataProvider = "executableData")
    public void testMethodExceptions(Executable e) throws Exception {
        testExceptions(e);
    }

    @Test(dataProvider = "genericExecutableData")
    public void testGenericMethodParameterTypes(Executable e) throws Exception {
        testMethodParameters(e);
    }

    @Test(dataProvider = "executableData")
    public void testMethodParameterTypes(Executable e) throws Exception {
        testMethodParameters(e);
    }

    @Test(dataProvider = "genericExecutableData")
    public void testGenericParameterTypes(Executable e) throws Exception {
        testParameters(e.getParameters());
    }

    @Test(dataProvider = "executableData")
    public void testParameterTypes(Executable e) throws Exception {
        testParameters(e.getParameters());
    }

    @Test(dataProvider = "genericMethodData")
    public void testGenericReceiverType(Executable e) throws Exception {
        testParameterizedReceiverType0(e);
    }

    @Test(dataProvider = "methodData")
    public void testReceiverType(Executable e) throws Exception {
        testReceiverType0(e);
    }

    @Test(dataProvider = "genericMethodData")
    public void testGenericMethodReturnType(Object o) throws Exception {
        // testng gets confused if the param to this method has type Method
        Method m = (Method)o;
        testReturnType(m);
    }

    @Test(dataProvider = "methodData")
    public void testMethodReturnType(Object o) throws Exception {
        // testng gets confused if the param to this method has type Method
        Method m = (Method)o;
        testReturnType(m);
    }

    private void testExceptions(Executable e) {
        Type[] ts = e.getGenericExceptionTypes();
        AnnotatedType[] ats = e.getAnnotatedExceptionTypes();
        assertEquals(ts.length, ats.length);

        for (int i = 0; i < ts.length; i++) {
            Type t = ts[i];
            AnnotatedType at = ats[i];
            assertSame(at.getType(), t, e.toString() + ": T: " + t + ", AT: " + at + ", AT.getType(): " + at.getType() + "\n");
        }
    }

    private void testMethodParameters(Executable e) {
        Type[] ts = e.getGenericParameterTypes();
        AnnotatedType[] ats = e.getAnnotatedParameterTypes();
        assertEquals(ts.length, ats.length);

        for (int i = 0; i < ts.length; i++) {
            Type t = ts[i];
            AnnotatedType at = ats[i];
            assertSame(at.getType(), t, e.toString() + ": T: " + t + ", AT: " + at + ", AT.getType(): " + at.getType() + "\n");
        }
    }

    private void testParameters(Parameter[] params) {
        for (Parameter p : params) {
            Type t = p.getParameterizedType();
            AnnotatedType at = p.getAnnotatedType();
            assertSame(at.getType(), t, p.toString() + ": T: " + t + ", AT: " + at + ", AT.getType(): " + at.getType() + "\n");
        }
    }

    private void testReceiverType0(Executable e) {
        if (Modifier.isStatic(e.getModifiers()))
            assertNull(e.getAnnotatedReceiverType());
        else
            assertSame(e.getAnnotatedReceiverType().getType(), e.getDeclaringClass());
    }

    private void testParameterizedReceiverType0(Executable e) {
        if (Modifier.isStatic(e.getModifiers()))
            assertNull(e.getAnnotatedReceiverType());
        else {
            assertTrue(e.getAnnotatedReceiverType().getType() instanceof ParameterizedType);
            assertSame(((ParameterizedType) e.getAnnotatedReceiverType().getType()).getRawType(), e.getDeclaringClass());
        }
    }

    private void testReturnType(Method m) {
        Type t = m.getGenericReturnType();
        AnnotatedType at = m.getAnnotatedReturnType();
        assertSame(at.getType(), t, m.toString() + ": T: " + t + ", AT: " + at + ", AT.getType(): " + at.getType() + "\n");
    }

    @DataProvider
    public Object[][] methodData() throws Exception {
        return filterData(Arrays.stream(Methods1.class.getMethods()), Methods1.class)
            .toArray(new Object[0][0]);
    }

    @DataProvider
    public Object[][] genericMethodData()  throws Exception {
        return filterData(Arrays.stream(GenericMethods1.class.getMethods()), GenericMethods1.class)
            .toArray(new Object[0][0]);
    }

    @DataProvider
    public Object[][] executableData() throws Exception {
    @SuppressWarnings("raw")
        List l = filterData(Arrays.stream(Methods1.class.getMethods()), Methods1.class);
        l.addAll(filterData(Arrays.stream(Methods1.class.getConstructors()), Methods1.class));
        l.addAll(filterData(Arrays.stream(Ctors1.class.getConstructors()), Ctors1.class));
        return ((List<Object[][]>)l).toArray(new Object[0][0]);
    }

    @DataProvider
    public Object[][] genericExecutableData() throws Exception {
    @SuppressWarnings("raw")
        List l = filterData(Arrays.stream(GenericMethods1.class.getMethods()), GenericMethods1.class);
        l.addAll(filterData(Arrays.stream(GenericMethods1.class.getConstructors()), GenericMethods1.class));
        l.addAll(filterData(Arrays.stream(GenericCtors1.class.getConstructors()), GenericCtors1.class));
        return ((List<Object[][]>)l).toArray(new Object[0][0]);
    }

    private List<?> filterData(Stream<? extends Executable> l, Class<?> c) {
        return l.filter(m -> (m.getDeclaringClass() == c)) // remove object methods
            .map(m -> { Object[] o = new Object[1]; o[0] = m; return o; })
            .collect(Collectors.toList());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface TA {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface TB {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface TC {}

    public static class Methods1 {
        public static void m1() throws Error, RuntimeException {;}
        public static long m2(int a, double b) throws Error, RuntimeException { return 0L; }
        public static Object m3(String s, List l) throws Error, RuntimeException { return null; }
        public static Object m4(String s, List<String> l) { return null; }
        public static Object m4(String s, List<String> l, boolean ... b){ return null; }

        public static void m10() throws @TA Error, @TB @TC RuntimeException {;}
        public static @TB long m20(@TC int a, @TA double b) throws @TA Error, @TB @TC RuntimeException { return 0L; }
        public static @TC Object m30(@TA String s, @TB List l) throws @TA Error, @TB @TC RuntimeException { return null; }
        public static @TA Object m40(@TB String s, @TC List<@TA String> l) { return null; }
        public static @TA Object m40(@TB String s, @TC List<@TA String> l, @TB boolean ... b) { return null; }

        public Methods1(int a, double b) {}
        public Methods1(String s, List<String> l, boolean ... b) {}
        public Methods1(@TC long a, @TA float b) {}
        public Methods1(@TA int i, @TB String s, @TC List<@TA String> l, @TB boolean ... b) {}
    }

    // test default ctor
    public static class Ctors1 {
    }

    public static class GenericMethods1<E> {
        public E m1(E e, Object o) throws Error, RuntimeException { return null; }
        public E m2(List<? extends List> e, int i) throws Error, RuntimeException { return null; }
        public E m3(double d, List<E> e) throws Error, RuntimeException { return null; }
        public <E extends List> E m4(byte[] b, GenericMethods1<? extends E> e) { return null; }
        public <E extends List> E m5(GenericMethods1<? super Number> e) { return null; }
        public <E extends List & Cloneable> E m6(char c, E e) { return null; }
        public <E extends List & Cloneable> E m7(char c, E e, byte ... b) { return null; }

        public static <M> M n1(M e) { return null; }
        public static <M> M n2(List<? extends List> e) { return null; }
        public static <M extends RuntimeException> M n3(List<M> e) throws Error, M { return null; }
        public static <M extends Number> M n4(GenericMethods1<? extends M> e) throws Error, RuntimeException { return null; }
        public static <M extends Object> M n5(GenericMethods1<? super Number> e) { return null; }
        public static <M extends List & Cloneable> M n6(M e) { return null; }

        public <M> E o1(E e) { return null; }
        public <M> E o2(List<? extends List> e) { return null; }
        public <M extends Error, N extends RuntimeException> E o3(GenericMethods1<E> this, List<E> e) throws M, N { return null; }
        public <M extends Number> E o4(GenericMethods1<? extends E> e) throws Error, RuntimeException { return null; }
        public <M extends Object> E o5(GenericMethods1<? super Number> e) { return null; }
        public <M extends List & Cloneable> E o6(E e) { return null; }


        // with annotations
        public @TA E m10(E e, @TC Object o) throws @TA Error, @TB @TC RuntimeException { return null; }
        public @TB E m20(@TA List<@TA ? extends @TA List> e, @TC int i) throws @TA Error, @TB @TC RuntimeException { return null; }
        public @TB E m30(@TC double d, List<E> e) throws @TA Error, @TB @TC RuntimeException { return null; }
        public <@TA E extends @TA List> @TA E m40(@TA byte @TB [] b, GenericMethods1<@TA ? extends E> e) { return null; }
        public <@TB E extends @TB List> E m50(@TA GenericMethods1<? super Number> e) { return null; }
        public <@TB E extends @TA List & Cloneable> E m60(@TC char c, E e) { return null; }
        public <@TB E extends @TA List & Cloneable> E m70(@TC char c, E e, @TA @TB byte ... b) { return null; }

        public static <@TA M> @TA M n10(M e) { return null; }
        public static <@TA @TB @TC M> M n20(List<@TA ? extends List> e) { return null; }
        @TA @TB @TC public static <M extends RuntimeException> M n30(List<@TB M> e) throws @TA Error, @TB @TC M { return null; }
        public static <@TC M extends Number> M n40(GenericMethods1<? extends @TA M> e) throws @TA Error, @TB @TC RuntimeException { return null; }
        @TA public static <M extends @TB Object> M n50(GenericMethods1<? super Number> e) { return null; }
        public static <@TA M extends @TB List & @TC @TB Cloneable> M n60(M e) { return null; }

        public <@TC M> E o10(@TA E e) { return null; }
        public <M> @TA E o20(@TB List<@TB ? extends @TB List> e) { return null; }
        @TC public <M extends Error, N extends RuntimeException> @TB E o30(@TA @TB @TC GenericMethods1<E> this, List<E> e) throws @TA M, @TB @TC N { return null; }
        public <@TA M extends Number> E o40(GenericMethods1<? extends @TA E> e) throws @TA Error, @TB @TC RuntimeException { return null; }
        public <M extends @TA Object> E o50(GenericMethods1<@TA ? super Number> e) { return null; }
        public <@TA M extends @TB List & @TC Cloneable> E o60(@TA E e) { return null; }


        // ctors
        public GenericMethods1(List<? extends List> e, int i) throws Error, RuntimeException { }
        public <E extends List & Cloneable> GenericMethods1(char c, E e, byte ... b) { }
        @TC public <M extends Error, N extends RuntimeException> GenericMethods1(List<@TC E> e) throws @TA M, @TB @TC N { }
        public <@TA M extends @TB List & @TC Cloneable> GenericMethods1(@TA E e, @TB M m) throws @TA Exception { }
        public <@TA M extends @TB List & @TC Cloneable> GenericMethods1(@TA E e, @TB M m, @TC byte ... b) throws Exception { }
    }

    // test default ctor
    public static class GenericCtors1<T> {
    }
}

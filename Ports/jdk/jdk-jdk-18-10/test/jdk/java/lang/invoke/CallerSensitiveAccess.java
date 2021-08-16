/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8196830 8235351
 * @modules java.base/jdk.internal.reflect
 * @run testng/othervm CallerSensitiveAccess
 * @summary Check Lookup findVirtual, findStatic and unreflect behavior with
 *          caller sensitive methods with focus on AccessibleObject.setAccessible
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.reflect.CallerSensitive;

import org.testng.annotations.DataProvider;
import org.testng.annotations.NoInjection;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CallerSensitiveAccess {

    /**
     * Caller sensitive methods in APIs exported by java.base.
     */
    @DataProvider(name = "callerSensitiveMethods")
    static Object[][] callerSensitiveMethods() {
        return callerSensitiveMethods(Object.class.getModule())
                .map(m -> new Object[] { m, shortDescription(m) })
                .toArray(Object[][]::new);
    }

    /**
     * Using publicLookup, attempt to use findVirtual or findStatic to obtain a
     * method handle to a caller sensitive method.
     */
    @Test(dataProvider = "callerSensitiveMethods",
            expectedExceptions = IllegalAccessException.class)
    public void testPublicLookupFind(@NoInjection Method method, String desc) throws Exception {
        Lookup lookup = MethodHandles.publicLookup();
        Class<?> refc = method.getDeclaringClass();
        String name = method.getName();
        MethodType mt = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        if (Modifier.isStatic(method.getModifiers())) {
            lookup.findStatic(refc, name, mt);
        } else {
            lookup.findVirtual(refc, name, mt);
        }
    }

    /**
     * Using publicLookup, attempt to use unreflect to obtain a method handle to a
     * caller sensitive method.
     */
    @Test(dataProvider = "callerSensitiveMethods",
            expectedExceptions = IllegalAccessException.class)
    public void testPublicLookupUnreflect(@NoInjection Method method, String desc) throws Exception {
        MethodHandles.publicLookup().unreflect(method);
    }

    /**
     * public accessible caller sensitive methods in APIs exported by java.base.
     */
    @DataProvider(name = "accessibleCallerSensitiveMethods")
    static Object[][] accessibleCallerSensitiveMethods() {
        return callerSensitiveMethods(Object.class.getModule())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(m -> { m.setAccessible(true); return m; })
                .map(m -> new Object[] { m, shortDescription(m) })
                .toArray(Object[][]::new);
    }

    /**
     * Using publicLookup, attempt to use unreflect to obtain a method handle to a
     * caller sensitive method.
     */
    @Test(dataProvider = "accessibleCallerSensitiveMethods",
            expectedExceptions = IllegalAccessException.class)
    public void testLookupUnreflect(@NoInjection Method method, String desc) throws Exception {
        MethodHandles.publicLookup().unreflect(method);
    }

    /**
     * Using a Lookup with no original access that can't lookup caller-sensitive
     * method
     */
    @Test(dataProvider = "callerSensitiveMethods",
            expectedExceptions = IllegalAccessException.class)
    public void testLookupNoOriginalAccessFind(@NoInjection Method method, String desc) throws Exception {
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.ORIGINAL);
        assertTrue(lookup.hasFullPrivilegeAccess());
        Class<?> refc = method.getDeclaringClass();
        String name = method.getName();
        MethodType mt = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        if (Modifier.isStatic(method.getModifiers())) {
            lookup.findStatic(refc, name, mt);
        } else {
            lookup.findVirtual(refc, name, mt);
        }
    }

    /**
     * Using a Lookup with no original access that can't unreflect caller-sensitive
     * method
     */
    @Test(dataProvider = "callerSensitiveMethods",
            expectedExceptions = IllegalAccessException.class)
    public void testLookupNoOriginalAccessUnreflect(@NoInjection Method method, String desc) throws Exception {
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.ORIGINAL);
        assertTrue(lookup.hasFullPrivilegeAccess());
        lookup.unreflect(method);
    }

    // -- Test method handles to setAccessible --

    private int aField;

    Field accessibleField() {
        try {
            return getClass().getDeclaredField("aField");
        } catch (NoSuchFieldException e) {
            fail();
            return null;
        }
    }

    Field inaccessibleField() {
        try {
            return String.class.getDeclaredField("hash");
        } catch (NoSuchFieldException e) {
            fail();
            return null;
        }
    }

    void findAndInvokeSetAccessible(Class<? extends AccessibleObject> refc, Field f) throws Throwable {
        MethodType mt = MethodType.methodType(void.class, boolean.class);
        MethodHandle mh = MethodHandles.lookup().findVirtual(refc, "setAccessible", mt);
        mh.invoke(f, true);  // may throw InaccessibleObjectException
        assertTrue(f.isAccessible());
    }

    void unreflectAndInvokeSetAccessible(Method m, Field f) throws Throwable {
        assertTrue(m.getName().equals("setAccessible"));
        assertFalse(Modifier.isStatic(m.getModifiers()));
        MethodHandle mh = MethodHandles.lookup().unreflect(m);
        mh.invoke(f, true);   // may throw InaccessibleObjectException
        assertTrue(f.isAccessible());
    }

    /**
     * Create a method handle to setAccessible and use it to suppress access to an
     * accessible member.
     */
    @Test
    public void testSetAccessible1() throws Throwable {
        findAndInvokeSetAccessible(AccessibleObject.class, accessibleField());
    }
    @Test
    public void testSetAccessible2() throws Throwable {
        findAndInvokeSetAccessible(Field.class, accessibleField());
    }
    @Test
    public void testSetAccessible3() throws Throwable {
        Method m = AccessibleObject.class.getMethod("setAccessible", boolean.class);
        unreflectAndInvokeSetAccessible(m, accessibleField());
    }
    @Test
    public void testSetAccessible4() throws Throwable {
        Method m = Field.class.getMethod("setAccessible", boolean.class);
        unreflectAndInvokeSetAccessible(m, accessibleField());
    }

    /**
     * Create a method handle to setAccessible and attempt to use it to suppress
     * access to an inaccessible member.
     */
    @Test(expectedExceptions = InaccessibleObjectException.class)
    public void testSetAccessible5() throws Throwable {
        findAndInvokeSetAccessible(AccessibleObject.class, inaccessibleField());
    }
    @Test(expectedExceptions = InaccessibleObjectException.class)
    public void testSetAccessible6() throws Throwable {
        findAndInvokeSetAccessible(Field.class, inaccessibleField());
    }
    @Test(expectedExceptions = InaccessibleObjectException.class)
    public void testSetAccessible7() throws Throwable {
        Method m = AccessibleObject.class.getMethod("setAccessible", boolean.class);
        unreflectAndInvokeSetAccessible(m, inaccessibleField());
    }
    @Test(expectedExceptions = InaccessibleObjectException.class)
    public void testSetAccessible8() throws Throwable {
        Method m = Field.class.getMethod("setAccessible", boolean.class);
        unreflectAndInvokeSetAccessible(m, inaccessibleField());
    }


    // -- Test sub-classes of AccessibleObject --

    /**
     * Custom AccessibleObject objects. One class overrides setAccessible, the other
     * does not override this method.
     */
    @DataProvider(name = "customAccessibleObjects")
    static Object[][] customAccessibleObjectClasses() {
        return new Object[][] { { new S1() }, { new S2() } };
    }
    public static class S1 extends AccessibleObject { }
    public static class S2 extends AccessibleObject {
        @Override
        public void setAccessible(boolean flag) {
            super.setAccessible(flag);
        }
    }

    void findAndInvokeSetAccessible(Lookup lookup, AccessibleObject obj) throws Throwable {
        MethodType mt = MethodType.methodType(void.class, boolean.class);
        MethodHandle mh = lookup.findVirtual(obj.getClass(), "setAccessible", mt);
        mh.invoke(obj, true);
        assertTrue(obj.isAccessible());
    }

    void unreflectAndInvokeSetAccessible(Lookup lookup, AccessibleObject obj) throws Throwable {
        Method m = obj.getClass().getMethod("setAccessible", boolean.class);
        MethodHandle mh = lookup.unreflect(m);
        mh.invoke(obj, true);
        assertTrue(obj.isAccessible());
    }

    /**
     * Using publicLookup, create a method handle to setAccessible and invoke it
     * on a custom AccessibleObject object.
     */
    @Test(expectedExceptions = IllegalAccessException.class)
    public void testPublicLookupSubclass1() throws Throwable {
        // S1 does not override setAccessible
        findAndInvokeSetAccessible(MethodHandles.publicLookup(), new S1());
    }
    @Test
    public void testPublicLookupSubclass2() throws Throwable {
        // S2 overrides setAccessible
        findAndInvokeSetAccessible(MethodHandles.publicLookup(), new S2());
    }
    @Test(expectedExceptions = IllegalAccessException.class)
    public void testPublicLookupSubclass3() throws Throwable {
        // S1 does not override setAccessible
        unreflectAndInvokeSetAccessible(MethodHandles.publicLookup(), new S1());
    }
    @Test
    public void testPublicLookupSubclass4() throws Throwable {
        // S2 overrides setAccessible
        unreflectAndInvokeSetAccessible(MethodHandles.publicLookup(), new S2());
    }

    /**
     * Using a full power lookup, create a method handle to setAccessible and
     * invoke it on a custom AccessibleObject object.
     */
    @Test(dataProvider = "customAccessibleObjects")
    public void testLookupSubclass1(AccessibleObject obj) throws Throwable {
        findAndInvokeSetAccessible(MethodHandles.lookup(), obj);
    }
    @Test(dataProvider = "customAccessibleObjects")
    public void testLookupSubclass2(AccessibleObject obj) throws Throwable {
        unreflectAndInvokeSetAccessible(MethodHandles.lookup(), obj);
    }

    /**
     * Using a full power lookup, create a method handle to setAccessible on a
     * sub-class of AccessibleObject and then attempt to invoke it on a Field object.
     */
    @Test(dataProvider = "customAccessibleObjects",
            expectedExceptions = ClassCastException.class)
    public void testLookupSubclass3(AccessibleObject obj) throws Throwable {
        MethodType mt = MethodType.methodType(void.class, boolean.class);
        Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(obj.getClass(), "setAccessible", mt);
        mh.invoke(accessibleField(), true);  // should throw ClassCastException
    }

    /**
     * Using a full power lookup, use unreflect to create a method handle to
     * setAccessible on a sub-class of AccessibleObject, then attempt to invoke
     * it on a Field object.
     */
    @Test
    public void testLookupSubclass4() throws Throwable {
        // S1 does not override setAccessible
        Method m = S1.class.getMethod("setAccessible", boolean.class);
        assertTrue(m.getDeclaringClass() == AccessibleObject.class);
        MethodHandle mh = MethodHandles.lookup().unreflect(m);
        Field f = accessibleField();
        mh.invoke(f, true);
        assertTrue(f.isAccessible());
    }
    @Test(expectedExceptions = InaccessibleObjectException.class)
    public void testLookupSubclass5() throws Throwable {
        // S1 does not override setAccessible
        Method m = S1.class.getMethod("setAccessible", boolean.class);
        assertTrue(m.getDeclaringClass() == AccessibleObject.class);
        MethodHandle mh = MethodHandles.lookup().unreflect(m);
        mh.invoke(inaccessibleField(), true);  // should throw InaccessibleObjectException
    }
    @Test(expectedExceptions = ClassCastException.class)
    public void testLookupSubclass6() throws Throwable {
        // S2 overrides setAccessible
        Method m = S2.class.getMethod("setAccessible", boolean.class);
        assertTrue(m.getDeclaringClass() == S2.class);
        MethodHandle mh = MethodHandles.lookup().unreflect(m);
        mh.invoke(accessibleField(), true);  // should throw ClassCastException
    }
    @Test(expectedExceptions = ClassCastException.class)
    public void testLookupSubclass7() throws Throwable {
        // S2 overrides setAccessible
        Method m = S2.class.getMethod("setAccessible", boolean.class);
        assertTrue(m.getDeclaringClass() == S2.class);
        MethodHandle mh = MethodHandles.lookup().unreflect(m);
        mh.invoke(inaccessibleField(), true);  // should throw ClassCastException
    }


    // -- supporting methods --

    /**
     * Returns a stream of all caller sensitive methods on public classes in packages
     * exported by a named module.
     */
    static Stream<Method> callerSensitiveMethods(Module module) {
        assert module.isNamed();
        ModuleReference mref = module.getLayer().configuration()
                .findModule(module.getName())
                .orElseThrow(() -> new RuntimeException())
                .reference();
        // find all ".class" resources in the module
        // transform the resource name to a class name
        // load every class in the exported packages
        // return the caller sensitive methods of the public classes
        try (ModuleReader reader = mref.open()) {
            return reader.list()
                    .filter(rn -> rn.endsWith(".class"))
                    .map(rn -> rn.substring(0, rn.length() - 6)
                                 .replace('/', '.'))
                    .filter(cn -> module.isExported(packageName(cn)))
                    .map(cn -> Class.forName(module, cn))
                    .filter(refc -> refc != null
                                    && Modifier.isPublic(refc.getModifiers()))
                    .map(refc -> callerSensitiveMethods(refc))
                    .flatMap(List::stream);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    static String packageName(String cn) {
        int last = cn.lastIndexOf('.');
        if (last > 0) {
            return cn.substring(0, last);
        } else {
            return "";
        }
    }

    /**
     * Returns a list of the caller sensitive methods directly declared by the given
     * class.
     */
    static List<Method> callerSensitiveMethods(Class<?> refc) {
        return Arrays.stream(refc.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(CallerSensitive.class))
                .collect(Collectors.toList());
    }

    /**
     * Returns a short description of the given method for tracing purposes.
     */
    static String shortDescription(Method m) {
        var sb = new StringBuilder();
        sb.append(m.getDeclaringClass().getName());
        sb.append('.');
        sb.append(m.getName());
        sb.append('(');
        StringJoiner sj = new StringJoiner(",");
        for (Class<?> parameterType : m.getParameterTypes()) {
            sj.add(parameterType.getTypeName());
        }
        sb.append(sj);
        sb.append(')');
        return sb.toString();
    }
}

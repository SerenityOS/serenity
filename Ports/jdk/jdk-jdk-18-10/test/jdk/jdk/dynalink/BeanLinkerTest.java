/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import static jdk.dynalink.StandardNamespace.ELEMENT;
import static jdk.dynalink.StandardNamespace.METHOD;
import static jdk.dynalink.StandardNamespace.PROPERTY;
import static jdk.dynalink.StandardOperation.CALL;
import static jdk.dynalink.StandardOperation.GET;
import static jdk.dynalink.StandardOperation.NEW;
import static jdk.dynalink.StandardOperation.REMOVE;
import static jdk.dynalink.StandardOperation.SET;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.NamedOperation;
import jdk.dynalink.NoSuchDynamicMethodException;
import jdk.dynalink.Operation;
import jdk.dynalink.beans.BeansLinker;
import jdk.dynalink.beans.StaticClass;
import jdk.dynalink.support.SimpleRelinkableCallSite;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @test
 * @run testng/othervm/java.security.policy=untrusted.security.policy BeanLinkerTest
 */
public class BeanLinkerTest {

    private DynamicLinker linker;
    private static final MethodHandles.Lookup MY_LOOKUP = MethodHandles.lookup();

    @SuppressWarnings("unused")
    @DataProvider
    private static Object[][] flags() {
        return new Object[][]{
            {Boolean.FALSE},
            {Boolean.TRUE}
        };
    }

    // helpers to create callsite objects
    private CallSite createCallSite(final boolean publicLookup, final Operation op, final MethodType mt) {
        return linker.link(new SimpleRelinkableCallSite(new CallSiteDescriptor(
                publicLookup ? MethodHandles.publicLookup() : MY_LOOKUP, op, mt)));
    }

    private CallSite createCallSite(final boolean publicLookup, final Operation op, final Object name, final MethodType mt) {
        return createCallSite(publicLookup, op.named(name), mt);
    }

    private CallSite createGetMethodCallSite(final boolean publicLookup, final String name) {
        return createCallSite(publicLookup, GET_METHOD, name, MethodType.methodType(Object.class, Object.class));
    }

    private static final MethodHandle throwArrayIndexOutOfBounds = findThrower("throwArrayIndexOutOfBounds");
    private static final MethodHandle throwIndexOutOfBounds = findThrower("throwIndexOutOfBounds");

    private static final Operation GET_PROPERTY = GET.withNamespace(PROPERTY);
    private static final Operation GET_ELEMENT = GET.withNamespace(ELEMENT);
    private static final Operation GET_METHOD = GET.withNamespace(METHOD);
    private static final Operation SET_ELEMENT = SET.withNamespace(ELEMENT);
    private static final Operation REMOVE_ELEMENT = REMOVE.withNamespace(ELEMENT);

    private static MethodHandle findThrower(final String name) {
        try {
            return MethodHandles.lookup().findStatic(BeanLinkerTest.class, name,
                    MethodType.methodType(Object.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            Assert.fail("Unexpected exception", e);
            return null;
        }
    }

    private static Object throwArrayIndexOutOfBounds(final Object receiver, final Object index) {
        throw new ArrayIndexOutOfBoundsException(String.valueOf(index));
    }

    private static Object throwIndexOutOfBounds(final Object receiver, final Object index) {
        throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    @BeforeTest
    public void initLinker() {
        final DynamicLinkerFactory factory = new DynamicLinkerFactory();
        factory.setFallbackLinkers(new BeansLinker((req, services) -> {
            // This is a MissingMemberHandlerFactory that creates a missing
            // member handler for element getters and setters that throw an
            // ArrayIndexOutOfBoundsException when applied to an array and an
            // IndexOutOfBoundsException when applied to a list.

            final CallSiteDescriptor desc = req.getCallSiteDescriptor();
            final Operation op = desc.getOperation();
            final Operation baseOp = NamedOperation.getBaseOperation(op);
            if (baseOp != GET_ELEMENT && baseOp != SET_ELEMENT && baseOp != REMOVE_ELEMENT) {
                // We only handle GET_ELEMENT, SET_ELEMENT and REMOVE_ELEMENT.
                return null;
            }

            final Object receiver = req.getReceiver();
            Assert.assertNotNull(receiver);

            final Class<?> clazz = receiver.getClass();
            final MethodHandle throwerHandle;
            if (clazz.isArray()) {
                throwerHandle = throwArrayIndexOutOfBounds;
            } else if (List.class.isAssignableFrom(clazz)) {
                throwerHandle = throwIndexOutOfBounds;
            } else if (Map.class.isAssignableFrom(clazz)) {
                return null;
            } else {
                Assert.fail("Unexpected receiver type " + clazz.getName());
                return null;
            }

            final Object name = NamedOperation.getName(op);
            final MethodHandle nameBoundHandle;
            if (name == null) {
                nameBoundHandle = throwerHandle;
            } else {
                // If the operation is for a fixed index, bind it
                nameBoundHandle = MethodHandles.insertArguments(throwerHandle, 1, name);
            }

            final MethodType callSiteType = desc.getMethodType();
            final MethodHandle arityMatchedHandle;
            if (baseOp == SET_ELEMENT) {
                // Drop "value" parameter for a setter
                final int handleArity = nameBoundHandle.type().parameterCount();
                arityMatchedHandle = MethodHandles.dropArguments(nameBoundHandle,
                        handleArity, callSiteType.parameterType(handleArity));
            } else {
                arityMatchedHandle = nameBoundHandle;
            }

            return arityMatchedHandle.asType(callSiteType);
        }));
        this.linker = factory.createLinker();
    }

    @AfterTest
    public void afterTest() {
        this.linker = null;
    }

    @Test(dataProvider = "flags")
    public void getPropertyTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(Object.class, Object.class, String.class);
        final CallSite cs = createCallSite(publicLookup, GET_PROPERTY, mt);
        Assert.assertEquals(cs.getTarget().invoke(new Object(), "class"), Object.class);
        Assert.assertEquals(cs.getTarget().invoke(new Date(), "class"), Date.class);
    }

    @Test(dataProvider = "flags")
    public void getPropertyNegativeTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(Object.class, Object.class, String.class);
        final CallSite cs = createCallSite(publicLookup, GET_PROPERTY, mt);
        Assert.assertNull(cs.getTarget().invoke(new Object(), "DOES_NOT_EXIST"));
    }

    @Test(dataProvider = "flags")
    public void getPropertyTest2(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(Object.class, Object.class);
        final CallSite cs = createCallSite(publicLookup, GET_PROPERTY, "class", mt);
        Assert.assertEquals(cs.getTarget().invoke(new Object()), Object.class);
        Assert.assertEquals(cs.getTarget().invoke(new Date()), Date.class);
    }

    @Test(dataProvider = "flags")
    public void getPropertyNegativeTest2(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(Object.class, Object.class);
        final CallSite cs = createCallSite(publicLookup, GET_PROPERTY, "DOES_NOT_EXIST", mt);

        try {
            cs.getTarget().invoke(new Object());
            throw new RuntimeException("Expected NoSuchDynamicMethodException");
        } catch (final Throwable th) {
            Assert.assertTrue(th instanceof NoSuchDynamicMethodException);
        }
    }

    @Test(dataProvider = "flags")
    public void getLengthPropertyTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(int.class, Object.class, String.class);
        final CallSite cs = createCallSite(publicLookup, GET_PROPERTY, mt);

        Assert.assertEquals((int) cs.getTarget().invoke(new int[10], "length"), 10);
        Assert.assertEquals((int) cs.getTarget().invoke(new String[33], "length"), 33);
    }

    @Test(dataProvider = "flags")
    public void getElementTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(int.class, Object.class, int.class);
        final CallSite cs = createCallSite(publicLookup, GET_ELEMENT, mt);

        final int[] arr = {23, 42};
        Assert.assertEquals((int) cs.getTarget().invoke(arr, 0), 23);
        Assert.assertEquals((int) cs.getTarget().invoke(arr, 1), 42);
        try {
            final int x = (int) cs.getTarget().invoke(arr, -1);
            throw new RuntimeException("expected ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
        }

        try {
            final int x = (int) cs.getTarget().invoke(arr, arr.length);
            throw new RuntimeException("expected ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
        }

        final List<Integer> list = new ArrayList<>();
        list.add(23);
        list.add(430);
        list.add(-4354);
        Assert.assertEquals((int) cs.getTarget().invoke(list, 0), (int) list.get(0));
        Assert.assertEquals((int) cs.getTarget().invoke(list, 1), (int) list.get(1));
        Assert.assertEquals((int) cs.getTarget().invoke(list, 2), (int) list.get(2));
        try {
            cs.getTarget().invoke(list, -1);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }

        try {
            cs.getTarget().invoke(list, list.size());
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }
    }

    private Object invokeWithFixedKey(boolean publicLookup, Operation op, Object name, MethodType mt, Object... args) throws Throwable {
        return createCallSite(publicLookup, op.named(name), mt).getTarget().invokeWithArguments(args);
    }

    @Test(dataProvider = "flags")
    public void getElementWithFixedKeyTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(int.class, Object.class);

        final int[] arr = {23, 42};
        Assert.assertEquals((int) invokeWithFixedKey(publicLookup, GET_ELEMENT, 0, mt, arr), 23);
        Assert.assertEquals((int) invokeWithFixedKey(publicLookup, GET_ELEMENT, 1, mt, arr), 42);
        try {
            invokeWithFixedKey(publicLookup, GET_ELEMENT, -1, mt, arr);
            throw new RuntimeException("expected ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
        }

        try {
            invokeWithFixedKey(publicLookup, GET_ELEMENT, arr.length, mt, arr);
            throw new RuntimeException("expected ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
        }

        final List<Integer> list = new ArrayList<>();
        list.add(23);
        list.add(430);
        list.add(-4354);
        for (int i = 0; i < 3; ++i) {
            Assert.assertEquals((int) invokeWithFixedKey(publicLookup, GET_ELEMENT, i, mt, list), (int) list.get(i));
        }
        try {
            invokeWithFixedKey(publicLookup, GET_ELEMENT, -1, mt, list);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }

        try {
            invokeWithFixedKey(publicLookup, GET_ELEMENT, list.size(), mt, list);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }
    }

    @Test(dataProvider = "flags")
    public void setElementTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(void.class, Object.class, int.class, int.class);
        final CallSite cs = createCallSite(publicLookup, SET_ELEMENT, mt);

        final int[] arr = {23, 42};
        cs.getTarget().invoke(arr, 0, 0);
        Assert.assertEquals(arr[0], 0);
        cs.getTarget().invoke(arr, 1, -5);
        Assert.assertEquals(arr[1], -5);

        try {
            cs.getTarget().invoke(arr, -1, 12);
            throw new RuntimeException("expected ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
        }

        try {
            cs.getTarget().invoke(arr, arr.length, 20);
            throw new RuntimeException("expected ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
        }

        final List<Integer> list = new ArrayList<>();
        list.add(23);
        list.add(430);
        list.add(-4354);

        cs.getTarget().invoke(list, 0, -list.get(0));
        Assert.assertEquals((int) list.get(0), -23);
        cs.getTarget().invoke(list, 1, -430);
        Assert.assertEquals((int) list.get(1), -430);
        cs.getTarget().invoke(list, 2, 4354);
        Assert.assertEquals((int) list.get(2), 4354);
        try {
            cs.getTarget().invoke(list, -1, 343);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }

        try {
            cs.getTarget().invoke(list, list.size(), 43543);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }
    }

    @Test(dataProvider = "flags")
    public void setElementWithFixedKeyTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(void.class, Object.class, int.class);

        final int[] arr = {23, 42};
        invokeWithFixedKey(publicLookup, SET_ELEMENT, 0, mt, arr, 0);
        Assert.assertEquals(arr[0], 0);
        invokeWithFixedKey(publicLookup, SET_ELEMENT, 1, mt, arr, -5);
        Assert.assertEquals(arr[1], -5);

        try {
            invokeWithFixedKey(publicLookup, SET_ELEMENT, -1, mt, arr, 12);
            throw new RuntimeException("expected ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
        }

        try {
            invokeWithFixedKey(publicLookup, SET_ELEMENT, arr.length, mt, arr, 20);
            throw new RuntimeException("expected ArrayIndexOutOfBoundsException");
        } catch (final ArrayIndexOutOfBoundsException ex) {
        }

        final List<Integer> list = new ArrayList<>();
        list.add(23);
        list.add(430);
        list.add(-4354);

        invokeWithFixedKey(publicLookup, SET_ELEMENT, 0, mt, list, -list.get(0));
        Assert.assertEquals((int) list.get(0), -23);
        invokeWithFixedKey(publicLookup, SET_ELEMENT, 1, mt, list, -430);
        Assert.assertEquals((int) list.get(1), -430);
        invokeWithFixedKey(publicLookup, SET_ELEMENT, 2, mt, list, 4354);
        Assert.assertEquals((int) list.get(2), 4354);
        try {
            invokeWithFixedKey(publicLookup, SET_ELEMENT, -1, mt, list, 343);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }

        try {
            invokeWithFixedKey(publicLookup, SET_ELEMENT, list.size(), mt, list, 43543);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }
    }

    @Test(dataProvider = "flags")
    public void newObjectTest(final boolean publicLookup) {
        final MethodType mt = MethodType.methodType(Object.class, Object.class);
        final CallSite cs = createCallSite(publicLookup, NEW, mt);

        Object obj = null;
        try {
            obj = cs.getTarget().invoke(StaticClass.forClass(Date.class));
        } catch (final Throwable th) {
            throw new RuntimeException(th);
        }

        Assert.assertTrue(obj instanceof Date);
    }

    @Test(dataProvider = "flags")
    public void staticPropertyTest(final boolean publicLookup) {
        final MethodType mt = MethodType.methodType(Object.class, Class.class);
        final CallSite cs = createCallSite(publicLookup, GET_PROPERTY, "static", mt);

        Object obj = null;
        try {
            obj = cs.getTarget().invoke(Object.class);
        } catch (final Throwable th) {
            throw new RuntimeException(th);
        }

        Assert.assertTrue(obj instanceof StaticClass);
        Assert.assertEquals(((StaticClass) obj).getRepresentedClass(), Object.class);

        try {
            obj = cs.getTarget().invoke(Date.class);
        } catch (final Throwable th) {
            throw new RuntimeException(th);
        }

        Assert.assertTrue(obj instanceof StaticClass);
        Assert.assertEquals(((StaticClass) obj).getRepresentedClass(), Date.class);

        try {
            obj = cs.getTarget().invoke(Object[].class);
        } catch (final Throwable th) {
            throw new RuntimeException(th);
        }

        Assert.assertTrue(obj instanceof StaticClass);
        Assert.assertEquals(((StaticClass) obj).getRepresentedClass(), Object[].class);
    }

    @Test(dataProvider = "flags")
    public void instanceMethodCallTest(final boolean publicLookup) {
        final CallSite cs = createGetMethodCallSite(publicLookup, "getClass");
        final MethodType mt2 = MethodType.methodType(Class.class, Object.class, Object.class);
        final CallSite cs2 = createCallSite(publicLookup, CALL, mt2);

        Object method = null;
        try {
            method = cs.getTarget().invoke(new Date());
        } catch (final Throwable th) {
            throw new RuntimeException(th);
        }

        Assert.assertNotNull(method);
        Assert.assertTrue(BeansLinker.isDynamicMethod(method));
        Class clz = null;
        try {
            clz = (Class) cs2.getTarget().invoke(method, new Date());
        } catch (final Throwable th) {
            throw new RuntimeException(th);
        }

        Assert.assertEquals(clz, Date.class);
    }

    @Test(dataProvider = "flags")
    public void staticMethodCallTest(final boolean publicLookup) {
        final CallSite cs = createGetMethodCallSite(publicLookup, "getProperty");
        final MethodType mt2 = MethodType.methodType(String.class, Object.class, Object.class, String.class);
        final CallSite cs2 = createCallSite(publicLookup, CALL, mt2);

        Object method = null;
        try {
            method = cs.getTarget().invoke(StaticClass.forClass(System.class));
        } catch (final Throwable th) {
            throw new RuntimeException(th);
        }

        Assert.assertNotNull(method);
        Assert.assertTrue(BeansLinker.isDynamicMethod(method));

        String str = null;
        try {
            str = (String) cs2.getTarget().invoke(method, null, "os.name");
        } catch (final Throwable th) {
            throw new RuntimeException(th);
        }
        Assert.assertEquals(str, System.getProperty("os.name"));
    }

    // try calling System.getenv and expect security exception
    @Test(dataProvider = "flags")
    public void systemGetenvTest(final boolean publicLookup) {
        final CallSite cs1 = createGetMethodCallSite(publicLookup, "getenv");
        final CallSite cs2 = createCallSite(publicLookup, CALL, MethodType.methodType(Object.class, Object.class, Object.class));

        try {
            final Object method = cs1.getTarget().invoke(StaticClass.forClass(System.class));
            cs2.getTarget().invoke(method, StaticClass.forClass(System.class));
            throw new RuntimeException("should not reach here in any case!");
        } catch (final Throwable th) {
            Assert.assertTrue(th instanceof SecurityException);
        }
    }

    // try getting a specific sensitive System property and expect security exception
    @Test(dataProvider = "flags")
    public void systemGetPropertyTest(final boolean publicLookup) {
        final CallSite cs1 = createGetMethodCallSite(publicLookup, "getProperty");
        final CallSite cs2 = createCallSite(publicLookup, CALL, MethodType.methodType(String.class, Object.class, Object.class, String.class));

        try {
            final Object method = cs1.getTarget().invoke(StaticClass.forClass(System.class));
            cs2.getTarget().invoke(method, StaticClass.forClass(System.class), "java.home");
            throw new RuntimeException("should not reach here in any case!");
        } catch (final Throwable th) {
            Assert.assertTrue(th instanceof SecurityException);
        }
    }

    // check a @CallerSensitive API and expect appropriate access check exception
    @Test(dataProvider = "flags")
    public void systemLoadLibraryTest(final boolean publicLookup) {
        final CallSite cs1 = createGetMethodCallSite(publicLookup, "loadLibrary");
        final CallSite cs2 = createCallSite(publicLookup, CALL, MethodType.methodType(void.class, Object.class, Object.class, String.class));

        try {
            final Object method = cs1.getTarget().invoke(StaticClass.forClass(System.class));
            cs2.getTarget().invoke(method, StaticClass.forClass(System.class), "foo");
            throw new RuntimeException("should not reach here in any case!");
        } catch (final Throwable th) {
            if (publicLookup) {
                Assert.assertTrue(th instanceof IllegalAccessError);
            } else {
                Assert.assertTrue(th instanceof AccessControlException, "Expected AccessControlException, got " + th.getClass().getName());
            }
        }
    }

    @Test(dataProvider = "flags")
    public void removeElementFromListTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(void.class, Object.class, int.class);
        final CallSite cs = createCallSite(publicLookup, REMOVE_ELEMENT, mt);

        final List<Integer> list = new ArrayList<>(List.of(23, 430, -4354));

        cs.getTarget().invoke(list, 1);
        Assert.assertEquals(list, List.of(23, -4354));
        cs.getTarget().invoke(list, 1);
        Assert.assertEquals(list, List.of(23));
        cs.getTarget().invoke(list, 0);
        Assert.assertEquals(list, List.of());
        try {
            cs.getTarget().invoke(list, -1);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }

        try {
            cs.getTarget().invoke(list, list.size());
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }
    }

    @Test(dataProvider = "flags")
    public void removeElementFromListWithFixedKeyTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(void.class, Object.class);

        final List<Integer> list = new ArrayList<>(List.of(23, 430, -4354));

        createCallSite(publicLookup, REMOVE_ELEMENT.named(1), mt).getTarget().invoke(list);
        Assert.assertEquals(list, List.of(23, -4354));
        createCallSite(publicLookup, REMOVE_ELEMENT.named(1), mt).getTarget().invoke(list);
        Assert.assertEquals(list, List.of(23));
        createCallSite(publicLookup, REMOVE_ELEMENT.named(0), mt).getTarget().invoke(list);
        Assert.assertEquals(list, List.of());
        try {
            createCallSite(publicLookup, REMOVE_ELEMENT.named(-1), mt).getTarget().invoke(list);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }

        try {
            createCallSite(publicLookup, REMOVE_ELEMENT.named(list.size()), mt).getTarget().invoke(list);
            throw new RuntimeException("expected IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException ex) {
        }
    }

    @Test(dataProvider = "flags")
    public void removeElementFromMapTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(void.class, Object.class, Object.class);
        final CallSite cs = createCallSite(publicLookup, REMOVE_ELEMENT, mt);

        final Map<String, String> map = new HashMap<>(Map.of("k1", "v1", "k2", "v2", "k3", "v3"));

        cs.getTarget().invoke(map, "k2");
        Assert.assertEquals(map, Map.of("k1", "v1", "k3", "v3"));
        cs.getTarget().invoke(map, "k4");
        Assert.assertEquals(map, Map.of("k1", "v1", "k3", "v3"));
        cs.getTarget().invoke(map, "k1");
        Assert.assertEquals(map, Map.of("k3", "v3"));
    }


    @Test(dataProvider = "flags")
    public void removeElementFromMapWithFixedKeyTest(final boolean publicLookup) throws Throwable {
        final MethodType mt = MethodType.methodType(void.class, Object.class);

        final Map<String, String> map = new HashMap<>(Map.of("k1", "v1", "k2", "v2", "k3", "v3"));

        createCallSite(publicLookup, REMOVE_ELEMENT.named("k2"), mt).getTarget().invoke(map);
        Assert.assertEquals(map, Map.of("k1", "v1", "k3", "v3"));
        createCallSite(publicLookup, REMOVE_ELEMENT.named("k4"), mt).getTarget().invoke(map);
        Assert.assertEquals(map, Map.of("k1", "v1", "k3", "v3"));
        createCallSite(publicLookup, REMOVE_ELEMENT.named("k1"), mt).getTarget().invoke(map);
        Assert.assertEquals(map, Map.of("k3", "v3"));
    }
}

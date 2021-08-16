/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.incubator.foreign
 *          java.base/jdk.internal.access.foreign
 *
 * @run testng/othervm -Xverify:all
 *   -Djdk.internal.foreign.SHOULD_ADAPT_HANDLES=false
 *   VarHandleTestExact
 * @run testng/othervm -Xverify:all
 *   -Djdk.internal.foreign.SHOULD_ADAPT_HANDLES=false
 *   -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true
 *   -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true
 *   VarHandleTestExact
 * @run testng/othervm -Xverify:all
 *   -Djdk.internal.foreign.SHOULD_ADAPT_HANDLES=false
 *   -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false
 *   -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false
 *   VarHandleTestExact
 * @run testng/othervm -Xverify:all
 *   -Djdk.internal.foreign.SHOULD_ADAPT_HANDLES=false
 *   -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false
 *   -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true
 *   VarHandleTestExact
 */

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.internal.access.foreign.MemorySegmentProxy;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.testng.Assert.*;

public class VarHandleTestExact {

    private static class Widget {
        static Object objectField_SRW;
        static long longField_SRW;
        static double doubleField_SRW;
        static Long aLongField_SRW;

        Object objectField_RW;
        long longField_RW;
        double doubleField_RW;
        Long aLongField_RW;

        final static Object objectField_SRO = new Object();
        final static long longField_SRO = 1234L;
        final static double doubleField_SRO = 1234D;
        final static Long aLongField_SRO = 1234L;

        final Object objectField_RO = new Object();
        final long longField_RO = 1234L;
        final double doubleField_RO = 1234D;
        final Long aLongField_RO = 1234L;
    }

    @Test(dataProvider = "dataObjectAccess")
    public void testExactSet(String fieldBaseName, Class<?> fieldType, boolean ro, Object testValue,
                             SetX setter, GetX getter,
                             SetStaticX staticSetter, GetStaticX staticGetter)
            throws NoSuchFieldException, IllegalAccessException {
        if (ro) throw new SkipException("Can not test setter with read only field");
        VarHandle vh = MethodHandles.lookup().findVarHandle(Widget.class, fieldBaseName + "_RW", fieldType);
        Widget w = new Widget();

        doTest(vh,
            tvh -> tvh.set(w, testValue),
            tvh -> setter.set(tvh, w, testValue),
            ".*\\Qexpected (Widget," + fieldType.getSimpleName() + ")void \\E.*");
    }

    @Test(dataProvider = "dataObjectAccess")
    public void testExactGet(String fieldBaseName, Class<?> fieldType, boolean ro, Object testValue,
                             SetX setter, GetX getter,
                             SetStaticX staticSetter, GetStaticX staticGetter)
            throws NoSuchFieldException, IllegalAccessException {
        VarHandle vh = MethodHandles.lookup().findVarHandle(Widget.class, fieldBaseName + (ro ? "_RO" : "_RW"), fieldType);
        Widget w = new Widget();

        doTest(vh,
            tvh -> tvh.get(w),
            tvh -> getter.get(tvh, w),
            ".*\\Qexpected (Widget)" + fieldType.getSimpleName() + " \\E.*");
    }

    @Test(dataProvider = "dataObjectAccess")
    public void testExactSetStatic(String fieldBaseName, Class<?> fieldType, boolean ro, Object testValue,
                                   SetX setter, GetX getter,
                                   SetStaticX staticSetter, GetStaticX staticGetter)
            throws NoSuchFieldException, IllegalAccessException {
        if (ro) throw new SkipException("Can not test setter with read only field");
        VarHandle vh = MethodHandles.lookup().findStaticVarHandle(Widget.class, fieldBaseName + "_SRW", fieldType);

        doTest(vh,
            tvh -> tvh.set(testValue),
            tvh -> staticSetter.set(tvh, testValue),
            ".*\\Qexpected (" + fieldType.getSimpleName() + ")void \\E.*");
    }

    @Test(dataProvider = "dataObjectAccess")
    public void testExactGetStatic(String fieldBaseName, Class<?> fieldType, boolean ro, Object testValue,
                                   SetX setter, GetX getter,
                                   SetStaticX staticSetter, GetStaticX staticGetter)
            throws NoSuchFieldException, IllegalAccessException {
        VarHandle vh = MethodHandles.lookup().findStaticVarHandle(Widget.class, fieldBaseName + (ro ? "_SRO" : "_SRW"), fieldType);

        doTest(vh,
            tvh -> tvh.get(),
            tvh -> staticGetter.get(tvh),
            ".*\\Qexpected ()" + fieldType.getSimpleName() + " \\E.*");
    }

    @Test(dataProvider = "dataSetArray")
    public void testExactArraySet(Class<?> arrayClass, Object testValue, SetArrayX setter) {
        VarHandle vh = MethodHandles.arrayElementVarHandle(arrayClass);
        Object arr = Array.newInstance(arrayClass.componentType(), 1);

        doTest(vh,
            tvh -> tvh.set(arr, 0, testValue),
            tvh -> setter.set(tvh, arr, testValue),
            ".*\\Qexpected (" + arrayClass.getSimpleName() + ",int," + arrayClass.componentType().getSimpleName() + ")void \\E.*");
    }

    @Test(dataProvider = "dataSetBuffer")
    public void testExactBufferSet(Class<?> arrayClass, Object testValue, SetBufferX setter) {
        VarHandle vh = MethodHandles.byteBufferViewVarHandle(arrayClass, ByteOrder.nativeOrder());
        ByteBuffer buff = ByteBuffer.allocateDirect(8);

        doTest(vh,
            tvh -> tvh.set(buff, 0, testValue),
            tvh -> setter.set(tvh, buff, testValue),
            ".*\\Qexpected (ByteBuffer,int," + arrayClass.componentType().getSimpleName() + ")void \\E.*");
    }

    @Test(dataProvider = "dataSetMemorySegment")
    public void testExactSegmentSet(Class<?> carrier, Object testValue, SetSegmentX setter) {
        VarHandle vh = MemoryHandles.varHandle(carrier, ByteOrder.nativeOrder());
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment seg = MemorySegment.allocateNative(8, scope);
            doTest(vh,
                tvh -> tvh.set(seg, 0L, testValue),
                tvh -> setter.set(tvh, seg, 0L, testValue),
                ".*\\Qexpected (MemorySegmentProxy,long," + carrier.getSimpleName() + ")void \\E.*");
        }
    }

    private static void doTest(VarHandle invokeHandle, Consumer<VarHandle> invokeTest,
                               Consumer<VarHandle> invokeExactTest, String expectedMessage) {
        assertFalse(invokeHandle.hasInvokeExactBehavior());
        assertSame(invokeHandle, invokeHandle.withInvokeBehavior());
        try {
            invokeTest.accept(invokeHandle);
        } catch (WrongMethodTypeException wmte) {
            fail("Unexpected exception", wmte);
        }

        VarHandle invokeExactHandle = invokeHandle.withInvokeExactBehavior();
        assertTrue(invokeExactHandle.hasInvokeExactBehavior());
        assertSame(invokeExactHandle, invokeExactHandle.withInvokeExactBehavior());
        try {
            invokeExactTest.accept(invokeExactHandle); // should throw
            fail("Exception expected");
        } catch (WrongMethodTypeException wmte) {
            assertMatches(wmte.getMessage(), expectedMessage);
        }

        // try going back
        VarHandle invokeHandle2 = invokeExactHandle.withInvokeBehavior();
        assertFalse(invokeHandle2.hasInvokeExactBehavior());
        try {
            invokeTest.accept(invokeHandle2);
        } catch (WrongMethodTypeException wmte) {
            fail("Unexpected exception", wmte);
        }
    }

    private static void assertMatches(String str, String pattern) {
        if (!str.matches(pattern)) {
            throw new AssertionError("'" + str + "' did not match the pattern '" + pattern + "'.");
        }
    }

    private interface SetX {
        void set(VarHandle vh, Widget w, Object testValue);
    }

    private interface SetStaticX {
        void set(VarHandle vh, Object testValue);
    }

    private interface GetX {
        void get(VarHandle vh, Widget w);
    }

    private interface GetStaticX {
        void get(VarHandle vh);
    }

    private interface SetArrayX {
        void set(VarHandle vh, Object array, Object testValue);
    }

    private interface SetBufferX {
        void set(VarHandle vh, ByteBuffer buff, Object testValue);
    }

    private interface SetSegmentX {
        void set(VarHandle vh, MemorySegment segment, long offset, Object testValue);
    }

    private static void consume(Object o) {}

    private static void testCaseObjectAccess(List<Object[]> cases, String fieldBaseName, Class<?> fieldType, Object testValue,
                                    SetX setter, GetX getter,
                                    SetStaticX staticSetter, GetStaticX staticGetter) {
        cases.add(new Object[] { fieldBaseName, fieldType, false, testValue, setter, getter, staticSetter, staticGetter });
        cases.add(new Object[] { fieldBaseName, fieldType, true, testValue, setter, getter, staticSetter, staticGetter });
    }

    private static void testCaseArraySet(List<Object[]> cases, Class<?> arrayType, Object testValue, SetArrayX setter) {
        cases.add(new Object[] { arrayType, testValue, setter });
    }

    private static void testCaseBufferSet(List<Object[]> cases, Class<?> arrayType, Object testValue, SetBufferX setter) {
        cases.add(new Object[] { arrayType, testValue, setter });
    }

    private static void testCaseSegmentSet(List<Object[]> cases, Class<?> carrier, Object testValue, SetSegmentX setter) {
        cases.add(new Object[] { carrier, testValue, setter });
    }

    @DataProvider
    public static Object[][] dataObjectAccess() {
        List<Object[]> cases = new ArrayList<>();

        // create a bunch of different sig-poly call sites
        testCaseObjectAccess(cases, "objectField", Object.class, "abcd",
                (vh, w, tv) -> vh.set(w, (String) tv),
                (vh, w) -> consume((String) vh.get(w)),
                (vh, tv) -> vh.set((String) tv),
                (vh) -> consume((String) vh.get()));
        testCaseObjectAccess(cases, "objectField", Object.class, Integer.valueOf(1234),
                (vh, w, tv) -> vh.set(w, (Integer) tv),
                (vh, w) -> consume((Integer) vh.get(w)),
                (vh, tv) -> vh.set((Integer) tv),
                (vh) -> consume((Integer) vh.get()));
        testCaseObjectAccess(cases, "longField", long.class, 1234,
                (vh, w, tv) -> vh.set(w, (int) tv),
                (vh, w) -> consume((int) vh.get(w)),
                (vh, tv) -> vh.set((int) tv),
                (vh) -> consume((int) vh.get()));
        testCaseObjectAccess(cases, "longField", long.class, (short) 1234,
                (vh, w, tv) -> vh.set(w, (short) tv),
                (vh, w) -> consume((short) vh.get(w)),
                (vh, tv) -> vh.set((short) tv),
                (vh) -> consume((short) vh.get()));
        testCaseObjectAccess(cases, "longField", long.class, (char) 1234,
                (vh, w, tv) -> vh.set(w, (char) tv),
                (vh, w) -> consume((char) vh.get(w)),
                (vh, tv) -> vh.set((char) tv),
                (vh) -> consume((char) vh.get()));
        testCaseObjectAccess(cases, "longField", long.class, (byte) 1234,
                (vh, w, tv) -> vh.set(w, (byte) tv),
                (vh, w) -> consume((byte) vh.get(w)),
                (vh, tv) -> vh.set((byte) tv),
                (vh) -> consume((byte) vh.get()));
        testCaseObjectAccess(cases, "longField", long.class, Long.valueOf(1234L),
                (vh, w, tv) -> vh.set(w, (Long) tv),
                (vh, w) -> consume((Long) vh.get(w)),
                (vh, tv) -> vh.set((Long) tv),
                (vh) -> consume((Long) vh.get()));
        testCaseObjectAccess(cases, "doubleField", double.class, 1234F,
                (vh, w, tv) -> vh.set(w, (float) tv),
                (vh, w) -> consume((float) vh.get(w)),
                (vh, tv) -> vh.set((float) tv),
                (vh) -> consume((float) vh.get()));
        testCaseObjectAccess(cases, "doubleField", double.class, 1234,
                (vh, w, tv) -> vh.set(w, (int) tv),
                (vh, w) -> consume((int) vh.get(w)),
                (vh, tv) -> vh.set((int) tv),
                (vh) -> consume((int) vh.get()));
        testCaseObjectAccess(cases, "doubleField", double.class, 1234L,
                (vh, w, tv) -> vh.set(w, (long) tv),
                (vh, w) -> consume((long) vh.get(w)),
                (vh, tv) -> vh.set((long) tv),
                (vh) -> consume((long) vh.get()));
        testCaseObjectAccess(cases, "doubleField", double.class, Double.valueOf(1234D),
                (vh, w, tv) -> vh.set(w, (Double) tv),
                (vh, w) -> consume((Double) vh.get(w)),
                (vh, tv) -> vh.set((Double) tv),
                (vh) -> consume((Double) vh.get()));
        testCaseObjectAccess(cases, "aLongField", Long.class, 1234L,
                (vh, w, tv) -> vh.set(w, (long) tv),
                (vh, w) -> consume((long) vh.get(w)),
                (vh, tv) -> vh.set((long) tv),
                (vh) -> consume((long) vh.get()));

        return cases.toArray(Object[][]::new);
    }

    @DataProvider
    public static Object[][] dataSetArray() {
        List<Object[]> cases = new ArrayList<>();

        // create a bunch of different sig-poly call sites
        testCaseArraySet(cases, Object[].class, "abcd",                (vh, arr, tv) -> vh.set((Object[]) arr, 0, (String) tv));
        testCaseArraySet(cases, Object[].class, Integer.valueOf(1234), (vh, arr, tv) -> vh.set((Object[]) arr, (Integer) tv));
        testCaseArraySet(cases, long[].class, 1234,                    (vh, arr, tv) -> vh.set((long[]) arr, 0, (int) tv));
        testCaseArraySet(cases, long[].class, (short) 1234,            (vh, arr, tv) -> vh.set((long[]) arr, 0, (short) tv));
        testCaseArraySet(cases, long[].class, (char)  1234,            (vh, arr, tv) -> vh.set((long[]) arr, 0, (char) tv));
        testCaseArraySet(cases, long[].class, (byte)  1234,            (vh, arr, tv) -> vh.set((long[]) arr, 0, (byte) tv));
        testCaseArraySet(cases, long[].class, Long.valueOf(1234L),     (vh, arr, tv) -> vh.set((long[]) arr, 0, (Long) tv));
        testCaseArraySet(cases, double[].class, 1234F,                 (vh, arr, tv) -> vh.set((double[]) arr, 0, (float) tv));
        testCaseArraySet(cases, double[].class, 1234,                  (vh, arr, tv) -> vh.set((double[]) arr, 0, (int) tv));
        testCaseArraySet(cases, double[].class, 1234L,                 (vh, arr, tv) -> vh.set((double[]) arr, 0, (long) tv));
        testCaseArraySet(cases, double[].class, Double.valueOf(1234D), (vh, arr, tv) -> vh.set((double[]) arr, 0, (Double) tv));
        testCaseArraySet(cases, Long[].class, 1234L,                   (vh, arr, tv) -> vh.set((Long[]) arr, 0, (long) tv));

        return cases.toArray(Object[][]::new);
    }

    @DataProvider
    public static Object[][] dataSetBuffer() {
        List<Object[]> cases = new ArrayList<>();

        // create a bunch of different sig-poly call sites
        testCaseBufferSet(cases, long[].class, 1234,                    (vh, buff, tv) -> vh.set(buff, 0, (int) tv));
        testCaseBufferSet(cases, long[].class, (short) 1234,            (vh, buff, tv) -> vh.set(buff, 0, (short) tv));
        testCaseBufferSet(cases, long[].class, (char)  1234,            (vh, buff, tv) -> vh.set(buff, 0, (char) tv));
        testCaseBufferSet(cases, long[].class, (byte)  1234,            (vh, buff, tv) -> vh.set(buff, 0, (byte) tv));
        testCaseBufferSet(cases, long[].class, Long.valueOf(1234L),     (vh, buff, tv) -> vh.set(buff, 0, (Long) tv));
        testCaseBufferSet(cases, double[].class, 1234F,                 (vh, buff, tv) -> vh.set(buff, 0, (float) tv));
        testCaseBufferSet(cases, double[].class, 1234,                  (vh, buff, tv) -> vh.set(buff, 0, (int) tv));
        testCaseBufferSet(cases, double[].class, 1234L,                 (vh, buff, tv) -> vh.set(buff, 0, (long) tv));
        testCaseBufferSet(cases, double[].class, Double.valueOf(1234D), (vh, buff, tv) -> vh.set(buff, 0, (Double) tv));

        return cases.toArray(Object[][]::new);
    }

    @DataProvider
    public static Object[][] dataSetMemorySegment() {
        List<Object[]> cases = new ArrayList<>();

        // create a bunch of different sig-poly call sites
        testCaseSegmentSet(cases, long.class, 1234,         (vh, seg, off, tv) -> vh.set((MemorySegmentProxy) seg, off, (int) tv));
        testCaseSegmentSet(cases, long.class, (char) 1234,  (vh, seg, off, tv) -> vh.set((MemorySegmentProxy) seg, off, (char) tv));
        testCaseSegmentSet(cases, long.class, (short) 1234, (vh, seg, off, tv) -> vh.set((MemorySegmentProxy) seg, off, (short) tv));
        testCaseSegmentSet(cases, long.class, (byte) 1234,  (vh, seg, off, tv) -> vh.set((MemorySegmentProxy) seg, off, (byte) tv));
        testCaseSegmentSet(cases, double.class, 1234F,      (vh, seg, off, tv) -> vh.set((MemorySegmentProxy) seg, off, (float) tv));

        return cases.toArray(Object[][]::new);
    }

}

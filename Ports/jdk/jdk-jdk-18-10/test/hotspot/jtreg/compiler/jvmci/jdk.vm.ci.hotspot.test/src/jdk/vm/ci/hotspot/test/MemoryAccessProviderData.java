/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.vm.ci.hotspot.test;

import java.lang.reflect.Field;

import org.testng.annotations.DataProvider;

import sun.hotspot.WhiteBox;
import jdk.internal.misc.Unsafe;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.hotspot.HotSpotVMConfigAccess;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.runtime.JVMCI;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class MemoryAccessProviderData {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final HotSpotConstantReflectionProvider CONSTANT_REFLECTION = (HotSpotConstantReflectionProvider) JVMCI.getRuntime().getHostJVMCIBackend().getConstantReflection();
    private static final TestClass TEST_OBJECT = new TestClass();
    private static final JavaConstant TEST_CONSTANT = CONSTANT_REFLECTION.forObject(TEST_OBJECT);
    private static final JavaConstant TEST_CLASS_CONSTANT = CONSTANT_REFLECTION.forObject(TestClass.class);

    private static final KindData[] PRIMITIVE_KIND_DATA = {
        new KindData(JavaKind.Boolean, TEST_OBJECT),
        new KindData(JavaKind.Byte, TEST_OBJECT),
        new KindData(JavaKind.Char, TEST_OBJECT),
        new KindData(JavaKind.Short, TEST_OBJECT),
        new KindData(JavaKind.Int, TEST_OBJECT),
        new KindData(JavaKind.Float, TEST_OBJECT),
        new KindData(JavaKind.Long, TEST_OBJECT),
        new KindData(JavaKind.Double, TEST_OBJECT)
    };
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();


    @DataProvider(name = "positiveObject")
    public static Object[][] getPositiveObjectJavaKind() {
        HotSpotJVMCIRuntime runtime = (HotSpotJVMCIRuntime) JVMCI.getRuntime();
        int offset = new HotSpotVMConfigAccess(runtime.getConfigStore()).getFieldOffset("Klass::_java_mirror", Integer.class, "OopHandle");
        Constant wrappedKlassPointer = CompilerToVMHelper.fromObjectClass(TestClass.class).klass();
        return new Object[][]{new Object[]{JavaKind.Object, wrappedKlassPointer, (long) offset, TEST_CLASS_CONSTANT, 0}};
    }

    @DataProvider(name = "positivePrimitive")
    public static Object[][] getPositivePrimitiveJavaKinds() {
        List<Object[]> result = new ArrayList<>();
        for (KindData k : PRIMITIVE_KIND_DATA) {
            result.add(new Object[] {k.kind, TEST_CONSTANT, k.instanceFieldOffset, k.instanceFieldValue, Math.max(8, k.kind.getBitCount())});
            result.add(new Object[] {k.kind, TEST_CLASS_CONSTANT, k.staticFieldOffset, k.staticFieldValue, Math.max(8, k.kind.getBitCount())});
        }
        return result.toArray(new Object[result.size()][]);
    }

    @DataProvider(name = "outOfBoundsInstanceFields")
    public static Object[][] getOutOfBoundsStaticFieldReads() {
        long instanceSize = WHITE_BOX.getObjectSize(TEST_OBJECT);
        List<Object[]> result = new ArrayList<>();
        for (KindData k : PRIMITIVE_KIND_DATA) {
            long lastValidOffset = instanceSize - (k.kind.getByteCount());
            result.add(new Object[] {k.kind, TEST_CONSTANT, lastValidOffset, false});
            result.add(new Object[] {k.kind, TEST_CONSTANT, (long) -1, true});
            result.add(new Object[] {k.kind, TEST_CONSTANT, lastValidOffset + 1, true});
            result.add(new Object[] {k.kind, TEST_CONSTANT, lastValidOffset + 100, true});
        }
        return result.toArray(new Object[result.size()][]);
    }

    @DataProvider(name = "outOfBoundsStaticFields")
    public static Object[][] getOutOfBoundsInstanceFieldReads() {
        long staticsSize = WHITE_BOX.getObjectSize(TEST_OBJECT.getClass());
        List<Object[]> result = new ArrayList<>();
        for (KindData k : PRIMITIVE_KIND_DATA) {
            long lastValidOffset = staticsSize - (k.kind.getByteCount());
            result.add(new Object[] {k.kind, TEST_CLASS_CONSTANT, lastValidOffset, false});
            result.add(new Object[] {k.kind, TEST_CLASS_CONSTANT, (long) -1, true});
            result.add(new Object[] {k.kind, TEST_CLASS_CONSTANT, lastValidOffset + 1, true});
            result.add(new Object[] {k.kind, TEST_CLASS_CONSTANT, lastValidOffset + 100, true});
        }
        return result.toArray(new Object[result.size()][]);
    }

    @DataProvider(name = "outOfBoundsObjectArray")
    public static Object[][] getOutOfBoundsObjectArrayReads() {
        List<Object[]> result = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            Object[] objects = new Object[i];
            for (int e = 0; e < i; e++) {
                objects[e] = e;
            }
            long firstValidOffset = UNSAFE.ARRAY_OBJECT_BASE_OFFSET;
            long endOfObjectOffset = UNSAFE.ARRAY_OBJECT_BASE_OFFSET + i * UNSAFE.ARRAY_OBJECT_INDEX_SCALE;
            JavaConstant constant = CONSTANT_REFLECTION.forObject(objects);
            result.add(new Object[] {JavaKind.Object, constant, firstValidOffset, i == 0});
            result.add(new Object[] {JavaKind.Object, constant, (long) 0, true});
            result.add(new Object[] {JavaKind.Object, constant, (long) -1, true});
            result.add(new Object[] {JavaKind.Object, constant, endOfObjectOffset - UNSAFE.ARRAY_OBJECT_INDEX_SCALE, i == 0});
            result.add(new Object[] {JavaKind.Object, constant, endOfObjectOffset, true});
            result.add(new Object[] {JavaKind.Object, constant, endOfObjectOffset + 100, true});
        }
        return result.toArray(new Object[result.size()][]);
    }

    @DataProvider(name = "negative")
    public static Object[][] getNegativeJavaKinds() {
        return new Object[][]{
                        new Object[]{JavaKind.Void, JavaConstant.NULL_POINTER},
                        new Object[]{JavaKind.Illegal, JavaConstant.INT_1}};
    }


    private static class TestClass {
        public final boolean booleanField = true;
        public final byte byteField = 2;
        public final short shortField = 3;
        public final int intField = 4;
        public final long longField = 5L;
        public final double doubleField = 6.0d;
        public final float floatField = 7.0f;
        public final char charField = 'a';
        public final String objectField = "abc";

        public static final boolean booleanStaticField = true;
        public static final byte byteStaticField = 2;
        public static final short shortStaticField = 3;
        public static final int intStaticField = 4;
        public static final long longStaticField = 5L;
        public static final double doubleStaticField = 6.0d;
        public static final float floatStaticField = 7.0f;
        public static final char charStaticField = 'a';
        public static final String objectStaticField = "abc";
    }


    static class KindData {
        final JavaKind kind;
        final Field instanceField;
        final Field staticField;
        final long instanceFieldOffset;
        final long staticFieldOffset;
        final JavaConstant instanceFieldValue;
        final JavaConstant staticFieldValue;
        KindData(JavaKind kind, Object testObject) {
            this.kind = kind;
            try {
                Class<?> c = testObject.getClass();
                instanceField = c.getDeclaredField(kind.getJavaName() + "Field");
                staticField = c.getDeclaredField(kind.getJavaName() + "StaticField");
                instanceField.setAccessible(true);
                staticField.setAccessible(true);
                instanceFieldOffset = UNSAFE.objectFieldOffset(instanceField);
                staticFieldOffset = UNSAFE.staticFieldOffset(staticField);
                instanceFieldValue = JavaConstant.forBoxedPrimitive(instanceField.get(testObject));
                staticFieldValue = JavaConstant.forBoxedPrimitive(staticField.get(null));
            } catch (Exception e) {
                throw new Error("TESTBUG for kind " + kind, e);
            }
        }
    }
}

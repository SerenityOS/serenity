/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.runtime.JVMCI;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TestHelper {

    public static final DummyClass DUMMY_CLASS_INSTANCE = new DummyClass();
    public static final HotSpotConstantReflectionProvider CONSTANT_REFLECTION_PROVIDER = (HotSpotConstantReflectionProvider) JVMCI.getRuntime().getHostJVMCIBackend().getConstantReflection();
    public static final JavaConstant DUMMY_CLASS_CONSTANT = CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE);

    public static final Map<ResolvedJavaField, JavaConstant> INSTANCE_FIELDS_MAP = new HashMap<>();

    static {
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "booleanField"),
                        JavaConstant.forBoolean(DUMMY_CLASS_INSTANCE.booleanField));
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "byteField"),
                        JavaConstant.forByte(DUMMY_CLASS_INSTANCE.byteField));
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "shortField"),
                        JavaConstant.forShort(DUMMY_CLASS_INSTANCE.shortField));
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "charField"),
                        JavaConstant.forChar(DUMMY_CLASS_INSTANCE.charField));
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "intField"),
                        JavaConstant.forInt(DUMMY_CLASS_INSTANCE.intField));
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "longField"),
                        JavaConstant.forLong(DUMMY_CLASS_INSTANCE.longField));
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "floatField"),
                        JavaConstant.forFloat(DUMMY_CLASS_INSTANCE.floatField));
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "doubleField"),
                        JavaConstant.forDouble(DUMMY_CLASS_INSTANCE.doubleField));
        INSTANCE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "objectField"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.objectField));
    }

    public static final Map<ResolvedJavaField, JavaConstant> INSTANCE_FINAL_FIELDS_MAP = new HashMap<>();

    static {
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalBooleanField"),
                        JavaConstant.forBoolean(
                                        DUMMY_CLASS_INSTANCE.finalBooleanField));
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalByteField"),
                        JavaConstant.forByte(DUMMY_CLASS_INSTANCE.finalByteField));
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalShortField"),
                        JavaConstant.forShort(DUMMY_CLASS_INSTANCE.finalShortField));
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalCharField"),
                        JavaConstant.forChar(DUMMY_CLASS_INSTANCE.finalCharField));
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalIntField"),
                        JavaConstant.forInt(DUMMY_CLASS_INSTANCE.finalIntField));
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalLongField"),
                        JavaConstant.forLong(DUMMY_CLASS_INSTANCE.finalLongField));
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalFloatField"),
                        JavaConstant.forFloat(DUMMY_CLASS_INSTANCE.finalFloatField));
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalDoubleField"),
                        JavaConstant.forDouble(
                                        DUMMY_CLASS_INSTANCE.finalDoubleField));
        INSTANCE_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "finalObjectField"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.finalObjectField));
    }

    public static final Map<ResolvedJavaField, JavaConstant> INSTANCE_FINAL_DEFAULT_FIELDS_MAP = new HashMap<>();

    static {
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultBooleanField"),
                        JavaConstant.forBoolean(
                                        DUMMY_CLASS_INSTANCE.finalDefaultBooleanField));
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultByteField"),
                        JavaConstant.forByte(
                                        DUMMY_CLASS_INSTANCE.finalDefaultByteField));
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultShortField"),
                        JavaConstant.forShort(
                                        DUMMY_CLASS_INSTANCE.finalDefaultShortField));
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultCharField"),
                        JavaConstant.forChar(
                                        DUMMY_CLASS_INSTANCE.finalDefaultCharField));
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultIntField"),
                        JavaConstant.forInt(
                                        DUMMY_CLASS_INSTANCE.finalDefaultIntField));
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultLongField"),
                        JavaConstant.forLong(
                                        DUMMY_CLASS_INSTANCE.finalDefaultLongField));
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultFloatField"),
                        JavaConstant.forFloat(
                                        DUMMY_CLASS_INSTANCE.finalDefaultFloatField));
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultDoubleField"),
                        JavaConstant.forDouble(
                                        DUMMY_CLASS_INSTANCE.finalDefaultDoubleField));
        INSTANCE_FINAL_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "finalDefaultObjectField"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.finalDefaultObjectField));
    }

    public static final Map<ResolvedJavaField, JavaConstant> INSTANCE_STABLE_FIELDS_MAP = new HashMap<>();

    static {
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableBooleanField"),
                        JavaConstant.forBoolean(
                                        DUMMY_CLASS_INSTANCE.stableBooleanField));
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableByteField"),
                        JavaConstant.forByte(DUMMY_CLASS_INSTANCE.stableByteField));
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableShortField"),
                        JavaConstant.forShort(
                                        DUMMY_CLASS_INSTANCE.stableShortField));
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableCharField"),
                        JavaConstant.forChar(DUMMY_CLASS_INSTANCE.stableCharField));
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableIntField"),
                        JavaConstant.forInt(DUMMY_CLASS_INSTANCE.stableIntField));
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableLongField"),
                        JavaConstant.forLong(DUMMY_CLASS_INSTANCE.stableLongField));
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableFloatField"),
                        JavaConstant.forFloat(
                                        DUMMY_CLASS_INSTANCE.stableFloatField));
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableDoubleField"),
                        JavaConstant.forDouble(
                                        DUMMY_CLASS_INSTANCE.stableDoubleField));
        INSTANCE_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "stableObjectField"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableObjectField));
    }

    public static final Map<ResolvedJavaField, JavaConstant> STATIC_FIELDS_MAP = new HashMap<>();

    static {
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticBooleanField"),
                        JavaConstant.forBoolean(DummyClass.staticBooleanField));
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticByteField"),
                        JavaConstant.forByte(DummyClass.staticByteField));
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticShortField"),
                        JavaConstant.forShort(DummyClass.staticShortField));
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticCharField"),
                        JavaConstant.forChar(DummyClass.staticCharField));
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticIntField"),
                        JavaConstant.forInt(DummyClass.staticIntField));
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticLongField"),
                        JavaConstant.forLong(DummyClass.staticLongField));
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticFloatField"),
                        JavaConstant.forFloat(DummyClass.staticFloatField));
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticDoubleField"),
                        JavaConstant.forDouble(DummyClass.staticDoubleField));
        STATIC_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticObjectField"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(DummyClass.staticObjectField));
    }

    public static final Map<ResolvedJavaField, JavaConstant> STATIC_FINAL_FIELDS_MAP = new HashMap<>();

    static {
        STATIC_FINAL_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticFinalBooleanField"),
                        JavaConstant.forBoolean(DummyClass.staticFinalBooleanField));
        STATIC_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticFinalByteField"),
                        JavaConstant.forByte(DummyClass.staticFinalByteField));
        STATIC_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticFinalShortField"),
                        JavaConstant.forShort(DummyClass.staticFinalShortField));
        STATIC_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticFinalCharField"),
                        JavaConstant.forChar(DummyClass.staticFinalCharField));
        STATIC_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticFinalIntField"),
                        JavaConstant.forInt(DummyClass.staticFinalIntField));
        STATIC_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticFinalLongField"),
                        JavaConstant.forLong(DummyClass.staticFinalLongField));
        STATIC_FINAL_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticFinalFloatField"),
                        JavaConstant.forFloat(DummyClass.staticFinalFloatField));
        STATIC_FINAL_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticFinalDoubleField"),
                        JavaConstant.forDouble(DummyClass.staticFinalDoubleField));
        STATIC_FINAL_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticFinalObjectField"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(DummyClass.staticFinalObjectField));
    }

    public static final Map<ResolvedJavaField, JavaConstant> STATIC_STABLE_FIELDS_MAP = new HashMap<>();

    static {
        STATIC_STABLE_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticStableBooleanField"),
                        JavaConstant.forBoolean(DummyClass.staticStableBooleanField));
        STATIC_STABLE_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticStableByteField"),
                        JavaConstant.forByte(DummyClass.staticStableByteField));
        STATIC_STABLE_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticStableShortField"),
                        JavaConstant.forShort(DummyClass.staticStableShortField));
        STATIC_STABLE_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticStableCharField"),
                        JavaConstant.forChar(DummyClass.staticStableCharField));
        STATIC_STABLE_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class, "staticStableIntField"),
                        JavaConstant.forInt(DummyClass.staticStableIntField));
        STATIC_STABLE_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticStableLongField"),
                        JavaConstant.forLong(DummyClass.staticStableLongField));
        STATIC_STABLE_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticStableFloatField"),
                        JavaConstant.forFloat(DummyClass.staticStableFloatField));
        STATIC_STABLE_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticStableDoubleField"),
                        JavaConstant.forDouble(DummyClass.staticStableDoubleField));
        STATIC_STABLE_FIELDS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "staticStableObjectField"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(DummyClass.staticStableObjectField));
    }

    public static final Map<ResolvedJavaField, JavaConstant> STATIC_STABLE_DEFAULT_FIELDS_MAP = new HashMap<>();

    static {
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultBooleanField"),
                        JavaConstant.forBoolean(
                                        DummyClass.staticStableDefaultBooleanField));
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultByteField"),
                        JavaConstant.forByte(
                                        DummyClass.staticStableDefaultByteField));
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultShortField"),
                        JavaConstant.forShort(
                                        DummyClass.staticStableDefaultShortField));
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultCharField"),
                        JavaConstant.forChar(
                                        DummyClass.staticStableDefaultCharField));
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultIntField"),
                        JavaConstant.forInt(
                                        DummyClass.staticStableDefaultIntField));
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultLongField"),
                        JavaConstant.forLong(
                                        DummyClass.staticStableDefaultLongField));
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultFloatField"),
                        JavaConstant.forFloat(
                                        DummyClass.staticStableDefaultFloatField));
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultDoubleField"),
                        JavaConstant.forDouble(
                                        DummyClass.staticStableDefaultDoubleField));
        STATIC_STABLE_DEFAULT_FIELDS_MAP.put(getResolvedJavaField(DummyClass.class,
                        "staticStableDefaultObjectField"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DummyClass.staticStableDefaultObjectField));
    }

    public static final int ARRAY_DIMENSION = 1;
    public static final int ARRAY_OF_ARRAYS_DIMENSION = 2;

    public static final Map<ResolvedJavaField, JavaConstant> ARRAYS_MAP = new HashMap<>();

    static {
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "booleanArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.booleanArrayWithValues));
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "byteArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.byteArrayWithValues));
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "shortArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.shortArrayWithValues));
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "charArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.charArrayWithValues));
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "intArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.intArrayWithValues));
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "longArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.longArrayWithValues));
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "floatArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.floatArrayWithValues));
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "doubleArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.doubleArrayWithValues));
        ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "objectArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.objectArrayWithValues));
    }

    public static final Map<ResolvedJavaField, JavaConstant> STABLE_ARRAYS_MAP = new HashMap<>();

    static {
        STABLE_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableBooleanArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableBooleanArrayWithValues));
        STABLE_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "stableByteArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableByteArrayWithValues));
        STABLE_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "stableShortArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableShortArrayWithValues));
        STABLE_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "stableCharArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableCharArrayWithValues));
        STABLE_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "stableIntArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableIntArrayWithValues));
        STABLE_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "stableLongArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableLongArrayWithValues));
        STABLE_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "stableFloatArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableFloatArrayWithValues));
        STABLE_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "stableDoubleArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableDoubleArrayWithValues));
        STABLE_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "stableObjectArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableObjectArrayWithValues));
    }

    public static final Map<ResolvedJavaField, JavaConstant> ARRAY_ARRAYS_MAP = new HashMap<>();

    static {
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "booleanArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.booleanArrayArrayWithValues));
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "byteArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.byteArrayArrayWithValues));
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "shortArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.shortArrayArrayWithValues));
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "charArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.charArrayArrayWithValues));
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "intArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.intArrayArrayWithValues));
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "longArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.longArrayArrayWithValues));
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "floatArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.floatArrayArrayWithValues));
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "doubleArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.doubleArrayArrayWithValues));
        ARRAY_ARRAYS_MAP.put(getResolvedJavaField(DummyClass.class, "objectArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.objectArrayArrayWithValues));
    }

    public static final Map<ResolvedJavaField, JavaConstant> STABLE_ARRAY_ARRAYS_MAP = new HashMap<>();

    static {
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableBooleanArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableBooleanArrayArrayWithValues));
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableByteArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableByteArrayArrayWithValues));
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableShortArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableShortArrayArrayWithValues));
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableCharArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableCharArrayArrayWithValues));
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableIntArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableIntArrayArrayWithValues));
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableLongArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableLongArrayArrayWithValues));
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableFloatArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableFloatArrayArrayWithValues));
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableDoubleArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableDoubleArrayArrayWithValues));
        STABLE_ARRAY_ARRAYS_MAP.put(
                        getResolvedJavaField(DummyClass.class, "stableObjectArrayArrayWithValues"),
                        CONSTANT_REFLECTION_PROVIDER.forObject(
                                        DUMMY_CLASS_INSTANCE.stableObjectArrayArrayWithValues));
    }

    public static ResolvedJavaField getResolvedJavaField(Class<?> clazz, String fieldName) {
        Field reflectionField = null;
        try {
            reflectionField = clazz.getDeclaredField(fieldName);
            reflectionField.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new Error("Test bug: Invalid field name: " + ex, ex);
        } catch (SecurityException ex) {
            throw new Error("Unexpected error: " + ex, ex);
        }
        MetaAccessProvider metaAccess = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
        return metaAccess.lookupJavaField(reflectionField);
    }
}

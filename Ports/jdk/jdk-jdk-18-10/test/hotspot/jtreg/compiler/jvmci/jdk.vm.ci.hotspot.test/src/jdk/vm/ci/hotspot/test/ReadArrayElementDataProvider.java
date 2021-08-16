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

import jdk.vm.ci.meta.JavaConstant;
import org.testng.annotations.DataProvider;

import java.util.LinkedList;
import java.util.stream.Stream;

import static jdk.vm.ci.hotspot.test.TestHelper.ARRAYS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.ARRAY_ARRAYS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.CONSTANT_REFLECTION_PROVIDER;
import static jdk.vm.ci.hotspot.test.TestHelper.DUMMY_CLASS_INSTANCE;
import static jdk.vm.ci.hotspot.test.TestHelper.INSTANCE_FIELDS_MAP;

public class ReadArrayElementDataProvider {

    @DataProvider(name = "readArrayElementDataProvider")
    public static Object[][] readArrayElementDataProvider() {
        LinkedList<Object[]> cfgSet = new LinkedList<>();
        for (int i : new int[]{0, 1}) {
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.booleanArrayWithValues),
                            i, JavaConstant.forBoolean(DUMMY_CLASS_INSTANCE.booleanArrayWithValues[i])});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.byteArrayWithValues),
                            i, JavaConstant.forByte(DUMMY_CLASS_INSTANCE.byteArrayWithValues[i])});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.shortArrayWithValues),
                            i, JavaConstant.forShort(DUMMY_CLASS_INSTANCE.shortArrayWithValues[i])});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.charArrayWithValues),
                            i, JavaConstant.forChar(DUMMY_CLASS_INSTANCE.charArrayWithValues[i])});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.intArrayWithValues),
                            i, JavaConstant.forInt(DUMMY_CLASS_INSTANCE.intArrayWithValues[i])});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.longArrayWithValues),
                            i, JavaConstant.forLong(DUMMY_CLASS_INSTANCE.longArrayWithValues[i])});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.floatArrayWithValues),
                            i, JavaConstant.forFloat(DUMMY_CLASS_INSTANCE.floatArrayWithValues[i])});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.doubleArrayWithValues),
                            i, JavaConstant.forDouble(DUMMY_CLASS_INSTANCE.doubleArrayWithValues[i])});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.objectArrayWithValues),
                            i, CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.objectArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.booleanArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.booleanArrayArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.byteArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.byteArrayArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.shortArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.shortArrayArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.charArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.charArrayArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.intArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.intArrayArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.longArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.longArrayArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.floatArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.floatArrayArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.doubleArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.doubleArrayArrayWithValues[i])});
            cfgSet.add(new Object[]{
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.objectArrayArrayWithValues), i,
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.objectArrayArrayWithValues[i])});
        }
        Stream.concat(ARRAYS_MAP.values().stream(), ARRAY_ARRAYS_MAP.values().stream()).forEach((array) -> {
            for (int i : new int[]{-1, 2}) {
                cfgSet.add(new Object[]{array, i, null});
            }
        });
        cfgSet.add(new Object[]{null, 0, null});
        cfgSet.add(new Object[]{JavaConstant.NULL_POINTER, 0, null});
        INSTANCE_FIELDS_MAP.values().forEach((constant) -> {
            cfgSet.add(new Object[]{constant, 0, null});
        });
        return cfgSet.toArray(new Object[0][0]);
    }
}

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

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import org.testng.annotations.DataProvider;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

import static jdk.vm.ci.hotspot.test.TestHelper.CONSTANT_REFLECTION_PROVIDER;
import static jdk.vm.ci.hotspot.test.TestHelper.DUMMY_CLASS_INSTANCE;

public class ConstantEqualsDataProvider {
    @DataProvider(name = "constantEqualsDataProvider")
    public static Object[][] constantEqualsDataProvider() {
        HashMap<Object, Constant> constMap = new HashMap<>();
        constMap.put(DUMMY_CLASS_INSTANCE.booleanField, JavaConstant.forBoolean(DUMMY_CLASS_INSTANCE.booleanField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultBooleanField,
                        JavaConstant.forBoolean(DUMMY_CLASS_INSTANCE.stableDefaultBooleanField));
        constMap.put(DUMMY_CLASS_INSTANCE.byteField, JavaConstant.forByte(DUMMY_CLASS_INSTANCE.byteField));
        constMap.put(DUMMY_CLASS_INSTANCE.finalByteField, JavaConstant.forByte(DUMMY_CLASS_INSTANCE.finalByteField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultByteField,
                        JavaConstant.forByte(DUMMY_CLASS_INSTANCE.stableDefaultByteField));
        constMap.put(DUMMY_CLASS_INSTANCE.shortField, JavaConstant.forShort(DUMMY_CLASS_INSTANCE.shortField));
        constMap.put(DUMMY_CLASS_INSTANCE.finalShortField, JavaConstant.forShort(DUMMY_CLASS_INSTANCE.finalShortField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultShortField,
                        JavaConstant.forShort(DUMMY_CLASS_INSTANCE.stableDefaultShortField));
        constMap.put(DUMMY_CLASS_INSTANCE.intField, JavaConstant.forInt(DUMMY_CLASS_INSTANCE.intField));
        constMap.put(DUMMY_CLASS_INSTANCE.finalIntField, JavaConstant.forInt(DUMMY_CLASS_INSTANCE.finalIntField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultIntField,
                        JavaConstant.forInt(DUMMY_CLASS_INSTANCE.stableDefaultIntField));
        constMap.put(DUMMY_CLASS_INSTANCE.longField, JavaConstant.forLong(DUMMY_CLASS_INSTANCE.longField));
        constMap.put(DUMMY_CLASS_INSTANCE.finalLongField, JavaConstant.forLong(DUMMY_CLASS_INSTANCE.finalLongField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultLongField,
                        JavaConstant.forLong(DUMMY_CLASS_INSTANCE.stableDefaultLongField));
        constMap.put(DUMMY_CLASS_INSTANCE.doubleField, JavaConstant.forDouble(DUMMY_CLASS_INSTANCE.doubleField));
        constMap.put(DUMMY_CLASS_INSTANCE.finalDoubleField,
                        JavaConstant.forDouble(DUMMY_CLASS_INSTANCE.finalDoubleField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultDoubleField,
                        JavaConstant.forDouble(DUMMY_CLASS_INSTANCE.stableDefaultDoubleField));
        constMap.put(DUMMY_CLASS_INSTANCE.floatField, JavaConstant.forFloat(DUMMY_CLASS_INSTANCE.floatField));
        constMap.put(DUMMY_CLASS_INSTANCE.finalFloatField, JavaConstant.forFloat(DUMMY_CLASS_INSTANCE.finalFloatField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultFloatField,
                        JavaConstant.forFloat(DUMMY_CLASS_INSTANCE.stableDefaultFloatField));
        constMap.put(DUMMY_CLASS_INSTANCE.charField, JavaConstant.forChar(DUMMY_CLASS_INSTANCE.charField));
        constMap.put(DUMMY_CLASS_INSTANCE.finalCharField, JavaConstant.forChar(DUMMY_CLASS_INSTANCE.finalCharField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultCharField,
                        JavaConstant.forChar(DUMMY_CLASS_INSTANCE.stableDefaultCharField));
        constMap.put(DUMMY_CLASS_INSTANCE.stringField,
                        CONSTANT_REFLECTION_PROVIDER.forString(DUMMY_CLASS_INSTANCE.stringField));
        constMap.put(DUMMY_CLASS_INSTANCE.stringField2,
                        CONSTANT_REFLECTION_PROVIDER.forString(DUMMY_CLASS_INSTANCE.stringField2));
        constMap.put(DUMMY_CLASS_INSTANCE.objectField,
                        CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.objectField));
        constMap.put(DUMMY_CLASS_INSTANCE.finalObjectField,
                        CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.finalObjectField));
        constMap.put(DUMMY_CLASS_INSTANCE.stableDefaultObjectField,
                        CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.stableDefaultObjectField));
        constMap.put(null, null);
        constMap.put(JavaConstant.NULL_POINTER, JavaConstant.NULL_POINTER);
        LinkedList<Object[]> cfgSet = new LinkedList<>();
        constMap.entrySet().stream().forEach((obj1) -> {
            constMap.entrySet().stream().forEach((obj2) -> {
                cfgSet.add(new Object[]{obj1.getValue(), obj2.getValue(),
                                Objects.equals(obj1.getKey(), obj2.getKey())});
            });
        });
        return cfgSet.toArray(new Object[0][0]);
    }
}

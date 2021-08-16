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

import static jdk.vm.ci.hotspot.test.TestHelper.CONSTANT_REFLECTION_PROVIDER;
import static jdk.vm.ci.hotspot.test.TestHelper.DUMMY_CLASS_INSTANCE;

public class UnboxPrimitiveDataProvider {

    @DataProvider(name = "unboxPrimitiveDataProvider")
    public static Object[][] unboxPrimitiveDataProvider() {
        LinkedList<Object[]> cfgSet = new LinkedList<>();
        // Testing boolean
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(
                        true), JavaConstant.forBoolean(true)});
        cfgSet.add(new Object[]{JavaConstant.forBoolean(true), null});
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(false),
                        JavaConstant.forBoolean(false)});
        cfgSet.add(new Object[]{JavaConstant.forBoolean(false), null});
        for (byte number : new byte[]{-128, 0, 1, 127}) {
            // Testing boxed primitives
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Byte.valueOf(number)),
                            JavaConstant.forByte(number)});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Short.valueOf(number)),
                            JavaConstant.forShort(number)});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Integer.valueOf(number)),
                            JavaConstant.forInt(number)});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Long.valueOf(number)),
                            JavaConstant.forLong(number)});
            if (number >= 0) {
                cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(
                                Character.valueOf((char) number)),
                                JavaConstant.forChar((char) number)});
            }
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(
                            Float.valueOf(number * 1.1f)),
                            JavaConstant.forFloat(number * 1.1f)});
            cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(
                            Double.valueOf(number * 1.1)),
                            JavaConstant.forDouble(number * 1.1)});
            // Testing non-boxed primitives (should result in returning of "null")
            cfgSet.add(new Object[]{JavaConstant.forByte(number), null});
            cfgSet.add(new Object[]{JavaConstant.forShort(number), null});
            cfgSet.add(new Object[]{JavaConstant.forInt(number), null});
            cfgSet.add(new Object[]{JavaConstant.forLong(number), null});
            cfgSet.add(new Object[]{JavaConstant.forChar((char) number), null});
            cfgSet.add(new Object[]{JavaConstant.forFloat(number), null});
            cfgSet.add(new Object[]{JavaConstant.forDouble(number), null});
        }
        // Testing boxed primitives with max values
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Short.MAX_VALUE),
                        JavaConstant.forShort(Short.MAX_VALUE)});
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Integer.MAX_VALUE),
                        JavaConstant.forInt(Integer.MAX_VALUE)});
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Long.MAX_VALUE),
                        JavaConstant.forLong(Long.MAX_VALUE)});
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Character.MAX_VALUE),
                        JavaConstant.forChar(Character.MAX_VALUE)});
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Float.MAX_VALUE),
                        JavaConstant.forFloat(Float.MAX_VALUE)});
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(Double.MAX_VALUE),
                        JavaConstant.forDouble(Double.MAX_VALUE)});
        // Non-primitives testing
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(
                        DUMMY_CLASS_INSTANCE.objectField), null});
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(
                        DUMMY_CLASS_INSTANCE.booleanArrayWithValues),
                        null});
        // Null testing
        cfgSet.add(new Object[]{JavaConstant.NULL_POINTER, null});
        cfgSet.add(new Object[]{null, null});
        return cfgSet.toArray(new Object[0][0]);
    }
}

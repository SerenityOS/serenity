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

import static jdk.vm.ci.hotspot.test.TestHelper.CONSTANT_REFLECTION_PROVIDER;
import static jdk.vm.ci.hotspot.test.TestHelper.DUMMY_CLASS_INSTANCE;

public class AsJavaTypeDataProvider {

    @DataProvider(name = "asJavaTypeDataProvider")
    public static Object[][] asJavaTypeDataProvider() {
        return new Object[][]{
                        {CONSTANT_REFLECTION_PROVIDER.forObject(DummyClass.class),
                                        "jdk.vm.ci.hotspot.test.DummyClass"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(boolean.class), "boolean"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(byte.class), "byte"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(short.class), "short"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(char.class), "char"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(int.class), "int"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(long.class), "long"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(float.class), "float"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(double.class), "double"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(Object.class), "java.lang.Object"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(boolean[].class), "boolean[]"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(boolean[][].class), "boolean[][]"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(Object[].class), "java.lang.Object[]"},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(Object[][].class), "java.lang.Object[][]"},
                        {JavaConstant.forBoolean(DUMMY_CLASS_INSTANCE.booleanField), null},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.objectField), null},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE), null},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.booleanArrayWithValues), null},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.booleanArrayArrayWithValues), null},
                        {JavaConstant.NULL_POINTER, null}, {null, null}};
    }
}

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

public class IsEmbeddableDataProvider {
    @DataProvider(name = "isEmbeddableDataProvider")
    public static Object[][] isEmbeddableDataProvider() {
        return new Object[][]{{JavaConstant.forBoolean(DUMMY_CLASS_INSTANCE.booleanField), true},
                        {JavaConstant.forByte(DUMMY_CLASS_INSTANCE.byteField), true},
                        {JavaConstant.forShort(DUMMY_CLASS_INSTANCE.shortField), true},
                        {JavaConstant.forInt(DUMMY_CLASS_INSTANCE.intField), true},
                        {JavaConstant.forLong(DUMMY_CLASS_INSTANCE.longField), true},
                        {JavaConstant.forChar(DUMMY_CLASS_INSTANCE.charField), true},
                        {JavaConstant.forFloat(DUMMY_CLASS_INSTANCE.floatField), true},
                        {JavaConstant.forDouble(DUMMY_CLASS_INSTANCE.doubleField), true},
                        {CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.objectField), true},
                        {JavaConstant.NULL_POINTER, true}, {null, true}};
    }
}

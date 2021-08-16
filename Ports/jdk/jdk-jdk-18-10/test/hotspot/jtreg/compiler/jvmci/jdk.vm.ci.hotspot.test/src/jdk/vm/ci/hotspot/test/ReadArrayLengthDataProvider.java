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
import java.util.List;

import static jdk.vm.ci.hotspot.test.TestHelper.CONSTANT_REFLECTION_PROVIDER;
import static jdk.vm.ci.hotspot.test.TestHelper.DUMMY_CLASS_INSTANCE;

public class ReadArrayLengthDataProvider {

    public static List<Object> createListOfDummyArrays(int length) {
        List<Object> arrays = new LinkedList<>();
        arrays.add(new boolean[length]);
        arrays.add(new byte[length]);
        arrays.add(new short[length]);
        arrays.add(new char[length]);
        arrays.add(new int[length]);
        arrays.add(new long[length]);
        arrays.add(new float[length]);
        arrays.add(new double[length]);
        arrays.add(new Object[length]);
        arrays.add(new boolean[length][2]);
        arrays.add(new byte[length][2]);
        arrays.add(new short[length][2]);
        arrays.add(new char[length][2]);
        arrays.add(new int[length][2]);
        arrays.add(new long[length][2]);
        arrays.add(new float[length][2]);
        arrays.add(new double[length][2]);
        arrays.add(new Object[length][2]);
        return arrays;
    }

    @DataProvider(name = "readArrayLengthDataProvider")
    public static Object[][] readArrayLengthDataProvider() {
        LinkedList<Object[]> cfgSet = new LinkedList<>();
        for (int i : new int[]{0, 1, 42}) {
            createListOfDummyArrays(i).stream().forEach((array) -> {
                cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(array), i});
            });
        }
        cfgSet.add(new Object[]{null, null});
        cfgSet.add(new Object[]{JavaConstant.NULL_POINTER, null});
        cfgSet.add(new Object[]{CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.intField), null});
        cfgSet.add(new Object[]{JavaConstant.forInt(DUMMY_CLASS_INSTANCE.intField), null});
        return cfgSet.toArray(new Object[0][0]);
    }
}

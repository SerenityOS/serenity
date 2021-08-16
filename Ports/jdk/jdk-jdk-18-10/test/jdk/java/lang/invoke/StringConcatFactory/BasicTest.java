/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8187089
 * @run main BasicTest
 */

import java.lang.invoke.*;
import java.util.Arrays;

public class BasicTest {
    static final int MAX_PARAM_SLOTS = 200;
    static int exceedMaxParamSlots = 0;
    public static void main(String[] args) throws Throwable {
        int expectionTestCases = 0;

        Class<?>[] types = new Class<?>[200];
        Arrays.fill(types, int.class);
        test(MethodType.methodType(String.class, types));

        types = new Class<?>[100];
        Arrays.fill(types, long.class);
        test(MethodType.methodType(String.class, types));

        // test cases exceeding 200 parameter slots
        expectionTestCases++;
        types = new Class<?>[101];
        Arrays.fill(types, 0, 50, long.class);
        Arrays.fill(types, 50, 100, double.class);
        types[100] = int.class;
        test(MethodType.methodType(String.class, types));

        expectionTestCases++;
        types = new Class<?>[201];
        Arrays.fill(types, int.class);
        test(MethodType.methodType(String.class, types));

        if (exceedMaxParamSlots != expectionTestCases) {
            throw new RuntimeException("expected one test case exceeding 200 param slots");
        }
    }

    /**
     * Tests if StringConcatException is thrown if the given concatType
     * has more than 200 parameter slots
     */
    static void test(MethodType concatType) throws StringConcatException {
        String recipe = "";
        int slots = 0;
        for (Class<?> c : concatType.parameterList()) {
            recipe += "\1";
            slots++;
            if (c == double.class || c == long.class) {
                slots++;
            }
        }
        if (slots > MAX_PARAM_SLOTS) {
            exceedMaxParamSlots++;
        }
        System.out.format("Test %s parameter slots%n", slots);
        try {
            StringConcatFactory.makeConcat(MethodHandles.lookup(), "name", concatType);
            if (slots > MAX_PARAM_SLOTS) {
                throw new RuntimeException("StringConcatException not thrown");
            }
        } catch (StringConcatException e) {
            if (slots <= MAX_PARAM_SLOTS) throw e;
        }

        try {
            StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "name",
                                                        concatType, recipe);
            if (slots > MAX_PARAM_SLOTS) {
                throw new RuntimeException("StringConcatException not thrown");
            }
        } catch (StringConcatException e) {
            if (slots <= MAX_PARAM_SLOTS) throw e;
        }
    }
}
